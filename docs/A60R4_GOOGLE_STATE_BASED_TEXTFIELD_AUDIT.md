# A60R4 — Step 7 Google state-based text-field rewrite

The current Google Compose guidance recommends state-based text fields for a
more complete and reliable editing model. `TextFieldState` owns text, cursor,
selection and IME composition. Google's migration guide also shows how an
existing outer/ViewModel state can remain in use: observe `TextFieldState.text`
with `snapshotFlow`, while the outer state no longer drives the TextField.

A60R4 applies that architecture to both Step 7 fields.

## Avtalt beløp
- state-based Material3 OutlinedTextField
- TextFieldLineLimits.SingleLine
- decimal keyboard
- InputTransformation for the existing allowed-character policy
- snapshotFlow mirrors the value to the existing settlement/draft state

## Begrunnelse
- state-based Material3 OutlinedTextField
- fixed five-line visible viewport using TextFieldLineLimits.MultiLine(5, 5)
- no text-length limit
- overflow scrolls vertically inside the TextField
- snapshotFlow mirrors the value to the existing settlement/draft state

The previous manual `height(176.dp)` workaround is removed.

Settlement persistence, autosave, next-step validation, PDF/snapshot behavior,
calculation rules and worktime rules are unchanged.
