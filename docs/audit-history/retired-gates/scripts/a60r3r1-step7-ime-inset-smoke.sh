#!/usr/bin/env bash
set -u
set -o pipefail
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A60R3R1_STEP7_IME_INSET_SMOKE=FAIL reason=$1"; exit 1; }
SETTLEMENT="$(sed -n '/private fun SettlementScreen(/,/private fun ControlScreen(/p' "$UI")"

if grep -Fq '.imePadding()' <<< "$SETTLEMENT"; then fail "double_ime_padding_remains"; fi
if grep -Fq 'import androidx.compose.foundation.layout.imePadding' "$UI"; then fail "unused_imePadding_import_remains"; fi

grep -Fq '.verticalScroll(rememberScrollState())' <<< "$SETTLEMENT" || fail "stable_parent_scroll_lost"
grep -Fq '.height(176.dp)' <<< "$SETTLEMENT" || fail "reason_viewport_lost"
grep -Fq 'keyboardOptions = KeyboardOptions(' <<< "$SETTLEMENT" || fail "amount_keyboard_options_missing"
grep -Fq 'keyboardType = KeyboardType.Decimal' <<< "$SETTLEMENT" || fail "amount_decimal_keyboard_missing"

echo "A60R3R1_STEP7_IME_INSET_SMOKE=PASS"
