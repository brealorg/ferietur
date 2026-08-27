# A57R3 — 3-character vaktkode + full interval in week row

## Runtime finding

A57R2 stopped an arbitrarily long code from destroying the week row, but an
8-character badge was still too wide. The row also repeated the code twice:

- once in the badge
- once again before the time interval

That left too little horizontal space for ordinary intervals such as
`07:30–14:45`.

## A57R3

- New vaktkoder are limited to **3 characters**.
- Existing pre-R2/pre-R3 long values still decode for repair.
- The date column is reduced from 82 dp to 74 dp.
- The code badge is reduced from 86 dp to 54 dp.
- Horizontal row gaps are reduced to 10 dp.
- A single-shift summary shows only the real interval, e.g. `07:30–14:45`.
- The code is shown only once, in the badge.
- For several shifts on one date the badge shows the count (`2×`) and the
  supporting line shows the first complete interval plus `+N`.
- The supporting interval uses `bodyMedium` to preserve readable complete times.

No free-day semantics, tariff logic, calculation formulas, payment logic,
travel logic, persistence schema, or flow logic are changed.
