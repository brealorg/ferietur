#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() {
    echo "SIGN01_CONTRACT=FAIL reason=$1"
    exit 1
}

DOC='docs/SIGN01_SIGNING_IDENTITY.md'
[[ -f "$DOC" ]] || fail 'signing_identity_doc_missing'

grep -Fq 'application ID: `app.ferietur`' "$DOC" || fail 'application_id_not_documented'
grep -Fq 'key algorithm: RSA 4096-bit' "$DOC" || fail 'rsa4096_not_documented'
grep -Fq 'keystore format: PKCS12' "$DOC" || fail 'pkcs12_not_documented'
grep -Fq 'key alias: `ferietur-release`' "$DOC" || fail 'alias_not_documented'
grep -Fq 'APK signing: v2 + v3' "$DOC" || fail 'apk_signing_schemes_not_documented'
grep -Fq '16 KiB alignment' "$DOC" || fail '16k_alignment_not_documented'
grep -Fq 'outside the canonical source tree' "$DOC" || fail 'external_keystore_policy_not_documented'
grep -Fq 'SIGN01A1 deliberately does not uninstall' "$DOC" || fail 'debug_lineage_origin_not_documented'
grep -Fq 'SIGN01A2 defines and qualifies that signature transition' "$DOC" || fail 'sign01a2_transition_not_documented'
grep -Fq 'hybrid PQC APK signing' "$DOC" || fail 'android17_pqc_decision_not_documented'

FINGERPRINT="$(sed -n 's/^- signing certificate SHA-256: `\([0-9a-fA-F]\{64\}\)`$/\1/p' "$DOC")"
[[ "$FINGERPRINT" =~ ^[0-9a-fA-F]{64}$ ]] || fail 'certificate_sha256_missing_or_invalid'
[[ "$FINGERPRINT" != '__SIGNING_CERT_SHA256__' ]] || fail 'certificate_placeholder_not_replaced'

grep -Fq 'versionCode = 50' app/build.gradle.kts || fail 'versionCode_changed'
grep -Fq 'versionName = "0.5.4-rc1"' app/build.gradle.kts || fail 'versionName_changed'
if grep -Eq 'signingConfigs|signingConfig[[:space:]]*=' app/build.gradle.kts; then
    fail 'private_signing_configuration_must_not_be_in_gradle_source'
fi

while IFS= read -r secret; do
    [[ -z "$secret" ]] && continue
    printf 'SIGN01_PRIVATE_MATERIAL_IN_SOURCE=%s\n' "$secret"
    fail 'private_signing_material_present_in_source'
done < <(find . -type f \
    \( -name '*.jks' -o -name '*.keystore' -o -name '*.p12' -o -name '*.pfx' -o -name '*.pk8' \) \
    -not -path './.gradle/*' -not -path './app/build/*' -not -path './build/*' -print)

for pattern in '*.jks' '*.keystore' '*.p12' '*.pfx' '*.pk8'; do
    grep -Fxq "$pattern" .gitignore || fail "gitignore_missing_${pattern//[^A-Za-z0-9]/_}"
done

printf '%s\n' \
  "SIGN01_APPLICATION_ID=app.ferietur" \
  "SIGN01_VERSION_CODE=50" \
  "SIGN01_VERSION_NAME=0.5.4-rc1" \
  "SIGN01_KEY_ALGORITHM=RSA_4096" \
  "SIGN01_KEYSTORE_FORMAT=PKCS12" \
  "SIGN01_KEY_ALIAS=ferietur-release" \
  "SIGN01_CERTIFICATE_SHA256=${FINGERPRINT,,}" \
  "SIGN01_PRIVATE_MATERIAL_IN_SOURCE=NONE" \
  "SIGN01_GRADLE_RELEASE_SIGNING=EXTERNAL_APKSIGNER" \
  "SIGN01_DEVICE_SIGNATURE_TRANSITION=SIGN01A2_DEFINED" \
  "SIGN01_CONTRACT=PASS"
