# A60R2 — Step 7 ordinary multiline text-area stability

A60R1 still showed viewport jumping while typing in Begrunnelse.

A60R2 fixes the container rather than limiting the text:

- Step 7 changes from LazyColumn to an eager Column with verticalScroll.
- IME padding is applied to the form viewport.
- Begrunnelse has a stable 176 dp editor viewport.
- There is no maxLines cap; text length is unrestricted and excess content
  scrolls inside the text field.
- helper/error text is rendered outside the TextField decoration, keeping the
  editor's measured geometry stable.
- touched-field validation remains.

No settlement, persistence, calculation, worktime-warning, PDF or flow
semantics are changed.
