# ICON01 — launcher identity polish

## Decision

ICON01 replaces the RELEASE01 simple suitcase mark with the selected Ferietur launcher identity while preserving the established Oslo palette.

Regular adaptive icon:

- background: Oslo dark blue `#2A2859`;
- foreground: Oslo yellow `#F9C66B`;
- mark: front-facing suitcase with separated side shells;
- travel motif: a dark route from a start point to a location pin integrated into the suitcase;
- no raster/AI-generated image is shipped in the application.

The selected concept image is a design reference only. The shipped launcher assets are deterministic Android `VectorDrawable` resources.

## Themed icon

Android 13+ uses a monochrome vector. For small-size legibility the monochrome layer deliberately simplifies the route treatment and keeps the suitcase plus location-pin cutout as the identifying silhouette.

## Scope

ICON01 is visual-resource polish on top of the qualified RC1 candidate. It does not change:

- `applicationId`, versionCode or versionName;
- production Kotlin;
- AndroidManifest;
- dependency coordinates or verification metadata;
- tariff data, calculation formulas or persistence schema;
- backup/security policy;
- signing state.

RC1 therefore remains `app.ferietur`, versionCode `50`, versionName `0.5.4-rc1` while ICON01 is qualified before SIGN01.

## Qualification

ICON01 requires:

1. exact canonical RC1 baseline verification;
2. resource mutation limited to launcher colors, foreground vector and monochrome vector;
3. launcher XML parsing and semantic icon gate;
4. canonical source-manifest regeneration;
5. offline strict unit/lint/debug/release/androidTest build surface;
6. in-place debug-lineage install without clearing app data;
7. six CA-010 device contracts and crash/ANR scan;
8. manual Pixel launcher/Recents/themed-icon visual sanity;
9. canonical ICON01 source/evidence checkpoint.
