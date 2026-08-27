#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A58R2R1_DAY_CARD_COMPILE_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

DAYCARD="$(sed -n '/private fun TripDayCard(/,/private fun DayTimeline(/p' "$UI")"

printf '%s\n' "$DAYCARD" | grep -Fq '.clickable(onClick = onClick)' &&
    fail "stale_day_level_onClick_reference_remains"

printf '%s\n' "$DAYCARD" | grep -Fq 'onEditPeriod: (LocalDate, Int) -> Unit' ||
    fail "period_edit_callback_missing"

printf '%s\n' "$DAYCARD" | grep -Fq 'Modifier.clickable { onEditPeriod(projected.sourceDate, sourceIndex) }' ||
    fail "period_row_edit_invocation_missing"

printf '%s\n' "$DAYCARD" | grep -Fq 'contentDescription = "Rediger periode"' ||
    fail "period_edit_affordance_missing"

grep -Fq 'private fun PlanPeriodEditorSheet(' "$UI" ||
    fail "shared_period_editor_missing"
! grep -Fq 'private fun DayPlanEditorSheet(' "$UI" ||
    fail "legacy_day_editor_regressed"

echo "A58R2R1_DAY_CARD_COMPILE_SMOKE=PASS"
