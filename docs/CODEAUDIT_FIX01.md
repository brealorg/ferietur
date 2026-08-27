# CODEAUDIT FIX01 — CA-001 + CA-004 + CA-006

## CA-001 — full selected date range

The old UI helper silently capped the inclusive date list at 31 calendar days.

FIX01 moves calendar coverage into `TripDateRangePolicy` and removes the cap.
The production calculation entry points call `requireCompleteCoverage()` before
delegating to `TripPlanEngine`.

Regression coverage:
- 31 days;
- 32 days;
- 46-day multi-month range;
- return travel on the last day of that long range;
- cross-year range;
- explicit rejection of an incomplete date list.

## CA-004 — new-trip defaults

The repeated development defaults:
- 10–16 August 2026;
- `Sommerferie`;

are retired.

`NewTripDefaultsFactory` derives the start from an injected `Clock` and uses a
blank title. The normal runtime factory uses the device/system timezone.

The existing salary step / weekly basis / weekend profile editor start values
remain provisional only. The mandatory payslip confirmation is still the flow
gate, and its supporting copy now explicitly says those values are unconfirmed
until the user checks them.

No saved-trip schema change is made in FIX01.

## CA-006 — salary table validity

`OsloSalaryTable2026` now exposes a typed `effectiveFromDate`.

`OsloSalaryTables` is an effective-dated catalog. It deliberately resolves a
trip only when one known table covers the whole range. This lets a future table
be added without silently applying one table across a boundary.

For the current catalog:
- earliest supported date = 2026-05-01;
- earlier dates are disabled in the Material 3 range picker;
- an old saved trip before that date is reopened at `Turen`, not at a
  calculation/summary screen;
- payslip confirmation is cleared for such a legacy unsupported range;
- the flow cannot leave `Turen` while the range is unsupported;
- every production calculation entry point has a hard
  `OsloSalaryTables.requireSupportedRange()` guard.

No tariff amount changes are made. The existing 2026 salary values remain
byte-for-byte the same.

## Android UI basis

Material 3's `DateRangePickerState` supports `SelectableDates`; disallowed dates
appear disabled. FIX01 uses that supported API rather than a custom date-picker
workaround.

Base UI SHA256:
`c955038fd2e73abcbe0ba37ce0577b88be652f851c044a76a84b64bfe870b80c`

FIX01 UI SHA256:
`d5ba3d15638314d0480de9fdbc27eb67c4d4529ee79b3d03d24b44e736ec51cb`
