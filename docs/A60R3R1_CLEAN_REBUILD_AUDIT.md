# A60R3R1 — clean rebuild after corrupted A60R3 payload

The previous A60R3 package was malformed during generation: offsets captured
before import edits were reused after the file length changed. That spliced
`SettlementScreen` into the end of `CalculationRulesSheet` and duplicated a
SettlementScreen tail. The resulting parser failure produced the large cascade
of unrelated unresolved references.

A60R3R1 discards the failed A60R3 tree and rebuilds from exact last-good A60R2
UI SHA256:

`1084af1e0c65d861b36093041ece6cdd0c2aafdd429cbe815a63ef6d8a6b4a72`

Only these intended source changes are made:

1. remove Step 7 `imePadding()`
2. remove its unused import
3. add `KeyboardOptions` / `KeyboardType`
4. request `KeyboardType.Decimal` for `Avtalt beløp`

The non-lazy form and unrestricted multiline Begrunnelse from A60R2 remain.

A dedicated source-integrity gate checks the exact structural area corrupted in
A60R3 before Gradle is allowed to run.
