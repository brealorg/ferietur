#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() {
    echo "SOURCE_SHA256_VERIFY=FAIL reason=$1"
    exit 1
}

[[ -f SOURCE-SHA256SUMS.txt ]] || fail "manifest_missing"

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT

python3 - "$TMP" <<'PY'
from pathlib import Path
import hashlib
import sys

root = Path('.').resolve()
out = Path(sys.argv[1])
excluded_dir_names = {'.git', '.gradle', '.idea', 'build'}
excluded_file_names = {'SOURCE-SHA256SUMS.txt', 'local.properties', '.DS_Store'}

rows = []
for path in root.rglob('*'):
    if not path.is_file():
        continue
    rel = path.relative_to(root)
    parts = rel.parts
    if any(part in excluded_dir_names for part in parts[:-1]):
        continue
    if path.name in excluded_file_names or path.suffix == '.iml':
        continue
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    rows.append((rel.as_posix(), digest))
rows.sort()
out.write_text(''.join(f'{digest}  {rel}\n' for rel, digest in rows), encoding='utf-8')
PY

cmp -s SOURCE-SHA256SUMS.txt "$TMP" || {
    echo "===== canonical source manifest diff ====="
    diff -u SOURCE-SHA256SUMS.txt "$TMP" || true
    fail "manifest_not_complete_or_current"
}

sha256sum -c SOURCE-SHA256SUMS.txt >/dev/null || fail "sha256sum_check_failed"
COUNT="$(wc -l < SOURCE-SHA256SUMS.txt | tr -d ' ')"
echo "SOURCE_SHA256_ENTRY_COUNT=$COUNT"
echo "SOURCE_SHA256_COMPLETENESS=PASS"
echo "SOURCE_SHA256_VERIFY=PASS"
