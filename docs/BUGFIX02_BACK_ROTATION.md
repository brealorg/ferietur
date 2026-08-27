# BUGFIX02 — system back + configuration state retention

These are the two old user-reported bugs being fixed in this slice.

## 1. Edge-swipe / system Back exits the whole trip

Cause:

The flow has its own `screen`, trip overview and `goBack()` hierarchy, and the
visible Back buttons use that hierarchy. There was no root Compose
`BackHandler`, so the Android system Back gesture fell through to the Activity
and could finish/leave it.

Fix:

A root `BackHandler` is enabled only while Ferietur owns an in-app back target:

- Trip overview -> close overview to Home.
- A trip step -> run the existing `goBack()`.
- Home -> no handler; Android owns Back normally.

This deliberately uses the same existing navigation behavior as the visible
Back controls. Nested Material/Compose handlers still have precedence.

Android Developers recommends `BackHandler` when a Compose app needs to
intercept system Back without consuming predictive-back progress.

## 2. Rotate to landscape -> active trip returns to Home

Cause:

The entire active trip/session was stored in `remember { mutableStateOf(...) }`.
`remember` survives recomposition, but not Activity recreation caused by
configuration changes such as rotation.

Fix:

The screen-level active-trip state moves to an Activity-scoped
`FerieturSessionViewModel`. `FerieturApp()` obtains it with Compose `viewModel()`.

This preserves:

- active trip id;
- current step;
- trip overview/direct-navigation state;
- trip dates/times and salary choices;
- roster and plans;
- travel state;
- settlement state;
- finalized summary and validation UI state.

`TripDraftStore` remains the durable local persistence layer for app/process
resume. This ViewModel slice is specifically for configuration-change
continuity.

Android Developers documents ViewModel as retained for the Activity scope and
recommends it for screen-level state that must survive configuration changes.

## Security baseline

Baseline: SECURITY03 hardened.

The UI source started at:

`9baf33a36683064cbb5ef3b53550158859d8cbb1b7aa7cdb2fac481450ace9dd`

Expected BUGFIX02 UI:

`da9103671da3168336561379db8368a49136a8b0c436e4b27410bcc1953b3db7`

The SECURITY03 backup policy and strict Gradle dependency verification remain
mandatory. Adding `lifecycle-viewmodel-compose:2.11.0` causes verification
metadata to be updated against the real Android task graph and then proven
under strict mode.

No tariff/calculation/control/PDF rules are changed.
