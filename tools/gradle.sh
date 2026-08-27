#!/usr/bin/env bash
set -euo pipefail
VERSION=9.7.1
CACHE_ROOT="${XDG_CACHE_HOME:-$HOME/.cache}/ferietur01/gradle"
DIST_DIR="$CACHE_ROOT/gradle-$VERSION"
GRADLE="$DIST_DIR/bin/gradle"
if [ -x "$GRADLE" ]; then
    exec "$GRADLE" "$@"
fi
CACHED="$(find "$HOME/.gradle/wrapper/dists/gradle-$VERSION-bin" -type f -path "*/gradle-$VERSION/bin/gradle" -perm -u+x -print -quit 2>/dev/null || true)"
if [ -n "$CACHED" ]; then
    exec "$CACHED" "$@"
fi
command -v curl >/dev/null 2>&1 || { echo "STOP=CURL_NOT_FOUND"; exit 70; }
command -v unzip >/dev/null 2>&1 || { echo "STOP=UNZIP_NOT_FOUND"; exit 71; }
mkdir -p "$CACHE_ROOT"
TMP="$(mktemp -d "$CACHE_ROOT/.bootstrap.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT
ZIP="$TMP/gradle-$VERSION-bin.zip"
SHA_FILE="$TMP/gradle-$VERSION-bin.zip.sha256"
URL="https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
curl -fL --retry 3 --retry-delay 2 -o "$SHA_FILE" "$URL.sha256"
EXPECTED="$(tr -d '[:space:]' < "$SHA_FILE")"
case "$EXPECTED" in
    ''|*[!0-9a-fA-F]*) echo "STOP=GRADLE_SHA256_INVALID"; exit 72 ;;
esac
curl -fL --retry 3 --retry-delay 2 -o "$ZIP" "$URL"
ACTUAL="$(sha256sum "$ZIP" | awk '{print $1}')"
if [ "${ACTUAL,,}" != "${EXPECTED,,}" ]; then
    echo "STOP=GRADLE_SHA256_MISMATCH"
    echo "EXPECTED=$EXPECTED"
    echo "ACTUAL=$ACTUAL"
    exit 73
fi
unzip -q "$ZIP" -d "$TMP/unpack"
rm -rf "$DIST_DIR"
mv "$TMP/unpack/gradle-$VERSION" "$DIST_DIR"
exec "$GRADLE" "$@"
