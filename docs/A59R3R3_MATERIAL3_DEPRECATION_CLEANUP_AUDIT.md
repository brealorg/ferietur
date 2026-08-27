# A59R3R3 — Material3 deprecation cleanup

A59R3R2 was functionally green but emitted six Material3 deprecation warnings.

## Bottom-sheet state

Four Step 6 drill-downs used the deprecated
`rememberModalBottomSheetState(skipPartiallyExpanded = true)`.

They now use the current Material3 state API:

- `rememberBottomSheetState`
- initial value `SheetValue.Hidden`
- enabled values `{Hidden, Expanded}`

This preserves the approved behavior: the lower partially-expanded anchor is
not available.

## ListItem

Two legacy `ListItem(headlineContent = ...)` calls are migrated to the current
`ListItem(content = ...)` overload while preserving their existing modifier,
supporting content and trailing content.

## Scope

No visual hierarchy, calculation, tariff, evidence, payment, PDF, roster,
travel, salary, persistence or flow behavior is changed.
