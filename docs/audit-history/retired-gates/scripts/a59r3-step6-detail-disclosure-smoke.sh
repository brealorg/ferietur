#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A59R3_STEP6_DETAIL_DISCLOSURE_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

DETAIL="$(sed -n '/private fun CalculationLineDetailSheet(/,/private fun CoveredRosterDetailSheet(/p' "$UI")"

grep -Fq 'enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)' <<< "$DETAIL" ||
    fail "line_detail_not_opening_expanded"
grep -Fq '"Kort forklart"' <<< "$DETAIL" ||
    fail "short_explanation_section_missing"
grep -Fq 'CalculationQuickReasonRow(' <<< "$DETAIL" ||
    fail "short_explanation_disclosures_missing"
grep -Fq 'line.evidence.take(4)' <<< "$DETAIL" ||
    fail "evidence_preview_not_limited"
grep -Fq '"Vis alle ${line.evidence.size}"' <<< "$DETAIL" ||
    fail "show_all_evidence_missing"
grep -Fq 'CalculationEvidenceCompactRow(evidence)' <<< "$DETAIL" ||
    fail "compact_evidence_rows_missing"
grep -Fq 'CalculationRuleBasisDisclosure(' <<< "$DETAIL" ||
    fail "rule_basis_disclosure_missing"

# The long tariff paragraph must not be rendered open by default anymore.
if grep -Fq 'Text(line.explanation' <<< "$DETAIL"; then
    fail "full_explanation_still_inline_by_default"
fi

grep -Fq 'private fun CalculationQuickReasonRow(' "$UI" ||
    fail "quick_reason_component_missing"
grep -Fq 'private fun CalculationEvidenceCompactRow(' "$UI" ||
    fail "compact_evidence_component_missing"
grep -Fq 'private fun CalculationRuleBasisDisclosure(' "$UI" ||
    fail "rule_basis_component_missing"

# Sample active-line plain-language contract.
grep -Fq '"Grunnturnusen brukes som sammenligning"' "$UI" ||
    fail "active_comparison_explanation_missing"
grep -Fq '"Arbeid utover grunnturnusen beregnes med +50 %"' "$UI" ||
    fail "active_50_percent_explanation_missing"
grep -Fq '"Reise med ansvar regnes som arbeidstid"' "$UI" ||
    fail "active_travel_explanation_missing"

# All Step 6 drill-downs use the expanded Material anchor.
for fn in \
    CalculationLineDetailSheet \
    CoveredRosterDetailSheet \
    CalculationDayAuditSheet \
    CalculationRulesSheet
do
    BLOCK="$(sed -n "/private fun ${fn}(/,/^}/p" "$UI")"
    grep -Fq 'enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)' <<< "$BLOCK" ||
        fail "${fn}_partial_anchor_not_skipped"
    grep -Fq 'sheetState = sheetState' <<< "$BLOCK" ||
        fail "${fn}_sheet_state_not_wired"
done

echo "A59R3_STEP6_DETAIL_DISCLOSURE_SMOKE=PASS"
