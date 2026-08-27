# A54R3 — Step 1 nb-NO resource-context closure

## Runtime defect

A54R2 localized the date-range surface, but Material3 `TimeInput` still rendered
the internal labels `Hour` and `Minute` on the test device.

## Root cause addressed

Material3 resolves the time-input strings through the current Android resource
context. A picker-local `Configuration` is not sufficient when
`LocalContext.current.resources` remains English.

## Change

`MainActivity.attachBaseContext()` now creates the Activity resource context from
the existing configuration with locale `nb-NO`.

This is intentionally app-wide for the current Norwegian-first product phase.
It keeps Material3 components on the same Norwegian resource context rather
than maintaining separate localization mechanisms for date and time pickers.

## Expected manual result

The Step 1 time-input dialog should render:

- `Time`
- `Minutt`

The date-range picker remains Norwegian.

## Scope

No tariff, calculation, trip-plan, persistence, export, or remote-source logic
is changed.
