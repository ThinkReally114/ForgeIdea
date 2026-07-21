#!/usr/bin/env bash
set -euo pipefail

BUILD_DIR="${1:-$(pwd)}"
OUTPUT_DIR="${2:-$BUILD_DIR/output}"
ARCH="${3:-aarch64}"

TERMUX_REPO="https://packages.termux.dev/apt/termux-main"
PACKAGES_URL="$TERMUX_REPO/dists/stable/main/binary-$ARCH/Packages.gz"

mkdir -p "$BUILD_DIR" "$OUTPUT_DIR"

cd "$BUILD_DIR"

echo "Downloading package index..."
curl -L -o Packages.gz "$PACKAGES_URL"
zcat Packages.gz > Packages.txt

get_filename() {
    local pkg="$1"
    awk -v pkg="$pkg" '
        /^Package: / { current = $2 }
        current == pkg && /^Filename: / { print $2; exit }
    ' Packages.txt
}

PROOT_FILE=$(get_filename "proot")
TALLOC_FILE=$(get_filename "libtalloc")
SHMEM_FILE=$(get_filename "libandroid-shmem")

echo "Downloading proot: $PROOT_FILE"
curl -L -o proot.deb "$TERMUX_REPO/$PROOT_FILE"

echo "Downloading libtalloc: $TALLOC_FILE"
curl -L -o libtalloc.deb "$TERMUX_REPO/$TALLOC_FILE"

echo "Downloading libandroid-shmem: $SHMEM_FILE"
curl -L -o libandroid-shmem.deb "$TERMUX_REPO/$SHMEM_FILE"

rm -rf extracted
mkdir extracted

dpkg-deb -x proot.deb extracted/
dpkg-deb -x libtalloc.deb extracted/
dpkg-deb -x libandroid-shmem.deb extracted/

PREFIX="extracted/data/data/com.termux/files/usr"

cp "$PREFIX/bin/proot" "$OUTPUT_DIR/libproot.so"
cp "$PREFIX/lib/libtalloc.so.2" "$OUTPUT_DIR/libtalloc.so.2"
cp "$PREFIX/lib/libandroid-shmem.so" "$OUTPUT_DIR/libandroid-shmem.so"
cp "$PREFIX/libexec/proot/loader" "$OUTPUT_DIR/loader"
cp "$PREFIX/libexec/proot/loader32" "$OUTPUT_DIR/loader32" || true

echo "Done. Output files:"
ls -lh "$OUTPUT_DIR"
