# A5.1R2 final regression version-gate hotfix

A5.1R1 failed only because `FinalRegressionPolicyTest` still asserted the pre-R1 product version `0.5.1-a51` while production correctly reports `0.5.1-r1-a51r1`.

The frozen contract is the accepted calculation behavior and ruleset `2026.3`, not a permanent lock to one UI/product build identifier. Therefore this hotfix:

- preserves all golden scenario amount assertions and the exact ruleset assertion;
- requires the product version metadata to be present without pinning it to an obsolete UI release;
- applies the same rule to `A44FinalRegressionSmoke.kt`;
- adds source-smoke guards against reintroducing the stale exact-version lock.

No production source is changed.
