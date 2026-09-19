#!/usr/bin/env bash
# Point README at a newly published direct APK. Run this only after the signed APK has been
# uploaded to the GitHub release for the current Gradle version.
# Usage: tools/update-readme-release.sh /path/to/Ferietur-<version>.apk
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

APK="${1:-}"
[ -n "$APK" ] && [ -f "$APK" ] || { echo "STOP=USAGE tools/update-readme-release.sh <signed-apk>"; exit 64; }

NEW_NAME="$(sed -n 's/^[[:space:]]*versionName = "\(.*\)"$/\1/p' app/build.gradle.kts | head -1)"
NEW_CODE="$(sed -n 's/^[[:space:]]*versionCode = \([0-9][0-9]*\)$/\1/p' app/build.gradle.kts | head -1)"
NOTES="release/notes/$NEW_NAME.md"
[ -f "$NOTES" ] || { echo "STOP=RELEASE_NOTES_MISSING file=$NOTES"; exit 65; }
[ "$(basename "$APK")" = "Ferietur-$NEW_NAME.apk" ] || { echo "STOP=APK_NAME_MUST_BE Ferietur-$NEW_NAME.apk"; exit 66; }

NEW_SHA="$(sha256sum "$APK" | awk '{print $1}')"

NEW_NAME="$NEW_NAME" NEW_CODE="$NEW_CODE" NEW_SHA="$NEW_SHA" NOTES="$NOTES" python3 - <<'PY'
import os, re, sys
from pathlib import Path

new_name, new_code, new_sha = os.environ["NEW_NAME"], os.environ["NEW_CODE"], os.environ["NEW_SHA"]
notes = Path(os.environ["NOTES"]).read_text(encoding="utf-8").strip() + "\n\n"
readme = Path("README.md")
text = readme.read_text(encoding="utf-8")

published = re.search(r"\*\*Siste publiserte APK:\*\* `([^`]+)` \(`versionCode (\d+)`\)", text)
if not published:
    sys.exit("STOP=README_PUBLISHED_LINE_NOT_FOUND")
old_name, old_code = published.group(1), published.group(2)
if old_name == new_name:
    sys.exit("STOP=README_ALREADY_POINTS_AT_" + new_name)

old_sha = re.search(r"### APK SHA-256\s+```(?:text)?\s+([0-9a-f]{64})\s+```", text)
if not old_sha:
    sys.exit("STOP=README_APK_SHA_BLOCK_NOT_FOUND")

section = re.search(r"^## Release " + re.escape(old_name) + r"\n.*?(?=^## )", text, re.S | re.M)
if not section:
    sys.exit("STOP=README_RELEASE_SECTION_NOT_FOUND")

# Replace the release section first so the old version numbers inside it are not rewritten.
text = text[:section.start()] + "\0NOTES\0" + text[section.end():]
text = text.replace(old_sha.group(1), new_sha)
text = text.replace(old_name, new_name)
text = text.replace(f"versionCode {old_code}", f"versionCode {new_code}")
text = text.replace(old_name.replace(".", ""), new_name.replace(".", ""))  # heading anchors
text = text.replace("\0NOTES\0", notes)
readme.write_text(text, encoding="utf-8")
print(f"README_PUBLISHED_RELEASE={new_name}")
print(f"README_PUBLISHED_VERSION_CODE={new_code}")
print(f"README_APK_SHA256={new_sha}")
PY

bash tools/generate-source-sha256.sh >/dev/null
echo "README_RELEASE_UPDATE=PASS"
echo "REMINDER=Update README_PUBLISHED_* and DIRECT_APK_* in release/current.env, then commit."
