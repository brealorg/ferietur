# EQS01B2A — period relation and travel-duty foundation

## Purpose

EQS01B1 stopped using the stored normal roster as an automatic Dok. 25 point 20.2 classifier.
That fail-closed state is intentional, but the calculation cannot become positive again until
Ferietur records the facts that the old model lacked.

B2A introduces those facts without changing money:

- `HolidayWorkPlanRelation` records whether one registered period is within the dedicated
  holiday-stay work plan, beyond that plan, or not clarified.
- `TravelDutyStatus` records whether a travel period occurred while the employee was on duty,
  off duty, or not clarified.

The two facts are separate. Supervision responsibility, sleep permission and travel notice remain
their own existing dimensions.

## Versioning

- editable draft schema: v7 -> v8;
- finalized snapshot binary format: v5 -> v6;
- pre-v8 plan rows default both new facts to `NOT_CLARIFIED` and receive migration provenance;
- finalized formats v1-v5 remain readable; pre-v6 work blocks receive conservative
  `NOT_CLARIFIED` values;
- the v5 top-level `HolidayWorkPlanStatus` reader boundary remains intact.

## Data-integrity propagation

B2A preserves the two fields through projection, visible-day clipping, travel overlay/splitting,
tariff slicing and whole-trip source reconstruction. The existing period editor also preserves
loaded hidden values so an edit cannot erase them before B2B exposes controls.

## Explicit non-goals

B2A does not:

- turn `BEYOND_HOLIDAY_WORK_PLAN` into point 20.2 money;
- interpret `NOT_APPROVED_OR_LATE` as automatic overtime;
- change the B1 `holiday-work-plan-scope-open` fail-closed calculation;
- make on-duty/off-duty travel affect working-time or pay yet;
- add visible editor choices.

EQS01B2B owns the visible period controls and period-aware calculation semantics.
