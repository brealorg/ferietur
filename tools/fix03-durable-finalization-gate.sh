#!/usr/bin/env bash
set -u
set -o pipefail

fail() {
    echo "FIX03_DURABLE_FINALIZATION_GATE=FAIL reason=$1"
    exit 1
}

DRAFT="app/src/main/java/app/ferietur/domain/SavedTripDraft.kt"
MIGRATOR="app/src/main/java/app/ferietur/domain/SavedTripDraftMigrator.kt"
FINAL="app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt"
CODEC="app/src/main/java/app/ferietur/domain/FinalizedTripSnapshotCodec.kt"
STORE="app/src/main/java/app/ferietur/data/TripDraftStore.kt"
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
PDF="app/src/main/java/app/ferietur/export/PdfExporter.kt"
LIB="app/src/main/java/app/ferietur/ui/TripLibraryPolicy.kt"

for f in "$DRAFT" "$MIGRATOR" "$FINAL" "$CODEC" "$STORE" "$UI" "$PDF" "$LIB"; do
    [[ -f "$f" ]] || fail "missing_${f//\//_}"
done

grep -Fq 'const val SCHEMA_VERSION = 6' "$DRAFT" ||
    fail "schema_v6_missing"
grep -Fq 'val finalizedSnapshot: FinalizedTripSnapshot? = null' "$DRAFT" ||
    fail "persisted_snapshot_missing"
grep -Fq 'val finalizationHistory: List<FinalizedTripSnapshot>' "$DRAFT" ||
    fail "history_missing"
grep -Fq 'val migrationHistory: Set<String>' "$DRAFT" ||
    fail "migration_provenance_missing"

grep -Fq 'object FinalizedTripSnapshotCodec' "$CODEC" ||
    fail "snapshot_codec_missing"
grep -Fq 'writeCalculation' "$CODEC" ||
    fail "calculation_not_encoded"
grep -Fq 'readCalculation' "$CODEC" ||
    fail "calculation_not_decoded"

grep -Fq 'UUID.randomUUID().toString()' "$FINAL" ||
    fail "uuid_identity_missing"
grep -Fq 'UUID.fromString(snapshotId)' "$FINAL" ||
    fail "uuid_validation_missing"
grep -Fq 'val appVersionName: String' "$FINAL" ||
    fail "app_version_metadata_missing"
grep -Fq 'val rulesetVersion: String' "$FINAL" ||
    fail "ruleset_metadata_missing"
grep -Fq 'val salaryTableId: String' "$FINAL" ||
    fail "salary_table_metadata_missing"

grep -Fq 'appVersionName = BuildConfig.VERSION_NAME' "$UI" ||
    fail "production_BuildConfig_version_missing"
grep -Fq 'appVersionCode = BuildConfig.VERSION_CODE' "$UI" ||
    fail "production_BuildConfig_code_missing"
grep -Fq 'rulesetVersion = FERIETUR_RULESET_VERSION' "$UI" ||
    fail "production_ruleset_missing"
grep -Fq 'finalizedSnapshot = effectiveSaved.finalizedSnapshot' "$UI" ||
    fail "persisted_snapshot_not_restored"
grep -Fq 'archiveCurrentFinalizationForEdit' "$UI" ||
    fail "edit_history_archival_missing"
grep -Fq 'finalizationHistory = finalizationHistory + current' "$UI" ||
    fail "historical_snapshot_not_preserved"
grep -Fq 'currentDraft()?.let(session::persistDraft)' "$UI" ||
    fail "finalization_not_immediately_persisted"

if grep -Fq 'rebuildFinalizedSnapshot' "$UI"; then
    fail "rebuild_on_reopen_regressed"
fi
if grep -Fq 'LegacyTripRecovery' "$UI"; then
    fail "runtime_legacy_recovery_regressed"
fi
[[ ! -f app/src/main/java/app/ferietur/domain/LegacyTripRecovery.kt ]] ||
    fail "LegacyTripRecovery_file_still_active"
[[ ! -f app/src/test/java/app/ferietur/domain/LegacyTripRecoveryTest.kt ]] ||
    fail "LegacyTripRecoveryTest_still_active"

if grep -Fiq 'solgården' "$MIGRATOR"; then
    fail "named_development_trip_branch_regressed"
fi
grep -Fq 'sourceSchemaVersion' "$MIGRATOR" ||
    fail "versioned_migration_missing"
grep -Fq 'MIGRATION_V6_SCHEMA' "$MIGRATOR" ||
    fail "migration_schema_marker_missing"
grep -Fq 'MIGRATION_V6_REFINALIZE_LEGACY_SUMMARY' "$MIGRATOR" ||
    fail "legacy_summary_refinalization_missing"

grep -Fq 'readDecoded' "$STORE" ||
    fail "store_not_using_decoded_schema_metadata"
grep -Fq 'sourceSchemaVersion < SavedTripDraftCodec.SCHEMA_VERSION' "$STORE" ||
    fail "one_time_storage_boundary_persist_missing"

grep -Fq 'finalizedSnapshot = null' "$LIB" ||
    fail "duplicate_keeps_finalized_snapshot"
grep -Fq 'finalizationHistory = emptyList()' "$LIB" ||
    fail "duplicate_keeps_finalization_history"

if grep -Fq 'FERIETUR_APP_VERSION' "$PDF"; then
    fail "hardcoded_app_version_regressed"
fi
grep -Fq '${s.appVersionName}' "$PDF" ||
    fail "PDF_not_using_frozen_app_version"
grep -Fq '${s.rulesetVersion}' "$PDF" ||
    fail "PDF_not_using_frozen_ruleset"
grep -Fq '${s.salaryTableId}' "$PDF" ||
    fail "PDF_not_using_frozen_salary_table"
grep -Fq '${snapshot.id}' "$PDF" ||
    fail "export_filename_missing_snapshot_uuid"

echo "FIX03_CA002_PERSISTED_FINALIZATION=PASS"
echo "FIX03_CA005_VERSIONED_MIGRATION=PASS"
echo "FIX03_CA008_UUID_EXPORT_IDENTITY=PASS"
echo "FIX03_CA009_FROZEN_METADATA=PASS"
echo "FIX03_DURABLE_FINALIZATION_GATE=PASS"
