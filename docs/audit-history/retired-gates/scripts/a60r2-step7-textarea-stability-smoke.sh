#!/usr/bin/env bash
set -u
set -o pipefail
UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
fail() { echo "A60R2_STEP7_TEXTAREA_STABILITY_SMOKE=FAIL reason=$1"; exit 1; }
SETTLEMENT="$(sed -n '/private fun SettlementScreen(/,/private fun ControlScreen(/p' "$UI")"

grep -Fq 'import androidx.compose.foundation.verticalScroll' "$UI" || fail "verticalScroll_import_missing"
grep -Fq '.verticalScroll(rememberScrollState())' <<< "$SETTLEMENT" || fail "nonlazy_scroll_missing"
if grep -Fq 'LazyColumn(' <<< "$SETTLEMENT"; then fail "LazyColumn_still_used_on_Step7"; fi
grep -Fq '.height(176.dp)' <<< "$SETTLEMENT" || fail "reason_textarea_viewport_missing"
if grep -Fq 'maxLines = 3' <<< "$SETTLEMENT"; then fail "three_line_cap_still_present"; fi
grep -Fq 'MethodFormSection(title = "Hva skal betalingsforslaget vise?")' <<< "$SETTLEMENT" || fail "A60_form_structure_lost"

echo "A60R2_STEP7_TEXTAREA_STABILITY_SMOKE=PASS"
