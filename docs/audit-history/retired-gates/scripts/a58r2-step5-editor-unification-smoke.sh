#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A58R2_STEP5_EDITOR_UNIFICATION_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

STEP5="$(sed -n '/private fun TripPlanScreen(/,/private fun CalculationScreen(/p' "$UI")"
DAYCARD="$(sed -n '/private fun TripDayCard(/,/private fun DayTimeline(/p' "$UI")"
FAB="$(sed -n '/private fun PlanFabMenu(/,/private fun PlanPeriodEditorSheet(/p' "$UI")"
EDITOR="$(sed -n '/private fun PlanPeriodEditorSheet(/,/private fun samePeriodCategory(/p' "$UI")"

# Main hierarchy.
printf '%s\n' "$STEP5" | grep -Fq 'ScreenHeader("Arbeidsplan", stepLabel, onBack)' ||
    fail "short_step_title_missing"
printf '%s\n' "$STEP5" | grep -Fq 'style = MaterialTheme.typography.bodyMedium' ||
    fail "compact_intro_typography_missing"

# Compact Expressive FAB menu.
printf '%s\n' "$FAB" | grep -Fq 'Text("Arbeid", style = MaterialTheme.typography.labelLarge)' ||
    fail "compact_work_label_missing"
printf '%s\n' "$FAB" | grep -Fq 'Text("Reise", style = MaterialTheme.typography.labelLarge)' ||
    fail "compact_travel_label_missing"
printf '%s\n' "$FAB" | grep -Fq 'Text("Hvilende natt", style = MaterialTheme.typography.labelLarge)' ||
    fail "compact_resting_label_missing"
printf '%s\n' "$FAB" | grep -Fq 'Text("Annen", style = MaterialTheme.typography.labelLarge)' ||
    fail "compact_other_label_missing"
printf '%s\n' "$FAB" | grep -Fq 'containerColor = { osloFabContainerColor }' ||
    fail "a58r1_color_fix_lost"

# One editor for both add and edit.
grep -Fq 'private fun PlanPeriodEditorSheet(' "$UI" ||
    fail "shared_period_editor_missing"
grep -Fq 'title = "Legg til periode"' "$UI" ||
    fail "shared_editor_not_used_for_add"
grep -Fq 'title = "Rediger periode"' "$UI" ||
    fail "shared_editor_not_used_for_edit"
grep -Fq 'var selectedPlanPeriod by remember' "$UI" ||
    fail "period_edit_target_state_missing"
grep -Fq 'onEditPeriod = { date, index -> selectedPlanPeriod = PlanPeriodEditTarget(date, index) }' "$UI" ||
    fail "period_row_edit_wiring_missing"

# Old editor generation is gone.
! grep -Fq 'private fun DayPlanEditorSheet(' "$UI" ||
    fail "legacy_day_editor_remains"
! grep -Fq 'private fun AddPeriodMenu(' "$UI" ||
    fail "legacy_add_period_menu_remains"
! grep -Fq 'private fun PeriodTypeMenu(' "$UI" ||
    fail "legacy_period_type_menu_remains"
! grep -Fq 'private fun AddPlanPeriodSheet(' "$UI" ||
    fail "separate_add_sheet_remains"
! grep -Fq 'selectedTripDate' "$UI" ||
    fail "legacy_selected_day_state_remains"
! grep -Fq 'Kopier turnusdelen som ligger i turen' "$UI" ||
    fail "legacy_copy_roster_button_remains"
! grep -Fq 'Sett dagen fri' "$UI" ||
    fail "legacy_set_day_free_button_remains"

# Every Step 5 time edit uses the already-approved Norwegian 24h TimeInput path.
printf '%s\n' "$EDITOR" | grep -Fq 'RosterTimeInputButton(' ||
    fail "24h_time_input_button_missing"
printf '%s\n' "$EDITOR" | grep -Fq 'TimeButton(' &&
    fail "dial_time_picker_path_still_used_in_period_editor"

# Existing periods are directly editable from compact activity rows.
printf '%s\n' "$DAYCARD" | grep -Fq 'onEditPeriod: (LocalDate, Int) -> Unit' ||
    fail "period_edit_callback_missing"
printf '%s\n' "$DAYCARD" | grep -Fq 'sourcePlanIndex(projected, plans)' ||
    fail "projected_source_period_resolution_missing"
printf '%s\n' "$DAYCARD" | grep -Fq 'contentDescription = "Rediger periode"' ||
    fail "period_edit_affordance_missing"

# Edit mode can save, move date and delete the real existing period.
grep -Fq 'source[target.index] = normalizeTravelSleepKind(targetDate, updatedBlock)' "$UI" ||
    fail "existing_period_replace_missing"
grep -Fq 'target.date to normalizedSource' "$UI" ||
    fail "period_move_source_update_missing"
grep -Fq 'targetDate to normalizedDestination' "$UI" ||
    fail "period_move_destination_update_missing"
printf '%s\n' "$EDITOR" | grep -Fq 'contentDescription = "Slett periode"' ||
    fail "delete_period_action_missing"
printf '%s\n' "$EDITOR" | grep -Fq 'Text("Lagre")' ||
    fail "save_period_action_missing"

# Travel/night semantics retained.
printf '%s\n' "$EDITOR" | grep -Fq '"Ansvar under reisen"' ||
    fail "travel_responsibility_missing"
printf '%s\n' "$EDITOR" | grep -Fq 'TravelNoticeSelector(' ||
    fail "travel_notice_selector_missing"
printf '%s\n' "$EDITOR" | grep -Fq 'NightTravelSleepSelector(' ||
    fail "night_sleep_selector_missing"
printf '%s\n' "$EDITOR" | grep -Fq '"Perioden slutter neste døgn."' ||
    fail "cross_midnight_help_missing"

echo "A58R2_STEP5_EDITOR_UNIFICATION_SMOKE=PASS"
