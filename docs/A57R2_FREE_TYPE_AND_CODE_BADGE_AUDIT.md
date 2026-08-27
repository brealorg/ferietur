# A57R2 — fridagstype + bounded vaktkode

## Runtime changes

A57R2 preserves the A57 manual-roster model and fixes the two issues found in
runtime review.

### Fridagstype

`Fri` no longer has one boolean checkbox that silently defaults to "not weekly
off".

A new free-day registration must explicitly choose one of:

- **Ukentlig fridag**
- **Annen eller ekstra fridag**

The local vaktkode remains a separate mandatory field. Therefore `F1`, `F2`,
`X7`, or any other workplace-local code has no tariff meaning by itself.

The existing `weeklyOff` domain flag remains the tariff-relevant data consumed
by the overtime logic. No `F1`/`F2` hardcoding is introduced.

### Vaktkode

New manual registrations:

- require a non-blank vaktkode
- allow at most **8 characters**

The overview badge has a fixed width and one-line ellipsis so a long historical
code can never squeeze the shift label into a vertical column.

A57 data created before this limit remains readable. Existing long `MR1` values
are decoded for review/editing, but must be shortened before they can be saved
again. Long historical codes are also excluded from the `Tidligere brukt`
template chips.

## Tests

A57R2 adds regression coverage for:

- tariff meaning independent of local code
- 8-character code limit on new entries
- pre-R2 long-code decode compatibility
- fixed/bounded overview badge contract

No tariff rate, salary table, payment, trip-plan, travel, PDF, or flow formula
is changed.
