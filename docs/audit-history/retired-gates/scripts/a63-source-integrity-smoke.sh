#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A63_SOURCE_INTEGRITY_SMOKE=FAIL reason=$1"; exit 1; }

for fn in \
    TripBasicsScreen \
    TripTitleTextField \
    MethodFormSection \
    MethodRadioRow \
    RosterCodeTextField \
    RosterDayEditorSheet \
    ScreenHeader \
    FinalSummaryScreen
do
    COUNT="$(grep -Fc "private fun ${fn}(" "$UI")"
    [[ "$COUNT" -eq 1 ]] || fail "${fn}_count_${COUNT}"
done

grep -Fq 'private fun ControlScreen(' "$UI" || fail "ControlScreen_missing"
grep -Fq 'private fun CalculationScreen(' "$UI" || fail "CalculationScreen_missing"

echo "A63_SOURCE_INTEGRITY_SMOKE=PASS"
