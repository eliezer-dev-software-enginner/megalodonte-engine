import importlib.util
import json
import tempfile
import unittest
from unittest.mock import patch
from pathlib import Path

spec = importlib.util.spec_from_file_location('release', Path(__file__).with_name('prepare-android-release.py'))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseExportTest(unittest.TestCase):
    @patch.object(release, "find_sdk", return_value=None)
    def test_project_is_portable_and_previous_export_is_preserved(self, _sdk):
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
            import xml.etree.ElementTree as ET
            application = ET.parse(project / 'app/src/main/AndroidManifest.xml').getroot().find('application')
            self.assertEqual('game', application.attrib['{http://schemas.android.com/apk/res/android}appCategory'])
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
            (game / "scripts/Test.java").write_text("// updated script")
            metadata["versionCode"] = 8
            (game / "game.json").write_text(json.dumps(metadata))
            release.prepare(engine, game)
            self.assertEqual("// updated script", (project / "scripts/Test.java").read_text())
            self.assertEqual(8, json.loads((project / "game/game.json").read_text())["versionCode"])
            self.assertEqual((engine / "engine-core/src/main/java/megalodonte/engine/CameraView.java").read_text(), (project / "engine-core/src/main/java/megalodonte/engine/CameraView.java").read_text())
            backups = list(project.parent.glob('project-backup-*'))
            self.assertEqual(1, len(backups))
            self.assertEqual('local user file', (backups[0] / 'my-keystore.jks').read_text())
            self.assertFalse((project / 'my-keystore.jks').exists())

    def test_detected_sdk_is_configured_without_copying_user_properties(self):
        engine = Path(__file__).resolve().parent.parent
        with tempfile.TemporaryDirectory() as folder:
            game = Path(folder) / 'game'; game.mkdir()
            sdk = Path(folder) / 'Android SDK'; (sdk / 'platforms').mkdir(parents=True)
            (game / 'game.json').write_text(json.dumps(dict(applicationId='game.test.release')))
            with patch.object(release, 'find_sdk', return_value=sdk):
                project = release.prepare(engine, game)
            self.assertEqual('sdk.dir=' + release.property_value(sdk) + '\n', (project / 'local.properties').read_text())
            self.assertIn('local.properties', (project / '.gitignore').read_text())
            (project / 'local.properties').write_text('custom.value=preserved\nsdk.dir=old\n')
            release.configure_sdk(project, sdk)
            self.assertIn('custom.value=preserved', (project / 'local.properties').read_text())
            self.assertNotIn('sdk.dir=old', (project / 'local.properties').read_text())

    def test_invalid_version_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            game = Path(folder)
            (game / 'game.json').write_text(json.dumps(dict(applicationId='game.test.release', versionCode=0)))
            with self.assertRaises(ValueError):
                release.prepare(Path(__file__).resolve().parent.parent, game)


if __name__ == '__main__':
    unittest.main()
