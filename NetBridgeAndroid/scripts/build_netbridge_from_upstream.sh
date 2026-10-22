#!/usr/bin/env bash
set -euo pipefail

# Build a NetBridge-branded derivative from the selected upstream release.
# Requires: git, python3, JDK 17, Android SDK API 35, NDK 26.1.10909125, CMake 3.22.1.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="${NETBRIDGE_WORK_DIR:-$ROOT/build-source}"
OUT="${NETBRIDGE_OUTPUT_DIR:-$ROOT/out}"
REPO="https://github.com/MushroomSquad/tg-ws-proxy-android.git"
TAG="v1.2.1"

mkdir -p "$WORK" "$OUT"
CHECKOUT="$WORK/tg-ws-proxy-android"
if [ ! -d "$CHECKOUT/.git" ]; then
  git clone --depth 1 --branch "$TAG" "$REPO" "$CHECKOUT"
else
  echo "Using existing checkout: $CHECKOUT"
fi
ACTUAL_TAG="$(git -C "$CHECKOUT" describe --tags --exact-match HEAD 2>/dev/null || true)"
if [ "$ACTUAL_TAG" != "$TAG" ]; then
  echo "ERROR: checkout must be exactly at upstream tag $TAG (found: ${ACTUAL_TAG:-unknown})." >&2
  echo "Move or remove $CHECKOUT and rerun this script to create a clean checkout." >&2
  exit 1
fi
UPSTREAM_SHA="$(git -C "$CHECKOUT" rev-parse HEAD)"
python3 "$ROOT/scripts/prepare_netbridge.py" "$CHECKOUT"
cd "$CHECKOUT"
chmod +x gradlew
./gradlew assembleDebug --no-daemon
mkdir -p "$OUT"
cp app/build/outputs/apk/debug/app-debug.apk "$OUT/NetBridge-debug.apk"
printf '%s\n' "Upstream tag: $TAG" "Upstream commit: $UPSTREAM_SHA" "Build variant: debug" > "$OUT/BUILD-INFO.txt"
if command -v sha256sum >/dev/null 2>&1; then sha256sum "$OUT/NetBridge-debug.apk" > "$OUT/SHA256SUMS.txt"; fi
printf '\nBuild complete: %s\n' "$OUT/NetBridge-debug.apk"
printf 'Upstream source: %s (%s)\n' "$TAG" "$UPSTREAM_SHA"
printf 'This is a debug-signed derivative build; device testing is still required.\n'
