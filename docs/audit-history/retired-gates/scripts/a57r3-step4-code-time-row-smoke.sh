#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
SHIFT="app/src/main/java/app/ferietur/domain/ShiftCatalog.kt"
TEST="app/src/test/java/app/ferietur/domain/ManualRosterEntryCodecTest.kt"

fail() {
    echo "A57R3_STEP4_CODE_AND_TIME_ROW_SMOKE=FAIL reason=$1"
    exit 1
}

for f in "$UI" "$SHIFT" "$TEST"; do
    [[ -f "$f" ]] || fail "missing_$f"
done

# New shift codes are real codes, not labels.
grep -Fq 'const val MAX_CODE_LENGTH = 3' "$SHIFT" || fail "max_code_not_three"
grep -Fq 'newManualEntriesRejectShiftCodesLongerThanThreeCharacters' "$TEST" || fail "three_char_regression_test_missing"
grep -Fq 'assertEquals(3, RosterEntryCodec.MAX_CODE_LENGTH)' "$TEST" || fail "three_char_test_expectation_missing"

# Compact week-row geometry gives the time column materially more room.
ROSTER_CARD="$(sed -n '/private fun RosterDayCard(/,/private fun TripDayCard(/p' "$UI")"
printf '%s\n' "$ROSTER_CARD" | grep -Fq 'Column(Modifier.width(74.dp))' || fail "day_column_not_compacted"
printf '%s\n' "$ROSTER_CARD" | grep -Fq 'Arrangement.spacedBy(10.dp)' || fail "row_gap_not_compacted"
printf '%s\n' "$ROSTER_CARD" | grep -Fq '.width(54.dp)' || fail "badge_not_compacted"
printf '%s\n' "$ROSTER_CARD" | grep -Fq 'val badge = if (shifts.size == 1) first.code else "${shifts.size}×"' || fail "multi_shift_badge_not_compact"
printf '%s\n' "$ROSTER_CARD" | grep -Fq 'style = MaterialTheme.typography.bodyMedium' || fail "time_summary_type_not_compact"

# The code is already in the badge; the summary must be time-only for one shift.
SUMMARY="$(sed -n '/private fun rosterDaySummary(/,/private fun shiftTimeLabel(/p' "$UI")"
printf '%s\n' "$SUMMARY" | grep -Fq 'shiftTimeLabel(first)' || fail "single_shift_time_summary_missing"
printf '%s\n' "$SUMMARY" | grep -Fq '"${shiftTimeLabel(first)} +${shifts.size - 1}"' || fail "multi_shift_summary_missing"
printf '%s\n' "$SUMMARY" | grep -Fq '${shift.code} · ${shiftTimeLabel(shift)}' && fail "duplicate_code_still_in_summary"
printf '%s\n' "$SUMMARY" | grep -Fq '${shift.code} ${shiftTimeLabel(shift)}' && fail "duplicate_multi_code_still_in_summary"

# Historical long A57 values still decode so the user can repair them.
grep -Fq 'preR2ManualEntryWithLongCodeRemainsReadableForEditing' "$TEST" || fail "long_code_decode_compatibility_lost"

echo "A57R3_STEP4_CODE_AND_TIME_ROW_SMOKE=PASS"
