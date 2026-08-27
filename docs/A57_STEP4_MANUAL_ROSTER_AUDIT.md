# A57 — Step 4 manual grunnturnus

## Product contract

A57 removes the development-only assumption that Ferietur knows the employee's
grunnturnus or that shift codes have common Oslo-wide meanings.

### New-trip behavior

- Step 1 supplies the trip dates.
- Step 4 starts with **no roster registrations**.
- Every trip date must be explicitly registered before the user can continue.
- A date is registered as either work or free.
- Every registration requires a **vaktkode**.
- Work additionally requires actual **Fra** and **Til** times.
- Multiple work intervals may be registered on the same date.
- Previously used work registrations in the current trip are offered as local
  reusable templates.
- A free day can be marked as **ukentlig fridag** because Dok. 25 overtime logic
  must not infer that tariff meaning from a local code.

### UI

The completed weekly overview is retained as the review/edit surface.

The old site-specific shift-catalogue picker is removed. Step 4 now has:

- rows for every trip date, initially `Ikke registrert`
- Extended FAB `Legg til vakter`
- direct day editor
- Material 24-hour time input
- mandatory local shift code
- local-template reuse
- support for more than one work interval per date
- explicit free-day registration
- overlap validation

The following old development copy is removed:

- `Langvakt og natt beholdes som sine reelle intervaller`
- `K6 er skjult for tur`

### Domain/persistence

The public draft shape remains `Map<LocalDate, String>` for backward file
compatibility, but A57 introduces an `MR1` roster-day encoding that stores real
user-defined intervals and codes. Calculation code decodes all intervals and no
longer assigns tariff meaning by looking up the user's code in the Solhaugen
catalogue.

The old catalogue remains decode-only compatibility data for old tests/files.
When an old pre-A57 draft is resumed, legacy catalogue roster values are removed
from the editable roster because they were never verified manual grunnturnus
data.

Saved draft schema is bumped from 4 to 5.

### Calculation invariants

- Actual time intervals remain the comparison/calculation basis.
- Night classification is derived from a shift crossing midnight, not the code.
- `ukentlig fridag` is explicit data, not inferred from `F1`.
- Multiple baseline intervals are included in roster overlap, supplements,
  uncovered-roster evidence and finalized snapshots.
- Overlapping roster intervals block Step 4 completion.

No tariff rates, salary table, payment model, travel model or PDF calculation
formula is changed.
