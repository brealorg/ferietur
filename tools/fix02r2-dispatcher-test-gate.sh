#!/usr/bin/env bash
set -u
set -o pipefail

TEST="app/src/test/java/app/ferietur/data/TripRepositoryTest.kt"

fail() {
    echo "FIX02R2_DISPATCHER_TEST_GATE=FAIL reason=$1"
    exit 1
}

[[ -f "$TEST" ]] || fail "TripRepositoryTest_missing"

ACTUAL="$(sha256sum "$TEST" | awk '{print $1}')"
[[ "$ACTUAL" == "ed067c01d69b51f5911ada077db6559e56fdc357c7fde3e43ceb8532bf1391bc" ]] ||
    fail "unexpected_test_SHA256_$ACTUAL"

grep -Fq 'val ioThread = AtomicReference<Thread>()' "$TEST" ||
    fail "expected_thread_identity_holder_missing"
grep -Fq 'Thread(runnable, "ferietur-io-test").also(ioThread::set)' "$TEST" ||
    fail "injected_executor_thread_capture_missing"
grep -Fq 'val threads = mutableListOf<Thread>()' "$TEST" ||
    fail "recording_source_not_recording_thread_identity"
grep -Fq 'assertTrue(source.threads.all { it === expectedThread })' "$TEST" ||
    fail "thread_identity_assertion_missing"

if grep -Fq 'source.threads.all { it == "ferietur-io-test" }' "$TEST"; then
    fail "brittle_thread_name_assertion_remains"
fi

echo "FIX02R2_DISPATCHER_TEST_GATE=PASS"
