# A5.4 – Step 1 UI and Norwegian time notation

Scope is deliberately narrow.

## Step 1 – Turen
- Shorter instruction: name the trip and choose start/end.
- Trip name is a compact single-line field with an external label.
- Date and time controls are compact, consistent controls with calendar/clock affordances.
- No workflow or validation logic changed.

## Clock notation
User-facing clock times now use Norwegian `HH:mm` notation, e.g. `06:00`, `11:30`, `23:00–07:00`. The same notation is used by UI evidence, domain-generated explanations and PDF clock rendering. Numeric calendar dates such as `01.05.2026` remain unchanged.

## Deferred bugs
Not touched in A5.4:
1. Starting a new trip and going back saves it as `Sommerferie`.
2. Rotating the app to landscape returns to the home/library screen.

These are intentionally deferred until the page-by-page UI review is complete.
