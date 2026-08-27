#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A61_SOURCE_INTEGRITY_SMOKE=FAIL reason=$1"; exit 1; }

[[ "$(grep -Fc 'private fun ControlScreen(' "$UI")" -eq 1 ]] ||
    fail "ControlScreen_count_wrong"
[[ "$(grep -Fc 'private fun FinalSummaryScreen(' "$UI")" -eq 1 ]] ||
    fail "FinalSummaryScreen_count_wrong"
[[ "$(grep -Fc 'private fun StatusRow(' "$UI")" -eq 1 ]] ||
    fail "StatusRow_count_wrong"

grep -Fq 'private fun compactControlFinding(' "$UI" ||
    fail "compact_finding_helper_missing"
grep -Fq 'private fun controlFindingIcon(' "$UI" ||
    fail "finding_icon_helper_missing"

echo "A61_SOURCE_INTEGRITY_SMOKE=PASS"
