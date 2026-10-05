"""Launch a real client into the disposable server smoke world and inspect logs."""
import argparse
import json
from pathlib import Path
import queue
import re
import shutil
import subprocess
import threading
import time

parser = argparse.ArgumentParser()
parser.add_argument('loader', choices=('fabric', 'neoforge'))
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
run = root / args.loader / 'run/client-smoke'
properties = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines() if '=' in line)
world_name = 'smoke-world-' + properties['minecraft_version']
world = run / 'saves' / world_name
run.mkdir(parents=True, exist_ok=True)
shutil.copytree(root / args.loader / 'run/smoke' / world_name, world, dirs_exist_ok=True)
(run / 'config').mkdir(exist_ok=True)
(run / 'config/cave_dweller.json').write_text(json.dumps({'time_until_leave': 300}))
(run / 'options.txt').write_text('renderDistance:4\nsimulationDistance:5\nsoundCategory_master:0.0\npauseOnLostFocus:false\n')
log_path = root / '.cache' / f'client-smoke-{args.loader}.log'
process = subprocess.Popen([str(root / 'gradlew.bat'), f':{args.loader}:runSmokeClient', '--console=plain', '--no-daemon'],
                           cwd=root, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                           text=True, encoding='utf-8', errors='replace',
                           creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
lines = queue.Queue()
def pump():
    with log_path.open('w', encoding='utf-8') as log:
        for line in process.stdout:
            log.write(line)
            log.flush()
            lines.put(line)
threading.Thread(target=pump, daemon=True).start()
deadline = time.monotonic() + 600
world_loaded = False
try:
    while time.monotonic() < deadline:
        try:
            line = lines.get(timeout=1)
        except queue.Empty:
            if process.poll() is not None:
                raise AssertionError(f'Client exited {process.returncode}; see {log_path}')
            continue
        if re.search(r'joined the game', line):
            if not world_loaded:
                print(line.strip(), flush=True)
                world_loaded = True
                deadline = time.monotonic() + 40
        if re.search(r'ReportedException|Rendering entity|Exception in server tick loop|Unable to load (model|animation)|Failed to load (model|animation)|Failed to create player model|Exception caught during firing event|Encountered an unexpected exception', line):
            raise AssertionError(line.strip())
    assert world_loaded, f'Quick Play world did not load; see {log_path}'
    print(f'PASS {args.loader}: client resources load and integrated world runs40 seconds without render/tick crash. Visual quality and player-driven AI require manual testing.', flush=True)
finally:
    if process.poll() is None:
        subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
