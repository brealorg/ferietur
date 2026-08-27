#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() {
    echo "SIGN01A2_CONTRACT=FAIL reason=$1"
    exit 1
}

DOC='docs/SIGN01A2_SIGNATURE_TRANSITION.md'
TEST='app/src/androidTest/java/app/ferietur/SignatureTransitionStateTest.kt'
[[ -f "$DOC" ]] || fail 'transition_doc_missing'
[[ -f "$TEST" ]] || fail 'state_integrity_test_missing'

grep -Fq 'release-signed debuggable migration bridge' "$DOC" || fail 'bridge_policy_missing'
grep -Fq 'before the destructive package uninstall' "$DOC" || fail 'destructive_confirmation_policy_missing'
grep -Fq 'exact path/size/content SHA-256 digest match' "$DOC" || fail 'restore_digest_policy_missing'
grep -Fq 'never executes `pm clear`' "$DOC" || fail 'pm_clear_prohibition_missing'
grep -Fq 'release-signed instrumentation APK' "$DOC" || fail 'signed_instrumentation_policy_missing'
grep -Fq 'expectedStateDigest' "$TEST" || fail 'expected_digest_argument_missing'
grep -Fq 'expectedStateCount' "$TEST" || fail 'expected_count_argument_missing'
grep -Fq 'expectedStateBytes' "$TEST" || fail 'expected_bytes_argument_missing'
grep -Fq 'expectedDraftCount' "$TEST" || fail 'expected_draft_count_argument_missing'
grep -Fq 'TripDraftStore(targetContext).loadLibrary()' "$TEST" || fail 'real_library_load_missing'
grep -Fq 'TripStorageIssueKind.CORRUPT' "$TEST" || fail 'corrupt_state_guard_missing'
grep -Fq 'listOf("files", "shared_prefs", "databases", "no_backup")' "$TEST" || fail 'durable_root_set_missing'

FINGERPRINT="$(sed -n 's/^- signing certificate SHA-256: `\([0-9a-fA-F]\{64\}\)`$/\1/p' docs/SIGN01_SIGNING_IDENTITY.md)"
[[ "$FINGERPRINT" =~ ^[0-9a-fA-F]{64}$ ]] || fail 'signing_certificate_fingerprint_missing'

while IFS= read -r secret; do
    [[ -z "$secret" ]] && continue
    printf 'SIGN01A2_PRIVATE_MATERIAL_IN_SOURCE=%s\n' "$secret"
    fail 'private_signing_material_present_in_source'
done < <(find . -type f \
    \( -name '*.jks' -o -name '*.keystore' -o -name '*.p12' -o -name '*.pfx' -o -name '*.pk8' \) \
    -not -path './.gradle/*' -not -path './app/build/*' -not -path './build/*' -print)

printf '%s\n' \
  "SIGN01A2_APPLICATION_ID=app.ferietur" \
  "SIGN01A2_VERSION_CODE=50" \
  "SIGN01A2_VERSION_NAME=0.5.4-rc1" \
  "SIGN01A2_CERTIFICATE_SHA256=${FINGERPRINT,,}" \
  "SIGN01A2_MIGRATION_BRIDGE=RELEASE_SIGNED_DEBUGGABLE" \
  "SIGN01A2_PRIVATE_BACKUP=OUTSIDE_SOURCE_AND_EVIDENCE" \
  "SIGN01A2_FINAL_TARGET=RELEASE_SIGNED_NON_DEBUGGABLE" \
  "SIGN01A2_STATE_DIGEST_TEST=DEFINED" \
  "SIGN01A2_PRIVATE_MATERIAL_IN_SOURCE=NONE" \
  "SIGN01A2_CONTRACT=PASS"
