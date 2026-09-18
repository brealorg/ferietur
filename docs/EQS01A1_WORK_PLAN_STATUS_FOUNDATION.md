# EQS01A1 — holiday work-plan status foundation

## Purpose

EQS routine 53398 distinguishes the ordinary roster from the dedicated work plan for a holiday
stay. A plan may deviate from the ordinary roster without automatically triggering overtime when
it is changed with at least 14 days notice and complies with working-time rules.

Ferietur 0.5.6 did not persist this fact independently. It used the ordinary roster as a calculation
comparison and could therefore not safely migrate directly to the later EQS01 semantics.

## Scope

This slice introduces `HolidayWorkPlanStatus` and freezes it in both editable drafts and finalized
snapshots. It deliberately does **not** change monetary calculations or user-facing choices yet.

- draft schema: v6 -> v7;
- finalized snapshot binary format: v4 -> v5;
- pre-v7 editable drafts migrate to `NOT_CLARIFIED`;
- genuine v6 finalized snapshots and history are preserved unchanged;
- old finalized binary formats remain readable and receive conservative `NOT_CLARIFIED` status.

## Follow-up

EQS01A2 will wire the status into live UI/finalization gates. EQS01B will replace automatic
`outside ordinary roster = +50%` classification with explicit holiday-plan/extra-work semantics.
