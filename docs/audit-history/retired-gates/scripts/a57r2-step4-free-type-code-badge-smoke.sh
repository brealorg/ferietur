#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A57R2_STEP4_FREE_TYPE_AND_CODE_BADGE_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

ROSTER="$(sed -n '/private fun RosterCodeTextField(/,/private fun RosterTimeInputButton(/p' "$UI")"
ROW="$(sed -n '/private fun RosterDayCard(/,/private fun TripDayCard(/p' "$UI")"

[[ -n "$ROSTER" ]] || fail "roster_editor_slice_missing"
[[ -n "$ROW" ]] || fail "roster_day_card_slice_missing"

# ---------------------------------------------------------------------------
# Vaktkode contract after A63 Google state-based migration.
# Android Developers recommends InputTransformation.maxLength(...) instead of
# filtering/truncating inside onValueChange for state-based TextFields.
# ---------------------------------------------------------------------------
grep -Fq 'private fun RosterCodeTextField(' "$UI" ||
    fail "RosterCodeTextField_missing"
grep -Fq 'rememberTextFieldState(initialText = value)' <<< "$ROSTER" ||
    fail "state_based_code_field_missing"
grep -Fq 'InputTransformation.maxLength(RosterEntryCodec.MAX_CODE_LENGTH)' <<< "$ROSTER" ||
    fail "work_code_input_not_bounded"
grep -Fq 'inputTransformation = codeInputTransformation' <<< "$ROSTER" ||
    fail "code_input_transformation_not_wired"
grep -Fq 'lineLimits = TextFieldLineLimits.SingleLine' <<< "$ROSTER" ||
    fail "code_single_line_contract_missing"
grep -Fq 'snapshotFlow { state.text.toString() }' <<< "$ROSTER" ||
    fail "code_outer_state_mirror_missing"

# Both editors use the same bounded component and keep existing state wiring.
grep -Fq 'value = draft.code' <<< "$ROSTER" ||
    fail "work_code_field_binding_missing"
grep -Fq 'draft.copy(code = value)' <<< "$ROSTER" ||
    fail "work_code_state_update_missing"
grep -Fq 'value = freeCode' <<< "$ROSTER" ||
    fail "free_code_field_binding_missing"
grep -Fq 'onValueChange = { freeCode = it }' <<< "$ROSTER" ||
    fail "free_code_state_update_missing"

# Save validation remains as defense in depth.
grep -Fq 'it.code.length <= RosterEntryCodec.MAX_CODE_LENGTH' <<< "$ROSTER" ||
    fail "work_code_save_validation_missing"
grep -Fq 'freeCode.length <= RosterEntryCodec.MAX_CODE_LENGTH' <<< "$ROSTER" ||
    fail "free_code_save_validation_missing"

# ---------------------------------------------------------------------------
# Fridagstype contract from A57R2.
# ---------------------------------------------------------------------------
grep -Fq 'title = "Ukentlig fridag"' <<< "$ROSTER" ||
    fail "weekly_off_option_missing"
grep -Fq 'title = "Annen eller ekstra fridag"' <<< "$ROSTER" ||
    fail "other_free_day_option_missing"
grep -Fq 'selected = weeklyOff == true' <<< "$ROSTER" ||
    fail "weekly_off_true_binding_missing"
grep -Fq 'selected = weeklyOff == false' <<< "$ROSTER" ||
    fail "weekly_off_false_binding_missing"
grep -Fq 'RosterEntryCodec.manualFree(' <<< "$ROSTER" ||
    fail "manual_free_encoding_missing"
grep -Fq 'requireNotNull(weeklyOff)' <<< "$ROSTER" ||
    fail "free_day_type_required_on_save"

# ---------------------------------------------------------------------------
# Compact vaktkode badge contract.
#
# The actual production component is RosterDayCard. The previous A63R1 gate
# accidentally searched for the non-existent historical name RosterDayRow,
# making ROW empty and causing the false "code_badge_value_missing" failure.
# ---------------------------------------------------------------------------
grep -Fq 'val badge = if (shifts.size == 1) first.code else "${shifts.size}×"' <<< "$ROW" ||
    fail "code_badge_value_missing"
grep -Fq '.width(54.dp)' <<< "$ROW" ||
    fail "code_badge_fixed_width_missing"
grep -Fq '.heightIn(min = 48.dp)' <<< "$ROW" ||
    fail "code_badge_touch_height_missing"
grep -Fq 'maxLines = 1' <<< "$ROW" ||
    fail "code_badge_single_line_missing"
grep -Fq 'overflow = TextOverflow.Ellipsis' <<< "$ROW" ||
    fail "code_badge_ellipsis_missing"

echo "A57R2_STEP4_FREE_TYPE_AND_CODE_BADGE_SMOKE=PASS"
