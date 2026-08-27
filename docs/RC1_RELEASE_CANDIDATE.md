# RC1 — release candidate qualification

## Scope

RC1 is the first release-candidate checkpoint after RELEASE01. It deliberately changes only Android package version identity and release-candidate documentation/tooling.

Product identity:

- application ID: `app.ferietur`
- versionCode: `50`
- versionName: `0.5.4-rc1`

## Permitted source mutations from RELEASE01

- `app/build.gradle.kts`: `versionCode 49 -> 50` and `versionName 0.5.4-r2 -> 0.5.4-rc1` only;
- RC1 documentation and semantic release gates;
- canonical `SOURCE-SHA256SUMS.txt` regenerated after those changes.

No production Kotlin, manifest, dependency coordinate, persistence schema, tariff amount, calculation formula or security-policy change is permitted.

## Qualification gates

RC1 requires:

1. exact RELEASE01 canonical baseline identity and semantic source gates;
2. production Kotlin and AndroidManifest byte identity with RELEASE01;
3. dependency-verification metadata byte identity with RELEASE01;
4. exact two-field Gradle version mutation only;
5. canonical RC1 source manifest completeness and SHA-256 verification;
6. strict JVM tests, release lint, debug/release APK builds and androidTest build;
7. debug and release `output-metadata.json` proving applicationId/versionCode/versionName;
8. debug APK in-place upgrade over the installed RELEASE01 lineage using `adb install -r` without `pm clear`;
9. a namespaced cache sentinel surviving that upgrade, proving app-private data preservation;
10. all six CA-010 instrumentation tests green after the upgrade;
11. no app fatal/ANR signal and a final launcher launch sanity;
12. canonical source and evidence bundles produced from the qualified tree.

## Signing state

The installed development lineage is debug-signed and RC1 uses the debug APK only for the in-place device qualification. The source tree has no release `signingConfig`; `assembleRelease` therefore yields `app-release-unsigned.apk`.

RC1 must not generate a release keystore automatically. A production/direct-distribution signing identity is a separate security decision because losing or replacing that key breaks future APK upgrade continuity.

Resulting state when RC1 is green:

- source/runtime candidate: qualified;
- current debug-lineage upgrade path: qualified;
- unsigned release-build output: qualified as a build artifact only;
- public direct-APK distribution: blocked until signing identity is established.
