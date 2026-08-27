# A4.0E night travel + passive-character audit

> Historical note: A4.1 later closes the point-20.2/chapter-12 and point-20.2/13.7.3 interactions. See `A41_TARIFF_PRIORITY_CLOSURE_AUDIT.md`.


## Source basis

This slice implements the explicit night-travel branch in Dok. 25 2026–28 point 20.3 and closes two display/coherence defects found in A4.0D.

### Point 20.3 — travel between 23.00 and 07.00

Point 20.3 states that travel time between 23.00 and 07.00 is calculated as work of a passive character when the employee has permission to sleep. Passive work counts as working time minute-for-minute, while one hour of passive work is paid at one-third of ordinary hourly pay.

FERIETUR01 therefore requires an explicit night-travel state for travel without supervision responsibility:

- **Sleep allowed:** the 23.00–07.00 portion is passive work, counts minute-for-minute in worktime control, and receives base pay at 1:3.
- **No sleep permission:** the night portion remains ordinary travel time under point 18.4; it is not converted into passive work by point 20.3.
- **Not clarified:** the 23.00–07.00 portion is held out of the payment basis and the calculation carries an unresolved point-20.3 rule until the user resolves the factual premise.

The non-night portion of the same travel period continues to follow point 18.4.

### Point 8.9 — supplements on passive work

Point 8.9 states that night, Saturday/Sunday, and holiday supplements for passive work are also paid at a 1:3 ratio. A4.0E therefore calculates these supplements separately for passive night travel where their ordinary time windows apply. The evening/night calculation uses the ordinary 17.00–06.00 window; passive travel is not treated as a `nattevakt` that extends the supplement to 08.00.

## Ground-roster comparison boundary

In `TURNUS_PLUS_EXTERNAL`, the stored ground roster remains the app's comparison model for what is assumed already covered. Only travel portions outside the ground roster enter the additional payment basis. In the separate-trip model, the registered travel portions are calculated directly.

## A4.0D display fixes folded into this slice

- Ground-roster control amounts and `rosterMinutes` are clipped to the part of the roster that actually overlaps the trip range. The raw roster remains preserved in the finalized snapshot/PDF for documentation.
- Evidence that spans midnight or more than one date renders both dates, e.g. `fre. 14. aug kl. 08.00 → lør. 15. aug kl. 10.00`, instead of an ambiguous same-date clock range such as `08.00–10.00 · 26 t`.

## Product boundary

At the A4.0E checkpoint, the existing rule questions about combining point 20.2's 50-percent compensation with chapter-12 supplements or point 13.7.3 were still open. A4.1 later closes those interactions. At this A4.0E checkpoint the point-18.4 late-notice exception remained an explicit assumption/control item; A4.2 later replaces that assumption with explicit notice input and automatic short-notice overtime handling.
