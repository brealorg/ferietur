# SIGN01 — long-lived direct-APK signing identity

## Decision

Ferietur direct-APK releases use one long-lived, self-managed signing identity. The private key is not part of the source repository, source snapshot, evidence bundle, APK metadata, or documentation.

- application ID: `app.ferietur`
- versionCode: `50`
- versionName: `0.5.4-rc1`
- key algorithm: RSA 4096-bit
- certificate signature algorithm: SHA256withRSA
- keystore format: PKCS12
- key alias: `ferietur-release`
- APK signing: v2 + v3; v1 disabled because minSdk is 26; v4 disabled for the distributable APK
- APK alignment: 4-byte ZIP alignment plus 16 KiB alignment for uncompressed shared libraries
- signing certificate SHA-256: `9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7`

The local keystore is expected at `${XDG_DATA_HOME:-$HOME/.local/share}/ferietur/signing/ferietur-release.p12`. That path is intentionally outside the canonical source tree.

## Key-continuity rule

Android uses the application signing identity to authorize updates. Losing this private key, losing its password, or distributing a future APK with an unrelated key breaks normal update continuity for installations signed with this identity. The keystore and its password therefore require independent durable backups before the first public release.

The public certificate fingerprint above is safe to publish and is the canonical identifier used to verify that future direct-APK artifacts use the same signing identity.

## Current debug-lineage transition

The development Pixel currently has `app.ferietur` installed on the historical AGP debug-signing lineage. SIGN01A1 deliberately does not uninstall, replace, or clear that installation. A new unrelated release certificate cannot be installed as an in-place update over the old debug certificate.

SIGN01A2 defines and qualifies that signature transition: it preserves durable private state, performs one controlled uninstall of the historical debug-certificate package, restores the state through a temporary debuggable bridge signed with the permanent identity, and then updates in place to the non-debuggable signed release candidate. The transition is accepted only after exact state-digest verification and signed-device runtime qualification.

## Android 17 PQC note

Android 17 introduces hybrid PQC APK signing (v3.2 / ML-DSA). SIGN01A1 intentionally establishes the classical RSA signing baseline only. A future PQC adoption is a separate signing-lineage migration and must not be silently mixed into the first permanent direct-APK identity.
