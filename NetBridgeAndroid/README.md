# NetBridge for Android — integration kit v0.3.1

This kit combines the original NetBridge 0.2.0 UI prototype with a safer, repeatable build path for a NetBridge-branded derivative of the real open-source Android engine in `MushroomSquad/tg-ws-proxy-android` at tag `v1.2.1`.

**The APK is not included yet.** The shell in this ChatGPT environment has no Android SDK and cannot clone GitHub, so a full Android build has not been performed here.

## What the engine provides

- Local MTProto ↔ WebSocket proxy for Telegram Android.
- Optional system-wide ByeDPI VPN.
- Existing upstream settings, service notifications, logs, and connection controls.

The upstream app is the implementation base; this kit does not claim that the starter prototype itself implements DPI bypass.

## Build on a computer

Requirements: Git, Python 3.10+, JDK 17, Android SDK Platform 35, NDK `26.1.10909125`, and CMake `3.22.1`.

- Windows PowerShell: `scripts\\build_netbridge_from_upstream.ps1`
- Linux/macOS: `bash scripts/build_netbridge_from_upstream.sh`

The script clones upstream tag `v1.2.1`, verifies the checkout tag, applies the NetBridge branding patch, and runs `assembleDebug`. Output files:

- `out/NetBridge-debug.apk`
- `out/BUILD-INFO.txt` (upstream commit and build variant)
- `out/SHA256SUMS.txt` (APK checksum)

The branding patch is designed to be safe to run more than once. The upstream Kotlin namespace is intentionally retained to avoid risky bulk refactoring, while the installable application ID is changed to `com.netbridge.app` so both apps can coexist.

## Build using GitHub Actions

1. Upload this kit to a GitHub repository you control.
2. Open **Actions → Build NetBridge APK → Run workflow**.
3. Wait for the workflow to finish successfully.
4. Download the `NetBridge-debug-apk` artifact from that run.

The workflow runs the Python patch tests, installs Android build tools, builds from the pinned upstream tag, and uploads the APK as an artifact. It does not publish a release or sign a production APK.

## Tests and validation

Run the local patch tests with:

```bash
python -m unittest discover -s tests -v
```

The integration-kit tests cover safe repeated patching and the key branding transformations. They do **not** test the native VPN engine, Telegram connectivity, or real-device behavior.

## Licensing and safety

The engine base contains GPL-3.0-derived code and third-party components under their own licenses. Keep the upstream `LICENSE`, `NOTICE.md`, `CREDITS.md`, and relevant source when redistributing. Review upstream `DISCLAIMER.md`. NetBridge is not affiliated with Telegram, Flowseal, or MushroomSquad.

## Current status

- [x] Identify and pin an engine containing both requested modes.
- [x] Add repeatable branding patch and Windows/Linux/macOS build scripts.
- [x] Add tests for the patching logic.
- [x] Add optional GitHub Actions workflow for producing a debug APK artifact.
- [ ] Execute Android build and inspect resulting APK.
- [ ] Install and test proxy/VPN on a real device.
- [ ] Replace and verify all upstream visual assets and screens.
- [ ] Create a separately signed release build after testing.
