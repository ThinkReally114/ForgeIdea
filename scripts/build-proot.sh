#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$REPO_ROOT/app/proot-build"
OUTPUT_DIR="$REPO_ROOT/app/src/main/jniLibs/arm64-v8a"
ARCH="aarch64"

mkdir -p "$BUILD_DIR" "$OUTPUT_DIR"

bash "$BUILD_DIR/build.sh" "$BUILD_DIR" "$OUTPUT_DIR" "$ARCH"

echo "Build complete. Output files:"
ls -lh "$OUTPUT_DIR"
file "$OUTPUT_DIR/libproot.so"
