# A58R1R1 — self-contained A58 retry

A58 successfully produced its successor source but failed to compile because
`ToggleFloatingActionButton.containerColor` is a plain `(Float) -> Color`
callback. The first A58R1 hotfix expected the failed intermediate
`~/Downloads/ferietur01-a58` tree to still exist; on the device it no longer did.

A58R1R1 is therefore self-contained:

- baseline is the known-good `~/Downloads/ferietur01-a57r3`
- the complete A58 Step 5 successor UI is supplied directly
- the FAB color compile fix is already incorporated
- no failed/intermediate A58 directory is required

The Oslo-yellow colors are resolved in composable `PlanFabMenu` scope and plain
`Color` values are captured by the non-composable FAB callback.

No tariff, calculation, domain, or flow formulas are changed.
