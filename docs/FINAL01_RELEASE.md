# FINAL01 — promote RC1 to Ferietur 0.5.4

## Purpose

FINAL01 promotes the fully qualified, permanently signed RC1 lineage to the first final direct-APK version without changing product behavior.

Final package identity:

- application ID: `app.ferietur`
- versionCode: `51`
- versionName: `0.5.4`
- permanent signing certificate SHA-256: `9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7`

The predecessor installed on the qualification device is release-signed RC1 (`versionCode 50 / versionName 0.5.4-rc1`) with the same permanent certificate.

## Permitted source mutation

From canonical SIGN01A2 source, FINAL01 permits only:

- `app/build.gradle.kts`: `versionCode 50 -> 51`;
- `app/build.gradle.kts`: `versionName 0.5.4-rc1 -> 0.5.4`;
- FINAL01 documentation, test and semantic-gate tooling;
- canonical `SOURCE-SHA256SUMS.txt` regeneration after those intentional changes.

No production Kotlin, `app/src/main` resource, AndroidManifest, dependency coordinate, dependency-verification metadata, persistence schema, tariff amount, calculation formula or security policy mutation is permitted.

## Final signed-device qualification

FINAL01 must exercise the ordinary update path users will rely on after release:

1. build the final source offline with strict dependency verification;
2. align the unsigned release APK for 16 KiB shared-library alignment;
3. sign it externally with the established `ferietur-release` identity using APK Signature Scheme v2 + v3;
4. verify the signed artifact certificate, version and ZIP alignment before installation;
5. install a release-signed instrumentation APK against the still-installed RC1 target;
6. run `FinalPromotionStateTest.capturePreUpgradeState`, which measures the durable roots `files`, `shared_prefs`, `databases`, and `no_backup` and stores the exact path/size/content SHA-256 aggregate plus current draft count in a namespaced app-private sentinel;
7. install final `0.5.4` using `adb install -r`; no uninstall and no `pm clear` are permitted;
8. before a normal Activity launch, run `FinalPromotionStateTest.verifyPostUpgradeStateAndCleanup`, which requires exact durable-state digest/count/byte equality, unchanged draft-library count, no corrupt/unsupported/I/O storage issue, final version identity and successful sentinel cleanup;
9. run all six CA-010 Android runtime contracts against the actual final non-debuggable target;
10. verify the installed APK uses the permanent certificate, `run-as` is denied, no fatal/ANR signal is present, and a final launcher start succeeds;
11. remove the instrumentation package and checkpoint the final source/evidence/artifact hashes.

## Signing policy

Private signing material remains outside the canonical source and evidence trees. Gradle remains unsigned for release; FINAL01 uses Android SDK `zipalign` and `apksigner` externally. The established signing certificate must not change during final promotion.

The final distributable artifact is named:

```text
ferietur01-0.5.4-release-signed.apk
```

Public distribution is authorized only when the associated qualification output records `FINAL01=PASS` and `PUBLIC_DISTRIBUTION_READY=YES`.
