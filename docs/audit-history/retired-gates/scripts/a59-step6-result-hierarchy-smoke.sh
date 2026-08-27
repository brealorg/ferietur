#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A59_STEP6_RESULT_HIERARCHY_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

CALC="$(sed -n '/private fun CalculationScreen(/,/private fun SettlementScreen(/p' "$UI")"

# Do not use `printf ... | grep -q` under pipefail for this large block:
# grep -q can exit as soon as it finds a match, causing printf to receive
# SIGPIPE and making the whole pipeline look like a failure.
grep -Fq '"Beløp som kommer i tillegg"' <<< "$CALC" ||
    fail "total heading missing"
grep -Fq '"Vanlig lønn etter grunnturnusen er ikke med."' <<< "$CALC" ||
    fail "total help missing"
grep -Fq 'CalculationSummaryRow(' <<< "$CALC" ||
    fail "summary rows missing"
grep -Fq '"Allerede dekket av grunnturnusen"' <<< "$CALC" ||
    fail "covered roster drilldown missing"
grep -Fq '"Dag-for-dag kontroll"' <<< "$CALC" ||
    fail "day audit drilldown missing"
grep -Fq '"Regler og beregningsgrunnlag"' <<< "$CALC" ||
    fail "rules drilldown missing"
grep -Fq 'CalculationLineDetailSheet(' <<< "$CALC" ||
    fail "line detail sheet missing"
grep -Fq 'CalculationDayAuditSheet(' <<< "$CALC" ||
    fail "day sheet missing"
grep -Fq 'CalculationRulesSheet(' <<< "$CALC" ||
    fail "rules sheet missing"

if grep -Fq 'ExplainableCalculationCard(' <<< "$CALC"; then
    fail "old inline line expansion remains"
fi
if grep -Fq 'DayCalculationAuditCard(' <<< "$CALC"; then
    fail "day audits remain inline"
fi
if grep -Fq 'CoveredRosterSummaryCard(' <<< "$CALC"; then
    fail "covered roster remains inline"
fi
if grep -Fq 'ExplainableRateCard(' <<< "$CALC"; then
    fail "hourly accordion remains inline"
fi

grep -Fq 'private fun CalculationLineDetailSheet(' "$UI" ||
    fail "line detail helper missing"
grep -Fq 'private fun CoveredRosterDetailSheet(' "$UI" ||
    fail "covered detail helper missing"
grep -Fq 'private fun CalculationDayAuditSheet(' "$UI" ||
    fail "day audit helper missing"
grep -Fq 'private fun CalculationRulesSheet(' "$UI" ||
    fail "rules helper missing"
grep -Fq 'DayCalculationContributionRow(contribution)' "$UI" ||
    fail "audit contribution details lost"
grep -Fq 'EvidenceRow(evidence)' "$UI" ||
    fail "evidence lost"
grep -Fq 'unresolved: List<DomainRule>' "$UI" ||
    fail "rule type incorrect"

echo "A59_STEP6_RESULT_HIERARCHY_SMOKE=PASS"
