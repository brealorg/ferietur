# A61 — Step 8 grouped control findings

## Google / Material basis

Android Developers describes `ModalBottomSheet` as the Material Compose
component for secondary content anchored to the bottom of the screen. A61 uses
that for control-category drill-down instead of expanding all evidence inline.

Android Developers describes Material list items as continuous vertical indexes
of text/content. A61 therefore uses compact `ListItem` rows for the scan-first
control overview and for the concrete periods inside a category.

Cards are reserved for coherent standalone content. The existing roster-gap
confirmation remains a card because it is an independent actionable control
with its own checkbox.

## Main screen

Step 8 now shows:

- compact payment proposal status
- one total count
- one row per non-OK control category
- one legal disclaimer at the bottom

The large explanatory paragraph and inline expansion of every finding are
removed.

## Category drill-down

Tapping a category opens a high Material modal bottom sheet.

Repeated interval findings are rendered as structured data instead of repeated
paragraphs:

- short rest: duration + end/start timestamps
- long continuous work: duration + start/end timestamps
- total-period warning: registered total + concise check instruction

Tapping one period shows a short "Hva bør kontrolleres?" explanation.

The original `ControlFinding.detail` remains available behind
`Vis original kontrolltekst` for audit/traceability.

## Invariants

No control-engine, worktime, calculation, settlement, PDF, roster, persistence
or flow semantics are changed.
