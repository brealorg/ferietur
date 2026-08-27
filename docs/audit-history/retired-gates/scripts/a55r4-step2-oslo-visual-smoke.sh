#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() { echo "A55R4_STEP2_OSLO_VISUAL_SMOKE=FAIL reason=$1"; exit 1; }

[[ -f "$UI" ]] || fail "FerieturApp_missing"
grep -Fq 'RadioButtonDefaults.colors(' "$UI" || fail "radio_colors_missing"
grep -Fq 'selectedColor = MaterialTheme.colorScheme.secondary' "$UI" || fail "oslo_yellow_radio_missing"
grep -Fq 'MaterialTheme.colorScheme.secondary.copy(alpha = 0.09f)' "$UI" || fail "selected_row_tint_missing"
grep -Fq 'MaterialTheme.typography.titleSmall' "$UI" || fail "compact_section_type_missing"
grep -Fq 'private fun MethodDropdownField(' "$UI" || fail "dropdown_field_missing"
grep -Fq '"Fast"' "$UI" || fail "locked_employer_affordance_missing"
grep -Fq 'private fun MethodRulesRow(' "$UI" || fail "compact_rules_row_missing"
grep -Fq 'Appen beregner bare tilleggene.' "$UI" || fail "compact_turnus_help_missing"
grep -Fq 'Turen beregnes som eget oppdrag.' "$UI" || fail "compact_separate_help_missing"

STEP2="$(sed -n '/private fun CalculationMethodScreen(/,/private fun PayBasisScreen(/p' "$UI")"
printf '%s\n' "$STEP2" | grep -Fq 'SimpleChoiceCard(' && fail "legacy_step2_cards_regressed"
printf '%s\n' "$STEP2" | grep -Fq 'OutlinedButton(' && fail "pill_dropdown_regressed"

echo "A55R4_STEP2_OSLO_VISUAL_SMOKE=PASS"
