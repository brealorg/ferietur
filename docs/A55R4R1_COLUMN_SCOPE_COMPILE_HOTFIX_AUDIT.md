# A55R4R1 — ColumnScope compile hotfix

## A55R4 build failure

Kotlin compiler error:

`Argument type mismatch: actual type is ComposableFunction0<Unit>, but
ComposableFunction1<ColumnScope, Unit> was expected.`

The A55R4 visual rewrite changed the previously compiling expression:

`Column { content() }`

to:

`Column(content = content)`

`MethodFormSection` receives `content` as a plain `@Composable () -> Unit`,
whereas `Column` expects a receiver lambda `@Composable ColumnScope.() -> Unit`.

## Fix

Restore the receiver block and invoke the supplied composable inside it:

`Column { content() }`

This is a one-line Compose type fix. The complete A55R4 Oslo visual contract is
preserved. No production domain, tariff, flow, payment, employer, persistence,
calculation, or PDF semantics are modified.
