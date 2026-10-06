"""Test built JARs on production loaders, with and without standalone GeckoLib.

Uses disposable localhost-only worlds under .cache/production, accepts the Minecraft
EULA for those test servers, and downloads the pinned official loader installers.
"""
import argparse
import json
import os
from pathlib import Path
import queue
import re
import shutil
import subprocess
import threading
import time
import urllib.request
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('loader', choices=('fabric', 'neoforge'))
parser.add_argument('--java', type=Path)
parser.add_argument('--external', type=Path, help='Optional compatible standalone GeckoLib; defaults to the bundled version')
parser.add_argument('--prepare-only', action='store_true')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
props = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines() if '=' in line)
mc = props['minecraft_version']
java = str(args.java or (Path(os.environ['JAVA_HOME']) / 'bin/java.exe'))
run = root / '.cache/production' / mc / args.loader
run.mkdir(parents=True, exist_ok=True)

def download(url, destination):
    if not destination.exists():
        print(f'Download {url}', flush=True)
        urllib.request.urlretrieve(url, destination)

if args.loader == 'fabric':
    launcher = run / 'fabric-server-launch.jar'
    download(f'https://meta.fabricmc.net/v2/versions/loader/{mc}/{props["fabric_loader_version"]}/1.1.2/server/jar', launcher)
    command = [java, '-Xmx1G', '-Dfabric.log.level=debug', '-jar', str(launcher), 'nogui']
else:
    neo = props['neoforge_version']
    argument_file = run / f'libraries/net/neoforged/neoforge/{neo}/win_args.txt'
    if not argument_file.exists():
        installer = run / 'neoforge-installer.jar'
        download(f'https://maven.neoforged.net/releases/net/neoforged/neoforge/{neo}/neoforge-{neo}-installer.jar', installer)
        with (run / 'install.log').open('w', encoding='utf-8') as log:
            subprocess.run([java, '-jar', str(installer), '--installServer'], cwd=run,
                           stdout=log, stderr=subprocess.STDOUT, check=True,
                           creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    command = [java, '-Xmx1G', '@' + str(argument_file), 'nogui']
if args.prepare_only:
    print(f'Prepared {mc} {args.loader}', flush=True)
    raise SystemExit(0)

artifacts = [p for p in (root / args.loader / 'build/libs').glob('*.jar')
             if not p.name.endswith(('-sources.jar', '-dev.jar'))]
assert len(artifacts) == 1, 'Run gradlew clean build first'
mods = run / 'mods'
mods.mkdir(exist_ok=True)
shutil.copy2(artifacts[0], mods / 'cave_dweller.jar')
with zipfile.ZipFile(artifacts[0]) as jar:
    if args.loader == 'fabric':
        nested_path = next(entry['file'] for entry in json.loads(jar.read('fabric.mod.json'))['jars'] if 'geckolib' in entry['file'])
    else:
        nested_path = next(entry['path'] for entry in json.loads(jar.read('META-INF/jarjar/metadata.json'))['jars'] if 'geckolib' in entry['identifier']['artifact'])
    bundled = jar.read(nested_path)
if args.loader == 'fabric':
    api = props['fabric_api_version']
    download(f'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{api}/fabric-api-{api}.jar', mods / 'fabric-api.jar')
(run / 'config').mkdir(exist_ok=True)
(run / 'config/cave_dweller.json').write_text(json.dumps({'time_until_leave': 300}))
(run / 'eula.txt').write_text('eula=true\n')
(run / 'server.properties').write_text('online-mode=false\nserver-ip=127.0.0.1\nserver-port=0\nlevel-name=bundle-smoke\n'
    'level-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":3}],"biome":"minecraft:plains"}\n'
    'view-distance=2\nsimulation-distance=2\nspawn-protection=0\npause-when-empty-seconds=0\n')

for scenario in ('bundled', 'standalone'):
    external = mods / 'geckolib-standalone.jar'
    if scenario == 'standalone':
        external.write_bytes(args.external.read_bytes() if args.external else bundled)
    elif external.exists():
        external.unlink()
    log_path = run / f'{scenario}.log'
    process = subprocess.Popen(command, cwd=run, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace',
                               creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
    lines = queue.Queue()
    def pump():
        with log_path.open('w', encoding='utf-8') as log:
            for line in process.stdout:
                log.write(line)
                log.flush()
                lines.put(line)
    worker = threading.Thread(target=pump, daemon=True)
    worker.start()
    def expect(pattern, timeout=60):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            try:
                line = lines.get(timeout=1)
            except queue.Empty:
                assert process.poll() is None, f'Server exit {process.returncode}; see {log_path}'
                continue
            if re.search(pattern, line):
                print(line.strip(), flush=True)
                return
            if 'Exception in server tick loop' in line or 'Failed to start the minecraft server' in line:
                raise AssertionError(line.strip())
        raise AssertionError(f'Timeout waiting for {pattern}; see {log_path}')
    def send(command):
        process.stdin.write(command + '\n')
        process.stdin.flush()
    try:
        expect(r'Done \([\d.,]+s\)', 600)
        send('forceload add 0 0')
        expect(r'Marked chunk|already marked|No chunks were marked')
        send('setblock 0 -60 0 minecraft:barrel')
        expect(r'Changed the block|Could not set the block')
        send('item replace block 0 -60 0 container.0 with cave_dweller:cave_dweller_spawn_egg')
        expect(r'Replaced a slot|Replaced slot')
        send('data get block 0 -60 0 Items')
        expect(r'cave_dweller:cave_dweller_spawn_egg')
        send('kill @e[type=cave_dweller:cave_dweller]')
        expect(r'Killed|No entity was found|No entities found')
        send('summon cave_dweller:cave_dweller 3 -60 3 {Tags:["bundle_smoke"]}')
        expect(r'Summoned new')
        send('data get entity @e[type=cave_dweller:cave_dweller,tag=bundle_smoke,limit=1] Health')
        expect(r'60\.0f')
        send('stop')
        expect(r'Stopping server')
        assert process.wait(timeout=90) == 0, f'Unclean exit; see {log_path}'
        worker.join(timeout=5)
        log = log_path.read_text(encoding='utf-8')
        # Loader's mod list must report one selected GeckoLib, not two loaded copies.
        matches = re.findall(r'^\s*[-|\\]+ geckolib\s+[^\s]+', log, re.MULTILINE) if args.loader == 'fabric' else re.findall(r'^\s*GeckoLib[^\n]*\(geckolib\)\s*$', log, re.MULTILINE)
        if args.loader == 'neoforge' and not matches:
            debug_log = (run / 'logs/debug.log').read_text(encoding='utf-8', errors='replace')
            matches = re.findall(r'^.*\|geckolib\s*\|.*$', debug_log, re.MULTILINE)
        assert len(matches) == 1, f'Expected one selected GeckoLib; found {matches}; see {log_path}'
        print(f'PASS {mc} {args.loader} {scenario}: production startup, single GeckoLib, registered spawn egg, summon60HP, clean stop', flush=True)
    finally:
        if process.poll() is None:
            try:
                send('stop')
                process.wait(timeout=30)
            except Exception:
                subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
