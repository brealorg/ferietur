# A59R3 — Step 6 calculation detail disclosure hierarchy

The A59 main calculation screen remains unchanged and approved.

A59R3 redesigns only the drill-down for an individual calculation line.

## Default detail view

Always visible:

- title
- amount
- calculation/formula
- certainty
- **Kort forklart**
- first four evidence intervals
- compact **Kilde og regelgrunnlag**

The former full `Hvorfor er dette med?` tariff paragraph is no longer rendered
open by default.

## Kort forklart

Each calculation type gets a small set of plain-language disclosure rows. Only
the row the user taps expands.

For the main `Arbeid og reise utenfor grunnturnusen` line this means:

- grunnturnusen is the comparison basis
- work beyond it is calculated at hourly rate + 50 percent
- travel with responsibility counts as work time

Other calculation-line types have equivalent short explanations based on the
same existing rule semantics.

## Evidence

Only the first four intervals are shown initially.

- `Vis alle N` reveals the full evidence list
- each compact interval row shows date, time and duration
- tapping an interval reveals its existing evidence note

## Full audit trail

`Kilde og regelgrunnlag` is a disclosure row. Expanding it reveals the complete
existing `line.explanation` and source, preserving the audit trail without
forcing it into the default reading path.

## Sheet position

All Step 6 drill-down sheets skip the partially-expanded Material anchor and
open directly at the higher expanded position requested in runtime review.

No calculation, tariff, evidence, payment, roster, PDF or flow formula is
changed.
