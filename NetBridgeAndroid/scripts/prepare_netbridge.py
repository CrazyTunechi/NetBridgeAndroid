#!/usr/bin/env python3
"""Apply repeatable, minimal NetBridge branding to a clean TgWsProxy checkout."""
from pathlib import Path
import re
import sys

EXPECTED_TAG = "v1.2.1"


def transform_gradle(text: str) -> str:
    # Accept upstream or already-patched values so the script is idempotent.
    text, n = re.subn(
        r'applicationId\s*=\s*"(?:com\.flowseal\.tgwsproxy|com\.netbridge\.app)"',
        'applicationId = "com.netbridge.app"', text, count=1)
    if n != 1:
        raise ValueError("Could not safely set applicationId in app/build.gradle.kts")
    text, n = re.subn(r'versionCode\s*=\s*(?:17|18)\b', 'versionCode = 18', text, count=1)
    if n != 1:
        raise ValueError("Could not safely set versionCode in app/build.gradle.kts")
    text, n = re.subn(
        r'versionName\s*=\s*"(?:1\.2\.1|1\.2\.1-netbridge\.1)"',
        'versionName = "1.2.1-netbridge.1"', text, count=1)
    if n != 1:
        raise ValueError("Could not safely set versionName in app/build.gradle.kts")
    return text


def transform_strings(text: str) -> str:
    text = text.replace('<string name="app_name">TgWsProxy</string>',
                        '<string name="app_name">NetBridge</string>')
    text = text.replace('<string name="notification_title">TgWsProxy running</string>',
                        '<string name="notification_title">NetBridge running</string>')
    return text


def transform_home(text: str) -> str:
    return text.replace('text = "TgWsProxy",', 'text = "NetBridge",')


def patch_file(root: Path, rel: str, transform, required: bool = True) -> None:
    path = root / rel
    if not path.is_file():
        if required:
            raise FileNotFoundError(f"Required upstream file missing: {rel}")
        print(f"[skip]      {rel} (not present in upstream revision)")
        return
    before = path.read_text(encoding="utf-8")
    after = transform(before)
    if after != before:
        path.write_text(after, encoding="utf-8")
        print(f"[patched]   {rel}")
    else:
        print(f"[unchanged] {rel}")


def append_once(path: Path, marker: str, addition: str) -> None:
    if not path.is_file():
        print(f"[skip]      {path.name} (not present)")
        return
    text = path.read_text(encoding="utf-8")
    if marker in text:
        print(f"[unchanged] {path.name}")
        return
    path.write_text(text.rstrip() + "\n\n" + addition.strip() + "\n", encoding="utf-8")
    print(f"[patched]   {path.name}")


def prepare(root: Path) -> None:
    root = root.resolve()
    if not (root / "gradlew").is_file() or not (root / "app").is_dir():
        raise ValueError(f"Not an Android Gradle checkout: {root}")
    patch_file(root, "app/build.gradle.kts", transform_gradle)
    patch_file(root, "app/src/main/res/values/strings.xml", transform_strings)
    patch_file(root, "app/src/main/java/com/flowseal/tgwsproxy/ui/HomeTab.kt", transform_home)

    append_once(root / "NOTICE.md", "## NetBridge derivative", """
## NetBridge derivative

This checkout has been rebranded as NetBridge. The upstream Kotlin package/namespace remains `com.flowseal.tgwsproxy`; the installable application ID is `com.netbridge.app`. Original upstream copyright and GPL-3.0 terms remain in force. This derivative is not affiliated with Telegram, Flowseal, or MushroomSquad.
""")
    credits = root / "CREDITS.md"
    if credits.is_file():
        text = credits.read_text(encoding="utf-8")
        text = text.replace(
            "Application id remains `com.flowseal.tgwsproxy` for install continuity with early sideload builds.",
            "The NetBridge derivative uses application ID `com.netbridge.app`; the upstream namespace is retained to minimize invasive refactoring.")
        if "## NetBridge derivative" not in text:
            text = text.rstrip() + "\n\n## NetBridge derivative\n\nNetBridge is an independent derivative build. Original project credits and license notices are retained. NetBridge is not affiliated with Telegram, Flowseal, or MushroomSquad.\n"
        if text != credits.read_text(encoding="utf-8"):
            credits.write_text(text, encoding="utf-8")
            print("[patched]   CREDITS.md")
        else:
            print("[unchanged] CREDITS.md")

    readme = root / "README.md"
    if readme.is_file():
        text = readme.read_text(encoding="utf-8")
        intro = "# NetBridge Android (derived build)\n\nThis build uses the TgWsProxy Android engine as its implementation base: local Telegram MTProto↔WebSocket proxy and optional ByeDPI VPN. It is a derivative, not an original implementation. See `LICENSE`, `NOTICE.md`, and `CREDITS.md`; upstream notices must remain with redistributions.\n\n"
        if not text.startswith("# NetBridge Android (derived build)"):
            readme.write_text(intro + text, encoding="utf-8")
            print("[patched]   README.md")
        else:
            print("[unchanged] README.md")


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: prepare_netbridge.py PATH_TO_TGWS_PROXY_CHECKOUT", file=sys.stderr)
        return 2
    try:
        prepare(Path(sys.argv[1]))
    except (OSError, ValueError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    print("\nBranding patch applied. Build requires JDK 17, Android SDK API 35, NDK 26.1.10909125, and CMake 3.22.1.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
