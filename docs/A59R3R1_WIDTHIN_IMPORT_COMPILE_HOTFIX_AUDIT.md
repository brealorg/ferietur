# A59R3R1 — widthIn import compile hotfix

A59R3 introduced `Modifier.widthIn(min = 72.dp)` in the compact evidence row,
but did not import `androidx.compose.foundation.layout.widthIn`.

Kotlin therefore failed with:

`Unresolved reference 'widthIn'`

A59R3R1 adds only that missing import.

No Step 6 presentation, tariff, calculation, evidence, payment, PDF, roster, or
flow behavior is changed.
