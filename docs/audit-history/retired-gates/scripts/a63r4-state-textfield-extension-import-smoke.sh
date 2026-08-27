#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A63R4_STATE_TEXTFIELD_EXTENSION_IMPORT_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

grep -Fq 'import androidx.compose.foundation.text.input.maxLength' "$UI" ||
    fail "maxLength_extension_import_missing"
grep -Fq 'import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd' "$UI" ||
    fail "setTextAndPlaceCursorAtEnd_extension_import_missing"

grep -Fq 'InputTransformation.maxLength(RosterEntryCodec.MAX_CODE_LENGTH)' "$UI" ||
    fail "Google_maxLength_usage_missing"
grep -Fq 'state.setTextAndPlaceCursorAtEnd(value)' "$UI" ||
    fail "Google_state_sync_usage_missing"

echo "A63R4_STATE_TEXTFIELD_EXTENSION_IMPORT_SMOKE=PASS"
