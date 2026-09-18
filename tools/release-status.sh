#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

STATE='release/current.env'
test -f "$STATE" || {
  echo 'RELEASE_STATUS=FAIL state_file_missing'
  exit 1
}

# shellcheck disable=SC1090
source "$STATE"

GRADLE_VERSION_NAME="$(sed -n 's/^[[:space:]]*versionName = "\(.*\)"/\1/p' app/build.gradle.kts | head -1)"
GRADLE_VERSION_CODE="$(sed -n 's/^[[:space:]]*versionCode = \([0-9][0-9]*\)$/\1/p' app/build.gradle.kts | head -1)"
REMOTE="$(git remote get-url origin 2>/dev/null || true)"
HEAD="$(git rev-parse HEAD)"
DIRTY="$(git status --porcelain)"

echo "RELEASE_STATE=$RELEASE_STATE"
echo "VERSION_NAME=$VERSION_NAME"
echo "VERSION_CODE=$VERSION_CODE"
echo "TAG=$TAG"
echo "APP_SOURCE_QUALIFIED_COMMIT=$APP_SOURCE_QUALIFIED_COMMIT"
echo "PLAY_UPLOAD_KEY_RESET_STATUS=$PLAY_UPLOAD_KEY_RESET_STATUS"
echo "PLAY_UPLOAD_KEY_ACTIVATES_AT_UTC=${PLAY_UPLOAD_KEY_ACTIVATES_AT_UTC:-UNKNOWN}"
echo "PLAY_UPLOAD_KEY_ACTIVATES_AT_OSLO=${PLAY_UPLOAD_KEY_ACTIVATES_AT_OSLO:-UNKNOWN}"
echo "PLAY_PRODUCTION_ACCESS_STATUS=${PLAY_PRODUCTION_ACCESS_STATUS:-UNKNOWN}"
echo "PLAY_AAB_STATUS=$PLAY_AAB_STATUS"
echo "PLAY_RELEASE_STATUS=$PLAY_RELEASE_STATUS"
echo "PLAY_SIGNED_UNIVERSAL_APK_STATUS=$PLAY_SIGNED_UNIVERSAL_APK_STATUS"
echo "GITHUB_RELEASE_STATUS=$GITHUB_RELEASE_STATUS"
echo "README_PUBLISHED_RELEASE=$README_PUBLISHED_RELEASE"
echo "README_SOURCE_VERSION=$README_SOURCE_VERSION"
echo "NEXT_ACTION=$NEXT_ACTION"
echo "CURRENT_HEAD=$HEAD"
echo "ORIGIN=$REMOTE"

test "$GRADLE_VERSION_NAME" = "$VERSION_NAME" || {
  echo "RELEASE_STATUS=FAIL version_name_mismatch gradle=$GRADLE_VERSION_NAME state=$VERSION_NAME"
  exit 2
}
test "$GRADLE_VERSION_CODE" = "$VERSION_CODE" || {
  echo "RELEASE_STATUS=FAIL version_code_mismatch gradle=$GRADLE_VERSION_CODE state=$VERSION_CODE"
  exit 3
}
test "$REMOTE" = 'https://github.com/brealorg/ferietur.git' || {
  echo "RELEASE_STATUS=FAIL unexpected_origin"
  exit 4
}

if [ -n "$DIRTY" ]; then
  echo 'WORKTREE=CURRENTLY_DIRTY'
else
  echo 'WORKTREE=CLEAN'
fi

echo 'RELEASE_STATUS=PASS'
