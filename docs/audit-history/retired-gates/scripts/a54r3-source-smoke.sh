#!/usr/bin/env bash
set -u
set -o pipefail

MAIN="app/src/main/java/app/ferietur/MainActivity.kt"

fail() {
    echo "A54R3_STEP1_NB_NO_RESOURCE_CONTEXT_SMOKE=FAIL reason=$1"
    exit 1
}

[[ -f "$MAIN" ]] || fail "MainActivity_missing"
grep -Fq 'A54R3_NB_NO_ACTIVITY_RESOURCE_CONTEXT' "$MAIN" || fail "marker_missing"
grep -Fq 'override fun attachBaseContext(newBase: Context)' "$MAIN" || fail "attachBaseContext_missing"
grep -Fq 'Configuration(newBase.resources.configuration)' "$MAIN" || fail "configuration_copy_missing"
grep -Fq 'Locale.forLanguageTag("nb-NO")' "$MAIN" || fail "nb_NO_locale_missing"
grep -Fq 'newBase.createConfigurationContext(norwegianConfiguration)' "$MAIN" || fail "localized_context_missing"

echo "A54R3_STEP1_NB_NO_RESOURCE_CONTEXT_SMOKE=PASS"
