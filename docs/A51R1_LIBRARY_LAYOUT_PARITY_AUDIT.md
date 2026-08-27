# A5.1R1 library layout parity audit

A5.1R1 keeps the approved A5.1 Oslo palette and rebuilds only the home trip library presentation.

## Accepted layout contract

- `Pågående` and `Klar for eksport` remain separate sections.
- Trips render as compact Material 3 `ListItem` rows inside one section surface rather than one large card per trip.
- The whole row resumes/opens the trip.
- The trailing overflow menu contains `Lag kopi` and `Slett`; those actions are not permanently laid out beside every trip.
- A safe delete confirmation remains mandatory.
- The duplicated top status strip is removed because the section headers already communicate lifecycle and counts.
- The main call to action is an extended `Ny tur` FAB.
- The rules reference no longer occupies a permanent card at the bottom of the library; it is available from the info action in the home header.
- Oslo identity shapes and the deterministic A5.1 palette remain unchanged.

## Non-goals

- no tariff changes;
- no ruleset changes;
- no draft/persistence changes;
- no PDF calculation changes;
- no change to copy/delete semantics from A5.0.
