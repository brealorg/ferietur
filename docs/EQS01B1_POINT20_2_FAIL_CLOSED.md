# EQS01B1 — point 20.2 fail-closed applicability

## Why this slice exists

The legacy `TURNUS_PLUS_EXTERNAL` model treated active work outside the stored normal roster as
Dok. 25 point 20.2 work and multiplied it by 1.50.

Oslo kommune EQS ID 53398 makes that classifier unsafe: a holiday-stay work plan may differ from
the ordinary roster without the deviation itself creating overtime when the plan is timely changed
and otherwise valid.

A1/A2 added and exposed `HolidayWorkPlanStatus`, but the period model still does not say whether
each actual active period belonged to the approved holiday plan or was work beyond it.

## B1 behavior

For the production runtime when `FundingMode.TURNUS_PLUS_EXTERNAL` has an explicit
`HolidayWorkPlanStatus`:

- the stored normal roster is not used to create an automatic point-20.2 `active` line;
- no +50 percent amount is invented from old-roster difference alone;
- active work and travel with responsibility produce the open line
  `holiday-work-plan-scope-open`;
- unresolved rule `D25_20_2_WORK_PLAN_SCOPE` is exposed;
- the unresolved amount stays outside the known payment basis.

A nullable status remains only for predecessor/migration compatibility. The Android app runtime
passes the explicit status and therefore uses the new fail-closed path.

The structured `RuleSourceCatalog` keeps `D25_20_2_WORK_PLAN_SCOPE` bound to Dok. 25 point 20.2 as
the canonical monetary source. EQS ID 53398 is the operational applicability source and remains
explicit in the domain-rule source text and this contract; it is not misregistered as a tariff
package component.

## Deliberately not solved in B1

B1 does not yet redesign:

- period-level `within approved holiday plan / beyond plan / unknown` state;
- on-duty/off-duty classification for travel without supervision responsibility;
- the remaining ground-roster treatment of resting-night and ordinary-service supplements;
- payer/cost-allocation semantics;
- final ruleset-version bump and release copy.

Those belong to later EQS01B slices. No B1 preview should be released as the final EQS-compatible
calculation model.
