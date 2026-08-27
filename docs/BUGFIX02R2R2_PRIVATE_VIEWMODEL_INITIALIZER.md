# BUGFIX02R2R2 — private ViewModel with explicit Compose initializer

The R2 runtime stack trace established why plain `viewModel()` crashed:
Lifecycle's default NewInstanceFactory reflectively tried to construct a
file-private `FerieturSessionViewModel`.

R2 then made the class public. That removed the reflection access problem, but
Kotlin correctly rejected the public API because the ViewModel exposes several
file-private UI types (`FlowScreen`, `PlanPeriodEditTarget`, `PlanFabAction`,
`SettlementMode`, `DraftSaveState`).

The correct fix is not to widen those implementation types.

Android Developers' current `lifecycle-viewmodel-compose` API has an initializer
overload:

```kotlin
val session: FerieturSessionViewModel = viewModel {
    FerieturSessionViewModel()
}
```

The initializer lambda constructs the ViewModel directly, so Lifecycle does not
need reflective access to its empty constructor.

R2R2 therefore:

- restores `FerieturSessionViewModel` to `private`;
- changes plain `viewModel()` to the explicit initializer overload;
- leaves the Activity-scoped ViewModelStore ownership unchanged;
- leaves all BackHandler/navigation and configuration-retained session state
  unchanged;
- removes the obsolete R2 reflection test, because the implementation is
  intentionally private and reflection is no longer the construction path.

Production source transition:

R2 public/reflection source:
`61a5c5b4575516971966f4bf91ce8321d1e45ade9ad4bff8799df702bc6cfb89`

R2R2 private/initializer source:
`d807a617006355bd37e9205bef5e3ac4ff68528092fa9c5ad5b647827bf20b38`

No domain, tariff, calculation, PDF, storage, backup, or security behavior is
changed.
