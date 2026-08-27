# A5.2 guided flow chrome audit

Scope: product-shell refinement only. Ruleset 2026.3, tariff arithmetic, trip persistence, PDF calculations and trip-library IA are unchanged.

## Changes

- Every 8/9-step workflow screen now exposes `Steg X av Y` as an explicit progress label.
- A deterministic Oslo-yellow progress rail sits below the compact screen header.
- Screen titles use the smaller `headlineSmall` hierarchy so forms begin higher on screen.
- Save state is a compact inline action (`Lagret`, `Lagrer…`, `Ikke lagret`) instead of a vertically stacked icon + label.
- The bottom navigation uses a low-noise text back action and one dominant next action.
- Home/library visual acceptance from A5.1R3 is untouched.

## Non-goals

- No step reordering or merging.
- No tariff, calculation, worktime, export or persistence changes.
- No new data requirements.
