#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A63_ANDROID_DEV_CONSISTENCY_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

# State-based text fields.
grep -Fq 'private fun TripTitleTextField(' "$UI" || fail "TripTitleTextField_missing"
grep -Fq 'private fun RosterCodeTextField(' "$UI" || fail "RosterCodeTextField_missing"
grep -Fq 'InputTransformation.maxLength(RosterEntryCodec.MAX_CODE_LENGTH)' "$UI" ||
    fail "roster_code_InputTransformation_missing"
grep -Fq 'snapshotFlow { state.text.toString() }' "$UI" ||
    fail "TextFieldState_outer_mirror_missing"

TRIP_MAIN="$(sed -n '/private fun TripBasicsScreen(/,/private fun TripTitleTextField(/p' "$UI")"
grep -Fq 'TripTitleTextField(' <<< "$TRIP_MAIN" || fail "trip_title_state_field_not_used"
if grep -Fq 'OutlinedTextField(' <<< "$TRIP_MAIN"; then
    fail "trip_title_old_inline_field_regressed"
fi

ROSTER="$(sed -n '/private fun RosterDayEditorSheet(/,/private fun RosterTimeInputButton(/p' "$UI")"
grep -Fq 'RosterCodeTextField(' <<< "$ROSTER" || fail "roster_code_state_field_not_used"
if grep -Fq 'draft.copy(code = value.take(RosterEntryCodec.MAX_CODE_LENGTH))' <<< "$ROSTER"; then
    fail "work_code_old_truncation_loop_remains"
fi
if grep -Fq 'freeCode = it.take(RosterEntryCodec.MAX_CODE_LENGTH)' <<< "$ROSTER"; then
    fail "free_code_old_truncation_loop_remains"
fi

# Radio semantics.
FORM="$(sed -n '/private fun MethodFormSection(/,/private fun MethodDropdownField(/p' "$UI")"
grep -Fq 'Modifier.selectableGroup()' <<< "$FORM" || fail "MethodFormSection_selectableGroup_missing"
grep -Fq '.selectable(' <<< "$FORM" || fail "MethodRadioRow_selectable_missing"
grep -Fq 'role = Role.RadioButton' <<< "$FORM" || fail "MethodRadioRow_role_missing"
grep -Fq 'onClick = null' <<< "$FORM" || fail "RadioButton_delegated_click_missing"
if grep -Fq '.clickable(onClick = onClick)' <<< "$FORM"; then
    fail "MethodRadioRow_double_click_semantics_remain"
fi

SELECTABLE_GROUP_COUNT="$(grep -Fc 'Modifier.selectableGroup()' "$UI")"
[[ "$SELECTABLE_GROUP_COUNT" -ge 3 ]] ||
    fail "expected_at_least_3_radio_groups_found_${SELECTABLE_GROUP_COUNT}"

# Checkbox semantics.
CHECKBOX_ROLE_COUNT="$(grep -Fc 'role = Role.Checkbox' "$UI")"
[[ "$CHECKBOX_ROLE_COUNT" -ge 2 ]] ||
    fail "expected_2_checkbox_roles_found_${CHECKBOX_ROLE_COUNT}"

DELEGATED_CHECKBOX_COUNT="$(grep -Fc 'onCheckedChange = null' "$UI")"
[[ "$DELEGATED_CHECKBOX_COUNT" -ge 2 ]] ||
    fail "expected_2_delegated_checkboxes_found_${DELEGATED_CHECKBOX_COUNT}"

# Heading semantics.
HEADING_COUNT="$(grep -Fc 'semantics { heading() }' "$UI")"
[[ "$HEADING_COUNT" -ge 4 ]] ||
    fail "expected_heading_semantics_found_${HEADING_COUNT}"

# Material determinate progress.
HEADER="$(sed -n '/private fun ScreenHeader(/,/private fun SaveStatusAction(/p' "$UI")"
grep -Fq 'LinearProgressIndicator(' <<< "$HEADER" || fail "Material_LinearProgressIndicator_missing"
grep -Fq 'progress = { it.fraction }' <<< "$HEADER" || fail "determinate_progress_lambda_missing"
grep -Fq 'color = MaterialTheme.colorScheme.secondary' <<< "$HEADER" || fail "Oslo_yellow_progress_color_lost"
grep -Fq 'trackColor = MaterialTheme.colorScheme.surfaceContainerHighest' <<< "$HEADER" || fail "progress_track_color_lost"
grep -Fq 'strokeCap = StrokeCap.Round' <<< "$HEADER" || fail "round_progress_cap_lost"
if grep -Fq 'fillMaxWidth(it.fraction)' <<< "$HEADER"; then
    fail "custom_progress_bar_remains"
fi

echo "A63_ANDROID_DEV_CONSISTENCY_SMOKE=PASS"
