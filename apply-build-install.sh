#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo 'APPROVED_OPERATION=FERIETUR01_PLAY01_SOURCE_GATES_STRICT_BUILD_AND_AAB'
echo 'DEVICE_MUTATION=NONE'
echo 'RELEASE_SIGNING=EXTERNAL_PLAY_UPLOAD_KEY_AFTER_BUILD'

"$ROOT/tools/source-smoke.sh"
"$ROOT/tools/gate01-contract.sh"

"$ROOT/tools/gradle.sh" --offline --dependency-verification=strict \
    clean \
    testDebugUnitTest \
    lintRelease \
    assembleDebug \
    assembleRelease \
    assembleDebugAndroidTest \
    bundleRelease

test -f app/build/outputs/bundle/release/app-release.aab

echo 'UNIT_TESTS=PASS'
echo 'LINT_RELEASE=PASS'
echo 'ASSEMBLE_DEBUG=PASS'
echo 'ASSEMBLE_RELEASE=PASS'
echo 'ASSEMBLE_DEBUG_ANDROIDTEST=PASS'
echo 'BUNDLE_RELEASE=PASS'
echo 'DEPENDENCY_VERIFICATION_STRICT=PASS'
echo 'GRADLE_OFFLINE=PASS'
echo 'DEVICE_MUTATION=NONE'
