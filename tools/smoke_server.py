"""Exercise the real dedicated server in a disposable dev directory."""
import argparse
import json
import os
from pathlib import Path
import queue
import re
import subprocess
import threading
import time

parser = argparse.ArgumentParser()
parser.add_argument('loader', choices=('fabric', 'neoforge'))
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
run = root / args.loader / 'run/smoke'
run.mkdir(parents=True, exist_ok=True)
(run / 'config').mkdir(exist_ok=True)
(run / 'config/cave_dweller.json').write_text(json.dumps({'time_until_leave': 10}))
(run / 'eula.txt').write_text('eula=true\n')
(run / 'server.properties').write_text(
    'online-mode=false\nserver-ip=127.0.0.1\nserver-port=0\nlevel-name=smoke-world\n'
    'level-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":3}],"biome":"minecraft:plains"}\nview-distance=2\nsimulation-distance=2\n'
    'spawn-protection=0\nmax-tick-time=60000\n')
log_dir = root / '.cache'
log_dir.mkdir(exist_ok=True)
log_path = log_dir / f'smoke-{args.loader}.log'
command = [str(root / 'gradlew.bat'), f':{args.loader}:runSmokeServer', '--console=plain', '--no-daemon']
process = subprocess.Popen(command, cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                           stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace',
                           creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
lines = queue.Queue()

def pump():
    with log_path.open('w', encoding='utf-8') as log:
        for line in process.stdout:
            log.write(line)
            log.flush()
            lines.put(line)

threading.Thread(target=pump, daemon=True).start()

def expect(pattern, timeout=60):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            line = lines.get(timeout=1)
        except queue.Empty:
            if process.poll() is not None:
                raise AssertionError(f'Server exited {process.returncode}; see {log_path}')
            continue
        if re.search(pattern, line):
            print(line.strip(), flush=True)
            return line
        if 'Exception in server tick loop' in line or 'Failed to start the minecraft server' in line:
            raise AssertionError(line.strip())
    raise AssertionError(f'Timeout waiting for {pattern}; see {log_path}')

def send(command):
    process.stdin.write(command + '\n')
    process.stdin.flush()

try:
    expect(r'Done \([\d.,]+s\)', timeout=480)
    send('summon cave_dweller:cave_dweller 0 -60 0')
    expect(r'Summoned new')
    selector = '@e[type=cave_dweller:cave_dweller,limit=1]'
    send(f'data get entity {selector} Health')
    expect(r'60\.0f')
    send(f'damage {selector} 10 minecraft:in_wall')
    expect(r'Target is invulnerable|could not be damaged|Failed to damage|not damage')
    send(f'damage {selector} 10 minecraft:fall')
    expect(r'Target is invulnerable|could not be damaged|Failed to damage|not damage')
    send(f'damage {selector} 10 minecraft:generic')
    expect(r'Applied 10|Dealt 10')
    send(f'data get entity {selector} Health')
    expect(r'50\.0f')
    time.sleep(12)
    send(f'data get entity {selector} Health')
    expect(r'No entity was found|No entities found')
    send('stop')
    expect(r'Stopping server')
    code = process.wait(timeout=90)
    assert code == 0, f'Exit {code}; see {log_path}'
    print(f'PASS {args.loader}: startup, summon, 60 HP, suffocation/fall immunity, damage, despawn, clean stop', flush=True)
finally:
    if process.poll() is None:
        try:
            send('stop')
            process.wait(timeout=30)
        except Exception:
            # Terminate only this test process and its children.
            subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
