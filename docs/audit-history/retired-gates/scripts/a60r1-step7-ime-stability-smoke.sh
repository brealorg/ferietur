#!/usr/bin/env bash
set -u
set -o pipefail
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A60R1_STEP7_IME_STABILITY_SMOKE=FAIL reason=$1"; exit 1; }
SETTLEMENT="$(sed -n '/private fun SettlementScreen(/,/private fun ControlScreen(/p' "$UI")"

grep -Fq 'var reasonTouched by remember(settlementMode)' <<< "$SETTLEMENT" || fail "reason_touch_state_missing"
grep -Fq '!reasonTouched -> null' <<< "$SETTLEMENT" || fail "reason_error_shown_before_interaction"
grep -Fq 'var amountTouched by remember(settlementMode)' <<< "$SETTLEMENT" || fail "amount_touch_state_missing"
grep -Fq '!amountTouched -> null' <<< "$SETTLEMENT" || fail "amount_error_shown_before_interaction"
grep -Fq '.verticalScroll(rememberScrollState())' <<< "$SETTLEMENT" || fail "stable_parent_scroll_missing"
grep -Fq '.height(176.dp)' <<< "$SETTLEMENT" || fail "stable_reason_viewport_missing"
if grep -Fq 'maxLines = 3' <<< "$SETTLEMENT"; then fail "three_line_cap_regressed"; fi
echo "A60R1_STEP7_IME_STABILITY_SMOKE=PASS"
