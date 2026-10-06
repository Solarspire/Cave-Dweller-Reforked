"""Verify packaged loader metadata and runtime assets, not source layout."""
import json
import io
import tomllib
from pathlib import Path
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PROPERTIES = dict(line.split('=', 1) for line in (ROOT / 'gradle.properties').read_text().splitlines() if '=' in line)

class Artifacts(unittest.TestCase):
    def test_both_loader_artifacts(self):
        for loader in ('fabric', 'neoforge'):
            with self.subTest(loader=loader):
                jars = [p for p in (ROOT / loader / 'build/libs').glob('*.jar')
                        if not p.name.endswith(('-sources.jar', '-dev.jar'))]
                self.assertEqual(1, len(jars), f'{loader}: run gradlew build first')
                with zipfile.ZipFile(jars[0]) as jar:
                    if loader == 'fabric':
                        nested = json.loads(jar.read('fabric.mod.json'))['jars']
                        paths = [entry['file'] for entry in nested if 'geckolib' in entry['file']]
                    else:
                        nested = json.loads(jar.read('META-INF/jarjar/metadata.json'))['jars']
                        paths = [entry['path'] for entry in nested if 'geckolib' in entry['identifier']['artifact']]
                    self.assertEqual(1, len(paths), 'Bundle exactly one GeckoLib mod via loader-native JAR-in-JAR')
                    with zipfile.ZipFile(io.BytesIO(jar.read(paths[0]))) as gecko:
                        if loader == 'fabric':
                            info = json.loads(gecko.read('fabric.mod.json'))
                            self.assertEqual('geckolib', info['id'])
                            self.assertEqual(PROPERTIES['geckolib_version'], info['version'])
                        else:
                            info = tomllib.loads(gecko.read('META-INF/neoforge.mods.toml').decode())
                            self.assertEqual('geckolib', info['mods'][0]['modId'])
                            self.assertEqual(PROPERTIES['geckolib_version'], info['mods'][0]['version'])
                    self.assertFalse(any(p.startswith(('com/geckolib/', 'software/bernie/geckolib/')) for p in jar.namelist()))
                    egg_model = json.loads(jar.read('assets/cave_dweller/models/item/cave_dweller_spawn_egg.json'))
                    self.assertTrue(egg_model.get('parent'))
                    for asset in ('geo/cave_dweller.geo.json', 'animations/cave_dweller.animation.json',
                                  'textures/entity/cave_dweller_texture.png',
                                  'textures/entity/cave_dweller_eyes_texture.png'):
                        self.assertGreater(len(jar.read('assets/cave_dweller/' + asset)), 0)
                    if PROPERTIES['geckolib_version'].startswith('5.'):
                        for kind, extension in (('models', 'geo'), ('animations', 'animation')):
                            adapted = jar.read(f'assets/cave_dweller/geckolib/{kind}/cave_dweller.{extension}.json')
                            original = jar.read('assets/cave_dweller/' + ('geo' if kind == 'models' else 'animations') + f'/cave_dweller.{extension}.json')
                            self.assertEqual(original, adapted)
                        item = json.loads(jar.read('assets/cave_dweller/items/cave_dweller_spawn_egg.json'))
                        self.assertEqual('cave_dweller:item/cave_dweller_spawn_egg', item['model']['model'])
                    sounds = json.loads(jar.read('assets/cave_dweller/sounds.json'))
                    language = json.loads(jar.read('assets/cave_dweller/lang/en_us.json'))
                    for sound in sounds.values():
                        self.assertIn(sound['subtitle'], language)
                        for entry in sound['sounds']:
                            name = entry if isinstance(entry, str) else entry['name']
                            namespace, name = name.split(':') if ':' in name else ('cave_dweller', name)
                            self.assertGreater(len(jar.read(f'assets/{namespace}/sounds/{name}.ogg')), 0)
                    for name in ('ServerConfig', 'ConfigValue'):
                        self.assertGreater(len(jar.read(f'de/cadentem/cave_dweller/config/{name}.class')), 0)
                    self.assertFalse(any(p.startswith('fade/') for p in jar.namelist()))
                    self.assertTrue(any(p.endswith('/entities/CaveDwellerEntity.class') for p in jar.namelist()))
                    if loader == 'fabric':
                        metadata = json.loads(jar.read('fabric.mod.json'))
                        self.assertEqual('cave_dweller', metadata['id'])
                        self.assertTrue(metadata['entrypoints']['client'])
                        self.assertEqual(PROPERTIES['minecraft_version'], metadata['depends']['minecraft'])
                        self.assertIsInstance(metadata['depends']['geckolib'], str)
                    else:
                        metadata = jar.read('META-INF/neoforge.mods.toml').decode()
                        self.assertIn('modId="cave_dweller"', metadata)
                        self.assertIn('versionRange="[' + PROPERTIES['minecraft_version'] + ']"', metadata)

if __name__ == '__main__':
    unittest.main()
