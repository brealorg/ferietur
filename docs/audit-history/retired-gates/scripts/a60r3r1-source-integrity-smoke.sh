#!/usr/bin/env bash
set -u
set -o pipefail
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A60R3R1_SOURCE_INTEGRITY_SMOKE=FAIL reason=$1"; exit 1; }

[[ -f "$UI" ]] || fail "FerieturApp_missing"
[[ "$(grep -Fc 'private fun SettlementScreen(' "$UI")" -eq 1 ]] || fail "SettlementScreen_count_wrong"
[[ "$(grep -Fc 'private fun SettlementInfoRow(' "$UI")" -eq 1 ]] || fail "SettlementInfoRow_count_wrong"

grep -Fq 'Text("Ingen åpne regelspørsmål er registrert for denne turen.")' "$UI" ||
    fail "CalculationRulesSheet_tail_corrupted"
if grep -Fq 'Ingen åpne regelspørsmål@Composable' "$UI"; then
    fail "SettlementScreen_spliced_into_string"
fi
if grep -Eq '^ *= result\.paymentBasisAmount,' "$UI"; then
    fail "orphan_settlement_tail_detected"
fi

echo "A60R3R1_SOURCE_INTEGRITY_SMOKE=PASS"
