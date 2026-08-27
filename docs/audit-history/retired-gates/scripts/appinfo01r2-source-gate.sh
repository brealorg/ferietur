#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
PREFS="app/src/main/java/app/ferietur/ui/AppInfoPreferences.kt"
CONTACT="app/src/main/java/app/ferietur/ui/AppInfoContact.kt"
GRADLE="app/build.gradle.kts"

fail() {
    echo "APPINFO01R2_SOURCE_GATE=FAIL reason=$1"
    exit 1
}

for f in "$UI" "$PREFS" "$CONTACT" "$GRADLE"; do
    [[ -f "$f" ]] || fail "missing_${f//\//_}"
done

ACTUAL_UI="$(sha256sum "$UI" | awk '{print $1}')"
[[ "$ACTUAL_UI" == "886135df718ec374927a604efa00bbad9b80efe0839916c2e3bd366a67bdc1e2" ]] ||
    fail "unexpected_UI_SHA256_$ACTUAL_UI"

echo "===== first-run explicit acknowledgement ====="
grep -Fq 'const val CURRENT_DISCLAIMER_VERSION: Int = 2' "$PREFS" ||
    fail "disclaimer_version_not_bumped_to_2"
grep -Fq 'var disclaimerConfirmed by rememberSaveable { mutableStateOf(false) }' "$UI" ||
    fail "checkbox_state_missing"
grep -Fq 'role = Role.Checkbox' "$UI" ||
    fail "checkbox_role_missing"
grep -Fq 'value = disclaimerConfirmed' "$UI" ||
    fail "whole_row_toggle_missing"
grep -Fq 'onValueChange = { disclaimerConfirmed = it }' "$UI" ||
    fail "whole_row_toggle_action_missing"
grep -Fq 'checked = disclaimerConfirmed' "$UI" ||
    fail "Material3_Checkbox_missing"
grep -Fq 'onCheckedChange = null' "$UI" ||
    fail "child_checkbox_should_delegate_to_row"
grep -Fq '"Jeg har lest og forstått"' "$UI" ||
    fail "checkbox_label_missing"
grep -Fq 'enabled = disclaimerConfirmed && !disclaimerWriteInProgress' "$UI" ||
    fail "continue_not_gated_by_checkbox"
grep -Fq 'Text(if (disclaimerWriteInProgress) "Lagrer…" else "Fortsett")' "$UI" ||
    fail "continue_button_missing"
echo "FIRST_RUN_CHECKBOX=LOCKED"

echo
echo "===== contact UI privacy ====="
python3 - "$UI" <<'PY'
from pathlib import Path
import sys
text = Path(sys.argv[1]).read_text(encoding="utf-8")
visible = """Text(
                        APP_CONTACT_EMAIL,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )"""
if visible in text:
    raise SystemExit("APPINFO01R2_SOURCE_GATE=FAIL reason=contact_email_visible_as_Text")
print("CONTACT_EMAIL_NOT_RENDERED=LOCKED")
PY

grep -Fq '"Send en tilbakemelding til utvikleren."' "$UI" ||
    fail "contact_supporting_text_missing"
grep -Fq 'Text("Send e-post")' "$UI" ||
    fail "send_email_button_missing"

echo
echo "===== Android intent dispatch ====="
grep -Fq 'Intent(Intent.ACTION_SENDTO)' "$UI" ||
    fail "ACTION_SENDTO_missing"
grep -Fq 'data = Uri.fromParts("mailto", APP_CONTACT_EMAIL, null)' "$UI" ||
    fail "mailto_recipient_not_in_data_uri"
grep -Fq 'context.startActivity(emailIntent)' "$UI" ||
    fail "direct_startActivity_missing"
grep -Fq 'catch (_: ActivityNotFoundException)' "$UI" ||
    fail "ActivityNotFoundException_handling_missing"
if grep -Fq 'resolveActivity(context.packageManager)' "$UI"; then
    fail "package_visibility_sensitive_resolveActivity_preflight_remains"
fi
if grep -Fq 'enabled = canSendEmail' "$UI"; then
    fail "email_button_still_disabled_by_preflight"
fi
grep -Fq '"Kunne ikke åpne en e-postapp."' "$UI" ||
    fail "real_failure_copy_missing"
echo "EMAIL_INTENT_DISPATCH=LOCKED"

grep -Fq 'implementation("androidx.datastore:datastore-preferences:1.2.1")' "$GRADLE" ||
    fail "DataStore_dependency_missing"

echo "APPINFO01R2_SOURCE_GATE=PASS"
