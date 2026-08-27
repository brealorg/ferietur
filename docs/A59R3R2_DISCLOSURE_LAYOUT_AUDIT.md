# A59R3R2 — disclosure layout / long-screenshot stability

Runtime review of A59R3 showed duplicated/overlapping explanation text at the bottom of an expanded **Kort forklart** card.

The calculation-line detail sheet used a `LazyColumn` while accordion rows could change height dynamically. A59R3R2 replaces that one detail sheet with a regular `Column.verticalScroll(rememberScrollState())`.

The detail sheet contains a bounded amount of content, so eager layout is appropriate here. It also avoids lazy-item reuse / scroll-capture seams around expanded disclosures.

Preserved unchanged: A59 main result hierarchy, high/expanded sheet anchor, short explanation disclosures, first-four evidence preview, `Vis alle / Vis færre`, expandable evidence notes, and full rule text behind `Kilde og regelgrunnlag`.

No calculation, tariff, evidence, payment, PDF or flow semantics are changed.
