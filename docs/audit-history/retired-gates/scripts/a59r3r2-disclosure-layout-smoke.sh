#!/usr/bin/env bash
set -u
set -o pipefail
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A59R3R2_DISCLOSURE_LAYOUT_SMOKE=FAIL reason=$1"; exit 1; }
[[ -f "$UI" ]] || fail "FerieturApp_missing"
grep -Fq 'import androidx.compose.foundation.verticalScroll' "$UI" || fail "verticalScroll_import_missing"
DETAIL="$(sed -n '/private fun CalculationLineDetailSheet(/,/private data class CalculationQuickReason/p' "$UI")"
grep -Fq '.verticalScroll(rememberScrollState())' <<< "$DETAIL" || fail "regular_scroll_container_missing"
if grep -Fq 'LazyColumn(' <<< "$DETAIL"; then fail "lazy_column_still_used_for_dynamic_disclosures"; fi
grep -Fq 'quickReasons.forEachIndexed' <<< "$DETAIL" || fail "quick_reason_rendering_missing"
grep -Fq 'evidenceToShow.forEach' <<< "$DETAIL" || fail "evidence_rendering_missing"
grep -Fq 'CalculationRuleBasisDisclosure(' <<< "$DETAIL" || fail "rule_disclosure_missing"
grep -Fq 'enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)' <<< "$DETAIL" || fail "expanded_sheet_anchor_lost"
echo "A59R3R2_DISCLOSURE_LAYOUT_SMOKE=PASS"
