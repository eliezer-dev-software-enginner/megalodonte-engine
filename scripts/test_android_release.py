import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location('release', Path(__file__).with_name('prepare-android-release.py'))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseExportTest(unittest.TestCase):
    def test_project_is_portable_and_previous_export_is_preserved(self):
        engine = Path(__file__).resolve().parent.parent
        with tempfile.TemporaryDirectory() as folder:
            game = Path(folder) / 'my game'
            game.mkdir()
            (game / 'assets').mkdir()
            (game / 'scripts').mkdir()
            (game / 'scripts/Test.java').write_text('// script')
            (game / 'assets/marker.txt').write_text('asset')
            metadata = dict(version=1, name='Teste — ação #1', applicationId='game.test.release', versionCode=7, versionName='1.2.3', entrySceneId='scene-1', scenes=[dict(id='scene-1')])
            (game / 'game.json').write_text(json.dumps(metadata))
            project = release.prepare(engine, game)
            self.assertTrue((project / 'engine-core/src/main/java/megalodonte/engine/Game.java').is_file())
            self.assertEqual('// script', (project / 'scripts/Test.java').read_text())
            properties = (project / 'gradle.properties').read_text()
            self.assertIn('gameAssets=../game', properties)
            self.assertIn('gameVersionCode=7', properties)
            self.assertIn('gameName=Teste\\ \\u2014\\ a\\u00e7\\u00e3o\\ \\#1', properties)
            self.assertNotIn(str(game), properties)
            self.assertIn('file("engine-core")', (project / 'settings.gradle.kts').read_text())
            self.assertFalse((project / 'local.properties').exists())
            self.assertFalse((project / 'app/build').exists())
            (project / 'my-keystore.jks').write_text('local user file')
            release.prepare(engine, game)
            backups = list(project.parent.glob('project-backup-*'))
            self.assertEqual(1, len(backups))
            self.assertEqual('local user file', (backups[0] / 'my-keystore.jks').read_text())
            self.assertFalse((project / 'my-keystore.jks').exists())

    def test_invalid_version_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            game = Path(folder)
            (game / 'game.json').write_text(json.dumps(dict(applicationId='game.test.release', versionCode=0)))
            with self.assertRaises(ValueError):
                release.prepare(Path(__file__).resolve().parent.parent, game)


if __name__ == '__main__':
    unittest.main()
