# A3.5 calculation audit

> Historical note: A4.1 later closes the point-20.2/chapter-12 and point-20.2/13.7.3 interactions. See `A41_TARIFF_PRIORITY_CLOSURE_AUDIT.md`.


## Day-by-day audit

The calculation result is still authoritative at trip level. A3.5 derives a second audit view from the same calculation lines and evidence intervals. Each day shows the calculation components that contribute to that date, their time intervals, amounts and source references.

The daily allowance in point 20.6 is not distributed across dates because it is calculated from the duration of the trip as a whole.

## Holiday/high-day supplement

Point 12.2.3 is modeled as tariff windows rather than a simple public-holiday boolean. The windows differ between 33.6-hour schedules and 35.5/37.5-hour schedules plus point 8.2.2.

The app derives movable dates from Easter for each year.

## Saturday/Sunday interaction

Point 12.2.2 says the Saturday/Sunday allowance is not paid for work that is compensated by another provision with more than a 50 percent addition. A3.5 therefore removes point 12.2.3 minutes from the Saturday/Sunday allowance before calculating the latter.

## Special holiday overtime

Point 13.7.3 specifies a 133 1/3 percent overtime addition for relevant employees on listed special days. Point 20.2 separately specifies hourly wage + 50 percent for work beyond ordinary hours during holiday stays. A3.5 detects the affected minutes and exposes the potential difference, but keeps it outside the known total until the interaction is resolved.


### A3.5R1 clarification
The holiday supplement line displays `hours × holiday supplement rate`. The displayed rate is already `hourly rate × 1 1/3`; no additional 1 1/3 multiplier is implied.
