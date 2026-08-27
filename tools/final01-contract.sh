#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() {
    echo "FINAL01_CONTRACT=FAIL reason=$1"
    exit 1
}

for f in \
    README.md \
    SOURCE-SHA256SUMS.txt \
    docs/FINAL01_RELEASE.md \
    docs/SIGN01_SIGNING_IDENTITY.md \
    docs/ICON01_LAUNCHER_POLISH.md \
    app/src/androidTest/java/app/ferietur/FinalPromotionStateTest.kt \
    tools/final01-contract.sh \
    tools/verify-source-sha256.sh
 do
    [[ -f "$f" ]] || fail "missing_${f//\//_}"
 done

GRADLE='app/build.gradle.kts'
TEST='app/src/androidTest/java/app/ferietur/FinalPromotionStateTest.kt'
FG='app/src/main/res/drawable/ic_launcher_foreground.xml'
MONO='app/src/main/res/drawable/ic_launcher_monochrome.xml'
EXPECTED_CERT='9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7'

[[ "$(head -1 README.md)" == "# Ferietur" ]] || fail 'README_not_canonical'
grep -Fq 'FINAL01 canonical source' README.md || fail 'README_final_state_missing'
grep -Fq 'versionName: `0.5.4`' README.md || fail 'README_final_version_name_missing'
grep -Fq 'versionCode: `51`' README.md || fail 'README_final_version_code_missing'

grep -Eq '^[[:space:]]*versionCode = 51$' "$GRADLE" || fail 'versionCode_not_51'
grep -Eq '^[[:space:]]*versionName = "0\.5\.4"$' "$GRADLE" || fail 'versionName_not_final'
grep -Fq 'applicationId = "app.ferietur"' "$GRADLE" || fail 'application_id_changed'
! grep -Eq '^[[:space:]]*signingConfigs[[:space:]]*\{' "$GRADLE" || fail 'unexpected_release_signing_config_added'
! grep -Eq '^[[:space:]]*signingConfig[[:space:]]*=' "$GRADLE" || fail 'unexpected_signingConfig_assignment_added'
grep -Fq 'implementation("androidx.core:core-ktx:1.18.0")' "$GRADLE" || fail 'core_ktx_alignment_regressed'

for marker in \
    'android:name="suitcase_handle"' \
    'android:name="suitcase_body"' \
    'android:name="route_start"' \
    'android:name="route_dash_1"' \
    'android:name="route_dash_2"' \
    'android:name="route_dash_3"' \
    'android:name="route_pin"' \
    'android:name="route_pin_hole"'
 do
    grep -Fq "$marker" "$FG" || fail "launcher_marker_missing_${marker//[^A-Za-z0-9]/_}"
 done
grep -Fq 'android:name="monochrome_body_with_pin_cutout"' "$MONO" || fail 'monochrome_launcher_regressed'

CERT="$(sed -n 's/^- signing certificate SHA-256: `\([0-9a-fA-F]\{64\}\)`$/\1/p' docs/SIGN01_SIGNING_IDENTITY.md | tr 'A-F' 'a-f')"
[[ "$CERT" == "$EXPECTED_CERT" ]] || fail 'permanent_signing_certificate_changed'

grep -Fq 'versionCode: `51`' docs/FINAL01_RELEASE.md || fail 'final_doc_version_code_missing'
grep -Fq 'versionName: `0.5.4`' docs/FINAL01_RELEASE.md || fail 'final_doc_version_name_missing'
grep -Fq "$EXPECTED_CERT" docs/FINAL01_RELEASE.md || fail 'final_doc_certificate_missing'
grep -Fq 'adb install -r' docs/FINAL01_RELEASE.md || fail 'same_certificate_upgrade_contract_missing'
grep -Fq 'no `pm clear`' docs/FINAL01_RELEASE.md || fail 'pm_clear_prohibition_missing'

grep -Fq 'fun capturePreUpgradeState()' "$TEST" || fail 'pre_upgrade_state_probe_missing'
grep -Fq 'fun verifyPostUpgradeStateAndCleanup()' "$TEST" || fail 'post_upgrade_state_probe_missing'
grep -Fq 'PackageInfoCompat.getLongVersionCode' "$TEST" || fail 'runtime_version_probe_missing'
grep -Fq 'SENTINEL_NAME = ".ferietur-final01-promotion-state"' "$TEST" || fail 'promotion_sentinel_missing'
grep -Fq 'listOf("files", "shared_prefs", "databases", "no_backup")' "$TEST" || fail 'durable_root_set_missing'
grep -Fq 'TripDraftStore(targetContext).loadLibrary()' "$TEST" || fail 'real_library_load_missing'
grep -Fq 'TripStorageIssueKind.CORRUPT' "$TEST" || fail 'corrupt_state_guard_missing'
grep -Fq 'assertEquals(props.getProperty("digest"), measured.digest)' "$TEST" || fail 'exact_digest_assertion_missing'

while IFS= read -r secret; do
    [[ -z "$secret" ]] && continue
    printf 'FINAL01_PRIVATE_MATERIAL_IN_SOURCE=%s\n' "$secret"
    fail 'private_signing_material_present_in_source'
done < <(find . -type f \
    \( -name '*.jks' -o -name '*.keystore' -o -name '*.p12' -o -name '*.pfx' -o -name '*.pk8' \) \
    -not -path './.gradle/*' -not -path './app/build/*' -not -path './build/*' -print)

"$ROOT/tools/release01-contract.sh" >/dev/null || fail 'RELEASE01_contract_regressed'
"$ROOT/tools/verify-source-sha256.sh" >/dev/null || fail 'canonical_source_manifest_verification_failed'

printf '%s\n' \
  'FINAL01_APPLICATION_ID=app.ferietur' \
  'FINAL01_VERSION_CODE=51' \
  'FINAL01_VERSION_NAME=0.5.4' \
  "FINAL01_SIGNING_CERTIFICATE_SHA256=$EXPECTED_CERT" \
  'FINAL01_PRODUCT_BEHAVIOR_MUTATIONS=NONE' \
  'FINAL01_SAME_CERTIFICATE_UPGRADE_TEST=DEFINED' \
  'FINAL01_EXACT_DURABLE_STATE_CONTINUITY_TEST=DEFINED' \
  'FINAL01_PRIVATE_MATERIAL_IN_SOURCE=NONE' \
  'FINAL01_CONTRACT=PASS'
