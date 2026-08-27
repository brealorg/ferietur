# CODEAUDIT FIX02 — CA-003 + CA-007

## Scope

FIX02 changes the persistence/I/O boundary only. It builds on FIX01 and does not
change tariff amounts, calculation formulas, saved-trip schema version, PDF
content, SECURITY03 policy or the APPINFO UI.

## CA-003 — atomic and recoverable local drafts

`TripDraftStore` remains the blocking file implementation but is no longer used
by Compose directly.

Writes now use `androidx.core.util.AtomicFile`:

- existing valid primary is copied to the existing per-trip backup history;
- the new primary is written through `startWrite()`;
- a successful write commits with `finishWrite()`;
- failures call `failWrite()` so the last known-good primary remains valid;
- no live-file delete happens before commit.

Reads return explicit states instead of `mapNotNull()` disappearance:

- valid;
- recovered from latest valid backup;
- corrupt with no recoverable backup;
- unsupported newer schema.

A corrupt primary is automatically restored from the newest valid backup.
An unsupported newer schema is **not** downgraded from an older backup; the
future-format primary is preserved and surfaced as unsupported.

`SavedTripDraftCodec` still writes schema version 5. FIX02 only gives the
unsupported-schema failure a typed exception so the data layer can distinguish
it from corruption.

## Repository boundary and mutual exclusion

`TripRepository` is the only UI/ViewModel-facing draft data API.

Its load/save/delete operations:

- execute under `withContext(Dispatchers.IO)`;
- serialize store access with a coroutine `Mutex`, matching AtomicFile's
  requirement that callers provide mutual exclusion;
- return the current library plus storage issues.

The Home screen now displays a concrete warning if recovery/corruption/newer
schema/storage I/O is encountered instead of silently showing fewer trips.

## CA-007 — main-safe I/O

The Activity-scoped `FerieturSessionViewModel` receives:

- `TripRepository`;
- `PdfExportRepository`.

The ViewModel owns library load, autosave, explicit save, delete, duplicate and
PDF creation through `viewModelScope`.

There is no `TripDraftStore` call from `FerieturApp.kt`.

PDF rendering/writing is performed by `PdfExportRepository` on
`Dispatchers.IO`. Compose receives a small `PdfExportUiState`:

- idle;
- creating short;
- creating full;
- ready;
- error.

Only the already-created file is handed back to the Android share sheet on the
main thread. `PdfExporter.share()` no longer creates a PDF synchronously.

## Regression tests

FIX02 adds focused tests for:

- failed AtomicFile write preserves last good primary;
- abandoned/incomplete AtomicFile write preserves last good primary;
- corrupt primary recovers from latest valid backup and restores the primary;
- unrecoverable corruption is surfaced;
- unsupported newer schema is surfaced and is not downgraded;
- replacement save keeps backup history and delete removes primary/backups;
- TripRepository storage operations execute on the injected I/O dispatcher;
- unsupported schema has a typed codec failure.

No schema bump is made here. The deliberate schema migration remains FIX03.

Base FIX01 UI SHA256:
`d5ba3d15638314d0480de9fdbc27eb67c4d4529ee79b3d03d24b44e736ec51cb`

FIX02 UI SHA256:
`41b6752210ff82dec76c97ba53ee195cb13e7719ec75467e70e4042a442cdaa5`
