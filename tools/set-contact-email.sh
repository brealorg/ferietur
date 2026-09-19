#!/usr/bin/env bash
# Replace the public contact address everywhere it is published: the in-app mailto target,
# the three copies of the privacy policy and the PLAY01 contract that checks the policy.
# Usage: tools/set-contact-email.sh kontakt@example.org
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

NEW="${1:-}"
case "$NEW" in
    *[[:space:]]*|'') echo "STOP=USAGE tools/set-contact-email.sh <adresse>"; exit 64 ;;
    ?*@?*.?*) ;;
    *) echo "STOP=INVALID_EMAIL"; exit 65 ;;
esac

CONTACT_FILE="app/src/main/java/app/ferietur/ui/AppInfoContact.kt"
OLD="$(sed -n 's/^internal const val APP_CONTACT_EMAIL: String = "\(.*\)"$/\1/p' "$CONTACT_FILE")"
[ -n "$OLD" ] || { echo "STOP=CURRENT_EMAIL_NOT_FOUND"; exit 66; }
[ "$OLD" != "$NEW" ] || { echo "CONTACT_EMAIL=UNCHANGED"; exit 0; }

FILES=(
    "$CONTACT_FILE"
    PRIVACY.md
    play/privacy-policy.md
    play/privacy-policy.html
    docs/privacy/index.html
    tools/play01-contract.sh
)

for file in "${FILES[@]}"; do
    [ -f "$file" ] || { echo "STOP=MISSING_FILE file=$file"; exit 67; }
    OLD="$OLD" NEW="$NEW" python3 - "$file" <<'PY'
import os, sys
from pathlib import Path
path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
path.write_text(text.replace(os.environ["OLD"], os.environ["NEW"]), encoding="utf-8")
PY
done

if grep -rFq --exclude-dir=.git --exclude-dir=audit-history "$OLD" .; then
    echo "STOP=OLD_EMAIL_STILL_PRESENT"
    grep -rFn --exclude-dir=.git --exclude-dir=audit-history "$OLD" . || true
    exit 68
fi

bash tools/generate-source-sha256.sh >/dev/null
echo "CONTACT_EMAIL=UPDATED"
echo "REMINDER=Publish docs/privacy/index.html (GitHub Pages) and update the contact address in Play Console."
