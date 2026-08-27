#!/usr/bin/env bash
set -u
set -o pipefail
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A55R3_STEP2_MATERIAL_FORM_SMOKE=FAIL reason=$1"; exit 1; }
[[ -f "$UI" ]] || fail "FerieturApp_missing"
grep -Fq 'private fun MethodRadioRow(' "$UI" || fail "radio_row_missing"
grep -Fq 'RadioButton(' "$UI" || fail "material_radio_button_missing"
grep -Fq 'private fun MethodDropdownField(' "$UI" || fail "dropdown_field_missing"
grep -Fq 'label = "Betalingsforslag"' "$UI" || fail "payment_dropdown_missing"
grep -Fq 'label = "Arbeidsgiver"' "$UI" || fail "employer_dropdown_missing"
grep -Fq 'enabled = separateTripSelected' "$UI" || fail "employer_lock_contract_missing"
grep -Fq 'showRuleInfo = true' "$UI" || fail "rules_dialog_entry_missing"
grep -Fq 'Regler og beregningsgrunnlag' "$UI" || fail "rules_row_missing"
grep -Fq 'Dette endrer ikke lønnsberegningen.' "$UI" || fail "payment_semantics_help_missing"
STEP2="$(sed -n '/private fun CalculationMethodScreen(/,/private fun PayBasisScreen(/p' "$UI")"
printf '%s\n' "$STEP2" | grep -Fq 'SimpleChoiceCard(' && fail "legacy_choice_cards_still_used_in_step2"
printf '%s\n' "$STEP2" | grep -Fq 'payerScope' && fail "dynamic_payment_copy_still_present"
echo "A55R3_STEP2_MATERIAL_FORM_SMOKE=PASS"
