# ADR01 — release dependency decisions

## Status

Accepted for RELEASE01 / direct-APK pre-RC.

## Material3 alpha — CA-016

Ferietur intentionally declares `androidx.compose.material3:material3:1.5.0-alpha26` even though the Compose BOM constrains a stable Material3 line. The current UI uses Material3 expressive APIs and CODEAUDIT02 did not demonstrate a defect attributable to the alpha dependency.

Decision: retain the explicit alpha override for RELEASE01. Do not downgrade solely to eliminate an audit observation.

Review trigger: re-evaluate when the expressive APIs used by Ferietur are available in a stable Material3 release, or immediately if a concrete alpha-related runtime/build regression appears.

## core-ktx — CA-015

The TEST01 baseline declared `androidx.core:core-ktx:1.17.0` while its effective release runtime graph already selected 1.18.0 transitively. RELEASE01 aligns the direct declaration to `1.18.0` so the source declaration matches the intended graph.

This is dependency hygiene only; no product behavior change is intended. RELEASE01 verifies the resolved `releaseRuntimeClasspath`, strict dependency verification, JVM tests, Android runtime tests, lint and device sanity after alignment.

## nb-NO / App Bundle — CA-014

Ferietur forces an nb-NO resource context at runtime independently of the device language. That is incompatible with relying on Play's default language-resource splits, because Norwegian library resources may be absent on a device whose selected system language is not Norwegian.

PLAY01 closes CA-014 by setting `android.bundle.language.enableSplit = false`. Language resources are therefore packaged with the base APKs generated from the App Bundle. Density and ABI splitting remain enabled by default. This matches Android's documented mitigation for apps that change language independently of the system language.

Decision: **FIXED for Play AAB distribution in PLAY01.** Revisit only if Ferietur later adopts Android per-app languages or on-demand language delivery.
