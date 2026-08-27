#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
GRADLE="app/build.gradle.kts"

fail() {
    echo "BUGFIX02R2R2_INITIALIZER_VIEWMODEL_GATE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"
[[ -f "$GRADLE" ]] || fail "build_gradle_missing"

grep -Fq 'private class FerieturSessionViewModel : ViewModel() {' "$UI" ||
    fail "private_session_ViewModel_missing"
grep -Fq 'val session: FerieturSessionViewModel = viewModel { FerieturSessionViewModel() }' "$UI" ||
    fail "explicit_initializer_missing"

if grep -Fq 'val session: FerieturSessionViewModel = viewModel()' "$UI"; then
    fail "reflection_based_default_factory_regressed"
fi

grep -Fq 'BackHandler(enabled = aboutOpen || tripOverviewOpen || screen != FlowScreen.HOME)' "$UI" ||
    grep -Fq 'BackHandler(enabled = tripOverviewOpen || screen != FlowScreen.HOME)' "$UI" ||
    fail "system_BackHandler_missing"

grep -Fq 'implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")' "$GRADLE" ||
    fail "lifecycle_viewmodel_compose_dependency_missing"

echo "BUGFIX02R2R2_INITIALIZER_VIEWMODEL_GATE=PASS"
