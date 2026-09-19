# CODEREVIEW01 — hardening after external source review

## Status

Built and verified 2026-09-19 on the developer machine:
`testDebugUnitTest lintRelease assembleRelease` with `--offline --dependency-verification=strict`
passed, and the minified, release-signed APK (2.1 MB) was installed in place over 0.6.0 and
manually reviewed: start-up with existing library, opening a finalized trip, both PDF variants
and sharing, `.ferietur` export/import, the Norwegian-time note in step 1 and the DST finding.
The Android instrumentation tests were not run as part of this review.

No calculation rule, rate, salary table or persisted schema is changed.
`FERIETUR_RULESET_VERSION` and `SavedTripDraftCodec.SCHEMA_VERSION` are untouched.

## SECURITY04 — trip IDs from imported backups are untrusted

Trip IDs were read from `.ferietur` backups without validation and used directly as file and
directory names (`trip-drafts/<id>.properties`, `trip-draft-backups/<id>/`). An ID such as
`../../x` wrote outside the store, and deleting a trip with ID `..` would have run
`deleteRecursively()` on `filesDir`. Impact was limited to the app's own sandbox and required
the user to import a manipulated file.

- `SafeStorageId` (domain): `[A-Za-z0-9_-]{1,64}`. App-generated UUIDs and all existing test IDs
  satisfy it. Enforced in `SavedTripDraftCodec` on both read and write.
- `TripDraftStore.fileFor/backupDirectoryFor`: validates again and verifies canonical-path
  containment (defense in depth).
- `PdfExporter.createBlocking`: validates `snapshot.id` before using it in the PDF file name.
- Tests: `SafeStorageIdTest`, `TripDraftStoreTest.hostileTripIdsCanNeverAddressPathsOutsideTheStore`,
  `TripLibraryBackupCodecTest.backupWithPathTraversalTripIdIsRejectedBeforeAnyDraftIsReturned`.

## TIME01 — wall-clock time, time zones and DST

The domain uses `LocalDateTime` and has no time-zone model. Decision: keep that model, make the
assumption explicit and surface the one case that can be detected.

- Step 1 and the PDF ("Grunnlaget som er brukt") now state that all times are Norwegian time.
- `ClockChangePolicy` reports a `REVIEW` control finding when a registered period crosses the
  Europe/Oslo change to/from daylight saving time, showing registered vs. elapsed duration.
  It never changes an amount. Finalized snapshots are frozen and are not re-evaluated.
- Tests: `ClockChangePolicyTest`.

Not solved: departure/arrival entered in two different local time zones cannot be detected.

## STORAGE02 — transient read errors are not corruption

`classifyReadFailure` mapped every failure to `Corrupt`, so a transient `IOException` triggered
restore-from-backup over the primary file. `IOException` now yields `TripStorageIssueKind.IO_ERROR`
and leaves primary and backups untouched. `IO_ERROR` already blocks backup export.

## EXPORT02 — PDF export hygiene

- `PdfDocument` is closed in `finally`; a half-written PDF is deleted on failure.
- PDFs older than 24 h are pruned from `cache/exports` whenever a new export is created
  (`PdfExportRetentionTest`).
- The share intent carries `ClipData` so the read grant follows the URI through the chooser.

## COROUTINE01 — cancellation-safe result handling

`runCatching` around suspend calls also captured `CancellationException`. The ten coroutine call
sites in `FerieturApp.kt` now use `runCatchingCancellable` (`CoroutineResultsTest`). The two
non-suspending uses are unchanged.

## BUILD02 — R8 and resource shrinking for release

`release` now sets `isMinifyEnabled` and `isShrinkResources` with `proguard-android-optimize.txt`
and `app/proguard-rules.pro`. Expected effect: a much smaller APK/AAB, mainly because
`material-icons-extended` is no longer shipped whole. `androidTest` runs against the unminified
debug build, so R8-specific regressions are only caught by reviewing the release APK:
import/export of `.ferietur`, opening an existing finalized trip, both PDF variants, first-run
disclaimer and the "Hva er nytt" prompt. Archive `app/build/outputs/mapping/release/mapping.txt`
with each release. Revert by removing the `release` block if a regression cannot be resolved.

## CI01 — GitHub Actions

`.github/workflows/ci.yml` verifies the source checksum manifest and runs
`testDebugUnitTest lintRelease assembleRelease` with strict dependency verification.
Untested: the runner must be able to obtain the compileSdk 37 platform.

## Contact address

`tools/set-contact-email.sh <adresse>` replaces the public contact address in the app, the three
privacy-policy copies and `tools/play01-contract.sh`, then regenerates the checksum manifest.
A privacy contact is required by Google Play, so the address must be replaced, not removed.
The old address remains in Git history and in already published APKs.

## GATE02 — stale assertions in `tools/current-source-contract.sh`

Five assertions no longer matched the published 0.6.0 source and failed before any
CODEREVIEW01 change was applied: `previousScreen(...)` gained a `workPlanBasis` argument,
draft schema is 10 (asserted 6), the package ruleset is 2026.4 (asserted 2026.3), the legacy
pin moved to `LEGACY_DOK25_2026_2028_RULESET_VERSION`, and the snapshot write format is 7
(asserted 4). They now assert the current values. The milestone contracts that still blocked `tools/source-smoke.sh` are handled in GATE03.

## GATE03 — milestone contracts unpinned, `source-smoke.sh` passes again

`release01-contract.sh` and `play01-contract.sh` asserted internal status wording in README
(CA-012, CA-010, "schema v6", "PLAY01 canonical successor") and a pinned `versionCode = 52` /
`versionName = "0.5.5"`. README is now a public product page and versions move per release, so
those assertions were retired. All other assertions (manifest, backup rules, bundle language
split, privacy policy, icons, signing hygiene) are kept. `uxfix01-contract.sh` failed for a real
reason, see UXFIX01R1.

## UXFIX01R1 — restored `workplan-screen` test tag

`UxFix01RuntimeTest` waits for the tag `workplan-screen`, but the tag disappeared when the
work-plan editor was unified into `WorkPlanEditorScreen`. `TripPlanScreen` now passes a
debug-only `Modifier.testTag("workplan-screen")` again. Not verified on a device.

## BUILD03 — `staging` build type in the Ferietur DEV slot

`staging` = the `release` configuration (R8, resource shrinking, not debuggable) with the same
application ID (`app.ferietur.dev`), label and debug signature as the debug build. It therefore
replaces "Ferietur DEV" in place instead of adding another app, and real trip data in
"Ferietur" is never touched:

```bash
tools/gradle.sh --offline --dependency-verification=strict installStaging   # minified DEV
tools/gradle.sh --offline --dependency-verification=strict installDebug     # back to normal DEV
```

DEV data survives both directions. "Om Ferietur" shows the version suffix `-dev-r8` while the
minified build is installed. Never distribute this variant; it is debug-signed.

## RELEASE02 — 0.6.1 (versionCode 55) source bump

Bundled release notes for 55 state that no calculation changes. `AppUpdatePresentationTest` no
longer pins a literal version; it requires the Gradle version to have matching bundled release
notes. `release/current.env` opens the 0.6.1 transaction with every artifact `PENDING`; the
full 0.6.0 record is preserved in `release/history/0.6.0.env`.

## LICENSE01 — GPL-3.0-only

`LICENSE` is the verbatim GNU GPL v3 text. README states the licence, the no-warranty position
and that the name "Ferietur" and the icon are not licensed for modified versions. All runtime
dependencies are Apache-2.0 and PDF generation uses the platform `PdfDocument`.

## Deliberately not changed

- `specialOvertimeFraction133 = 1.3333333333`: only used for an informational, non-included
  "possible difference" line; it is part of the rate-set contract asserted by `TariffRateSetTest`
  and the catalog snapshot tests. Not worth a rate-set change.
- Splitting `FerieturApp.kt` and `calculatePreliminaryFromProjectedBlocks`: large mechanical
  refactors that must be done with a compiler and the full test suite available.
- Moving `docs/` history: several contract scripts reference these paths.
