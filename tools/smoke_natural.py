"""Observe timer-driven spawns from the packaged mod with a real AFK client.

Run smoke_bundled.py --prepare-only first. Only disposable production worlds
and the development client directory are used. No summon or spawn egg is used.
"""
import argparse
import json
from pathlib import Path
import queue
import re
import shutil
import socket
import subprocess
import threading
import time

parser = argparse.ArgumentParser()
parser.add_argument('loader', choices=('fabric', 'neoforge'))
parser.add_argument('--java', type=Path, help='Server Java executable; defaults to java on PATH')
parser.add_argument('--artifact', type=Path, help='Packaged mod JAR; defaults to this branch build output')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
props = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines() if '=' in line)
mc = props['minecraft_version']
java = args.java or shutil.which('java')
assert java, 'Provide --java or install Java on PATH'
run = root / '.cache/production' / mc / args.loader
assert run.exists(), 'Prepare the production loader first'
artifact = args.artifact or root / args.loader / 'build/libs' / f'cave_dweller-{args.loader}-{props["mod_version"]}+{mc}.jar'
shutil.copy2(artifact, run / 'mods/cave_dweller.jar')
external = run / 'mods/geckolib-standalone.jar'
if external.exists():
    external.unlink()
with socket.socket() as probe:
    probe.bind(('127.0.0.1', 0))
    port = probe.getsockname()[1]
(run / 'config/cave_dweller.json').write_text(json.dumps({
    'can_spawn_min': 5, 'can_spawn_max': 5, 'can_spawn_cooldown_chance': 0,
    'spawn_chance_per_tick': 1, 'reset_noise_min': 86400, 'reset_noise_max': 86400,
    'time_until_leave': 300,
}))
(run / 'server.properties').write_text(f'online-mode=false\nserver-ip=127.0.0.1\nserver-port={port}\nlevel-name=natural-smoke\n'
    'level-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":3}],"biome":"minecraft:plains"}\n'
    'gamemode=creative\nforce-gamemode=true\ndifficulty=normal\nview-distance=5\nsimulation-distance=5\nspawn-protection=0\npause-when-empty-seconds=0\n')
if args.loader == 'fabric':
    command = [str(java), '-Xmx1G', '-Dfabric.log.level=debug', '-jar', str(run / 'fabric-server-launch.jar'), 'nogui']
else:
    command = [str(java), '-Xmx1G', '@' + str(run / f'libraries/net/neoforged/neoforge/{props["neoforge_version"]}/win_args.txt'), 'nogui']

lines = queue.Queue()
server = subprocess.Popen(command, cwd=run, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace', creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
client = None
client_log = None
log_path = run / 'natural.log'
def pump():
    with log_path.open('w', encoding='utf-8') as log:
        for line in server.stdout:
            log.write(line)
            log.flush()
            lines.put(line)
worker = threading.Thread(target=pump, daemon=True)
worker.start()
def send(command):
    server.stdin.write(command + '\n')
    server.stdin.flush()
def expect(pattern, timeout=60):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            line = lines.get(timeout=1)
        except queue.Empty:
            assert server.poll() is None, f'Server exited; see {log_path}'
            assert client is None or client.poll() is None, 'Client exited before the spawn test finished'
            continue
        if re.search(pattern, line):
            print(line.strip(), flush=True)
            return line
        if re.search(r'Exception in server tick loop|Failed to start the minecraft server|Encountered an unexpected exception', line):
            raise AssertionError(line.strip())
    raise AssertionError(f'Timeout waiting for {pattern}; see {log_path}')
def assert_absent(label):
    send('execute if entity @e[type=cave_dweller:cave_dweller] run say UNEXPECTED_DWELLER')
    send('say ABSENCE_CHECK_DONE')
    line = expect(r'UNEXPECTED_DWELLER|ABSENCE_CHECK_DONE')
    assert 'UNEXPECTED_DWELLER' not in line, label
    print(f'PASS {mc} {args.loader}: {label}', flush=True)
try:
    expect(r'Done \([\d.,]+s\)', 600)
    send('forceload add -64 -64 64 64')
    expect(r'Marked|already marked|No chunks were marked')
    send('fill -64 -61 -64 64 -61 64 minecraft:stone')
    expect(r'Successfully filled|No blocks were filled')
    for y in (-54, -53, -52):
        send(f'fill -64 {y} -64 64 {y} 64 minecraft:stone')
        expect(r'Successfully filled|No blocks were filled')
    send('fill -8 59 -8 8 59 8 minecraft:stone')
    expect(r'Successfully filled|No blocks were filled')
    send('kill @e[type=cave_dweller:cave_dweller]')
    expect(r'Killed|No entity was found|No entities found')
    send('setworldspawn 0 -60 0')
    expect(r'Set the world spawn')
    client_run = root / args.loader / 'run/client-smoke'
    client_run.mkdir(parents=True, exist_ok=True)
    (client_run / 'options.txt').write_text('renderDistance:4\nsimulationDistance:5\nsoundCategory_master:0.0\npauseOnLostFocus:false\n')
    client_log = (run / 'natural-client.log').open('w', encoding='utf-8')
    client = subprocess.Popen([str(root / 'gradlew.bat'), f':{args.loader}:runSmokeClient',
        f'-PsmokeServerAddress=127.0.0.1:{port}', '--console=plain', '--no-daemon', '--no-watch-fs'],
        cwd=root, stdout=client_log, stderr=subprocess.STDOUT, creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    expect(r'joined the game', 600)
    send('tp @a 0 -60 0')
    expect(r'Teleported')
    send('gamemode creative @a')
    send('execute if entity @a[gamemode=creative] run say CREATIVE_READY')
    expect(r'CREATIVE_READY')
    time.sleep(8)
    assert_absent('Creative AFK player is excluded')
    send('gamemode spectator @a')
    expect(r'game mode')
    time.sleep(8)
    assert_absent('Spectator AFK player is excluded')
    send('tp @a 0 60 0')
    expect(r'Teleported')
    send('gamemode survival @a')
    expect(r'game mode')
    time.sleep(8)
    assert_absent('Survival AFK on the surface at Y=60 is excluded')
    send('effect give @a minecraft:resistance 300 4 true')
    expect(r'Applied effect')
    # Both the cover and path checks retain shipped defaults. The player stays still.
    send('tp @a 0 -60 0')
    expect(r'Teleported')
    start = time.monotonic()
    spawned = False
    while time.monotonic() - start < 30:
        send('execute if entity @e[type=cave_dweller:cave_dweller] run say NATURAL_SPAWN_PASS')
        send('say POLL_DONE')
        line = expect(r'NATURAL_SPAWN_PASS|POLL_DONE', 10)
        if 'NATURAL_SPAWN_PASS' in line:
            spawned = True
            break
        time.sleep(0.5)
    assert spawned, 'No natural spawn in the covered cave with an eligible Survival AFK player'
    elapsed = time.monotonic() - start
    assert elapsed >= 3, f'Timer advanced while the player was ineligible: spawned after {elapsed:.2f}s'
    send('data get entity @e[type=cave_dweller:cave_dweller,limit=1] Health')
    expect(r'60\.0f')
    print(f'PASS {mc} {args.loader}: timer-driven natural spawn after {elapsed:.1f}s, AFK Survival Y=-60, covered cave, default16-block distance and path check, packaged mod with bundled GeckoLib', flush=True)
    send('stop')
    expect(r'Stopping server')
    assert server.wait(timeout=90) == 0
finally:
    if client is not None and client.poll() is None:
        subprocess.run(['taskkill', '/PID', str(client.pid), '/T', '/F'], capture_output=True)
    if client_log is not None:
        client_log.close()
    if server.poll() is None:
        try:
            send('stop')
            server.wait(timeout=30)
        except Exception:
            subprocess.run(['taskkill', '/PID', str(server.pid), '/T', '/F'], capture_output=True)
