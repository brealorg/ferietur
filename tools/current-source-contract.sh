#!/usr/bin/env bash
set -u
set -o pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

fail() {
    echo "CURRENT_SOURCE_CONTRACT=FAIL reason=$1"
    exit 1
}

for f in \
    app/build.gradle.kts \
    app/src/main/AndroidManifest.xml \
    app/src/main/java/app/ferietur/ui/FerieturApp.kt \
    app/src/main/java/app/ferietur/ui/AppInfoPreferences.kt \
    app/src/main/java/app/ferietur/ui/AppInfoContact.kt \
    app/src/main/java/app/ferietur/domain/TripPlanEngine.kt \
    app/src/main/java/app/ferietur/domain/TariffCatalog.kt \
    app/src/main/java/app/ferietur/domain/TariffRateSet.kt \
    app/src/main/java/app/ferietur/domain/TariffResolution.kt \
    app/src/main/java/app/ferietur/domain/SavedTripDraft.kt \
    app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt \
    app/src/main/java/app/ferietur/domain/FinalizedTripSnapshotCodec.kt \
    app/src/main/java/app/ferietur/domain/SavedTripDraftMigrator.kt \
    app/src/main/java/app/ferietur/ui/TripLibraryPolicy.kt \
    app/src/main/java/app/ferietur/export/PdfExporter.kt \
    app/src/main/java/app/ferietur/data/TripDraftStore.kt \
    app/src/main/java/app/ferietur/data/TripRepository.kt \
    app/src/main/java/app/ferietur/export/PdfExportRepository.kt
 do
    [[ -f "$f" ]] || fail "missing_${f//\//_}"
 done

python3 - <<'PY2'
from pathlib import Path
import re

root = Path('.')
ui = (root/'app/src/main/java/app/ferietur/ui/FerieturApp.kt').read_text(encoding='utf-8')
prefs = (root/'app/src/main/java/app/ferietur/ui/AppInfoPreferences.kt').read_text(encoding='utf-8')
manifest = (root/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
gradle = (root/'app/build.gradle.kts').read_text(encoding='utf-8')
engine = (root/'app/src/main/java/app/ferietur/domain/TripPlanEngine.kt').read_text(encoding='utf-8')
tariff_catalog = (root/'app/src/main/java/app/ferietur/domain/TariffCatalog.kt').read_text(encoding='utf-8')
tariff_rates = (root/'app/src/main/java/app/ferietur/domain/TariffRateSet.kt').read_text(encoding='utf-8')
tariff_resolution = (root/'app/src/main/java/app/ferietur/domain/TariffResolution.kt').read_text(encoding='utf-8')
draft = (root/'app/src/main/java/app/ferietur/domain/SavedTripDraft.kt').read_text(encoding='utf-8')
finalized = (root/'app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt').read_text(encoding='utf-8')
snapshot_codec = (root/'app/src/main/java/app/ferietur/domain/FinalizedTripSnapshotCodec.kt').read_text(encoding='utf-8')
migrator = (root/'app/src/main/java/app/ferietur/domain/SavedTripDraftMigrator.kt').read_text(encoding='utf-8')
library_policy = (root/'app/src/main/java/app/ferietur/ui/TripLibraryPolicy.kt').read_text(encoding='utf-8')
store = (root/'app/src/main/java/app/ferietur/data/TripDraftStore.kt').read_text(encoding='utf-8')
repository = (root/'app/src/main/java/app/ferietur/data/TripRepository.kt').read_text(encoding='utf-8')
pdf_exporter = (root/'app/src/main/java/app/ferietur/export/PdfExporter.kt').read_text(encoding='utf-8')
pdf_repository = (root/'app/src/main/java/app/ferietur/export/PdfExportRepository.kt').read_text(encoding='utf-8')

def req(ok, reason):
    if not ok:
        raise SystemExit(f'CURRENT_SOURCE_CONTRACT=FAIL reason={reason}')

# Build/platform contract: version numbers may change; required capabilities may not.
req('namespace = "app.ferietur"' in gradle, 'namespace')
req('applicationId = "app.ferietur"' in gradle, 'applicationId')
req('androidx.lifecycle:lifecycle-viewmodel-compose:' in gradle, 'lifecycle_viewmodel_dependency')
req('androidx.datastore:datastore-preferences:' in gradle, 'datastore_dependency')
req('org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0' in gradle, 'direct_coroutines_dependency')

# Security/privacy contract.
req('android:allowBackup="false"' in manifest, 'allowBackup_false')
req('android:dataExtractionRules="@xml/data_extraction_rules"' in manifest, 'dataExtractionRules')
req('android:fullBackupContent="@xml/backup_rules"' in manifest, 'fullBackupContent')
req('android.permission.INTERNET' not in manifest, 'unexpected_INTERNET_permission')
req('androidx.core.content.FileProvider' in manifest, 'FileProvider_missing')
req('android:exported="false"' in manifest, 'nonexported_provider_contract')

# Activity-scoped UI state construction. Do not care about line formatting or source hash.
req(re.search(r'private\s+class\s+FerieturSessionViewModel\s*\([\s\S]*?TripRepository[\s\S]*?PdfExportRepository[\s\S]*?\)\s*:\s*ViewModel\(\)', ui), 'repository_backed_session_ViewModel')
req(re.search(r'val\s+session\s*:\s*FerieturSessionViewModel\s*=\s*viewModel\s*\{[\s\S]*?tripRepository\s*=\s*tripRepository[\s\S]*?pdfExportRepository\s*=\s*pdfExportRepository[\s\S]*?\}', ui), 'explicit_repository_ViewModel_initializer')
req('val session: FerieturSessionViewModel = viewModel()' not in ui, 'reflection_factory_regressed')

# Back hierarchy: find one BackHandler that is enabled for active flow/overview/About.
back_calls = re.findall(r'BackHandler\s*\(\s*enabled\s*=\s*([^\n)]*)\)\s*\{([\s\S]*?)\n\s*\}', ui)
req(any('screen != FlowScreen.HOME' in cond and 'tripOverviewOpen' in cond and 'aboutOpen' in cond
        for cond, body in back_calls), 'root_BackHandler_enablement')
req('aboutOpen -> aboutOpen = false' in ui, 'About_back_action')
req('closeTripOverview()' in ui, 'trip_overview_back_action')
req('screen = previousScreen(screen, fundingMode)' in ui, 'flow_previous_screen_action')

# First-run warning: semantic/accessibility contract rather than exact layout.
req('Role.Checkbox' in ui, 'disclaimer_checkbox_role')
req('checked = disclaimerConfirmed' in ui, 'disclaimer_checkbox_state')
req('onCheckedChange = null' in ui, 'delegated_checkbox_click')
req('Jeg har lest og forstått' in ui, 'disclaimer_checkbox_label')
req(re.search(r'enabled\s*=\s*disclaimerConfirmed\s*&&\s*!disclaimerWriteInProgress', ui), 'disclaimer_continue_gate')
req('dismissOnBackPress = false' in ui and 'dismissOnClickOutside = false' in ui, 'disclaimer_non_dismissible')
vm = re.search(r'CURRENT_DISCLAIMER_VERSION\s*:\s*Int\s*=\s*(\d+)', prefs)
req(vm is not None and int(vm.group(1)) >= 2, 'versioned_disclaimer_ack')
req('preferencesDataStore(name = "ferietur_app_info")' in prefs, 'preferences_DataStore')

# Contact dispatch: direct ACTION_SENDTO; do not depend on package-visibility preflight.
req('Intent(Intent.ACTION_SENDTO)' in ui, 'ACTION_SENDTO')
req('Uri.fromParts("mailto", APP_CONTACT_EMAIL, null)' in ui, 'mailto_recipient')
req('context.startActivity(emailIntent)' in ui, 'email_startActivity')
req('ActivityNotFoundException' in ui, 'email_launch_failure_handling')
req('resolveActivity(context.packageManager)' not in ui, 'resolveActivity_preflight_regressed')

# Current About information-detail structure. Scope the check to About only.
start = ui.find('@Composable\nprivate fun AboutFerieturScreen(')
end = ui.find('\n@Composable\nprivate fun HomeScreen(', start)
req(start >= 0 and end > start, 'About_screen_boundary')
about = ui[start:end]
req('Surface(' not in about, 'About_Surface_container_regressed')
req('OutlinedButton(' not in about, 'About_contact_card_button_regressed')
req('Send tilbakemelding' in about, 'About_contact_action')
req('Regler og beregningsgrunnlag' in about, 'About_rules_action')
req('BuildConfig.VERSION_NAME' in about and 'BuildConfig.VERSION_CODE' in about, 'About_version')
req('APP_CONTACT_EMAIL' not in re.sub(r'data\s*=\s*Uri\.fromParts\([^\n]+', '', about), 'contact_email_rendering_or_copy')

# Domain/data boundaries that must exist while the audit fixes are applied.
req('object TripPlanEngine' in engine or 'class TripPlanEngine' in engine, 'TripPlanEngine_missing')
req('data class SavedTripDraft' in draft, 'SavedTripDraft_missing')
req('FinalizedTripSnapshot' in finalized, 'FinalizedTripSnapshot_missing')
req('class TripDraftStore' in store, 'TripDraftStore_missing')
req('class TripRepository' in repository, 'TripRepository_missing')
req('class PdfExportRepository' in pdf_repository, 'PdfExportRepository_missing')

# CODEAUDIT FIX03 durable finalization/migration contract.
req('const val SCHEMA_VERSION = 6' in draft, 'schema_v6_missing')
req('val finalizedSnapshot: FinalizedTripSnapshot? = null' in draft, 'persisted_current_snapshot_field_missing')
req('val finalizationHistory: List<FinalizedTripSnapshot>' in draft, 'finalization_history_field_missing')
req('val migrationHistory: Set<String>' in draft, 'migration_provenance_field_missing')
req('FinalizedTripSnapshotCodec.encode' in draft and 'FinalizedTripSnapshotCodec.decode' in draft, 'snapshot_codec_not_wired_to_draft')
req('LEGACY_FORMAT_VERSION = 1' in snapshot_codec and 'FORMAT_VERSION = 2' in snapshot_codec, 'snapshot_codec_version_migration_missing')
req('writeCalculation' in snapshot_codec and 'readCalculation' in snapshot_codec, 'calculation_not_persisted_in_snapshot_codec')
req('UUID.fromString(snapshotId)' in finalized, 'snapshot_uuid_validation_missing')
req('UUID.randomUUID().toString()' in finalized, 'snapshot_uuid_default_missing')
req('val appVersionName: String' in finalized and 'val appVersionCode: Int' in finalized, 'snapshot_build_metadata_missing')
req('val rulesetVersion: String' in finalized, 'snapshot_ruleset_metadata_missing')
req('val tariffPackageId: String' in finalized and 'val tariffRateSetId: String' in finalized, 'snapshot_tariff_provenance_missing')
req('val salaryTableId: String' in finalized and 'val salaryTableEffectiveFrom: LocalDate' in finalized, 'snapshot_salary_table_metadata_missing')
req('BuildConfig.VERSION_NAME' in ui and 'appVersionName = BuildConfig.VERSION_NAME' in ui, 'production_build_version_not_frozen')
req('appVersionCode = BuildConfig.VERSION_CODE' in ui, 'production_build_code_not_frozen')
req('rulesetVersion = tariffPackage.rulesetVersion' in ui, 'production_ruleset_not_bound_to_tariff_package')
req('tariffPackageId = tariffPackage.id' in ui and 'tariffRateSetId = tariffRateSet.id' in ui, 'production_tariff_provenance_not_frozen')
req('DOK25_2026_2028_RULESET_VERSION = "2026.3"' in tariff_catalog, 'tariff_package_ruleset_not_immutable')
req('rulesetVersion = DOK25_2026_2028_RULESET_VERSION' in tariff_catalog, 'tariff_package_uses_moving_global_ruleset')
req('rateSetId:' not in tariff_catalog, 'tariff_package_still_owns_single_rate_set')
req('class TariffRateSetCatalog' in tariff_rates and 'fun requireForRange' in tariff_rates, 'effective_dated_rate_catalog_missing')
req('effectiveFrom: LocalDate' in tariff_rates and 'effectiveTo: LocalDate' in tariff_rates, 'rate_set_effective_dates_missing')
req('object FerieturTariffResolver' in tariff_resolution and 'ResolvedTariffContext' in tariff_resolution, 'coherent_tariff_resolver_missing')
req('FerieturTariffs.DOK25_2026_2028_RULESET_VERSION' in snapshot_codec, 'legacy_v1_tariff_inference_not_version_pinned')
req('FerieturTariffRates.requireById(snapshot.tariffRateSetId)' in pdf_exporter, 'PDF_not_bound_to_frozen_rate_set')
req('${s.tariffPackageId}' in pdf_exporter and '${s.tariffRateSetId}' in pdf_exporter, 'PDF_tariff_provenance_not_rendered')
req('salaryTableId = salaryTable.id' in ui and 'salaryTableEffectiveFrom = salaryTable.effectiveFrom' in ui, 'resolved_salary_table_not_frozen')
req('rebuildFinalizedSnapshot' not in ui, 'finalized_snapshot_still_rebuilt_on_reopen')
req('finalizedSnapshot = effectiveSaved.finalizedSnapshot' in ui, 'persisted_snapshot_not_restored')
req('archiveCurrentFinalizationForEdit' in ui, 'editing_does_not_archive_prior_finalization')
req('finalizationHistory = finalizationHistory + current' in ui, 'historical_snapshot_not_preserved')
req('currentDraft()?.let(session::persistDraft)' in ui, 'finalization_not_persisted_immediately')
req('LegacyTripRecovery' not in ui, 'runtime_legacy_recovery_regressed')
req(not (root/'app/src/main/java/app/ferietur/domain/LegacyTripRecovery.kt').exists(), 'LegacyTripRecovery_source_still_active')
req('solgården' not in migrator.lower(), 'named_development_trip_branch_regressed')
req('sourceSchemaVersion' in migrator and 'MIGRATION_V6_SCHEMA' in migrator, 'versioned_migration_missing')
req('MIGRATION_V6_REFINALIZE_LEGACY_SUMMARY' in migrator, 'legacy_summary_migration_missing')
req('readDecoded' in store and 'sourceSchemaVersion < SavedTripDraftCodec.SCHEMA_VERSION' in store, 'storage_boundary_migration_persist_missing')
req('schemaVersion=6' not in ui, 'schema_logic_leaked_into_UI')
req('finalizedSnapshot = null' in library_policy and 'finalizationHistory = emptyList()' in library_policy, 'duplicate_preserves_historical_finalization')
req('FERIETUR_APP_VERSION' not in pdf_exporter, 'hardcoded_app_version_regressed')
req('${s.appVersionName}' in pdf_exporter and '${s.appVersionCode}' in pdf_exporter, 'PDF_not_using_frozen_build_metadata')
req('${s.rulesetVersion}' in pdf_exporter, 'PDF_not_using_frozen_ruleset_metadata')
req('${s.salaryTableId}' in pdf_exporter, 'PDF_not_using_frozen_salary_table_metadata')
req('${snapshot.id}' in pdf_exporter and 'suffix.pdf' in pdf_exporter, 'export_filename_missing_snapshot_uuid')
print('FIX03_CA002_PERSISTED_FINALIZATION_CONTRACT=PASS')
print('FIX03_CA005_VERSIONED_MIGRATION_CONTRACT=PASS')
print('FIX03_CA008_UUID_EXPORT_IDENTITY_CONTRACT=PASS')
print('FIX03_CA009_FROZEN_BUILD_RULESET_RATE_METADATA_CONTRACT=PASS')

# CODEAUDIT FIX02 persistence/main-safety contract.
req('AtomicFile' in store, 'AtomicFile_missing')
req('.delete()' not in store[store.find('override fun save'):store.find('override fun delete')], 'precommit_delete_regressed')
req('finishWrite' in store and 'failWrite' in store, 'AtomicFile_commit_protocol_missing')
req('TripStorageIssueKind.RECOVERED' in store, 'backup_recovery_missing')
req('TripStorageIssueKind.CORRUPT' in store, 'corrupt_state_missing')
req('TripStorageIssueKind.UNSUPPORTED_SCHEMA' in store, 'unsupported_schema_state_missing')
req('UnsupportedSavedTripSchemaException' in draft, 'typed_unsupported_schema_failure_missing')
req('withContext(ioDispatcher)' in repository, 'TripRepository_not_main_safe')
req('Mutex()' in repository and 'withLock' in repository, 'AtomicFile_mutual_exclusion_missing')
req('Dispatchers.IO' in repository, 'TripRepository_IO_dispatcher_missing')
req('withContext(ioDispatcher)' in pdf_repository and 'Dispatchers.IO' in pdf_repository, 'Pdf_export_not_main_safe')
req('TripDraftStore' not in ui, 'UI_still_talks_directly_to_TripDraftStore')
req('draftStore.' not in ui, 'UI_direct_draft_IO_regressed')
req('PdfExporter.share(' not in ui, 'UI_blocking_PdfExporter_share_regressed')
req('PdfExporter.sharePrepared' in ui, 'prepared_pdf_share_missing')
req('viewModelScope.launch' in ui, 'ViewModel_owned_async_work_missing')
req('storageIssues' in ui and 'libraryLoaded' in ui, 'storage_issue_UI_state_missing')
req('PdfExportUiState' in ui and 'CREATING_SHORT' in ui and 'CREATING_FULL' in ui, 'export_progress_state_missing')
req('internal fun createBlocking' in pdf_exporter, 'PdfExporter_blocking_boundary_not_explicit')
req('fun sharePrepared' in pdf_exporter, 'prepared_share_API_missing')

print('FIX02_CA003_ATOMIC_RECOVERABLE_PERSISTENCE_CONTRACT=PASS')
print('FIX02_CA007_MAIN_SAFE_IO_CONTRACT=PASS')

# CODEAUDIT FIX01 correctness contract.
range_policy_path = root/'app/src/main/java/app/ferietur/domain/TripDateRangePolicy.kt'
salary_tables_path = root/'app/src/main/java/app/ferietur/domain/OsloSalaryTables.kt'
new_defaults_path = root/'app/src/main/java/app/ferietur/domain/NewTripDefaults.kt'
for path, reason in [
    (range_policy_path, 'TripDateRangePolicy_missing'),
    (salary_tables_path, 'OsloSalaryTables_missing'),
    (new_defaults_path, 'NewTripDefaults_missing'),
]:
    req(path.is_file(), reason)

range_policy = range_policy_path.read_text(encoding='utf-8')
salary_tables = salary_tables_path.read_text(encoding='utf-8')
new_defaults = new_defaults_path.read_text(encoding='utf-8')
salary2026 = (root/'app/src/main/java/app/ferietur/domain/OsloSalaryTable2026.kt').read_text(encoding='utf-8')
tariff_rates_path = root/'app/src/main/java/app/ferietur/domain/TariffRateSet.kt'
req(tariff_rates_path.is_file(), 'TariffRateSet_missing')

# CA-001
req('coerceAtMost(30)' not in ui, '31_day_truncation_regressed')
req('TripDateRangePolicy.inclusiveDates(start, end)' in ui, 'tripDates_not_using_range_policy')
req('fun inclusiveDates' in range_policy, 'inclusive_range_policy_missing')
req('fun requireCompleteCoverage' in range_policy, 'range_coverage_invariant_missing')
req('TripDateRangePolicy.requireCompleteCoverage' in ui, 'calculation_range_guard_missing')

# CA-004
req(ui.count('NewTripDefaultsFactory.current()') >= 2, 'dynamic_new_trip_defaults_missing')
req('tripTitle = "Sommerferie"' not in ui, 'Sommerferie_default_regressed')
req('LocalDate.of(2026, 8, 10)' not in ui[:ui.find('@Composable\nfun FerieturApp')], 'fixed_August_ViewModel_default_regressed')
req('val title: String' in new_defaults and 'title = ""' in new_defaults, 'blank_new_trip_title_missing')
req('Clock.systemDefaultZone()' in new_defaults, 'clock_based_defaults_missing')
req('ubekreftede startverdier' in ui, 'provisional_pay_defaults_not_disclosed')
req(re.search(r'FlowScreen\.PAY\s*->\s*payslipChecked', ui), 'pay_confirmation_gate_regressed')

# CA-006
req('effectiveFromDate: LocalDate' in salary2026, 'typed_salary_effective_date_missing')
req('object OsloSalaryTables' in salary_tables, 'effective_dated_salary_catalog_missing')
req('class SalaryTableCatalog' in salary_tables and 'class SalaryTablePeriod' in salary_tables, 'extensible_salary_catalog_missing')
req('earliestSupportedDate' in salary_tables, 'salary_earliest_supported_date_missing')
req('salaryRangeSupported' in ui and 'FerieturTariffResolver.supportsRange(startDate, endDate)' in ui, 'tariff_salary_range_support_state_missing')
req('FlowScreen.TRIP -> validRange && chapter20Applicable && salaryRangeSupported' in ui, 'unsupported_salary_range_not_blocking_flow')
req('SelectableDates' in ui and 'minimumDate' in ui, 'salary_min_date_picker_guard_missing')
req('FerieturTariffResolver.requireSupportedRange' in ui, 'coherent_tariff_calculation_guard_missing')
req('object FerieturTariffRates' in tariff_rates and 'DOK25_2026_2028_RATE_SET_ID' in tariff_rates, 'versioned_tariff_rate_set_missing')
req('rateSet: TariffRateSet = FerieturTariffRates.current' in engine, 'calculation_rate_set_not_injected')
req('private fun checkedPreliminaryCalculation' in ui and 'val tariffContext = FerieturTariffResolver.requireSupportedRange' in ui and 'val rateSet = tariffContext.rateSet' in ui and 'rateSet = rateSet' in ui, 'interactive_calculation_not_date_resolved_to_rate_set')
req('private fun tariffRateSetForRange' in ui and 'FerieturTariffResolver.requireSupportedRange(start, end).rateSet' in ui and 'TripPlanEngine.controlFindings(blocks, unresolvedCount, roster, rateSet)' in ui, 'interactive_control_findings_not_date_resolved_to_rate_set')
for token in [
    'rateSet.chapter20ActiveMultiplier',
    'rateSet.passiveWorkDivisor',
    'rateSet.stayAllowancePerDay',
    'rateSet.stayAllowanceRemainderThresholdMinutes',
    'rateSet.shortNoticeMaxMinutes',
    'rateSet.overtimeRoundingStepMinutes',
    'rateSet.overtimeStandardFraction',
    'rateSet.overtimeHighFraction',
    'rateSet.eveningStart',
    'rateSet.nightEnd',
    'rateSet.nightWatchSupplementEnd',
    'rateSet.travelSleepWindowStart',
    'rateSet.travelSleepWindowEnd',
]:
    req(token in engine, f'rate_set_value_not_consumed_{token.split(".")[-1]}')
for forbidden in [
    'BigDecimal("1.50")',
    'BigDecimal("110")',
    'BigDecimal("0.50")',
    'BigDecimal("1.3333333333")',
    'LocalTime.of(17, 0)',
    'LocalTime.of(6, 0)',
    'LocalTime.of(8, 0)',
    'LocalTime.of(23, 0)',
    'LocalTime.of(20, 0)',
]:
    req(forbidden not in engine, f'calculation_constant_leaked_back_into_engine_{forbidden}')
req('unsupportedSalaryRange' in ui and 'FlowScreen.TRIP' in ui, 'legacy_unsupported_salary_resume_guard_missing')
req('D25_20_6_EXACT_THRESHOLD' in engine, 'exact_six_hour_boundary_not_fail_closed')
req('stay-allowance-exact-threshold-open' in engine, 'exact_six_hour_open_line_missing')

print('FIX01_CA001_RANGE_CORRECTNESS_CONTRACT=PASS')
print('FIX01_CA004_NEW_TRIP_DEFAULTS_CONTRACT=PASS')
print('FIX01_CA006_SALARY_EFFECTIVE_DATE_CONTRACT=PASS')


print('CURRENT_UI_LIFECYCLE_CONTRACT=PASS')
print('CURRENT_APPINFO_CONTRACT=PASS')
print('CURRENT_SECURITY_MANIFEST_CONTRACT=PASS')
print('CURRENT_DOMAIN_BOUNDARY_CONTRACT=PASS')
PY2

RC=$?
[[ "$RC" -eq 0 ]] || exit "$RC"

echo "CURRENT_SOURCE_CONTRACT=PASS"
