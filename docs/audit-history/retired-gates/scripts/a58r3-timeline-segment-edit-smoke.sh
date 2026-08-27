#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A58R3_TIMELINE_SEGMENT_EDIT_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

DAYCARD="$(sed -n '/private fun TripDayCard(/,/private fun PlanActivityRow(/p' "$UI")"
TIMELINE="$(sed -n '/private fun DayTimeline(/,/private fun RosterDayEditorSheet(/p' "$UI")"

printf '%s\n' "$DAYCARD" | grep -Fq 'DayTimeline(date, blocks, plans, onEditPeriod)' ||
    fail "timeline_edit_wiring_missing"

grep -Fq 'import androidx.compose.foundation.gestures.detectTapGestures' "$UI" ||
    fail "detect_tap_gestures_import_missing"
grep -Fq 'import androidx.compose.ui.input.pointer.pointerInput' "$UI" ||
    fail "pointer_input_import_missing"

printf '%s\n' "$TIMELINE" | grep -Fq 'plans: Map<LocalDate, List<PlannedBlock>>' ||
    fail "timeline_plans_missing"
printf '%s\n' "$TIMELINE" | grep -Fq 'onEditPeriod: (LocalDate, Int) -> Unit' ||
    fail "timeline_edit_callback_missing"
printf '%s\n' "$TIMELINE" | grep -Fq '.pointerInput(date, blocks, plans)' ||
    fail "timeline_pointer_input_missing"
printf '%s\n' "$TIMELINE" | grep -Fq 'detectTapGestures { tap ->' ||
    fail "timeline_tap_detector_missing"
printf '%s\n' "$TIMELINE" | grep -Fq 'val laneIndex = (tap.y / laneBand)' ||
    fail "timeline_lane_hit_testing_missing"
printf '%s\n' "$TIMELINE" | grep -Fq 'val hitPadding = 8.dp.toPx()' ||
    fail "timeline_horizontal_hit_padding_missing"
printf '%s\n' "$TIMELINE" | grep -Fq 'val sourceIndex = sourcePlanIndex(projected, plans)' ||
    fail "timeline_source_period_resolution_missing"
printf '%s\n' "$TIMELINE" | grep -Fq 'onEditPeriod(projected.sourceDate, sourceIndex)' ||
    fail "timeline_edit_invocation_missing"

# Empty track must not become a generic day-edit target.
printf '%s\n' "$TIMELINE" | grep -Fq 'tap.x >= startX - hitPadding && tap.x <= endX + hitPadding' ||
    fail "colored_segment_x_hit_test_missing"

# Existing row editing and unified editor remain intact.
grep -Fq 'Modifier.clickable { onEditPeriod(projected.sourceDate, sourceIndex) }' "$UI" ||
    fail "activity_row_editing_lost"
grep -Fq 'private fun PlanPeriodEditorSheet(' "$UI" ||
    fail "unified_period_editor_lost"
! grep -Fq 'private fun DayPlanEditorSheet(' "$UI" ||
    fail "legacy_day_editor_regressed"

echo "A58R3_TIMELINE_SEGMENT_EDIT_SMOKE=PASS"
