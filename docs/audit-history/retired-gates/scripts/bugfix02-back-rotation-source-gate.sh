#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
GRADLE="app/build.gradle.kts"

fail() {
    echo "BUGFIX02_BACK_ROTATION_SOURCE_GATE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"
[[ -f "$GRADLE" ]] || fail "app_build_gradle_missing"

echo "===== bug 1: system back gesture follows in-app hierarchy ====="

# Successor-safe: APPINFO01 adds `aboutOpen` as another root Back destination.
BACK_HANDLER_LINE="$(
    grep -F 'BackHandler(enabled =' "$UI" |
    grep -F 'tripOverviewOpen' |
    grep -F 'screen != FlowScreen.HOME' |
    head -1 || true
)"
[[ -n "$BACK_HANDLER_LINE" ]] ||
    fail "root_system_back_handler_missing"

grep -Fq 'if (tripOverviewOpen) {' "$UI" ||
    fail "trip_overview_back_branch_missing"
grep -Fq 'closeTripOverview()' "$UI" ||
    fail "trip_overview_close_action_missing"
grep -Fq 'goBack()' "$UI" ||
    fail "in_trip_back_action_missing"

grep -Fq 'if (directFromOverview) {' "$UI" ||
    fail "direct_from_overview_back_contract_missing"
grep -Fq 'screen = previousScreen(screen, fundingMode)' "$UI" ||
    fail "previous_step_back_contract_missing"

# If About exists, it must participate in the same root back hierarchy.
if grep -Fq 'val aboutOpen = mutableStateOf(false)' "$UI"; then
    grep -Fq 'aboutOpen -> aboutOpen = false' "$UI" ||
        fail "about_back_action_missing"
    grep -Fq 'BackHandler(enabled = aboutOpen || tripOverviewOpen || screen != FlowScreen.HOME)' "$UI" ||
        fail "about_not_in_root_back_handler"
fi

echo "BUG1_SYSTEM_BACK_GESTURE=LOCKED"

echo
echo "===== bug 2: configuration changes preserve active trip UI state ====="

grep -Fq 'import androidx.lifecycle.ViewModel' "$UI" ||
    fail "ViewModel_import_missing"
grep -Fq 'import androidx.lifecycle.viewmodel.compose.viewModel' "$UI" ||
    fail "viewModel_compose_import_missing"
grep -Fq 'FerieturSessionViewModel : ViewModel() {' "$UI" ||
    fail "session_ViewModel_missing"

SESSION_LINE="$(
    grep -F 'val session: FerieturSessionViewModel = viewModel' "$UI" |
    head -1 || true
)"
[[ -n "$SESSION_LINE" ]] ||
    fail "activity_scoped_session_ViewModel_not_used"

if grep -Fq 'private class FerieturSessionViewModel : ViewModel() {' "$UI"; then
    grep -Fq 'val session: FerieturSessionViewModel = viewModel { FerieturSessionViewModel() }' "$UI" ||
        fail "private_ViewModel_requires_explicit_initializer"
fi

for field in \
    currentTripId screen employerKind payingParty rosterComparisonMode tripTitle \
    startDate endDate startTime endTime salaryStep weeklyBasis weekendProfile \
    payslipChecked rosterGapConfirmed roster plans outboundArrival returnDeparture \
    outboundTravelKind returnTravelKind settlementMode settlementAmountText \
    settlementReason finalizedSnapshot tripOverviewOpen directFromOverview
do
    grep -Fq "var $field by session.$field" "$UI" ||
        fail "session_state_binding_missing_$field"
done

if grep -Fq 'val aboutOpen = mutableStateOf(false)' "$UI"; then
    grep -Fq 'var aboutOpen by session.aboutOpen' "$UI" ||
        fail "about_state_not_session_bound"
fi

if grep -Fq 'var screen by remember { mutableStateOf(FlowScreen.HOME) }' "$UI"; then
    fail "screen_still_composition_scoped"
fi
if grep -Fq 'var currentTripId by remember { mutableStateOf<String?>(null) }' "$UI"; then
    fail "active_trip_id_still_composition_scoped"
fi

grep -Fq 'implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")' "$GRADLE" ||
    fail "lifecycle_viewmodel_compose_dependency_missing"

echo "BUG2_CONFIGURATION_STATE=LOCKED"
echo "BUGFIX02_BACK_ROTATION_SOURCE_GATE=PASS"
