# A55R1R1 — Step 2 gate and copy hotfix

A55R1 stopped before Gradle because it ran the legacy A54R3 `source-smoke.sh`
after deliberately changing the UI-copy contract. That gate is now executed on
the untouched A54R3 baseline before the successor patch. A dedicated A55R1R1
successor gate validates the new Step 2 copy after the patch.

A55R1 also missed three long payment-target descriptions because the Kotlin
source splits those visible strings across adjacent literals. A55R1R1 uses a
quote/concatenation-tolerant replacement for those three display strings.

Production scope remains UI presentation copy only. No tariff, calculation,
trip-plan, persistence, PDF calculation, or payment-scenario semantics change.
