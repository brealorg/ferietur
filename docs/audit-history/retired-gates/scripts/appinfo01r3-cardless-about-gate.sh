#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"

fail() {
    echo "APPINFO01R3_CARDLESS_ABOUT_GATE=FAIL reason=$1"
    exit 1
}

[[ -f "$UI" ]] || fail "FerieturApp_missing"

ACTUAL="$(sha256sum "$UI" | awk '{print $1}')"
[[ "$ACTUAL" == "c955038fd2e73abcbe0ba37ce0577b88be652f851c044a76a84b64bfe870b80c" ]] ||
    fail "unexpected_UI_SHA256_$ACTUAL"

python3 - "$UI" <<'PY'
from pathlib import Path
import sys

text = Path(sys.argv[1]).read_text(encoding="utf-8")
start = text.index("@Composable\nprivate fun AboutFerieturScreen(")
end = text.index("\n@Composable\nprivate fun HomeScreen(", start)
about = text[start:end]

checks = {
    "intro_title": 'Text(\n                    "Ferietur",' in about,
    "inline_warning": 'Icons.Rounded.Warning' in about and 'tint = MaterialTheme.colorScheme.error' in about,
    "inline_privacy": 'MethodSectionHeading("Personvern")' in about,
    "contact_list_item": '"Send tilbakemelding"' in about and 'supportingContent = {' in about,
    "contact_click": '.clickable { openContactEmail() }' in about,
    "rules_list_item": '"Regler og beregningsgrunnlag"' in about,
    "version": 'BuildConfig.VERSION_NAME' in about,
}

for name, ok in checks.items():
    if not ok:
        raise SystemExit(f"APPINFO01R3_CARDLESS_ABOUT_GATE=FAIL reason={name}_missing")

# The About page itself must be card/surface-free. AlertDialog is allowed.
if "Surface(" in about:
    raise SystemExit("APPINFO01R3_CARDLESS_ABOUT_GATE=FAIL reason=Surface_remains_in_About")

if "OutlinedButton(" in about:
    raise SystemExit("APPINFO01R3_CARDLESS_ABOUT_GATE=FAIL reason=outlined_contact_button_remains")

if 'Text(\n                        APP_CONTACT_EMAIL' in about:
    raise SystemExit("APPINFO01R3_CARDLESS_ABOUT_GATE=FAIL reason=contact_email_visible")

# Preserve fixed direct intent dispatch.
if 'data = Uri.fromParts("mailto", APP_CONTACT_EMAIL, null)' not in about:
    raise SystemExit("APPINFO01R3_CARDLESS_ABOUT_GATE=FAIL reason=mailto_recipient_regressed")
if "resolveActivity(context.packageManager)" in about:
    raise SystemExit("APPINFO01R3_CARDLESS_ABOUT_GATE=FAIL reason=resolveActivity_regressed")
if "catch (_: ActivityNotFoundException)" not in about:
    raise SystemExit("APPINFO01R3_CARDLESS_ABOUT_GATE=FAIL reason=email_failure_handling_regressed")

print("ABOUT_SURFACE_COUNT=0")
print("CONTACT_OUTLINED_BUTTON=RETIRED")
print("APPINFO01R3_CARDLESS_ABOUT_GATE=PASS")
PY
