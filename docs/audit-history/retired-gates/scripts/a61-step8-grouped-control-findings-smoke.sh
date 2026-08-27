#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A61_STEP8_GROUPED_CONTROL_FINDINGS_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

CONTROL="$(sed -n '/private fun ControlScreen(/,/private fun FinalSummaryScreen(/p' "$UI")"

grep -Fq 'ControlSettlementStatusRow(settlementSummary)' <<< "$CONTROL" ||
    fail "compact_payment_status_missing"
grep -Fq 'ControlFindingSummaryRow(' <<< "$CONTROL" ||
    fail "group_summary_rows_missing"
grep -Fq 'ControlFindingGroupSheet(' <<< "$CONTROL" ||
    fail "group_drilldown_sheet_missing"
grep -Fq 'selectedGroupTitle' <<< "$CONTROL" ||
    fail "selected_group_state_missing"
grep -Fq 'selectedFindingIndex' <<< "$CONTROL" ||
    fail "selected_finding_state_missing"
grep -Fq '"Kontrollen peker på forhold som bør vurderes.' <<< "$CONTROL" ||
    fail "single_legal_disclaimer_missing"

if grep -Fq 'expandedGroups' <<< "$CONTROL"; then
    fail "inline_expansion_state_remains"
fi
if grep -Fq 'ControlFindingGroupRow(' <<< "$CONTROL"; then
    fail "legacy_inline_group_row_remains"
fi
if grep -Fq 'Åpne en kategori for å se de konkrete periodene' <<< "$CONTROL"; then
    fail "old_instruction_paragraph_remains"
fi

grep -Fq 'private fun ControlFindingGroupSheet(' "$UI" ||
    fail "group_sheet_helper_missing"
grep -Fq 'rememberBottomSheetState(' "$UI" ||
    fail "Material_sheet_state_missing"
grep -Fq 'enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)' "$UI" ||
    fail "expanded_sheet_anchor_missing"
grep -Fq 'private fun ControlFindingCompactRow(' "$UI" ||
    fail "compact_finding_rows_missing"

grep -Fq '"Kort hvile mellom arbeidsperioder"' "$UI" ||
    fail "short_rest_compaction_missing"
grep -Fq '"Lang sammenhengende arbeidsperiode"' "$UI" ||
    fail "long_work_compaction_missing"
grep -Fq '"Mer enn 48 timer i den viste perioden"' "$UI" ||
    fail "period_total_compaction_missing"

grep -Fq '"Vis original kontrolltekst"' "$UI" ||
    fail "audit_text_disclosure_missing"

echo "A61_STEP8_GROUPED_CONTROL_FINDINGS_SMOKE=PASS"
