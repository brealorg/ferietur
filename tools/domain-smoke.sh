#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
"$ROOT/tools/gradle.sh" --no-daemon :app:testDebugUnitTest
printf '%s\n' "FERIETUR01_DOMAIN_SMOKE=PASS"
