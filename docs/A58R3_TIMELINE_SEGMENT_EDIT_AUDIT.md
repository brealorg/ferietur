# A58R3 — direct editing from timeline segments

## Runtime finding

A58R2 unified add/edit and made activity rows editable, but the colored period
segments in the 24-hour timeline were still visualization-only.

That violates the natural direct-manipulation expectation: if the user sees the
blue `07:00–15:00` segment, tapping that segment should edit that exact period.

## Change

`DayTimeline` now receives the real plan map and period-edit callback.

The Canvas uses pointer hit-testing:

- vertical position selects the visual lane / projected period
- horizontal position must fall on the colored interval, with 8 dp forgiveness
- the projected period is mapped back to its real source period with the
  existing `sourcePlanIndex`
- editing targets `projected.sourceDate`, preserving correct behavior for
  cross-midnight continuation fragments

The empty background track remains non-interactive.

Activity-row editing remains available as a second edit target.

No plan, tariff, calculation, travel, roster, persistence, PDF, salary or flow
semantics are changed.
