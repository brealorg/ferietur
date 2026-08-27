#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() {
    echo "RC1_CONTRACT=FAIL reason=$1"
    exit 1
}

for f in \
    README.md \
    SOURCE-SHA256SUMS.txt \
    docs/RC1_RELEASE_CANDIDATE.md \
    docs/RELEASE01_CANONICAL_RELEASE.md \
    tools/release01-contract.sh \
    tools/rc1-contract.sh \
    tools/verify-source-sha256.sh
 do
    [[ -f "$f" ]] || fail "missing_${f//\//_}"
 done

GRADLE='app/build.gradle.kts'

grep -Fq 'RC1 canonical source' README.md || fail 'README_RC1_state_missing'
grep -Fq 'versionCode 50' README.md || fail 'README_versionCode_50_missing'
grep -Fq '0.5.4-rc1' README.md || fail 'README_versionName_missing'

grep -Eq '^[[:space:]]*versionCode = 50$' "$GRADLE" || fail 'versionCode_not_50'
grep -Eq '^[[:space:]]*versionName = "0\.5\.4-rc1"$' "$GRADLE" || fail 'versionName_not_RC1'
! grep -Eq '^[[:space:]]*signingConfigs[[:space:]]*\{' "$GRADLE" || fail 'unexpected_release_signing_config_added'
! grep -Eq '^[[:space:]]*signingConfig[[:space:]]*=' "$GRADLE" || fail 'unexpected_signingConfig_assignment_added'

grep -Fq 'debug-signed' docs/RC1_RELEASE_CANDIDATE.md || fail 'debug_lineage_signing_state_missing'
grep -Fq 'app-release-unsigned.apk' docs/RC1_RELEASE_CANDIDATE.md || fail 'unsigned_release_state_missing'
grep -Fq 'must not generate a release keystore automatically' docs/RC1_RELEASE_CANDIDATE.md || fail 'signing_key_safety_decision_missing'

"$ROOT/tools/release01-contract.sh" >/dev/null || fail 'RELEASE01_contract_regressed'
"$ROOT/tools/verify-source-sha256.sh" >/dev/null || fail 'canonical_source_manifest_verification_failed'

printf '%s\n' \
  'RC1_APPLICATION_ID=app.ferietur' \
  'RC1_VERSION_CODE=50' \
  'RC1_VERSION_NAME=0.5.4-rc1' \
  'RC1_PRODUCT_BEHAVIOR_MUTATIONS=NONE' \
  'RC1_RELEASE_SIGNING=DEFERRED_EXPLICIT' \
  'RC1_CONTRACT=PASS'
