# FIX02R1 — InlineMessage named-argument compile hotfix

FIX02 source contracts passed, then Kotlin compilation failed at FerieturApp.kt
because the new storage-issue call used:

```kotlin
InlineMessage(
    severity = severity,
    title = title,
    body = issue.detail,
)
```

The existing local composable is declared as:

```kotlin
private fun InlineMessage(
    severity: FindingSeverity,
    title: String,
    detail: String,
)
```

R1 changes only:

`body = issue.detail` -> `detail = issue.detail`

No persistence, repository, coroutine, PDF, calculation, tariff, UI behavior or
security logic changes.

Broken FIX02 UI SHA256:
`41b6752210ff82dec76c97ba53ee195cb13e7719ec75467e70e4042a442cdaa5`

FIX02R1 UI SHA256:
`53570b11fcfce8b75500724ca37df12447933ed19dfed046ca191c7529aa87f2`
