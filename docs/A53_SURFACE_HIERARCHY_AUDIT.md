# A5.3 Surface hierarchy audit

## Design rule

Default presentation is flat content, list rows, typography and dividers. A card/surface is reserved for one of these roles:

1. the primary result/hero amount;
2. a genuine semantic alert or blocker;
3. a complex editor that benefits from a bounded work area;
4. one list-group container where the items themselves remain flat.

## Changes

- Lønn og betaling: choice cards become selectable rows.
- Lønnsopplysninger: the monolithic form card is removed.
- Grunnturnus: day cards become rows inside one grouped list surface.
- Arbeidsplan: day cards become flat day sections with dividers; editor blocks remain bounded because they are complex editable units.
- Beregning: rate, calculation lines, ground-roster control and day audits become expandable rows. Hero amount stays a hero surface.
- Betalingsforslag: method cards become selectable rows. Hero amount stays.
- Kontroll: repeated findings are grouped by title, count and expandable detail. Blocking roster-gap alert stays an alert.
- Oppsummering: status card becomes a checklist/list. Hero amount stays.

## Explicitly unchanged

- FERIETUR01 ruleset 2026.3
- tariff calculations
- payment-basis calculations
- draft persistence
- finalized snapshots
- PDF calculations/export semantics
