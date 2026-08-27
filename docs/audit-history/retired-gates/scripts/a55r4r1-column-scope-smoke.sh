#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "A55R4R1_COLUMN_SCOPE_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

METHOD_FORM="$(
    sed -n '/private fun MethodFormSection(/,/private fun MethodSectionHeading(/p' "$UI"
)"

[[ -n "$METHOD_FORM" ]] || fail "MethodFormSection_missing"

# Original A55R4 compile defect must remain impossible.
! grep -Fq 'Column(content = content)' <<< "$METHOD_FORM" ||
    fail "invalid_column_scope_assignment_remains"

# Successor-safe ColumnScope contract:
# MethodFormSection still accepts a plain @Composable () -> Unit, and invokes it
# inside Column's receiver lambda. A63 legitimately added selectableGroup() to
# that Column, so the old exact string `Column { content() }` is obsolete.
grep -Fq 'content: @Composable () -> Unit' <<< "$METHOD_FORM" ||
    fail "plain_composable_content_contract_missing"
grep -Fq 'Column(' <<< "$METHOD_FORM" ||
    fail "column_receiver_container_missing"
grep -Fq 'content()' <<< "$METHOD_FORM" ||
    fail "column_scope_invocation_missing"

# A63's Android Developers radio-group semantics must coexist with the original
# ColumnScope fix rather than being treated as a regression.
grep -Fq 'modifier = Modifier.selectableGroup()' <<< "$METHOD_FORM" ||
    fail "A63_selectable_group_semantics_missing"

# Preserve the A55R4 visual contract this gate originally protected.
grep -Fq 'RadioButtonDefaults.colors(' "$UI" ||
    fail "A55R4_radio_polish_lost"
grep -Fq 'selectedColor = MaterialTheme.colorScheme.secondary' "$UI" ||
    fail "A55R4_oslo_yellow_lost"
grep -Fq 'private fun MethodRulesRow(' "$UI" ||
    fail "A55R4_rules_row_lost"

echo "A55R4R1_COLUMN_SCOPE_SMOKE=PASS"
