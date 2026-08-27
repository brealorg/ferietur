#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() { echo "A56_STEP3_COMPACT_FORM_SMOKE=FAIL reason=$1"; exit 1; }

[[ -f "$UI" ]] || fail "FerieturApp_missing"

STEP3="$(sed -n '/private fun PayBasisScreen(/,/private fun RosterScreen(/p' "$UI")"

printf '%s\n' "$STEP3" | grep -Fq 'MethodSectionHeading("Lønnstrinn")' || fail "salary_heading_missing"
printf '%s\n' "$STEP3" | grep -Fq 'MaterialTheme.colorScheme.secondary' || fail "salary_oslo_yellow_ring_missing"
printf '%s\n' "$STEP3" | grep -Fq 'Lønnstabell fra 1. mai 2026' || fail "salary_table_date_missing"
printf '%s\n' "$STEP3" | grep -Fq 'label = "Arbeidsuke"' || fail "weekly_dropdown_missing"
printf '%s\n' "$STEP3" | grep -Fq 'WeekendProfileSelector(' || fail "weekend_field_missing"
printf '%s\n' "$STEP3" | grep -Fq 'Jeg har kontrollert opplysningene mot en nyere lønnsslipp' || fail "compact_payslip_confirmation_missing"
printf '%s\n' "$STEP3" | grep -Fq 'CheckboxDefaults.colors(' || fail "oslo_checkbox_colors_missing"
printf '%s\n' "$STEP3" | grep -Fq 'MethodRulesRow(' || fail "rules_row_missing"

printf '%s\n' "$STEP3" | grep -Fq 'Kontroller opplysningene mot en nyere lønnsslipp. Appen kan regne' && fail "old_intro_still_present"
printf '%s\n' "$STEP3" | grep -Fq 'BasisChip(' && fail "weekly_basis_chips_still_present"
printf '%s\n' "$STEP3" | grep -Fq 'Sjekk satsen mot lønnsslippen' && fail "redundant_inline_warning_still_present"
printf '%s\n' "$STEP3" | grep -Fq 'Jeg har kontrollert lønnstrinn og lørdags-/søndagssats' && fail "old_long_confirmation_still_present"

WEEKEND="$(sed -n '/private fun WeekendProfileSelector(/,/private fun BasisChip(/p' "$UI")"
printf '%s\n' "$WEEKEND" | grep -Fq 'surfaceContainerLow' || fail "weekend_material_field_surface_missing"
printf '%s\n' "$WEEKEND" | grep -Fq '"Helgetillegg"' || fail "weekend_field_label_missing"
printf '%s\n' "$WEEKEND" | grep -Fq 'ModalBottomSheet' || fail "weekend_detail_sheet_lost"
printf '%s\n' "$WEEKEND" | grep -Fq 'Dok. 25 2026–28, punkt 12.2.2' || fail "weekend_source_guidance_lost"

echo "A56_STEP3_COMPACT_FORM_SMOKE=PASS"
