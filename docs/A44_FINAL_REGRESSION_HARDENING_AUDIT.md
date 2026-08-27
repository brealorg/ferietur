# A4.4 — final regression hardening audit

## Scope

A4.4 is a consolidation slice. It does not add a tariff branch. It hardens the last mode-specific copy defect found in the separate-trip E2E run and creates golden regression coverage across the accepted Oslo kommune holiday-stay scenarios.

## Mode-sensitive work-plan copy

The work-plan flow has two materially different models:

- `TURNUS_PLUS_EXTERNAL`: the ground roster is a comparison basis for what is assumed already covered;
- `VACATION_SEPARATE`: the entire registered trip work plan is the calculation basis and no ground-roster comparison is part of the user flow.

The work-plan screen and day editor now describe the selected model instead of always mentioning a ground roster.

## Mode-sensitive PDF copy

The short-export footer and the calculation-method note now follow the finalized snapshot's `RosterComparisonMode`.

For a separate trip, the full-basis description lists the registered work plan, day-by-day control, explanations and sources without claiming that a ground roster is included. A normal-roster calculation continues to document the ground roster explicitly.

## Golden regression scenarios

### Solgården

Accepted basis:

- trip: 11 August 2026 06.00 to 18 August 2026 23.00;
- salary step 32, 35.5-hour weekly basis, standard weekend profile;
- payment basis: `43 267,74 kr`;
- roster gap: 60 minutes, explicitly confirmable;
- no open calculation rules under ruleset `2026.3`.

The test locks the accepted component amounts and a finalized, confirmed export snapshot.

### Night travel without supervision responsibility

For 23.00–07.00 travel with notice known by the previous day:

- sleep permission `Ja`: `1 308,59 kr`, including passive-character treatment and 8 hours worktime;
- sleep permission `Ikke avklart`: `110,00 kr` known basis and the point-20.3 sleep-permission rule remains open;
- sleep permission `Nei`: `2 773,52 kr` as ordinary travel-time treatment outside the ground roster.

### Separate trip

For a 24-hour holiday stay with 15.00–19.00 active work on the first day:

- active work: `1 331,76 kr`;
- evening supplement 17.00–19.00: `266,36 kr`;
- stay allowance: `110,00 kr`;
- total: `1 708,12 kr`.

An overlapping stored roster must not change the total, create roster-gap requirements, or emit `ALREADY_COVERED_BY_NORMAL_ROSTER` lines.

## Final manual acceptance after build/install

The automated gate protects the calculation contracts. Runtime acceptance is intentionally short:

1. reopen Solgården and verify `43 267,74 kr`, no open calculation rules, and full/short PDF export;
2. reopen the accepted passive night-travel case and verify the `Ja` path remains `1 308,59 kr` with 8 hours worktime;
3. reopen the separate-trip case and verify `1 708,12 kr` and mode-correct work-plan/PDF language.

If those three remain intact, the Oslo kommune holiday-stay tariff core is ready to freeze at ruleset `2026.3`.
