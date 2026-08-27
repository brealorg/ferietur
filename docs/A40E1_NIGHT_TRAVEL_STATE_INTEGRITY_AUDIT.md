# A4.0E1 – night-travel state integrity

## Scope

A4.0E1 hardens the UI/draft boundary for the A4.0E night-travel rule path. It does not change the tariff arithmetic introduced in A4.0E.

## Finding

The domain engine and saved-draft codec already distinguish the three authoritative states for travel without supervision during the night window:

- sleep permission confirmed (`TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED`),
- no sleep permission (`TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP`),
- not clarified (`TRAVEL_WITHOUT_RESPONSIBILITY`).

The UI had one hazardous transition: the generic **Uten ansvar** chip was visually selected for all three states, but pressing that already-selected chip always rewrote the kind to the generic unresolved state. This made it too easy to erase a previously recorded sleep decision without an explicit sleep-state action. The day summary also hid the sleep state, which made runtime verification unnecessarily ambiguous.

## Changes

- Pressing **Uten ansvar** while already in any without-responsibility state is now idempotent and preserves the current sleep decision.
- Explicit changes of sleep state remain owned by the dedicated **Ja / Nei / Ikke avklart** selector.
- The day-plan summary exposes the night-travel sleep decision: `søvntillatelse: ja`, `nei`, or `ikke avklart` when relevant.
- Policy tests cover the UI transition contract.
- Saved-draft codec coverage now includes all three night-travel states.

## Deliberately unchanged

- Passive-night pay at 1:3.
- Evening/night, weekend, and holiday supplement arithmetic.
- Worktime counting for passive night travel.
- Open-rule behavior for unresolved sleep permission.
