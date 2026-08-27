# A63R4 — state-based TextField extension import compile hotfix

A63 used two APIs documented by Android Developers, but omitted their extension
imports:

- `TextFieldState.setTextAndPlaceCursorAtEnd(...)`
- `InputTransformation.maxLength(...)`

Both are extension functions in
`androidx.compose.foundation.text.input`, so Kotlin requires these imports:

```kotlin
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
```

The A63 architecture remains unchanged:

- state-based TextFieldState
- snapshotFlow mirroring to existing outer draft state
- InputTransformation max-length enforcement
- Android Developers radio/checkbox semantics
- heading semantics
- Material LinearProgressIndicator

No domain, calculation, control, PDF, persistence, settlement or flow behavior
is changed.
