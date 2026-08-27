# A5.1 Oslo visual refresh audit

## Scope

A5.1 is presentation-only. It does not change FERIETUR01 tariff arithmetic, ruleset 2026.3, saved-trip lifecycle semantics, draft schema, PDF calculations or finalization logic.

## Approved visual reference

The implementation follows the approved dark Ferietur library concept:

- deep navy working surface;
- cool blue primary actions and in-progress state;
- Oslo yellow for completion/status accents;
- Oslo red for destructive actions;
- compact lifecycle cards with `Fortsett`, `Lag kopi` and safe delete;
- status summary for `Pågående` and `Klar for eksport`;
- Oslo-inspired geometric identity shapes.

## Oslo design-manual anchors

Official Oslo palette values used directly:

- Oslo mørk blå `#2A2859`
- Oslo rød `#FF8274`
- Oslo gul `#F9C66B`
- Oslo blå `#6FE9FF`
- Oslo lys blå `#B3F5FF`
- Oslo sort `#2C2C2C`
- Oslo lys beige `#F8F0DD`

The approved Ferietur concept retains its dark navy working surfaces while brand/status accents use the Oslo palette. Dynamic Android wallpaper colours are disabled so the app has the same identity on every device.

## Library hierarchy

The A5.0 lifecycle remains authoritative:

- `Pågående`
- `Klar for eksport`

The screen adds a compact status strip and stronger visual status treatment without introducing an archive lifecycle or changing persistence.

## Regression contract

- ruleset stays `2026.3`;
- app version is `0.5.1-a51`;
- copy/delete behavior is unchanged;
- delete confirmation remains mandatory;
- completed trips use Oslo yellow instead of green;
- destructive controls use Oslo red;
- no dynamic colour scheme is allowed to override the approved palette.

## Logo asset

A5.1 does not bundle or redraw the protected Oslologo. The design manual requires the symbol and `Oslo` name to stay together and says correct logo originals must be used. The app therefore uses colour/form language only until an approved logo original is supplied for the product.
