# A58R2 — Step 5 editor unification + 24h TimeInput

## Runtime findings from A58R1R1

The new compact day cards and Expressive add menu worked, but Step 5 exposed two
different editing generations at once:

- new compact `Legg til periode` bottom sheet
- old full-day editor with large buttons, legacy type menu, and dial time picker

A58R2 removes that split.

## Main screen

- title shortened to **Arbeidsplan**
- intro keeps the same meaning but uses a calmer `bodyMedium`
- compact day cards and 24-hour timelines are retained
- each existing activity row is directly editable
- the day-level legacy chevron/editor is removed

## One period editor

Both new and existing periods now use the same `PlanPeriodEditorSheet`.

The sheet supports:

- date
- type
- Fra / Til
- Norwegian 24-hour `TimeInput`
- travel responsibility
- existing travel short-notice handling
- existing night/sleep handling
- next-day indication for cross-midnight periods
- Save
- Delete when editing

Moving an existing period to another trip date removes it from the old date and
adds it to the new date through the existing normalization logic.

## Removed legacy Step 5 UI

- `DayPlanEditorSheet`
- `AddPeriodMenu`
- `PeriodTypeMenu`
- `Kopier turnusdelen som ligger i turen`
- `Sett dagen fri`
- Step 5 use of the old platform/dial `TimeButton`

## Expressive FAB

The official M3 Expressive FAB menu is retained, but labels are shortened to:

- Arbeid
- Reise
- Hvilende natt
- Annen

Icons are also reduced to 20 dp so the menu behaves more like a quick-action
menu and obscures less of the plan.

## Non-goals

No domain, tariff, overtime, resting-night, travel-payment, PDF, salary, roster
or flow formula is changed.
