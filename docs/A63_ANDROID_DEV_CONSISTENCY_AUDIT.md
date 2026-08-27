# A63 — Android Developers consistency & accessibility hardening

A63 is deliberately not a visual redesign. It hardens the approved Home +
Step 1–9 flow against current Android Developers / Jetpack Compose guidance.

## State-based text fields

The remaining editable fields are migrated to `TextFieldState`:

- Step 1 trip name
- Step 4 work-shift code
- Step 4 free-day code

The existing outer draft state is mirrored from `snapshotFlow`, matching
Google's migration guidance. External value changes are synchronized back with
`setTextAndPlaceCursorAtEnd`.

Roster-code length enforcement moves from `onValueChange` truncation to
`InputTransformation.maxLength`.

## Radio semantics

The shared radio pattern now follows the Android Developers example:

- `selectableGroup()` on each radio group
- `selectable(... role = Role.RadioButton)` on the whole row
- `RadioButton(onClick = null)`

The two manual Step 4 radio groups are also marked as selectable groups.

## Checkbox semantics

The labelled row owns `toggleable(... role = Role.Checkbox)` and the visual
Checkbox delegates with `onCheckedChange = null`.

Applied to:

- Step 3 payslip confirmation
- Step 8 roster-gap confirmation

## Heading semantics

Screen titles, reusable section headings and the main Step 8 review heading are
marked with `semantics { heading() }`.

## Progress

The custom Step progress bar is replaced with Material3
`LinearProgressIndicator(progress = { ... })`.

The approved appearance is preserved:

- 4 dp
- rounded cap
- Oslo yellow active indicator
- existing track color
- zero indicator/track gap
- no stop marker

## Deferred

A63 does not migrate the approved custom dropdowns or screen header to
ExposedDropdownMenuBox / TopAppBar. Those are larger architectural/visual
changes and are not necessary for final hardening.

No domain, tariff, calculation, control, settlement, PDF, persistence or flow
semantics are changed.
