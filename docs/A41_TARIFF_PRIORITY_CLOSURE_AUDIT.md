# A4.1 tariff priority closure audit

## Scope

This slice resolves two tariff interactions that earlier versions deliberately left open:

1. whether chapter-12 evening/night and Saturday/Sunday supplements should be stacked on the same minutes already compensated under point 20.2; and
2. whether point 13.7.3's 133 1/3-percent overtime addition should replace point 20.2's +50-percent compensation on listed special holiday dates.

No change is made to the app's separate **comparison-model assumption** that the stored ground roster is used to distinguish work assumed already covered from work treated as additional. Which actual holiday-stay work plan and any averaging arrangement is valid must still be clarified with the employer.

## Source reading

### Point 20.2

For holiday stays, point 20.2 is the specific worktime provision. It states that worktime beyond ordinary worktime under chapter 8 is compensated with hourly wage + 50 percent.

### Chapter 12 on the same minutes

Point 12.1.1 grants the 40-percent evening/night supplement for **ordinary service** and explicitly says the supplement is not paid for overtime.

Point 12.2.2 is likewise a supplement for **ordinary service** on Saturday/Sunday and explicitly excludes overtime. It also excludes work that under another provision is paid with more than a 50-percent addition.

A4.1 therefore does not stack point 12.1.1 or 12.2.2 on minutes the app has already classified and compensated under point 20.2.

### Point 13.7.3 on special holiday dates

Point 13.1 says the chapter-13 overtime provisions apply unless otherwise provided in the tariff agreement. Point 20.2 is a specific chapter-20 adaptation for holiday stays and expressly fixes the compensation for worktime beyond ordinary worktime at hourly wage + 50 percent.

A4.1 therefore uses point 20.2 for those minutes instead of substituting the point-13.7.3 rate.

## Product consequence

The calculation no longer creates these provisional/open lines:

- `outside-evening-night-open`
- `outside-weekend-open`
- `holiday-overtime-open`

The associated rule IDs remain in the rule catalogue as **implemented rule-priority decisions**, not unresolved questions:

- `D25_20_2_X12_13`
- `D25_20_2_X13_7_3`

The calculation and PDF explain the priority rule on the point-20.2 work line instead of presenting hypothetical extra amounts.

## Boundary

This is FERIETUR01's rule-priority interpretation of the text in Dok. 25 2026–28. If Oslo kommune or the tariff parties provide a written interpretation that establishes a different priority, this rule must be revisited rather than silently layering another amount on top.

The point-18.4 late-notice exception remains outside this A4.1 slice; A4.2 later implements it as explicit travel-notice input and short-notice overtime handling.
