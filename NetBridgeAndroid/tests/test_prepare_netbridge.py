import importlib.util
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "prepare_netbridge.py"
spec = importlib.util.spec_from_file_location("prepare_netbridge", SCRIPT)
module = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(module)


class BrandingTransformTests(unittest.TestCase):
    def test_gradle_transform_is_idempotent(self):
        source = '''defaultConfig {\n applicationId = "com.flowseal.tgwsproxy"\n versionCode = 17\n versionName = "1.2.1"\n}'''
        patched = module.transform_gradle(source)
        self.assertIn('applicationId = "com.netbridge.app"', patched)
        self.assertIn('versionCode = 18', patched)
        self.assertIn('versionName = "1.2.1-netbridge.1"', patched)
        self.assertEqual(module.transform_gradle(patched), patched)

    def test_ui_labels_change_without_package_rename(self):
        strings = '<string name="app_name">TgWsProxy</string>\n<string name="notification_title">TgWsProxy running</string>'
        patched = module.transform_strings(strings)
        self.assertIn('<string name="app_name">NetBridge</string>', patched)
        self.assertIn('<string name="notification_title">NetBridge running</string>', patched)
        home = 'Text(text = "TgWsProxy", color = Color.White)'
        self.assertEqual(module.transform_home(home), 'Text(text = "NetBridge", color = Color.White)')

    def test_prepare_twice_is_safe(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "gradlew").write_text("#!/bin/sh\n", encoding="utf-8")
            (root / "app/src/main/res/values").mkdir(parents=True)
            (root / "app/src/main/java/com/flowseal/tgwsproxy/ui").mkdir(parents=True)
            (root / "app/build.gradle.kts").write_text(
                'applicationId = "com.flowseal.tgwsproxy"\nversionCode = 17\nversionName = "1.2.1"\n', encoding="utf-8")
            (root / "app/src/main/res/values/strings.xml").write_text(
                '<string name="app_name">TgWsProxy</string>\n<string name="notification_title">TgWsProxy running</string>', encoding="utf-8")
            (root / "app/src/main/java/com/flowseal/tgwsproxy/ui/HomeTab.kt").write_text(
                'text = "TgWsProxy",', encoding="utf-8")
            (root / "NOTICE.md").write_text("# Notices\n", encoding="utf-8")
            (root / "CREDITS.md").write_text("# Credits\n", encoding="utf-8")
            (root / "README.md").write_text("# Upstream\n", encoding="utf-8")
            module.prepare(root)
            snapshot = {str(p.relative_to(root)): p.read_bytes() for p in root.rglob("*") if p.is_file()}
            module.prepare(root)
            after = {str(p.relative_to(root)): p.read_bytes() for p in root.rglob("*") if p.is_file()}
            self.assertEqual(snapshot, after)


if __name__ == "__main__":
    unittest.main()
