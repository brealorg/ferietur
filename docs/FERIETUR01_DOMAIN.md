# FERIETUR01 domain contract — A4.2

The application keeps the following concepts separate.

1. **Employer relationship** — who is the employer for the trip.
2. **Payment scenario** — the payer/refunder selected for the proposal and PDF. This is documentation metadata: it does not change tariff arithmetic and is not a legal determination of liability.
3. **Roster comparison** — whether the normal roster is used as a comparison basis.
4. **Work plan** — what actually happened, including travel, active work, night watch, resting night watch and free time.
5. **Rule-based calculation** — what the implemented rules calculate from the work plan.
6. **Payment proposal** — the amount documented for the proposed settlement. It normally follows the calculated payment basis; an already agreed alternative amount can be documented without changing the calculation.

A payment scenario is not inferred to be the employer. A roster-comparison choice is not used to infer either employer or payment scenario. The app does not determine who is legally liable for the cost.

## Employer and rule basis

When `EmployerKind.OSLO_KOMMUNE` is selected, the explicit rule basis is Oslo kommune Dok. 25 2026–28, chapter 20.

When `EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED` is selected, the employer relationship and rule basis are not treated as finally clarified. The app may preserve and display a preliminary calculation for documentation, but must not present it as a confirmed total employer cost.

Legacy saved trips created before A3.9A contain no reliable employer/payer field. They are migrated without guessing: employer and payer become `UNSPECIFIED`, while the previous calculation choice is preserved only as roster-comparison mode.

## Roster comparison

`USE_NORMAL_ROSTER` compares trip work with the stored normal roster and allows the calculation engine to distinguish work inside and outside that roster.

`DO_NOT_USE_NORMAL_ROSTER` treats the trip work plan separately and does not use the normal roster as a comparison basis.

These choices do not establish who is the employer or who is legally liable for payment.

## Travel

`TRAVEL_WITH_RESPONSIBILITY` is a more precise description of active work while travelling with responsibility for the resident. It may overlap an active-work period in the editor. Those overlapping minutes are counted once in salary/work-time totals. Travel that extends beyond an active-work period contributes only the uncovered minutes to active work.

`TRAVEL_WITHOUT_RESPONSIBILITY` and `TRAVEL_UNCERTAIN` remain distinct because their pay treatment is not identical to active travel.

## Resting night watch

A resting night watch is work time minute-for-minute, while its base pay is calculated in the ratio 1:3. Thus a 9-hour resting watch corresponds to 3 hours of base-pay equivalent.

Night, Saturday/Sunday and holiday/high-day allowances for passive work follow the same 1:3 ratio under Dok. 25 point 8.9.

Active work during the resting watch is a separate component under Dok. 25 point 20.4. Actual active minutes are summed per resting watch and rounded to the nearest half hour: 14 minutes or less are discarded, 15 minutes or more round up to the next half hour. The rounded time is paid at hourly rate + 50%.

## Holiday/high-day calculation

Point 12.2.3 is modeled as explicit time windows. The windows differ between 33.6-hour schedules and 35.5/37.5-hour schedules plus point 8.2.2.

Movable Easter, Ascension and Pentecost dates are calculated from the calendar year. Ordinary service in an applicable window receives 1 1/3 hourly wage per worked hour as an additional supplement.

Saturday/Sunday allowance is removed from minutes that already receive point 12.2.3 holiday/high-day supplement, because point 12.2.2 excludes work that is compensated under another provision by more than 50 percent.

## Point-20.2 rule priority — A4.1

Once the comparison model classifies minutes as work compensated under Dok. 25 point 20.2, the app pays hourly wage + 50 percent for those minutes and does not stack ordinary-service supplements from point 12.1.1 or 12.2.2 on the same minutes. Point 12.1.1 concerns ordinary service and explicitly excludes overtime; point 12.2.2 concerns ordinary service and also excludes overtime.

On the special holiday dates listed in point 13.7.3, the app still uses point 20.2 for the same chapter-20 minutes. Point 13.1 makes chapter 13 applicable unless the tariff agreement provides otherwise; point 20.2 is the specific holiday-stay provision and expressly sets hourly wage + 50 percent for worktime beyond ordinary worktime.

These two interactions are therefore implemented rule-priority decisions in ruleset 2026.2 rather than open possible additions. This does not remove the separate comparison-model assumption: the stored ground roster remains an app comparison basis, not a verbatim substitute for the holiday-stay work plan required by point 20.2.

## Payment proposal

The rule-based **payment basis** and the **payment proposal** are separate concepts. The payment basis is the amount produced by the calculation engine. The payment proposal normally mirrors that amount. If the parties have already agreed another settlement amount, the app can document that amount and the reason for the difference without rewriting the work plan, rule-based calculation or open-rule status.

Employer relationship, payment scenario and payment proposal are independent data. The payment scenario is descriptive, does not change the calculated payment basis and does not determine legal liability.

## Work-time control

Work-time warnings remain separate from payment. A lower agreed payment amount does not make a work-time warning disappear and does not constitute approval of the work-time arrangement.

## Travel without supervision responsibility — A4.2

Dok. 25 point 20.3 refers to point 18.4 for travel time. Travel without active supervision responsibility is therefore kept separate from active work. In the normal-roster comparison model, ordinary travel outside the stored ground roster is paid at ordinary hourly rate. In the separate-trip model, registered ordinary travel is paid at ordinary hourly rate.

The point-18.4 notice fact is explicit input on each travel period: **known by previous day**, **not known by previous day**, or **not clarified**. Ordinary travel pay is not withheld while the notice fact is open. When short notice is confirmed, the app adds the overtime supplement for up to two actual hours of ordinary travel required outside ordinary working time. The app applies the two-hour cap once across the current trip so UI block splitting cannot multiply it, then applies point-13.3 rounding to each rate band.

Point 13.2 supplies the ordinary overtime rates: 50 percent during 07.00–20.00 and 100 percent during 20.00–07.00, on Sundays and public holidays, with the day-before rule applied after the end of ordinary work where the stored roster gives that evidence. An explicit `F1` maps to the weekly day off in point 13.7.1 and therefore receives 100 percent. `F2` is not promoted to that status without another tariff basis.

On dates listed in point 13.7.3, the app does not infer the employee-specific eligibility condition from the trip alone. It includes the confirmed ordinary overtime rate and exposes only a possible difference up to 133 1/3 percent as an open rule when that date is actually hit.

For travel without supervision responsibility that overlaps 23.00–07.00, the work plan also records whether the employee had permission to sleep. **Yes** uses point 20.3 passive-character treatment: worktime time-for-time, base pay 1:3 and applicable passive-work supplements 1:3. **No** uses ordinary travel treatment under point 18.4. **Not clarified** keeps the affected night travel outside the payment basis and exposes an unresolved sleep-permission rule. Passive night minutes do not also receive the point-18.4 short-notice overtime treatment.
