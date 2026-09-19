#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() { echo "PLAY01_CONTRACT=FAIL reason=$1"; exit 1; }

for f in \
    README.md \
    SOURCE-SHA256SUMS.txt \
    docs/PLAY01_GOOGLE_PLAY.md \
    play/privacy-policy.html \
    play/privacy-policy.md \
    play/store-listing-nb-NO.md \
    play/data-safety-draft.md \
    play/app-content-draft.md \
    play/play-app-signing.md \
    app/src/main/java/app/ferietur/ui/AppInfoContact.kt \
    app/src/main/java/app/ferietur/ui/FerieturApp.kt \
    tools/verify-source-sha256.sh
 do
    [[ -f "$f" ]] || fail "missing_${f//\//_}"
 done

GRADLE='app/build.gradle.kts'
MANIFEST='app/src/main/AndroidManifest.xml'
UI='app/src/main/java/app/ferietur/ui/FerieturApp.kt'
INFO='app/src/main/java/app/ferietur/ui/AppInfoContact.kt'
EXPECTED_CERT='9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7'
PRIVACY_URL='https://brealorg.github.io/ferietur/privacy/'

# GATE03: README is now a public product page and the version moves with each release.
# The former assertions on internal README status wording (CA-012/CA-010/schema v6/PLAY01) and
# on a pinned versionCode/versionName were retired; release/current.env records versions.
grep -qx '# Ferietur' README.md || fail 'README_not_canonical'

grep -Fq 'applicationId = "app.ferietur"' "$GRADLE" || fail 'application_id_changed'
grep -Fq 'targetSdk = 37' "$GRADLE" || fail 'target_sdk_regressed'
grep -Fq 'bundle {' "$GRADLE" || fail 'bundle_block_missing'
grep -Fq 'language {' "$GRADLE" || fail 'bundle_language_block_missing'
grep -Fq 'enableSplit = false' "$GRADLE" || fail 'language_split_not_disabled'
! grep -Eq '^[[:space:]]*signingConfigs[[:space:]]*\{' "$GRADLE" || fail 'private_signing_config_must_not_enter_gradle'

grep -Fq "APP_PRIVACY_POLICY_URL: String = \"$PRIVACY_URL\"" "$INFO" || fail 'privacy_policy_URL_missing'
grep -Fq 'Les personvernerklæringen' "$UI" || fail 'privacy_policy_in_app_action_missing'
grep -Fq 'har ingen annonser eller analyseverktøy' "$UI" || fail 'privacy_no_ads_analytics_copy_missing'
grep -Fq 'PDF-er deles bare når du selv velger det.' "$UI" || fail 'privacy_user_initiated_export_copy_missing'
grep -Fq 'Intent(Intent.ACTION_VIEW, Uri.parse(APP_PRIVACY_POLICY_URL))' "$UI" || fail 'privacy_external_intent_missing'
! grep -Fq 'android.permission.INTERNET' "$MANIFEST" || fail 'unexpected_INTERNET_permission'

grep -Fq 'Personvernerklæring for Ferietur' play/privacy-policy.html || fail 'privacy_title_missing'
grep -Fq 'enaasen@gmail.com' play/privacy-policy.html || fail 'privacy_contact_missing'
grep -Fq 'Oppbevaring og sletting' play/privacy-policy.html || fail 'privacy_retention_deletion_missing'
grep -Fq 'har ingen annonser, analyseverktøy eller sporing' play/privacy-policy.html || fail 'privacy_collection_disclosure_missing'
grep -Fq "$PRIVACY_URL" play/store-listing-nb-NO.md || fail 'store_privacy_URL_missing'
grep -Fq 'er ikke en offisiell app fra Oslo kommune' play/store-listing-nb-NO.md || fail 'government_disclaimer_missing'
grep -Fq 'https://www.oslo.kommune.no/jobb-i-oslo-kommune/tariffoppgjor/' play/store-listing-nb-NO.md || fail 'official_tariff_source_missing'
grep -Fq 'https://www.oslo.kommune.no/jobb-i-oslo-kommune/' play/store-listing-nb-NO.md || fail 'official_salary_source_missing'
grep -Fq "$EXPECTED_CERT" play/play-app-signing.md || fail 'existing_app_signing_identity_missing'

grep -Fq 'enableSplit = false' docs/ADR01_DEPENDENCY_DECISIONS.md || fail 'CA014_ADR_closure_missing'
grep -Fq 'FIXED for Play AAB distribution in PLAY01' docs/ADR01_DEPENDENCY_DECISIONS.md || fail 'CA014_ADR_decision_missing'

while IFS= read -r secret; do
    [[ -z "$secret" ]] && continue
    printf 'PLAY01_PRIVATE_MATERIAL_IN_SOURCE=%s\n' "$secret"
    fail 'private_signing_material_present_in_source'
done < <(find . -type f \
    \( -name '*.jks' -o -name '*.keystore' -o -name '*.p12' -o -name '*.pfx' -o -name '*.pk8' \) \
    -not -path './.gradle/*' -not -path './app/build/*' -not -path './build/*' -print)

"$ROOT/tools/release01-contract.sh" >/dev/null || fail 'release01_contract_regressed'
"$ROOT/tools/verify-source-sha256.sh" >/dev/null || fail 'canonical_source_manifest_verification_failed'

printf '%s\n' \
  'PLAY01_APPLICATION_ID=app.ferietur' \
  "PLAY01_VERSION_CODE=$(sed -n 's/^[[:space:]]*versionCode = \([0-9]*\)$/\1/p' "$GRADLE")" \
  "PLAY01_VERSION_NAME=$(sed -n 's/^[[:space:]]*versionName = "\(.*\)"$/\1/p' "$GRADLE")" \
  'PLAY01_TARGET_SDK=37' \
  'PLAY01_CA014_LANGUAGE_SPLIT=FIXED' \
  "PLAY01_PRIVACY_POLICY_URL=$PRIVACY_URL" \
  "PLAY01_EXPECTED_APP_SIGNING_CERTIFICATE_SHA256=$EXPECTED_CERT" \
  'PLAY01_INTERNET_PERMISSION=ABSENT' \
  'PLAY01_PRIVATE_MATERIAL_IN_SOURCE=NONE' \
  'PLAY01_CONTRACT=PASS'
