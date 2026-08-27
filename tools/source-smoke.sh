#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

"$ROOT/tools/current-source-contract.sh"
"$ROOT/tools/test01-runtime-contract-gate.sh"
"$ROOT/tools/security03-backup-policy-smoke.sh"
"$ROOT/tools/release01-contract.sh"
"$ROOT/tools/play01-contract.sh"
"$ROOT/tools/uxfix01-contract.sh"

printf '%s\n' \
  "ACTIVE_SOURCE_GATE_MODEL=SUCCESSOR_SEMANTIC" \
  "HISTORICAL_EXACT_TEXT_GATES=INACTIVE" \
  "HISTORICAL_PHASE_CONTRACTS=RETAINED_NOT_EXECUTED" \
  "TOP_LEVEL_SOURCE_SHA_MANIFEST=CANONICAL_PLAY01_QUAL01" \
  "FERIETUR01_SOURCE_SMOKE=PASS"
