# PLAY01 — Google Play readiness

PLAY01 is the first canonical successor after the direct APK 0.5.4 release and prepares Ferietur for Google Play without changing its package identity.

## Identity

- applicationId: `app.ferietur`
- candidate versionName: `0.5.5`
- candidate versionCode: `52`
- targetSdk / compileSdk: 37
- direct APK app-signing certificate SHA-256 remains `9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7`
- Play upload key is a separate key and must not replace the app-signing key.

## Product changes in PLAY01

1. Add a visible Play privacy-policy entry in About, pointing to `https://brealorg.github.io/ferietur/privacy/`.
2. Expand the in-app privacy summary to state local-only storage, no account, no ads/analytics, no internet access and user-initiated PDF sharing.
3. Close CA-014 by disabling App Bundle language splits. Ferietur forces `nb-NO` independently of the device locale, so all library language resources must remain available in generated base APKs.
4. Add Play Console metadata and policy working documents.

No tariff amount, calculation formula, persistence schema or Android permission is added or changed.

## Play App Signing

The app already has an external/direct-APK install base. Play App Signing must therefore receive a copy of the existing permanent app-signing key before public Play distribution. The AAB itself is signed with a separate Play upload key.

PLAY01A1 deliberately does not export the permanent private app-signing key. PEPK handoff is a separate console-bound checkpoint because the Console supplies the current PEPK tool and encryption public key.

## Privacy publication gate

The static `play/privacy-policy.html` file is ready to publish at:

`https://brealorg.github.io/ferietur/privacy/`

Closed/Production submission must not proceed until that URL is live and matches the in-app/Play Console disclosures.

## Store assets

Visual store assets are deferred to STOREASSET01. Screenshots must be captured from the real app; generated UI mockups are not accepted as evidence or store screenshots.
