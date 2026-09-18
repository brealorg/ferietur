# EQS01B2B1 — active period relation and point 20.2

## Scope

B2B1 turns the period-level `HolidayWorkPlanRelation` introduced in B2A into live UI and
active-work calculation semantics for Oslo kommune's normal-roster comparison mode.

When the holiday work plan is `APPROVED_AND_TIMELY_NOTIFIED`:

- `WITHIN_HOLIDAY_WORK_PLAN` is ordinary planned service. Base salary is not added again.
  Applicable chapter-12 supplements are calculated from these periods.
- `BEYOND_HOLIDAY_WORK_PLAN` is the positive point-20.2 source and is calculated with
  hourly pay plus the chapter-20 active multiplier.
- `NOT_CLARIFIED` remains fail-closed and emits `holiday-work-plan-scope-open`.

The stored normal roster is retained for comparison/audit but is not the positive point-20.2
classifier in this explicit holiday-plan path.

If the top-level work-plan status is `NOT_APPROVED_OR_LATE` or `NOT_CLARIFIED`, the period
relation does not by itself create point-20.2 money. Active periods remain open.

## UI

The period editor exposes "Forhold til feriearbeidsplanen" for:

- active work;
- active night watch;
- travel with supervision responsibility.

For an approved and timely notified holiday plan, an applicable active period with
`NOT_CLARIFIED` blocks progression from the work-plan screen until the relation is selected.

## Compatibility

A null top-level `HolidayWorkPlanStatus` keeps the pre-EQS predecessor behavior for migration
and historical qualification tests only.

## Deferred

B2B1 deliberately does not change:

- `TravelDutyStatus` or pay/worktime semantics for travel without supervision responsibility;
- resting-night selection against the ground roster;
- final release status.

Those are separate B2B2/B2C qualification slices.
