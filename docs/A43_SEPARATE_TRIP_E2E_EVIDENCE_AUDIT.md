# A4.3 — separate-trip E2E + open-rule evidence hardening

## Scope

A4.3 does not introduce a new tariff branch. It hardens two things before final regression:

1. an unresolved point-18.4 travel-notice fact must show the monetary consequence that is currently knowable without adding it to the payment basis;
2. the `DO_NOT_USE_NORMAL_ROSTER` / separate-trip path must be protected end-to-end so a stored roster cannot silently reduce or reclassify the registered trip plan.

## Open travel-notice amount

Ordinary travel pay remains confirmed and included. When the notice fact is `NOT_CLARIFIED`, the open line now carries a possible short-notice supplement amount when that amount can be calculated from the current trip.

The possible amount is the positive delta between:

- the currently confirmed short-notice overtime supplement; and
- a hypothetical calculation where the currently unresolved ordinary-travel blocks are also treated as short notice.

Both calculations share the same point-18.4 two-hour cap across the trip. The possible amount is therefore evidence only: it is `OPEN`, `includedInKnownTotal = false`, and never changes `paymentBasisAmount` until the user actually resolves the notice state.

If already-confirmed short-notice travel has exhausted the shared two-hour cap, the open line does not invent a new monetary addition.

## Separate-trip E2E contract

For `RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER`:

- the registered work plan is the calculation basis;
- the normal roster is not subtracted from active work, resting night work or travel;
- normal-roster supplement lines are not emitted as `ALREADY_COVERED_BY_NORMAL_ROSTER`;
- roster-gap confirmation is not required for finalization;
- the stored roster may still be preserved as source context, but it must not change the separate-trip payment basis.

Automated E2E policy tests compare the same separate trip with and without an overlapping roster and require identical payment-basis totals.

## Versioning

A4.3 changes app presentation/evidence and regression coverage. The tariff ruleset remains `2026.3` because no new tariff interpretation is introduced.
