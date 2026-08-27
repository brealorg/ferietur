# A5.4R1 Step 1 Material pickers audit

Scope: Step 1 (`Turen`) only, on top of A5.4 Norwegian `HH:mm` formatting.

## Accepted interaction model

- Trip dates are one interval and are selected through Material 3 `DateRangePicker`.
- The visible field shows a compact Norwegian range such as `11.–18. august 2026`.
- Departure and return times use Material 3 `TimeInput`, not the clock dial and not the scroll wheel.
- Time input is fixed to 24-hour mode (`is24Hour = true`).
- The screen keeps a single-line trip name, one date-range field, and two time fields.
- Existing trip range validation and the chapter-20 day-trip gate are unchanged.

## Explicitly deferred bugs

1. Starting a new trip and backing out can persist repeated default `Sommerferie` drafts.
2. Device rotation currently returns the flow to the home/library screen.

These are intentionally deferred until the page-by-page UI review is complete.
