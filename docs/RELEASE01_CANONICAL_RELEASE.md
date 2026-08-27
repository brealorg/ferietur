# RELEASE01 — product identity and canonical release trust

RELEASE01 closes CODEAUDIT02 CA-012 Stage B and CA-013, and performs the late CA-015 dependency alignment after the correctness/persistence/runtime blockers are already green.

## Changes

- replaces the stale development-era README with the current architecture/release state;
- adds an adaptive launcher icon using Oslo yellow and Oslo dark blue;
- provides round-icon resources and an Android 13+ monochrome layer;
- wires `android:icon` and `android:roundIcon` in the application manifest;
- aligns the direct `androidx.core:core-ktx` declaration from 1.17.0 to the already intended/effective 1.18.0 graph;
- records the accepted Material3 alpha decision and direct-APK locale scope in ADR01;
- activates a canonical `SOURCE-SHA256SUMS.txt` with deterministic generation and completeness verification;
- changes GATE01 from “checksum regeneration deferred” to the final RELEASE01 canonical-manifest state.

## Non-goals

- no tariff amount or calculation formula changes;
- no saved-trip schema change;
- no production Kotlin behavior change;
- no broad `FerieturApp.kt` architecture rewrite (CA-011 remains post-V1);
- no Play App Bundle locale change (CA-014 remains conditional);
- no Material3 downgrade (CA-016 remains an explicit accepted risk).

## Release verification

The RELEASE01 bootstrap requires:

1. current semantic source contracts;
2. canonical source manifest completeness + `sha256sum` verification;
3. `releaseRuntimeClasspath` resolving direct/effective core-ktx 1.18.0;
4. strict JVM tests, release lint, debug/release APK builds and androidTest build;
5. absence of `MissingApplicationIcon` in the release lint report;
6. six CA-010 instrumentation tests green on the target Pixel 6;
7. no post-test app fatal/ANR signal;
8. manual launcher/Recents/themed-icon sanity where supported;
9. final canonical source and evidence checkpoints.
