#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A62_STEP9_SUMMARY_EXPORT_HIERARCHY_SMOKE=FAIL reason=$1"; exit 1; }

[[ -f "$UI" ]] || fail "FerieturApp_missing"
SUMMARY="$(sed -n '/private fun FinalSummaryScreen(/,/private fun StatusRow(/p' "$UI")"

# Result -> export -> status -> documentation ordering.
RESULT_LINE="$(grep -n -m1 'currency(snapshot.calculation.paymentBasisAmount)' <<< "$SUMMARY" | cut -d: -f1)"
EXPORT_LINE="$(grep -n -m1 'MethodSectionHeading("Eksporter")' <<< "$SUMMARY" | cut -d: -f1)"
STATUS_LINE="$(grep -n -m1 'MethodSectionHeading("Status")' <<< "$SUMMARY" | cut -d: -f1)"
DOC_LINE="$(grep -n -m1 'MethodSectionHeading("Dokumentasjon")' <<< "$SUMMARY" | cut -d: -f1)"

[[ -n "$RESULT_LINE" && -n "$EXPORT_LINE" && -n "$STATUS_LINE" && -n "$DOC_LINE" ]] ||
    fail "summary_section_marker_missing"
(( RESULT_LINE < EXPORT_LINE && EXPORT_LINE < STATUS_LINE && STATUS_LINE < DOC_LINE )) ||
    fail "summary_section_order_wrong"

# Google Material button hierarchy.
grep -Fq 'Button(' <<< "$SUMMARY" ||
    fail "primary_export_button_missing"
grep -Fq 'Text("Lag og del kort oppsummering")' <<< "$SUMMARY" ||
    fail "short_export_not_primary"
grep -Fq 'OutlinedButton(' <<< "$SUMMARY" ||
    fail "secondary_outlined_export_missing"
grep -Fq 'Text("Lag og del fullt beregningsgrunnlag")' <<< "$SUMMARY" ||
    fail "full_export_label_missing"
if grep -Fq 'FilledTonalButton(' <<< "$SUMMARY"; then
    fail "competing_tonal_export_button_remains"
fi

# Compact status.
grep -Fq 'FinalSummaryStatusRow(' <<< "$SUMMARY" ||
    fail "compact_status_rows_missing"
for label in Arbeidsgiver Betalingsscenario Lønnsopplysninger Beregningsregler Registrering
do
    grep -Fq "\"$label\"" <<< "$SUMMARY" ||
        fail "status_${label}_missing"
done
if grep -Fq 'Regler og beregningsgrunnlag: ${snapshot.ruleBasis}' <<< "$SUMMARY"; then
    fail "full_rule_basis_still_in_status"
fi

# Worktime finding is the one attention row and returns to Control.
grep -Fq '"$reviewCount arbeidstidsforhold bør vurderes"' <<< "$SUMMARY" ||
    fail "worktime_attention_row_missing"
grep -Fq '.clickable(onClick = onBack)' <<< "$SUMMARY" ||
    fail "worktime_attention_navigation_missing"

# Documentation is compact.
grep -Fq '"Grunnturnusen følger med i fullt grunnlag"' <<< "$SUMMARY" ||
    fail "roster_documentation_row_missing"
grep -Fq '"${minutesUi(snapshot.calculation.rosterMinutes)} overlapper turen."' <<< "$SUMMARY" ||
    fail "roster_overlap_summary_missing"
if grep -Fq 'PDF-en tar med et snapshot av grunnturnusen' <<< "$SUMMARY"; then
    fail "old_roster_paragraph_remains"
fi

grep -Fq '"Beregning-ID: ${snapshot.id}"' <<< "$SUMMARY" ||
    fail "calculation_id_missing"

echo "A62_STEP9_SUMMARY_EXPORT_HIERARCHY_SMOKE=PASS"
