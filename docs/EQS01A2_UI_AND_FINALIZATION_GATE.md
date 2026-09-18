# EQS01A2 — holiday work-plan UI and finalization gate

## Purpose

EQS01A1 introduced the versioned `HolidayWorkPlanStatus`. A2 makes that status explicit in the
editable workflow and prevents a new Oslo calculation from being frozen while the status remains
unknown.

Source basis: Oslo kommune EQS ID 53398, revision 1.2, valid from 22.06.2026.

## Product behavior

A2:

- exposes three explicit work-plan states for Oslo kommune;
- restores and persists the state in editable drafts;
- resets new trips conservatively to `NOT_CLARIFIED`;
- includes the state in autosave invalidation;
- gates both METHOD progression and CONTROL -> SUMMARY finalization;
- adds an imperative finalization fence for resumed/direct-navigation paths;
- freezes the chosen state into new finalized snapshots;
- shows the frozen state in the final summary;
- keeps historical snapshots viewable.

A2 deliberately does **not** change any amount, multiplier, travel rule, worktime calculation,
payer allocation or PDF calculation wording.

## Follow-up

EQS01B replaces the legacy `outside ordinary roster = +50%` monetary classification with explicit
holiday-work-plan / work-beyond-plan semantics.
