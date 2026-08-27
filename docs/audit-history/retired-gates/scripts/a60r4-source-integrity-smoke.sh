#!/usr/bin/env bash
set -u
set -o pipefail
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A60R4_SOURCE_INTEGRITY_SMOKE=FAIL reason=$1"; exit 1; }

[[ "$(grep -Fc 'private fun SettlementScreen(' "$UI")" -eq 1 ]] || fail "SettlementScreen_count_wrong"
[[ "$(grep -Fc 'private fun SettlementInfoRow(' "$UI")" -eq 1 ]] || fail "SettlementInfoRow_count_wrong"
[[ "$(grep -Fc 'private fun ControlScreen(' "$UI")" -eq 1 ]] || fail "ControlScreen_count_wrong"
grep -Fq 'Ingen åpne regelspørsmål er registrert for denne turen.' "$UI" || fail "rules_sheet_tail_corrupted"
if grep -Fq 'Ingen åpne regelspørsmål@Composable' "$UI"; then fail "function_splice_detected"; fi

echo "A60R4_SOURCE_INTEGRITY_SMOKE=PASS"
