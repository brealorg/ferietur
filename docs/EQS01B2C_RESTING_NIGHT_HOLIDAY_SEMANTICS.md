# EQS01B2C — resting-night holiday semantics

## Tariff basis

Dok. 25 point 20.4 specifically regulates night watch during a chapter-20 holiday stay.
Night watch between 23:00 and 07:00 is normally passive work. Passive work counts as worktime
minute-for-minute and one hour is paid at one-third of ordinary hourly pay. Active work during the
watch has its own point-20.4 rule with hourly pay + 50 percent and per-watch half-hour rounding.

Point 8.9 applies the same 1:3 ratio to night, Saturday/Sunday and holiday supplements on passive
work.

## B2C product semantics

For the modern explicit holiday-plan path, a registered `RESTING_NIGHT_WATCH` is calculated
directly under point 20.4. The stored normal roster no longer decides whether the registered
holiday night watch gets the 1:3 treatment.

`HolidayWorkPlanRelation` is not an additional payment gate for this specific rule. The point-20.4
night-watch rule applies because the period is registered as a resting night watch during the
holiday stay.

The same selected resting-watch blocks feed the existing 1:3 passive supplements.

## Compatibility

A null top-level `HolidayWorkPlanStatus` retains the previous ground-roster filter only for
legacy/predecessor compatibility. Production runtime supplies an explicit status.

## Deferred to EQS01C

B2C completes the identified calculation-semantic blockers from EQS01B. Before commit/release the
combined A1/A2/B1/B2A/B2B1/B2B2/B2C tree still requires a ruleset-version bump, runtime install/
visual preview and final source/test/snapshot compatibility qualification.
