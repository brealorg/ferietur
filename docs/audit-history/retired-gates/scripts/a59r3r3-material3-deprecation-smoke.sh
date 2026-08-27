#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A59R3R3_MATERIAL3_DEPRECATION_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

# Global deprecation invariants.
! grep -Fq 'rememberModalBottomSheetState' "$UI" ||
    fail "deprecated_rememberModalBottomSheetState_remains"
! grep -Fq 'headlineContent =' "$UI" ||
    fail "deprecated_ListItem_headlineContent_overload_remains"

grep -Fq 'import androidx.compose.material3.rememberBottomSheetState' "$UI" ||
    fail "rememberBottomSheetState_import_missing"
grep -Fq 'import androidx.compose.material3.SheetValue' "$UI" ||
    fail "SheetValue_import_missing"

# A59R3R3 owns four Step 6 sheets. Check those four semantically instead of
# asserting that the entire app will forever contain exactly four sheets.
for fn in \
    CalculationLineDetailSheet \
    CoveredRosterDetailSheet \
    CalculationDayAuditSheet \
    CalculationRulesSheet
do
    BLOCK="$(sed -n "/private fun ${fn}(/,/^}/p" "$UI")"

    [[ -n "$BLOCK" ]] ||
        fail "${fn}_missing"

    grep -Fq 'rememberBottomSheetState(' <<< "$BLOCK" ||
        fail "${fn}_current_sheet_state_missing"

    grep -Fq 'enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)' <<< "$BLOCK" ||
        fail "${fn}_expanded_only_state_missing"

    grep -Fq 'sheetState = sheetState' <<< "$BLOCK" ||
        fail "${fn}_sheet_state_not_wired"
done

CONTENT_COUNT="$(grep -Fc 'content =' "$UI")"
[[ "$CONTENT_COUNT" -ge 2 ]] ||
    fail "new_ListItem_content_overload_not_present"

echo "A59R3R3_MATERIAL3_DEPRECATION_SMOKE=PASS"
