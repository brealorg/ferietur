#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

python3 - <<'PY'
from pathlib import Path
import hashlib

root = Path('.').resolve()
out = root / 'SOURCE-SHA256SUMS.txt'

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
print(f'SOURCE_SHA256_ENTRY_COUNT={len(rows)}')
print('SOURCE_SHA256_GENERATED=PASS')
PY
