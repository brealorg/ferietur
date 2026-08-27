# BUGFIX02R2 — ViewModel construction crash

Observed runtime exception:

`java.lang.RuntimeException: Cannot create an instance of class
app.ferietur.ui.FerieturSessionViewModel`

Root cause in the nested exception:

`java.lang.IllegalAccessException: FerieturSessionViewModel is not accessible
from androidx.lifecycle.viewmodel.internal.JvmViewModelProviders`

BUGFIX02 declared the Activity-scoped state holder as:

```kotlin
private class FerieturSessionViewModel : ViewModel()
```

`viewModel()` used Lifecycle's default provider factory. Android Developers
documents `ViewModelProvider.NewInstanceFactory` as a simple factory that calls
the ViewModel's empty constructor. A private top-level class is not reflectively
accessible from Lifecycle's package.

R2 changes exactly one production declaration:

```kotlin
class FerieturSessionViewModel : ViewModel()
```

No state, navigation, tariff, calculation, PDF, security or persistence logic is
changed.

A JVM regression test now requires:
- public ViewModel class;
- public no-arg constructor;
- successful reflective construction.

The runtime installer also clears logcat before launch and hard-fails if the new
APK emits a fresh `FATAL EXCEPTION`/ANR. This prevents old crash records from
being confused with the current run.

BUGFIX02 source SHA256:
`da9103671da3168336561379db8368a49136a8b0c436e4b27410bcc1953b3db7`

BUGFIX02R2 source SHA256:
`61a5c5b4575516971966f4bf91ce8321d1e45ade9ad4bff8799df702bc6cfb89`
