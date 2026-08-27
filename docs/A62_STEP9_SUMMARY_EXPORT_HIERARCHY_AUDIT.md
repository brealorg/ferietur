# A62 — Step 9 summary and export hierarchy

## Android Developers / Material basis

Android Developers defines filled `Button` as high-emphasis for the primary
action, and `OutlinedButton` as medium-emphasis for important but secondary
alternatives. A62 therefore uses one filled primary export action and one
outlined secondary export action.

Material guidance also treats cards as containers for a single coherent piece
of content and lists/rows as compact vertical indexes. The amount remains the
single coherent hero result, while status becomes compact key/value rows.

## Screen order

Step 9 now reads in the order:

1. result
2. export
3. status
4. worktime attention item
5. documentation
6. calculation ID

This moves the actual completion action directly below the result instead of
burying it below diagnostic status content.

## Export hierarchy

Primary:
`Lag og del kort oppsummering`

Secondary:
`Lag og del fullt beregningsgrunnlag`

The full-export description is reduced to one compact supporting paragraph.

## Status

The former sentence-heavy status list is compressed to:

- Arbeidsgiver
- Betalingsscenario
- Lønnsopplysninger
- Beregningsregler
- Registrering

The detailed rule-basis string is not repeated on the main summary.

Worktime findings are the only dedicated attention row. Tapping it calls the
existing `onBack`, which returns directly to Step 8 Kontroll.

## Documentation

The long grunnturnus PDF explanation becomes:

`Grunnturnusen følger med i fullt grunnlag`
`<time> overlapper turen.`

Custom-agreement information remains available when applicable.

## Invariants

No PDF content generation, FinalizedTripSnapshot data, settlement, calculation,
control engine, persistence or flow semantics are changed.
