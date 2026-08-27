#!/usr/bin/env bash
set -u
set -o pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1
fail() { echo "GATE01_CONTRACT=FAIL reason=$1"; exit 1; }

[[ -f SOURCE-SHA256SUMS.txt ]] || fail 'canonical_top_level_source_checksum_manifest_missing'
[[ -x tools/verify-source-sha256.sh ]] || fail 'canonical_source_checksum_verifier_missing'
[[ -f docs/audit-history/checksums/SOURCE-SHA256SUMS.pre-codeaudit01.txt ]] || fail 'historical_checksum_archive_missing'
[[ -f docs/audit-history/retired-gates/source-smoke.pre-gate01.sh ]] || fail 'historical_source_smoke_archive_missing'
[[ -f tools/current-source-contract.sh ]] || fail 'current_source_contract_missing'
[[ -f tools/source-smoke.sh ]] || fail 'source_smoke_wrapper_missing'
[[ -f tools/play01-contract.sh ]] || fail 'play01_contract_missing'
[[ -f tools/uxfix01-contract.sh ]] || fail 'uxfix01_contract_missing'

for f in tools/final01-contract.sh tools/rc1-contract.sh tools/icon01-contract.sh tools/sign01-contract.sh tools/sign01a2-contract.sh; do
    [[ -f "$f" ]] || fail "historical_phase_contract_missing_${f//\//_}"
done

for pattern in 'a*-smoke.sh' 'a*-source-smoke.sh' 'appinfo*-gate.sh' 'appinfo*-source-gate.sh' 'bugfix*-gate.sh' 'bugfix*-source-gate.sh'; do
    if compgen -G "tools/$pattern" >/dev/null; then
        compgen -G "tools/$pattern" || true
        fail "historical_phase_gate_still_active"
    fi
done

LINES="$(wc -l < tools/source-smoke.sh)"
[[ "$LINES" -lt 80 ]] || fail "source_smoke_still_monolithic_lines_$LINES"
grep -Fq 'current-source-contract.sh' tools/source-smoke.sh || fail 'source_smoke_not_wired_to_current_contract'
grep -Fq 'release01-contract.sh' tools/source-smoke.sh || fail 'source_smoke_not_wired_to_release_contract'
grep -Fq 'play01-contract.sh' tools/source-smoke.sh || fail 'source_smoke_not_wired_to_play_contract'
grep -Fq 'uxfix01-contract.sh' tools/source-smoke.sh || fail 'source_smoke_not_wired_to_uxfix_contract'
for retired in final01-contract.sh rc1-contract.sh icon01-contract.sh sign01-contract.sh sign01a2-contract.sh; do
    if grep -Fq "$retired" tools/source-smoke.sh; then
        fail "historical_phase_contract_still_active_${retired}"
    fi
done

if grep -Eqi 'sha256sum.*(FerieturApp|app/src)|EXPECTED_.*UI_SHA' tools/current-source-contract.sh tools/source-smoke.sh; then
    fail 'active_semantic_source_gate_contains_historical_exact_source_sha_lock'
fi

if grep -Fq 'SOURCE-SHA256SUMS.txt' apply-build-install.sh; then
    fail 'apply_script_directly_pins_source_checksum_manifest'
fi
grep -Fq 'tools/source-smoke.sh' apply-build-install.sh || fail 'apply_script_not_using_current_source_gate'
grep -Fq 'bundleRelease' apply-build-install.sh || fail 'play_apply_script_missing_bundleRelease'
if grep -Eq 'adb[^\n]*(install|uninstall|shell pm clear)' apply-build-install.sh; then
    fail 'play_apply_script_must_not_mutate_device'
fi

"$ROOT/tools/verify-source-sha256.sh" >/dev/null || fail 'canonical_source_manifest_verification_failed'

echo 'GATE01_HISTORICAL_GATES_ARCHIVED=PASS'
echo 'GATE01_PRE_PLAY_PHASE_CONTRACTS=RETAINED_NOT_ACTIVE'
echo 'GATE01_ACTIVE_GATE_SET=SUCCESSOR_SEMANTIC_PLUS_CANONICAL_PLAY01_QUAL01_MANIFEST'
echo 'GATE01_FINAL_CHECKSUM_REGEN=PASS_QUAL01'
echo 'GATE01_CONTRACT=PASS'
