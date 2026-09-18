# EQS01B2B2 — travel duty UI, pay and worktime semantics

## Problem removed

The legacy normal-roster model used the stored ground roster to infer both whether travel without
supervision responsibility happened in ordinary working time and whether those minutes counted as
worktime or separate travel pay. EQS ID 53398 distinguishes those questions by actual duty status.

## Explicit model

For `TURNUS_PLUS_EXTERNAL` with the modern holiday-work-plan status present:

- `ON_DUTY`: ordinary travel parts count as worktime; no separate point-18.4 base travel line is
  added; when the holiday plan is approved, `HolidayWorkPlanRelation` decides whether the period
  is planned service or work beyond the holiday plan under point 20.2.
- `OFF_DUTY`: ordinary travel parts are paid under point 18.4 and do not count as worktime; the
  stored ground roster does not suppress this travel payment.
- `NOT_CLARIFIED`: ordinary travel pay fails closed, `travel-duty-status-open` is emitted, and
  unresolved rule `D25_20_3_TRAVEL_DUTY_STATUS` is exposed.

Night travel with sleep permission remains a separate point-20.3 passive-work rule and counts as
worktime time-for-time even when the ordinary travel status is `OFF_DUTY`.

## UI and validation

The unified period editor exposes `Vaktstatus under reisen` for travel without supervision
responsibility. Travel notice is shown only for `OFF_DUTY`. `ON_DUTY` additionally participates in
the holiday-work-plan relation selector. In normal-roster mode an unresolved duty status blocks
progression from the work-plan screen.

## Compatibility

Legacy/pre-EQS blocks without explicit duty status retain the old roster fallback in the worktime
control helper so historical predecessor tests remain interpretable. New Oslo planning cannot
progress with that unresolved state.

## Deferred

B2B2 does not change resting-night compensation. That remains release-blocking for EQS01B2C.
