#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "FIX02R1_INLINE_MESSAGE_COMPILE_GATE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

ACTUAL="$(sha256sum "$UI" | awk '{print $1}')"
[[ "$ACTUAL" == "53570b11fcfce8b75500724ca37df12447933ed19dfed046ca191c7529aa87f2" ]] ||
    fail "unexpected_UI_SHA256_$ACTUAL"

grep -Fq 'private fun InlineMessage(severity: FindingSeverity, title: String, detail: String)' "$UI" ||
    fail "InlineMessage_signature_changed"

grep -Fq 'detail = issue.detail,' "$UI" ||
    fail "TripStorageIssue_detail_named_argument_missing"

if grep -Fq 'body = issue.detail,' "$UI"; then
    fail "stale_body_named_argument_remains"
fi

echo "FIX02R1_INLINE_MESSAGE_COMPILE_GATE=PASS"
