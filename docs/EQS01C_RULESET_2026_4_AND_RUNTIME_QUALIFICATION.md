# EQS01C — ruleset 2026.4 and runtime qualification

## Why the ruleset changes

EQS01 changes calculation semantics, not merely presentation:

- point 20.2 no longer uses the stored normal roster as the automatic positive classifier;
- active periods use explicit holiday-work-plan relation;
- travel without supervision responsibility uses explicit duty status;
- resting night watch uses the specific point-20.4 holiday-stay treatment in the explicit path.

The current calculation ruleset therefore moves from `2026.3` to `2026.4`.

## Persistence

No new persistence format is introduced in EQS01C. Draft schema 8 and finalized snapshot format 6
already persist the factual period metadata introduced in EQS01B2A.

Historical finalized snapshots keep their stored ruleset version. New runtime calculations and new
finalizations use ruleset `2026.4`.

## Qualification

EQS01C requires:

1. focused EQS01 semantics and provenance tests;
2. complete debug unit-test suite;
3. debug APK build;
4. DEV-package installation without touching `app.ferietur`;
5. launch/runtime crash smoke;
6. manual visual review before commit/release.

The EQS01C1 preview does not commit, push or change the public app version.
