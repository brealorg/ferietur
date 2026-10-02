# ICON02 — luggage-tag launcher icon on Oslo yellow

## Decision

ICON02 replaces the ICON01 route-and-pin motif. At launcher size the dashed route ending in a
location pin read as an unintended figure, so the travel motif is now a luggage tag hanging
from the suitcase handle. `docs/ICON01_LAUNCHER_POLISH.md` and its retired gate remain as
history and are not edited.

Regular adaptive icon (all colours from Oslo kommune's palette):

- background: Oslo yellow `#F9C66B`;
- suitcase: Oslo dark blue `#2A2859` — the ICON01 suitcase geometry, unchanged;
- luggage tag: Oslo blue `#6FE9FF`, string in Oslo light beige `#F8F0DD`, tag hole shows the suitcase;
- the tag hangs from the right handle leg and is rotated 14° clockwise about `(67, 40)`.

The colour resource names keep their meaning: `launcher_icon_background` is the layer behind
the mark and `launcher_icon_foreground` is the suitcase. Two resources are new:
`launcher_icon_tag` and `launcher_icon_tag_string`.

Ferietur is not an Oslo kommune service. Only palette colours are used; the Oslo logo and the
Oslo Sans typeface are deliberately not used anywhere in the app or its store listing.

## Safe zone

The ICON01 mark spanned 66dp horizontally, so its side-shell corners lay up to 41dp from the
centre and were clipped by circular launcher masks (36dp radius). ICON02 wraps the whole mark in
one group — scale `0.85` about `(54, 57.5)`, `translateY -3.5` — which centres it vertically and
keeps every point within the 33dp safe-zone radius. The Play listing icon (512 px, unmasked
square) uses the same artwork at scale `1.0`, centred.

## Themed icon

The Android 13+ monochrome layer keeps the suitcase with the tag as an even-odd cut-out and the
tag hole as a solid dot. The string is omitted for small-size legibility. The tag outline is the
foreground tag path with the 14° rotation baked in, because a single even-odd path cannot carry
a nested group transform.

## Scope

ICON02 changes only launcher colours, the foreground vector, the monochrome vector, this note
and `tools/icon02-contract.sh`. It does not change `applicationId`, production Kotlin,
AndroidManifest, dependencies, tariff data, calculation formulas, persistence schema,
backup/security policy or signing state. The assets remain deterministic Android
`VectorDrawable` resources; no raster image is shipped in the application.
