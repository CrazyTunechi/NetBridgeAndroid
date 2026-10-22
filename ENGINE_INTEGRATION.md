# NetBridge engine integration status — v0.3.1

## Engine choice

The build uses `MushroomSquad/tg-ws-proxy-android` pinned at `v1.2.1`, which contains:

- a local MTProto ↔ WebSocket bridge for Telegram Android;
- optional ByeDPI system VPN and its native/tunnel dependencies.

This is a derivative build, not an independent reimplementation. GPL-3.0 and third-party license obligations apply.

## Build

Requirements: Git, Python 3.10+, JDK 17, Android SDK Platform 35, NDK `26.1.10909125`, CMake `3.22.1`.

- Windows: `scripts\\build_netbridge_from_upstream.ps1`
- Linux/macOS: `bash scripts/build_netbridge_from_upstream.sh`
- Patch tests: `python -m unittest discover -s tests -v`

The build scripts verify that the checkout is exactly at `v1.2.1`, apply an idempotent branding patch, run `assembleDebug`, and produce `out/NetBridge-debug.apk`, `out/BUILD-INFO.txt`, and a SHA-256 checksum file.

The patch changes the Android application ID to `com.netbridge.app`, updates the visible app label and home heading, and preserves the upstream Kotlin namespace to minimize risky package refactoring. It retains the original license and attribution files and adds derivative notices.

## GitHub Actions option

`.github/workflows/build-netbridge.yml` runs the patch tests and then builds the APK in a GitHub-hosted Android build environment. Upload the kit to a repository under your control, then run **Actions → Build NetBridge APK → Run workflow**. The APK is made available as a 14-day artifact. This workflow does not create a public release or a production-signed APK.

## Limits / not yet verified

- This ChatGPT runtime has no Android SDK and its local shell cannot reach GitHub, so the Android build has not run here.
- No APK is included in this kit.
- Python tests validate only the patching script, not the Android project, native code, VPN routing, or Telegram connectivity.
- The upstream launcher icon and some internal theme names remain. Review every screen and test on a real device before calling the app finished.
- The debug APK should not be treated as a production release. A production release needs a controlled signing key and real-device validation.

## References

- Source: https://github.com/MushroomSquad/tg-ws-proxy-android
- Pinned release: https://github.com/MushroomSquad/tg-ws-proxy-android/releases/tag/v1.2.1
- Keep `LICENSE`, `NOTICE.md`, `CREDITS.md`, and the upstream disclaimer.
