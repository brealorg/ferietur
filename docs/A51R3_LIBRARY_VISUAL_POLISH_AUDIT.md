# A5.1R3 library visual polish audit

A5.1R3 is a presentation-only refinement on top of the accepted A5.1R1 compact trip-library architecture and the A5.1R2 regression-gate hotfix.

## Runtime findings addressed

The first R1 device pass confirmed the list architecture but exposed four visual issues:

1. the interactive rule-info button shared the same top-right area as the Oslo identity decoration and became visually obscured;
2. the header and persistent library helper copy used more vertical space than necessary;
3. status circles were visually heavier than the surrounding list rows;
4. long trip titles were forced to a single line and became prematurely truncated.

## Changes

- the Oslo decoration and the rule-info `IconButton` now occupy separate layout slots; the info control keeps a dedicated 48 dp touch target and uses the normal foreground colour rather than the yellow status accent;
- header typography is compacted from display-scale to headline-scale and the subtitle is shortened to `Beregn ferietur uten Excel.`;
- library helper copy is shortened to `Utkast lagres automatisk. Kopier endrer ikke originalen.`;
- home-list top padding and inter-item spacing are reduced without changing section structure;
- status circles are reduced from 42 dp to 40 dp and their icon from 24 dp to 22 dp;
- trip titles support two lines before ellipsis;
- the divider inset follows the reduced status-icon footprint.

## Explicit non-goals

No changes to:

- trip lifecycle or persistence;
- copy/delete behaviour;
- tariff calculations or ruleset;
- PDF generation;
- A5.1 Oslo colour palette;
- section architecture or primary `Ny tur` FAB.

The frozen tariff ruleset remains `2026.3`.
