# A5.0 trip library lifecycle audit

## Scope

Product lifecycle and local-data safety around the already frozen calculation engine.

## Contracts

- No `TariffMath`, `TripPlanEngine`, `Rules` or calculation-policy changes.
- Saved trips are grouped as **Pågående** or **Oppsummering klar** from their persisted flow state.
- Progress is mode-aware: the ground-roster flow has nine steps and the separate-trip flow has eight.
- Delete is destructive and therefore requires confirmation.
- `Lag kopi` always creates a new id and returns the copy to the first editable step. It resets confirmations that should not silently carry to a new editable copy: payslip check, roster-gap confirmation and custom settlement.
- Original saved data is not mutated by duplication.
- Ruleset remains `2026.3`.
