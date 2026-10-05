"""Verify packaged loader metadata and runtime assets, not source layout."""
import json
from pathlib import Path
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]

class Artifacts(unittest.TestCase):
    def test_both_loader_artifacts(self):
        for loader in ('fabric', 'neoforge'):
            with self.subTest(loader=loader):
                jars = [p for p in (ROOT / loader / 'build/libs').glob('*.jar')
                        if not p.name.endswith(('-sources.jar', '-dev.jar'))]
                self.assertEqual(1, len(jars), f'{loader}: run gradlew build first')
                with zipfile.ZipFile(jars[0]) as jar:
                    for asset in ('geo/cave_dweller.geo.json', 'animations/cave_dweller.animation.json',
                                  'textures/entity/cave_dweller_texture.png',
                                  'textures/entity/cave_dweller_eyes_texture.png'):
                        self.assertGreater(len(jar.read('assets/cave_dweller/' + asset)), 0)
                    sounds = json.loads(jar.read('assets/cave_dweller/sounds.json'))
                    for sound in sounds.values():
                        for entry in sound['sounds']:
                            name = entry if isinstance(entry, str) else entry['name']
                            namespace, name = name.split(':') if ':' in name else ('cave_dweller', name)
                            self.assertGreater(len(jar.read(f'assets/{namespace}/sounds/{name}.ogg')), 0)
                    self.assertFalse(any(p.startswith('fade/') for p in jar.namelist()))
                    self.assertTrue(any(p.endswith('/entities/CaveDwellerEntity.class') for p in jar.namelist()))
                    if loader == 'fabric':
                        metadata = json.loads(jar.read('fabric.mod.json'))
                        self.assertEqual('cave_dweller', metadata['id'])
                        self.assertTrue(metadata['entrypoints']['client'])
                        self.assertIn('geckolib', metadata['depends'])
                    else:
                        self.assertIn('cave_dweller', jar.read('META-INF/neoforge.mods.toml').decode())

if __name__ == '__main__':
    unittest.main()
