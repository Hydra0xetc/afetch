#!/usr/bin/env bash

set -e

# configuration
VERSION=v1.1.6
BUILD_TYPE=release
ARCH="$(uname -m)"
ABI=""
OUTPUT_DIR="$TMPDIR"
INSTALL_PATH="$PREFIX"

if [[ "$ARCH" == "aarch64" ]]; then
  ABI="arm64-v8a"
elif [[ "$ARCH" == "armv7l" ]]; then
  ABI="armeabi-v7a"
else
  echo "[!] Unsupported arch '$ARCH'"
  exit 1
fi

echo "[*] Downloading afetch for '$ABI'"
wget "https://github.com/Hydra0xetc/afetch/releases/download/$VERSION/afetch-$VERSION-$ABI-$BUILD_TYPE.tar.gz" \
  -q \
  -O "$OUTPUT_DIR/afetch-$VERSION-$ABI-$BUILD_TYPE.tar.gz"

echo "[*] Unpacking tar file"
tar -xf "$OUTPUT_DIR/afetch-$VERSION-$ABI-$BUILD_TYPE.tar.gz" -C "$OUTPUT_DIR"

echo "[*] Installing to '$INSTALL_PATH'"
cp -rv "$OUTPUT_DIR/afetch-$VERSION-$ABI-$BUILD_TYPE/"* "$INSTALL_PATH"

INSTALLED_VER="$(afetch --version)"
echo "[*] Done... installing afetch version $VERSION"
