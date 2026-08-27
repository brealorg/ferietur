# Ferietur

Ferietur is a Norwegian Android application for planning staffed holiday trips and producing an explainable wage/payment basis. This tree is the **PLAY01 canonical successor** to the qualified direct-APK release `0.5.4`, preparing the same package identity for Google Play.

## Play candidate identity

- application ID: `app.ferietur`
- versionName: `0.5.5`
- versionCode: `52`
- min SDK: 26
- target/compile SDK: 37
- UI: Jetpack Compose / Material 3
- candidate distribution: Google Play App Bundle
- Play App Signing: must import a copy of the established direct-APK app-signing key
- permanent app-signing certificate SHA-256: `9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7`
- upload signing: separate Play upload key outside source/evidence

## Release state carried forward

The release-blocking CODEAUDIT02 work remains closed through the Android runtime, persistence and release-trust layers:

- CA-001: complete selected trip interval representation and boundary tests;
- CA-002/005/008/009: schema v6 durable finalized snapshots, migration provenance, UUID identity and frozen build/rate metadata;
- CA-003/007: atomic/recoverable draft persistence and main-safe repository/PDF I/O;
- CA-004/006: neutral/time-derived defaults and effective-dated salary applicability;
- CA-010: six Android runtime contracts for cold launch, disclaimer persistence, system Back, rotation, draft recovery and FileProvider sharing;
- CA-012 Stage B: canonical README/source manifest/release evidence;
- CA-013/ICON01: adaptive, round and monochrome suitcase/route/pin identity;
- CA-014: **fixed in PLAY01** by disabling language-resource splitting for the AAB because Ferietur forces nb-NO independently of system locale;
- CA-015: direct `core-ktx` alignment to 1.18.0;
- CA-016: Material3 `1.5.0-alpha26` remains an accepted dependency decision.

The direct APK `0.5.4 / 51` remains the public baseline. PLAY01 changes product code only to add explicit Play privacy disclosure/linking, plus the AAB language-split build configuration and version promotion to `0.5.5 / 52`. It does not change tariff amounts, calculation formulas, persistence schema, Android permissions or package identity.

## Privacy and Google Play

The app still has no `INTERNET` permission. Data entered in Ferietur remains local unless the user explicitly exports/shares a PDF or chooses to contact the developer through an external email app.

Canonical privacy-policy target:

```text
https://brealorg.github.io/ferietur/privacy/
```

The source tree contains the static publication artifact at `play/privacy-policy.html` plus a Play Console Data safety draft and store-listing copy. The web policy must be live before Closed/Production submission.

## Signing continuity

The permanent Android app-signing certificate is unchanged:

```text
9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7
```

Because existing direct-APK installs already use this identity, Google Play must be configured with **Provide a copy of your app signing key** before public distribution. PLAY01A1 creates a separate Play upload key used only to sign the `.aab` uploaded to Play.

Private signing material is never stored in this tree, source snapshot or evidence bundle. See `play/play-app-signing.md` and `docs/SIGN01_SIGNING_IDENTITY.md`.

## Build and trust gates

Canonical source is protected by `SOURCE-SHA256SUMS.txt`. Active gates are successor-semantic plus the PLAY01 canonical source manifest. Historical FINAL01/SIGN01/RC1 phase contracts are retained as audit history but are no longer active version gates.

`apply-build-install.sh` intentionally performs no device installation. It runs source gates plus the strict offline JVM/lint/debug/release/androidTest/AAB build set. PLAY01A1 then signs the AAB externally with the separate upload key.
