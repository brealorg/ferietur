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
    app/src/main/java/app/ferietur/domain/TariffSegmentation.kt \
    app/src/main/java/app/ferietur/domain/TariffSegmentCalculation.kt \
    app/src/main/java/app/ferietur/domain/TariffWholeTripScope.kt \
    app/src/main/java/app/ferietur/domain/TariffSegmentedMonetaryCalculation.kt \
    app/src/main/java/app/ferietur/domain/TariffRuntimeCalculation.kt \
    app/src/main/java/app/ferietur/domain/OsloSalaryTables.kt \
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
tariff_segmentation = (root/'app/src/main/java/app/ferietur/domain/TariffSegmentation.kt').read_text(encoding='utf-8')
tariff_segment_calculation = (root/'app/src/main/java/app/ferietur/domain/TariffSegmentCalculation.kt').read_text(encoding='utf-8')
tariff_whole_trip_scope = (root/'app/src/main/java/app/ferietur/domain/TariffWholeTripScope.kt').read_text(encoding='utf-8')
tariff_segmented_monetary = (root/'app/src/main/java/app/ferietur/domain/TariffSegmentedMonetaryCalculation.kt').read_text(encoding='utf-8')
tariff_runtime = (root/'app/src/main/java/app/ferietur/domain/TariffRuntimeCalculation.kt').read_text(encoding='utf-8')
salary_tables = (root/'app/src/main/java/app/ferietur/domain/OsloSalaryTables.kt').read_text(encoding='utf-8')
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
req('LEGACY_FORMAT_VERSION = 1' in snapshot_codec and 'SINGLE_CONTEXT_FORMAT_VERSION = 2' in snapshot_codec and 'FORMAT_VERSION = 3' in snapshot_codec, 'snapshot_codec_version_migration_missing')
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
req('class TariffSegmentPlanner' in tariff_segmentation, 'tariff_segment_planner_missing')
req('SEMANTIC_RULESET_CHANGE' in tariff_segmentation, 'semantic_ruleset_split_gate_missing')
req('fun planSegments' in tariff_resolution and 'TariffSegmentationResult' in tariff_resolution, 'segmented_tariff_resolver_API_missing')
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
print('CURRENT_TARIFF_SEGMENTATION_CONTRACT=PASS')
req('data class TariffEffectiveDateRange' in tariff_segmentation, 'effective_trip_date_range_missing')
req('tripEnd.toLocalTime() == LocalTime.MIDNIGHT' in tariff_segmentation, 'midnight_end_exclusive_policy_missing')
req('data class TariffCalculationSlice' in tariff_segment_calculation, 'tariff_calculation_slice_missing')
req('data class SegmentedWorkBlock' in tariff_segment_calculation and 'sourceIndex' in tariff_segment_calculation, 'segment_block_provenance_missing')
req('class TariffCalculationSliceBuilder' in tariff_segment_calculation, 'segment_slice_builder_missing')
req('WORK_BLOCK_OUTSIDE_TRIP' in tariff_segment_calculation, 'silent_outside_trip_clipping_guard_missing')
req('TariffMath.hourlyRate(annualSalary, weeklyBasis, segment.rateSet)' in tariff_segment_calculation, 'per_segment_hourly_rate_resolution_missing')
req('annualSalaryForTable' in salary_tables, 'frozen_salary_table_lookup_missing')
req('fun planSegments(tripStart: LocalDateTime, tripEnd: LocalDateTime)' in tariff_resolution, 'datetime_segment_planner_missing')
print('CURRENT_TARIFF_SEGMENT_INPUT_CONTRACT=PASS')
projected_core_start = engine.find('fun calculatePreliminaryFromProjectedBlocks(')
projected_core_end = engine.find('\n    fun rosterUncoveredEvidence(', projected_core_start)
req(projected_core_start >= 0 and projected_core_end > projected_core_start, 'projected_block_calculation_core_missing')
projected_core = engine[projected_core_start:projected_core_end]
req('blocks: List<WorkBlock>' in projected_core, 'projected_block_input_missing')
req('projectRange(' not in projected_core, 'projected_core_reprojects_blocks')
req('return calculatePreliminaryFromProjectedBlocks(' in engine, 'planned_entrypoint_not_delegating_to_projected_core')
req('val blocks = projectRange(dates, plans)' in engine, 'planned_entrypoint_projection_missing')
print('CURRENT_PROJECTED_BLOCK_CALCULATION_CORE_CONTRACT=PASS')
# A4A4 calculation-scope ownership contract.
req('object TariffCalculationLineScopes' in tariff_whole_trip_scope, 'calculation_line_scope_catalog_missing')
req('object TariffWholeTripScopeCoordinator' in tariff_whole_trip_scope, 'whole_trip_scope_coordinator_missing')
req('INCONSISTENT_STAY_ALLOWANCE_POLICY' in tariff_whole_trip_scope, 'stay_allowance_transition_gate_missing')
req('INCONSISTENT_SHORT_NOTICE_POLICY' in tariff_whole_trip_scope, 'short_notice_transition_gate_missing')
req('ACTIVE_EVENT_RATE_ALLOCATION_REQUIRED' in tariff_whole_trip_scope, 'active_event_cross_rate_gate_missing')
engine_line_ids = set(re.findall(r'id\s*=\s*"([^"]+)"', projected_core))
scope_line_ids = set(re.findall(r'"([^"]+)"\s+to\s+TariffCalculationLineScope\.', tariff_whole_trip_scope))
req(engine_line_ids == scope_line_ids, 'calculation_line_scope_catalog_not_exhaustive')
print('CURRENT_TARIFF_WHOLE_TRIP_SCOPE_CONTRACT=PASS')
# A4A5 segmented monetary coordination contract.
req('object TariffSourceBlockReconstructor' in tariff_whole_trip_scope, 'source_block_reconstructor_not_shared')
req('object TariffSegmentedMonetaryCoordinator' in tariff_segmented_monetary, 'segmented_monetary_coordinator_missing')
req('TariffWholeTripScopeCoordinator.coordinate(plan)' in tariff_segmented_monetary, 'whole_trip_scope_gate_not_used_by_monetary_coordinator')
req('TariffCalculationLineScope.SEGMENT_LOCAL' in tariff_segmented_monetary, 'segment_local_line_filter_missing')
req('wholeTripStayLines(plan)' in tariff_segmented_monetary, 'whole_trip_stay_coordination_missing')
req('watchSourceIndex = watchScope.watchSourceIndex' in tariff_segmented_monetary, 'per_watch_line_provenance_missing')
req('SPLIT_TRAVEL_NOTICE_COORDINATION_REQUIRED' in tariff_segmented_monetary, 'split_short_notice_fail_closed_gate_missing')
req('TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY' in tariff_segmented_monetary, 'known_travel_split_gate_missing')
req('calculatePreliminaryFromProjectedBlocks(' in tariff_segmented_monetary, 'projected_core_not_reused_by_segmented_monetary')
req('app/src/main/java/app/ferietur/ui/FerieturApp.kt' not in tariff_segmented_monetary, 'ui_dependency_leaked_into_segmented_monetary_domain')
print('CURRENT_TARIFF_SEGMENTED_MONETARY_CONTRACT=PASS')
# A4A6 runtime calculation gateway contract.
req('object FerieturTariffRuntimeCalculator' in tariff_runtime, 'tariff_runtime_calculator_missing')
req('TariffRuntimeCalculation.SingleContext' in tariff_runtime, 'single_context_runtime_result_missing')
req('TariffRuntimeCalculation.SegmentedContexts' in tariff_runtime, 'segmented_runtime_result_missing')
req('FerieturTariffResolver.planSegments(tripStart, tripEnd)' in tariff_runtime, 'runtime_not_using_half_open_segment_resolver')
req('FerieturTariffCalculationSlices.build(' in tariff_runtime, 'runtime_slice_plan_builder_missing')
req('TariffSegmentedMonetaryCoordinator.calculate(' in tariff_runtime, 'runtime_segmented_money_coordinator_missing')
req('TripPlanEngine.calculatePreliminaryFromProjectedBlocks(' in tariff_runtime, 'runtime_single_context_parity_path_missing')
req('TariffRuntimeProvenanceSlice' in tariff_runtime and 'salaryTableId' in tariff_runtime and 'tariffRateSetId' in tariff_runtime, 'runtime_multi_context_provenance_missing')
req('WORK_BLOCK_OVERLAP' in tariff_runtime and 'WORK_BLOCK_OUTSIDE_TRIP' in tariff_runtime, 'runtime_plan_validation_gate_missing')
req('MONETARY_COORDINATION_FAILED' in tariff_runtime and 'segmentationReason' in tariff_runtime and 'monetaryReason' in tariff_runtime, 'runtime_typed_failure_mapping_missing')
req('app/src/main/java/app/ferietur/ui/FerieturApp.kt' not in tariff_runtime, 'ui_dependency_leaked_into_runtime_domain_gateway')
print('CURRENT_TARIFF_RUNTIME_CALCULATION_CONTRACT=PASS')
# A4A7 multi-context finalized snapshot provenance contract.
req('data class FinalizedTariffContextSnapshot' in finalized, 'finalized_tariff_context_snapshot_missing')
req('val tariffContexts: List<FinalizedTariffContextSnapshot>' in finalized, 'snapshot_multi_context_provenance_field_missing')
req('FinalizedTariffContextSnapshot.fromRuntime' in finalized, 'runtime_to_snapshot_provenance_bridge_missing')
req('writeList(snapshot.tariffContexts) { writeTariffContext(it) }' in snapshot_codec, 'snapshot_v3_tariff_contexts_not_written')
req('readList { readTariffContext() }' in snapshot_codec, 'snapshot_v3_tariff_contexts_not_read')
req('legacySingleContext(' in snapshot_codec, 'legacy_v1_v2_context_synthesis_missing')
req('version == SINGLE_CONTEXT_FORMAT_VERSION' in snapshot_codec, 'snapshot_v2_read_compat_missing')
req('version == LEGACY_FORMAT_VERSION' in snapshot_codec, 'snapshot_v1_read_compat_missing')
print('CURRENT_MULTI_CONTEXT_SNAPSHOT_PROVENANCE_CONTRACT=PASS')

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
