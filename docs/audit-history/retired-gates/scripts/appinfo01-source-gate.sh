#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
PREFS="app/src/main/java/app/ferietur/ui/AppInfoPreferences.kt"
CONTACT="app/src/main/java/app/ferietur/ui/AppInfoContact.kt"
GRADLE="app/build.gradle.kts"

fail() {
    echo "APPINFO01_SOURCE_GATE=FAIL reason=$1"
    exit 1
}

for f in "$UI" "$PREFS" "$CONTACT" "$GRADLE"; do
    [[ -f "$f" ]] || fail "missing_${f//\//_}"
done

echo "===== first-run disclaimer ====="
grep -Fq 'private const val APP_DISCLAIMER_TITLE = "Kontroller alltid beregningen"' "$UI" ||
    fail "disclaimer_title_missing"
grep -Fq 'Appen kan inneholde feil' "$UI" ||
    fail "disclaimer_error_copy_missing"
grep -Fq 'Du er selv ansvarlig for å kontrollere opplysningene og beregningen' "$UI" ||
    fail "disclaimer_responsibility_copy_missing"
grep -Fq 'Text(if (disclaimerWriteInProgress) "Lagrer…" else "Jeg har forstått")' "$UI" ||
    fail "acknowledgement_button_missing"
grep -Fq 'dismissOnBackPress = false' "$UI" ||
    fail "first_run_dialog_back_dismiss_not_disabled"
grep -Fq 'dismissOnClickOutside = false' "$UI" ||
    fail "first_run_dialog_outside_dismiss_not_disabled"

grep -Fq 'const val CURRENT_DISCLAIMER_VERSION: Int = 1' "$PREFS" ||
    fail "versioned_acknowledgement_missing"
grep -Fq 'intPreferencesKey("disclaimer_ack_version")' "$PREFS" ||
    fail "acknowledgement_key_missing"
grep -Fq 'preferencesDataStore(name = "ferietur_app_info")' "$PREFS" ||
    fail "Preferences_DataStore_delegate_missing"
grep -Fq 'acknowledgeCurrentDisclaimer' "$PREFS" ||
    fail "DataStore_ack_write_missing"

echo "FIRST_RUN_DISCLAIMER=LOCKED"

echo
echo "===== About Ferietur ====="
grep -Fq 'private fun AboutFerieturScreen(' "$UI" ||
    fail "About_screen_missing"
grep -Fq 'contentDescription = "Om Ferietur"' "$UI" ||
    fail "Home_About_affordance_missing"
grep -Fq 'APP_INDEPENDENCE_TEXT' "$UI" ||
    fail "independence_copy_missing"
grep -Fq 'er ikke en offisiell app fra Oslo kommune' "$UI" ||
    fail "not_official_Oslo_copy_missing"
grep -Fq 'Opplysninger lagres lokalt på enheten. Ferietur krever ingen konto og ' "$UI" ||
    fail "privacy_copy_missing"
grep -Fq 'har ikke internettilgang.' "$UI" ||
    fail "no_internet_copy_missing"
grep -Fq 'BuildConfig.VERSION_NAME' "$UI" ||
    fail "version_name_missing"
grep -Fq 'BuildConfig.VERSION_CODE' "$UI" ||
    fail "version_code_missing"
grep -Fq '"Regler og beregningsgrunnlag"' "$UI" ||
    fail "rules_entry_missing"

echo "ABOUT_SCREEN=LOCKED"

echo
echo "===== contact ====="
grep -Eq '^internal const val APP_CONTACT_EMAIL: String = ".+@.+\..+"$' "$CONTACT" ||
    fail "configured_contact_email_missing"
grep -Fq 'Intent(Intent.ACTION_SENDTO)' "$UI" ||
    fail "ACTION_SENDTO_missing"
grep -Fq 'data = Uri.parse("mailto:")' "$UI" ||
    fail "mailto_scheme_missing"
grep -Fq 'putExtra(Intent.EXTRA_EMAIL, arrayOf(APP_CONTACT_EMAIL))' "$UI" ||
    fail "recipient_binding_missing"
grep -Fq 'putExtra(Intent.EXTRA_SUBJECT, "Ferietur – tilbakemelding")' "$UI" ||
    fail "email_subject_missing"
grep -Fq 'Text("Send e-post")' "$UI" ||
    fail "send_email_button_missing"

echo "CONTACT_POINT=LOCKED"

echo
echo "===== state/back integration ====="
grep -Fq 'val aboutOpen = mutableStateOf(false)' "$UI" ||
    fail "about_state_missing"
grep -Fq 'var aboutOpen by session.aboutOpen' "$UI" ||
    fail "about_state_not_ViewModel_bound"
grep -Fq 'BackHandler(enabled = aboutOpen || tripOverviewOpen || screen != FlowScreen.HOME)' "$UI" ||
    fail "About_not_in_system_Back_hierarchy"
grep -Fq 'aboutOpen -> aboutOpen = false' "$UI" ||
    fail "About_system_Back_action_missing"

grep -Fq 'implementation("androidx.datastore:datastore-preferences:1.2.1")' "$GRADLE" ||
    fail "DataStore_dependency_missing"

echo "APPINFO01_SOURCE_GATE=PASS"
