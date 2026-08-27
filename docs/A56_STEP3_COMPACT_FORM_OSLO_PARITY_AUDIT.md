# A56 — Step 3 compact form + Oslo parity

## Runtime goals

- remove the redundant introductory paragraph
- retain the existing salary-step +/- control and circular step indicator
- add a restrained Oslo-yellow ring to the salary-step circle
- show annual salary as a concise yearly amount
- replace four weekly-basis chips with one Material dropdown
- keep the weekend-rate selector as one Material field while preserving its detailed bottom-sheet guidance
- shorten the mandatory payslip confirmation
- move source/rule explanation into the same compact rules-row pattern as Step 2
- reduce vertical density without changing salary semantics

## State/logic invariants

The existing callbacks remain authoritative: `onSalaryStep`, `onWeeklyBasis`, `onWeekendProfile`, and `onPayslipChecked`.

The parent screen still clears the payslip confirmation whenever salary step, weekly basis, or weekend profile changes.

No production tariff, salary-table, calculation, persistence, PDF, payment, or flow semantics are changed.
