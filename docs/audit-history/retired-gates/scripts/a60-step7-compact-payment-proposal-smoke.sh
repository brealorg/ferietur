#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A60_STEP7_COMPACT_PAYMENT_PROPOSAL_SMOKE=FAIL reason=$1"; exit 1; }
[[ -f "$UI" ]] || fail "FerieturApp_missing"
SETTLEMENT="$(sed -n '/private fun SettlementScreen(/,/private fun ControlScreen(/p' "$UI")"

grep -Fq '"Beregnet grunnlag"' <<< "$SETTLEMENT" || fail "compact_calculated_basis_card_missing"
grep -Fq 'MethodFormSection(title = "Hva skal betalingsforslaget vise?")' <<< "$SETTLEMENT" || fail "material_selection_group_missing"
grep -Fq 'title = "Bruk beregnet beløp"' <<< "$SETTLEMENT" || fail "calculated_amount_option_missing"
grep -Fq 'title = "Dokumenter avtalt beløp"' <<< "$SETTLEMENT" || fail "custom_amount_option_missing"
grep -Fq 'label = { Text("Avtalt beløp") }' <<< "$SETTLEMENT" || fail "custom_amount_field_missing"
grep -Fq '"Beløp må fylles ut"' <<< "$SETTLEMENT" || fail "inline_amount_validation_missing"
grep -Fq 'label = { Text("Begrunnelse") }' <<< "$SETTLEMENT" || fail "reason_field_missing"
grep -Fq '"Forklar hvorfor beløpet avviker"' <<< "$SETTLEMENT" || fail "inline_reason_validation_missing"
grep -Fq 'isError = amountError != null' <<< "$SETTLEMENT" || fail "amount_error_state_missing"
grep -Fq 'isError = reasonError != null' <<< "$SETTLEMENT" || fail "reason_error_state_missing"
grep -Fq '"Et avtalt beløp endrer ikke beregningen eller arbeidstidsvarslene."' <<< "$SETTLEMENT" || fail "single_semantic_info_line_missing"

if grep -Fq 'Beregningen viser appens beregnede grunnlag.' <<< "$SETTLEMENT"; then fail "old_intro_still_present"; fi
if grep -Fq 'MethodChoiceCard(' <<< "$SETTLEMENT"; then fail "legacy_choice_cards_still_present"; fi
if grep -Fq '"Fyll ut betalingsforslaget"' <<< "$SETTLEMENT"; then fail "large_validation_message_still_present"; fi
if grep -Fq '"Arbeidstidsvarsler står uansett"' <<< "$SETTLEMENT"; then fail "duplicate_worktime_info_card_still_present"; fi

grep -Fq 'onSettlementMode(SettlementMode.FULL_CALCULATION)' <<< "$SETTLEMENT" || fail "full_calculation_wiring_lost"
grep -Fq 'onSettlementMode(SettlementMode.CUSTOM_AGREEMENT)' <<< "$SETTLEMENT" || fail "custom_agreement_wiring_lost"

echo "A60_STEP7_COMPACT_PAYMENT_PROPOSAL_SMOKE=PASS"
