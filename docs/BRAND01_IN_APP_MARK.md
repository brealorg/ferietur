# BRAND01 — Ferietur's own mark inside the app and in the PDFs

## Decision

The home header no longer draws squares and a circle in Oslo kommune's pattern language.
Ferietur is not a municipal service, and those shapes were the strongest visual cue that it
might be. The app now shows its own mark — the ICON02 launcher artwork on its Oslo-yellow
tile — in three places:

- home header, as one lockup with the name, tagline full width underneath (48dp);
- «Om Ferietur», next to the name (40dp);
- the running header of every PDF page, left of the document code (13pt, true vector).

The wizard steps deliberately do not carry the mark: their header already holds back, step,
title and save status, and the palette carries the identity there.

## One artwork, three carriers

`res/drawable/ic_ferietur_mark.xml` is the 72-unit launcher window of
`ic_launcher_foreground.xml` on a rounded tile; its suitcase and tag paths are the foreground
paths verbatim, which `tools/icon02-contract.sh` enforces. `export/PdfBrandMark.kt` draws the
same coordinates with `android.graphics.Path`, because a `VectorDrawable` drawn into a PDF canvas
is rasterised at the size of its bounds. The gate pins its rotation and safe-zone scale to the
drawable's.

## SAVEUI01 — quiet save status

Autosave is the normal case, so the normal case is a status, not a button: a dimmed check when
the draft is saved, the save glyph in the primary colour while a save is in flight (the tint
fades and follows the system animation scale). A lone grey save glyph read as a disabled button,
which is why the saved state is a check. A failed save is the only action — warning icon,
«Ikke lagret» and error colour, tap to retry — the one state that must not be missed and the
only one where saving by hand does anything. Screen readers get «Utkastet er lagret»,
«Lagrer utkastet» or «Prøv å lagre på nytt».

## UXFIX02 — two control-flow presentation fixes

- Step 2: the helper text under «Hvilken arbeidsplan har arbeidsgiver fastsatt?» sat inside the
  rounded card without padding, so the corner clipped its first letter. `MethodFormSection` now
  takes `supporting` and renders it under the heading, outside the card.
- Step 8: the seven-day work-time finding repeated its title as its own supporting text.
  `ControlFindingPresentationPolicy` reads hours and window out of the domain-owned sentence, so
  the row shows e.g. «138 t på sju dager». Unknown sentence shapes fall back to the generic
  layout. The finding text and the rule itself are unchanged.

## Scope

No calculation rule, rate, salary table, persisted schema, manifest entry or dependency
coordinate is changed. `gradle/verification-metadata.xml` gains one verified entry
(`guava-parent-33.4.0-jre.pom`) that a cold-cache resolution needs, e.g. on CI.
