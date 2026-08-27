# CODEAUDIT FIX03 — CA-002 + CA-005 + CA-008 + CA-009

## One deliberate schema migration

FIX03 bumps local draft schema exactly once:

`5 -> 6`

Schema v6 adds:

- current `FinalizedTripSnapshot`;
- `finalizationHistory`;
- `migrationHistory`.

The snapshot is persisted as a versioned binary payload encoded into the existing
Properties draft format. The snapshot codec includes the full calculation,
day-audit evidence, roster/work blocks, findings and unresolved rules — not just
the inputs used to recreate them.

## CA-002 — finalized means frozen

A v6 draft reopening on SUMMARY restores the persisted snapshot directly.

`rebuildFinalizedSnapshot()` is retired.

Pre-v6 drafts could never contain a frozen result. If such a draft was saved on
SUMMARY, migration moves it back to CONTROL and records
`V6_LEGACY_SUMMARY_REFINALIZATION_REQUIRED`. The user explicitly finalizes it
once under the current build instead of Ferietur silently inventing a new
historical result.

When a finalized trip is edited, the current snapshot is moved to
`finalizationHistory` before editing. A new finalization creates a new snapshot
identity. Duplicating a trip deliberately clears both current and historical
finalizations.

## CA-005 — named development recovery removed

`LegacyTripRecovery.kt` and its old test are removed from the active source.

The old A39A1 damaged shape is handled only while migrating a pre-v6 schema. The
migration:

- does not inspect or branch on the trip title/name;
- requires the exact legacy date/time and travel-only damaged shape;
- records `V6_A39A1_TRAVEL_ONLY_PLAN_REPAIR`;
- is persisted by `TripDraftStore` as schema v6 during library load.

Manual-roster normalization is moved into the same versioned migration boundary.

## CA-008 — immutable UUID identity

Every new finalization uses a UUID.

Two finalizations in the same second therefore cannot collide.

Both PDF variants now include the full snapshot UUID in the export filename:

`ferietur-<date>-<snapshot-uuid>-<variant>.pdf`

## CA-009 — frozen build/rules/rate metadata

Each snapshot persists:

- `BuildConfig.VERSION_NAME`;
- `BuildConfig.VERSION_CODE`;
- `FERIETUR_RULESET_VERSION`;
- resolved salary-table id;
- salary-table effective date;
- salary-table source label.

PDF metadata reads those persisted snapshot fields. It no longer references the
retired hard-coded `FERIETUR_APP_VERSION`.

## Regression coverage

Focused FIX03 tests cover:

- full finalized-snapshot codec roundtrip equality;
- schema-v6 persistence of current snapshot + history + migration provenance;
- same-second UUID uniqueness;
- frozen build/ruleset/salary-table metadata;
- v5 SUMMARY -> CONTROL requiring explicit re-finalization;
- legacy A39A1 migration without a named-trip branch;
- v5 -> v6 persisted migration at TripDraftStore boundary.

Base FIX02R2 UI SHA256:
`53570b11fcfce8b75500724ca37df12447933ed19dfed046ca191c7529aa87f2`

FIX03 UI SHA256:
`e773117da890bc17e7761e6f6090044a0675e6f0c6a24c86016eae98b3b5761e`
