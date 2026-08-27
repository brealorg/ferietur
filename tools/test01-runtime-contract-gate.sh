#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() {
    echo "TEST01_CA010_RUNTIME_CONTRACT_GATE=FAIL reason=$1"
    exit 1
}

BUILD='app/build.gradle.kts'
TEST='app/src/androidTest/java/app/ferietur/RuntimeContractsTest.kt'

[[ -f "$BUILD" ]] || fail 'build_gradle_missing'
[[ -f "$TEST" ]] || fail 'runtime_contract_test_missing'

grep -Fq 'testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"' "$BUILD" || fail 'instrumentation_runner_missing'
grep -Fq 'androidTestImplementation("androidx.compose.ui:ui-test-junit4")' "$BUILD" || fail 'compose_ui_test_dependency_missing'
grep -Fq 'androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")' "$BUILD" || fail 'espresso_android17_compat_dependency_missing'
grep -Fq 'androidTestImplementation(composeBom)' "$BUILD" || fail 'compose_bom_android_test_missing'

for method in \
    coldLaunchDoesNotCrash \
    disclaimerAcceptancePersistsAcrossRestart \
    systemBackFollowsAppHierarchy \
    rotationRetainsActiveTripAndStep \
    storedDraftAndRecoverySurviveRealActivityLifecycle \
    pdfShareUsesValidFileProviderContentUri
 do
    grep -Fq "fun $method()" "$TEST" || fail "missing_$method"
 done

grep -Fq 'createEmptyComposeRule()' "$TEST" || fail 'empty_compose_rule_missing'
[[ "$(grep -Fc 'onNodeWithText("Ny tur", useUnmergedTree = true).performClick()' "$TEST")" -eq 2 ]] || fail 'ny_tur_unmerged_tree_click_contract_missing'
grep -Fq 'ActivityScenario.launch(MainActivity::class.java)' "$TEST" || fail 'activity_scenario_launch_missing'
grep -Fq 'onBackPressedDispatcher.onBackPressed()' "$TEST" || fail 'system_back_dispatch_missing'
grep -Fq 'requestedOrientation' "$TEST" || fail 'rotation_configuration_change_missing'
grep -Fq 'trip-draft-backups' "$TEST" || fail 'real_backup_recovery_fixture_missing'
grep -Fq 'not-a-valid-ferietur-draft' "$TEST" || fail 'corrupt_primary_recovery_fixture_missing'
grep -Fq 'PdfExporter.sharePrepared' "$TEST" || fail 'real_pdf_share_boundary_missing'
grep -Fq 'Intent.FLAG_GRANT_READ_URI_PERMISSION' "$TEST" || fail 'uri_permission_assertion_missing'
grep -Fq 'contentResolver.openInputStream' "$TEST" || fail 'fileprovider_resolvability_assertion_missing'
grep -Fq '__FERIETUR_TEST01_CA010__' "$TEST" || fail 'test_data_namespace_missing'
grep -Fq 'deleteDraftFiles' "$TEST" || fail 'test_data_cleanup_missing'

printf '%s\n' \
  'TEST01_01_COLD_LAUNCH_CONTRACT=DEFINED' \
  'TEST01_02_DISCLAIMER_RESTART_CONTRACT=DEFINED' \
  'TEST01_03_SYSTEM_BACK_CONTRACT=DEFINED' \
  'TEST01_04_ROTATION_STATE_CONTRACT=DEFINED' \
  'TEST01_05_DRAFT_RECOVERY_LIFECYCLE_CONTRACT=DEFINED' \
  'TEST01_06_FILEPROVIDER_SHARE_CONTRACT=DEFINED' \
  'TEST01_CA010_RUNTIME_CONTRACT_GATE=PASS'
