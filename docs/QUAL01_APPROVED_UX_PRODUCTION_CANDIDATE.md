# QUAL01 — approved UX production candidate, no release signing

Basis:
- canonical PLAY01A1R2 source, package `app.ferietur`, version 0.5.5 / 52
- manually accepted PREVIEW01R4R1 UX behavior
- R4 empty-workplan-day physical touch propagation fix
- Compose test rule migration to junit4 v2

Approved UX changes:
1. neutral trip-name example
2. roster editor opens expanded
3. previously-used roster template replaces primary shift with code + times
4. empty work-plan day is physically tappable and opens Add period for that date
5. extended/high-contrast Add period FAB
6. higher-contrast period editor
7. clearer Documentation/PDF actions
8. IME Done closes trip-name keyboard

R4 root cause:
`DayTimeline` previously installed `pointerInput/detectTapGestures` even when
there were no blocks. The empty child timeline could consume the physical tap,
preventing the parent empty-day card click. QUAL01 keeps the accepted fix:
timeline pointer input is installed only when `blocks.isNotEmpty()`.

Temporary PREVIEW01 callback-state diagnostics are removed. Stable DEBUG-only
semantic test tags remain to support deterministic regression testing.

QUAL01 itself performs no permanent app signing, no Play upload signing,
no AAB generation, and no mutation of the installed `app.ferietur`.
