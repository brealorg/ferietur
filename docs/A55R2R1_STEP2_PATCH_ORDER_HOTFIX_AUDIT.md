# A55R2R1 — Step 2 patch-order hotfix

A55R2 created a sentence beginning with `Vanlig lønn` and then performed a
global replacement of the same substring while renaming an employer heading.
It therefore rewrote its own new sentence and correctly failed its successor
gate before build/install.

A55R2R1 replaces only the complete Kotlin heading literal and performs that
rename before creating the new salary-help sentence. A regression assertion
explicitly rejects the collided sentence.

No production domain/calculation logic is changed.
