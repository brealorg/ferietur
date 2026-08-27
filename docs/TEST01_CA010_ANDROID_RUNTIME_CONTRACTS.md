# TEST01 — CA-010 Android runtime contracts

TEST01 adds one deliberately small on-device Compose/instrumentation suite.
Tariff mathematics and domain engines stay in the JVM suite; TEST01 covers only
Android framework/lifecycle boundaries that JVM tests cannot prove.

## Six contracts

1. `coldLaunchDoesNotCrash`
   - launches the real `MainActivity` with `ActivityScenario`;
   - normalizes the versioned first-run disclaimer if it is shown;
   - requires the Home/library surface to reach `RESUMED` without a crash.

2. `disclaimerAcceptancePersistsAcrossRestart`
   - accepts the real disclaimer when needed;
   - closes the Activity completely and launches a new Activity instance;
   - requires the disclaimer to stay dismissed and Home to load.

3. `systemBackFollowsAppHierarchy`
   - creates a namespaced test trip;
   - advances from `Turen` to `Lønn og betaling`;
   - invokes Android's `OnBackPressedDispatcher` twice;
   - requires METHOD -> TRIP -> HOME without finishing the Activity.

4. `rotationRetainsActiveTripAndStep`
   - creates a test trip and advances to `Lønn og betaling`;
   - requests the opposite physical orientation to force a configuration change;
   - requires the current step to survive;
   - returns to `Turen` and requires the entered trip title to survive too.

5. `storedDraftAndRecoverySurviveRealActivityLifecycle`
   - creates a valid schema-v6 backup under the app's real private draft tree;
   - places a corrupt primary file in front of it;
   - launches the real Activity and requires `TripDraftStore` recovery to surface;
   - closes the Activity, launches a new one, and requires the healed draft to
     still be resumable from the library.

6. `pdfShareUsesValidFileProviderContentUri`
   - prepares a PDF-shaped file in the real `cache/exports` FileProvider path;
   - calls the real `PdfExporter.sharePrepared` boundary;
   - captures the chooser without launching external UI;
   - requires `ACTION_SEND`, `application/pdf`, read-grant flag, the expected
     `${applicationId}.files` content authority, and a URI that can actually be
     opened through `ContentResolver`.

## Test-data safety

TEST01 does not clear app data and does not delete ordinary Ferietur drafts.
Temporary draft titles use the `__FERIETUR_TEST01_CA010__` prefix, recovery fixtures use their own
UUID, and teardown removes only those test artifacts. This allows the suite to
run against the connected development device without destroying the existing
local trip library.

## Build surface

TEST01 adds:

- `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`;
- Compose BOM-managed `androidx.compose.ui:ui-test-junit4` to `androidTest`;
- `RuntimeContractsTest.kt`;
- `tools/test01-runtime-contract-gate.sh` wired into the active source smoke.

No production Kotlin, tariff formula, salary table, persistence schema, manifest
security policy, or PDF implementation is changed by TEST01.
