#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A59R3R1_WIDTHIN_IMPORT_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"
grep -Fq 'import androidx.compose.foundation.layout.widthIn' "$UI" ||
    fail "widthIn_import_missing"
grep -Fq 'modifier = Modifier.widthIn(min = 72.dp)' "$UI" ||
    fail "A59R3_compact_evidence_widthIn_usage_missing"

echo "A59R3R1_WIDTHIN_IMPORT_SMOKE=PASS"
