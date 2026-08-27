#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
TEST="app/src/test/java/app/ferietur/ui/FerieturSessionViewModelConstructionTest.kt"

fail() {
    echo "BUGFIX02R2_VIEWMODEL_CONSTRUCTION_GATE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"
[[ -f "$TEST" ]] || fail "ViewModel_construction_test_missing"

ACTUAL="$(sha256sum "$UI" | awk '{print $1}')"
[[ "$ACTUAL" == "61a5c5b4575516971966f4bf91ce8321d1e45ade9ad4bff8799df702bc6cfb89" ]] ||
    fail "unexpected_UI_SHA256_$ACTUAL"

grep -Fq 'class FerieturSessionViewModel : ViewModel() {' "$UI" ||
    fail "public_ViewModel_declaration_missing"

if grep -Fq 'private class FerieturSessionViewModel : ViewModel() {' "$UI"; then
    fail "private_ViewModel_declaration_remains"
fi

grep -Fq 'val session: FerieturSessionViewModel = viewModel()' "$UI" ||
    fail "Compose_viewModel_binding_missing"

grep -Fq 'BackHandler(enabled = tripOverviewOpen || screen != FlowScreen.HOME)' "$UI" ||
    fail "BUGFIX02_BackHandler_regressed"

grep -Fq 'Modifier.isPublic(clazz.modifiers)' "$TEST" ||
    fail "public_class_regression_assertion_missing"
grep -Fq 'val constructor = clazz.getConstructor()' "$TEST" ||
    fail "public_no_arg_constructor_regression_assertion_missing"
grep -Fq 'assertNotNull(constructor.newInstance())' "$TEST" ||
    fail "reflective_construction_regression_assertion_missing"

echo "BUGFIX02R2_VIEWMODEL_CONSTRUCTION_GATE=PASS"
