# A4.2 — point 18.4 travel-notice / short-notice audit

## Source rule

Dok. 25 point 18.4 distinguishes three facts that the app must not conflate:

1. travel time in ordinary working time counts fully as worktime;
2. travel time outside ordinary working time is paid at ordinary hourly rate;
3. if the employee did not learn of the journey no later than the previous day, up to two hours of the travel time required outside ordinary working time is paid as overtime, while any excess remains ordinary travel pay.

Chapter 13 then determines the overtime supplement. Point 13.2 gives 50 percent from 07.00–20.00 and 100 percent from 20.00–07.00, on Sundays/public holidays, and on the day before these after ordinary work ends. Point 13.3 pays the overtime supplement for each commenced half hour. Point 13.7.1 gives a turnus worker 100 percent on the weekly day off. Point 13.7.3 can give 133 1/3 percent on listed special days for workers who meet its employee-specific condition.

## A4.2 model

`TravelNoticeStatus` is stored on the work-plan travel block and frozen through the saved-draft / finalized calculation chain:

- `KNOWN_BY_PREVIOUS_DAY`
- `NOT_KNOWN_BY_PREVIOUS_DAY`
- `NOT_CLARIFIED`

Schema 4 persists the value. Older 3-field work-plan rows migrate conservatively to `NOT_CLARIFIED`; no old trip is silently treated as known.

For `NOT_KNOWN_BY_PREVIOUS_DAY`, ordinary travel pay remains a separate confirmed line. The overtime line contains only the additional overtime percentage. This makes it impossible to count the ordinary hourly wage twice.

The engine applies the point-18.4 two-hour limit once across the current trip and to actual eligible ordinary travel minutes before point-13.3 rounding. This is an explicit FERIETUR01 model choice to prevent users from multiplying the tariff limit merely by splitting one journey into several UI blocks.

`F1` is the local catalogue's explicit `Ukefridag` and is mapped to point 13.7.1. `F2` is `Ekstra ukefridag` and is not automatically treated as the point-13.7.1 weekly day off.

On a point-13.7.3 special date, the engine includes the otherwise confirmed overtime rate and opens only the possible difference to 133 1/3 percent. The app does not infer the employee-specific eligibility condition from a short trip roster snapshot.

Passive 23.00–07.00 travel with sleep permission stays under the specific point-20.3 / point-8.9 passive-work path and is excluded from the point-18.4 short-notice overtime bucket for those same minutes.

## Acceptance / regression gates

Automated tests cover:

- known / not-known / not-clarified notice states;
- 50 percent and 100 percent rates;
- point-13.3 half-hour rounding;
- the two-hour cap across multiple UI blocks;
- `F1` versus `F2`;
- the day-before-Sunday rule when the roster supplies an ordinary-work end;
- conditional point-13.7.3 handling;
- passive night travel exclusion;
- schema-3 migration and schema-4 round-trip persistence.
