#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A58R1R1_STEP5_EXPRESSIVE_WORK_PLAN_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

STEP5="$(sed -n '/private fun TripPlanScreen(/,/private fun CalculationScreen(/p' "$UI")"
DAYCARD="$(sed -n '/private fun TripDayCard(/,/private fun DayTimeline(/p' "$UI")"
FAB="$(sed -n '/private fun PlanFabMenu(/,/private fun PlanPeriodEditorSheet(/p' "$UI")"
EDITOR="$(sed -n '/private fun PlanPeriodEditorSheet(/,/private fun samePeriodCategory(/p' "$UI")"

printf '%s\n' "$STEP5" | grep -Fq 'Registrer det du faktisk gjør på turen.' ||
    fail "compact_intro_missing"
printf '%s\n' "$DAYCARD" | grep -Fq 'PlanActivityRow(' ||
    fail "compact_activity_rows_missing"
printf '%s\n' "$DAYCARD" | grep -Fq 'lønnsekvivalent' &&
    fail "calculation_detail_still_on_step5"

printf '%s\n' "$FAB" | grep -Fq 'FloatingActionButtonMenu(' || fail "fab_menu_missing"
printf '%s\n' "$FAB" | grep -Fq 'ToggleFloatingActionButton(' || fail "toggle_fab_missing"
printf '%s\n' "$FAB" | grep -Fq 'FloatingActionButtonMenuItem(' || fail "fab_menu_items_missing"
printf '%s\n' "$FAB" | grep -Fq 'containerColor = { osloFabContainerColor }' ||
    fail "fab_color_compile_fix_missing"

printf '%s\n' "$EDITOR" | grep -Fq 'ModalBottomSheet(onDismissRequest = onDismiss)' ||
    fail "period_editor_sheet_missing"
printf '%s\n' "$EDITOR" | grep -Fq 'RosterTimeInputButton(' ||
    fail "material_time_input_entry_missing"
printf '%s\n' "$EDITOR" | grep -Fq 'TravelNoticeSelector(' ||
    fail "travel_notice_logic_missing"

grep -Fq 'plans[date].orEmpty() + normalizeTravelSleepKind(date, block)' "$UI" ||
    fail "real_plan_append_missing"

echo "A58R1R1_STEP5_EXPRESSIVE_WORK_PLAN_SMOKE=PASS"
