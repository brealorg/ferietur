# A58R2R1 — TripDayCard stale onClick compile hotfix

## A58R2 compiler failure

A58R2 intentionally removed the old day-level `DayPlanEditorSheet` and its
`onClick` callback from `TripDayCard`.

One line from the old card implementation remained:

`Modifier.fillMaxWidth().clickable(onClick = onClick)`

Because `TripDayCard` no longer has an `onClick` parameter, Kotlin correctly
failed with:

`Unresolved reference 'onClick'`

## Fix

The day card itself is now a non-clickable review surface. Existing periods are
edited through the explicit period-row edit affordance introduced by A58R2.

The stale modifier becomes simply:

`Modifier.fillMaxWidth()`

No period editor, FAB, TimeInput, tariff, domain, calculation or flow behavior
is otherwise changed.
