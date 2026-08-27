# SIGN01A2 — data-safe debug→release signature transition

## Purpose

SIGN01A2 moves the physical qualification device from the historical AGP debug certificate to the permanent Ferietur direct-APK signing certificate established by SIGN01A1, without accepting silent loss of durable app state.

Android only permits an in-place APK update when the installed and incoming packages have a compatible signing identity. The historical debug certificate and the new Ferietur release certificate are unrelated, so the transition necessarily contains one controlled uninstall. Before that uninstall, SIGN01A2 captures the app-owned durable private state from the debuggable install and verifies a content digest.

## Transition contract

1. Verify the canonical SIGN01 source and the permanent public certificate fingerprint.
2. Build the normal debug, release and androidTest APKs offline with strict dependency verification.
3. Produce three release-certificate artifacts externally with `apksigner`:
   - the ordinary non-debuggable release APK;
   - a **release-signed debuggable migration bridge**, made by re-signing the normal debug APK;
   - a release-signed instrumentation APK so Android can instrument the non-debuggable release target.
4. Force-stop the old debug install and capture durable private state before uninstall. The private archive is stored outside source/evidence under the local Ferietur data directory with mode 0600.
5. Require an explicit local confirmation before the destructive package uninstall.
6. Uninstall the old debug-certificate package, install the release-signed debuggable migration bridge, and restore the private archive through `run-as` under the new package UID.
7. Re-capture the restored bridge state and require an exact path/size/content SHA-256 digest match before proceeding.
8. Install the ordinary non-debuggable release APK in place over the bridge. Because bridge and release APK use the same permanent certificate, Android preserves the restored private state.
9. Verify that `run-as` now fails for the non-debuggable release target, verify the installed certificate, then run a release-signed instrumentation state-integrity test before any Activity launch.
10. Run the six CA-010 Android runtime contracts against the actual non-debuggable release-signed target, scan for fatal/ANR signals, and perform a fresh launcher start.
11. Remove the instrumentation package. The permanent release APK remains installed.

## Durable-state scope

Current Ferietur production code owns durable state in `files/`:

- `files/trip-drafts/`
- `files/trip-draft-backups/`
- `files/datastore/` (including `ferietur_app_info` DataStore state)

SIGN01A2 additionally preserves `shared_prefs/`, `databases/`, and `no_backup/` when they exist, as a conservative package-state safeguard. Cache/code-cache are intentionally excluded.

The backup archive and its private file manifest are never placed in the canonical source tree or evidence bundle. Evidence records only aggregate file count/bytes and a content-manifest SHA-256 digest.

## Instrumentation signing rule

The final release target is non-debuggable. The androidTest APK is therefore re-signed with the same permanent Ferietur release certificate before installation. This preserves the normal instrumentation target/package relationship without adding a release signing configuration or private key material to Gradle/source.

## Rollback posture

If restore verification fails, SIGN01A2 stops with the release-signed debuggable bridge installed and retains the private backup. It does **not** continue to the non-debuggable release APK. This keeps a `run-as` recovery path available.

If signed release qualification fails after the exact restore proof, the permanent release-signed app remains installed and the private backup is retained for diagnosis. The script never executes `pm clear`.
