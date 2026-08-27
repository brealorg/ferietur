#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A62_SOURCE_INTEGRITY_SMOKE=FAIL reason=$1"; exit 1; }

[[ "$(grep -Fc 'private fun FinalSummaryScreen(' "$UI")" -eq 1 ]] ||
    fail "FinalSummaryScreen_count_wrong"
[[ "$(grep -Fc 'private fun FinalSummaryStatusRow(' "$UI")" -eq 1 ]] ||
    fail "FinalSummaryStatusRow_count_wrong"
[[ "$(grep -Fc 'private fun StatusRow(' "$UI")" -eq 1 ]] ||
    fail "StatusRow_count_wrong"

grep -Fq 'PdfExporter.Variant.SHORT' "$UI" ||
    fail "short_pdf_export_lost"
grep -Fq 'PdfExporter.Variant.FULL' "$UI" ||
    fail "full_pdf_export_lost"

echo "A62_SOURCE_INTEGRITY_SMOKE=PASS"
