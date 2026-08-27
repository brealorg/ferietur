#!/usr/bin/env bash
set -u
set -o pipefail
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A60R4_GOOGLE_STATE_BASED_TEXTFIELD_SMOKE=FAIL reason=$1"; exit 1; }
SETTLEMENT="$(sed -n '/private fun SettlementScreen(/,/private fun ControlScreen(/p' "$UI")"

grep -Fq 'rememberTextFieldState(initialText = customAmountText)' <<< "$SETTLEMENT" || fail "amount_TextFieldState_missing"
grep -Fq 'rememberTextFieldState(initialText = reason)' <<< "$SETTLEMENT" || fail "reason_TextFieldState_missing"
grep -Fq 'snapshotFlow { amountFieldState.text.toString() }' <<< "$SETTLEMENT" || fail "amount_snapshotFlow_missing"
grep -Fq 'snapshotFlow { reasonFieldState.text.toString() }' <<< "$SETTLEMENT" || fail "reason_snapshotFlow_missing"
grep -Fq 'state = amountFieldState' <<< "$SETTLEMENT" || fail "amount_state_field_missing"
grep -Fq 'state = reasonFieldState' <<< "$SETTLEMENT" || fail "reason_state_field_missing"
grep -Fq 'inputTransformation = amountInputTransformation' <<< "$SETTLEMENT" || fail "amount_input_transformation_missing"
grep -Fq 'lineLimits = TextFieldLineLimits.SingleLine' <<< "$SETTLEMENT" || fail "amount_line_limits_missing"
grep -Fq 'lineLimits = TextFieldLineLimits.MultiLine(' <<< "$SETTLEMENT" || fail "reason_line_limits_missing"
grep -Fq 'minHeightInLines = 5' <<< "$SETTLEMENT" || fail "reason_min_lines_missing"
grep -Fq 'maxHeightInLines = 5' <<< "$SETTLEMENT" || fail "reason_max_visible_lines_missing"

if grep -Fq 'value = customAmountText' <<< "$SETTLEMENT"; then fail "amount_outer_value_driven"; fi
if grep -Fq 'value = reason' <<< "$SETTLEMENT"; then fail "reason_outer_value_driven"; fi
if grep -Fq '.height(176.dp)' <<< "$SETTLEMENT"; then fail "manual_reason_height_remains"; fi

echo "A60R4_GOOGLE_STATE_BASED_TEXTFIELD_SMOKE=PASS"
