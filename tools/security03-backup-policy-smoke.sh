#!/usr/bin/env bash
set -u
set -o pipefail

MANIFEST="app/src/main/AndroidManifest.xml"
DATA="app/src/main/res/xml/data_extraction_rules.xml"
LEGACY="app/src/main/res/xml/backup_rules.xml"

fail() {
    echo "SECURITY03_BACKUP_POLICY_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$MANIFEST" ]] || fail "manifest_missing"
[[ -f "$DATA" ]] || fail "data_extraction_rules_missing"
[[ -f "$LEGACY" ]] || fail "legacy_backup_rules_missing"

grep -Fq 'android:allowBackup="false"' "$MANIFEST" ||
    fail "allowBackup_false_lost"
grep -Fq 'android:dataExtractionRules="@xml/data_extraction_rules"' "$MANIFEST" ||
    fail "dataExtractionRules_manifest_link_missing"
grep -Fq 'android:fullBackupContent="@xml/backup_rules"' "$MANIFEST" ||
    fail "fullBackupContent_manifest_link_missing"

for section in '<cloud-backup>' '<device-transfer>'; do
    grep -Fq "$section" "$DATA" || fail "data_rules_section_missing_${section}"
done

for domain in \
    root file database sharedpref external \
    device_root device_file device_database device_sharedpref
do
    COUNT="$(grep -Fc "<exclude domain=\"$domain\" path=\".\" />" "$DATA")"
    [[ "$COUNT" -eq 2 ]] ||
        fail "data_rules_domain_${domain}_expected_twice_found_${COUNT}"

    grep -Fq "<exclude domain=\"$domain\" path=\".\" />" "$LEGACY" ||
        fail "legacy_rules_domain_${domain}_missing"
done

echo "SECURITY03_BACKUP_POLICY_SMOKE=PASS"
