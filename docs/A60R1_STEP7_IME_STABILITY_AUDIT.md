# A60R1 — Step 7 IME / multiline stability

Runtime testing of A60 exposed a viewport jump while typing in Begrunnelse.

Fix:
- Begrunnelse has exactly three visible lines (`minLines = 3`, `maxLines = 3`).
- Longer text scrolls inside the field instead of growing the parent layout.
- Supporting text is always present, keeping field height stable.
- Required errors are shown only after the user has focused and left a field.
- The disabled Kontroll gate remains authoritative until both custom values are valid.

No settlement persistence, calculation, PDF, worktime-warning or flow semantics change.
