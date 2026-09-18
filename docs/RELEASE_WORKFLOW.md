# Ferietur release transaction

This file and `release/current.env` are the canonical release-control record.

The release process must not depend on chat history, shell history, or human memory. No secret values are stored here.

## State machine

1. `SOURCE_QUALIFIED`
   - `main` contains the intended app version.
   - versionName/versionCode are fixed.
   - unit tests, release lint, and release build qualification pass.
   - the exact qualified app-source commit is recorded in `release/current.env`.

2. `UPLOAD_KEY_READY`
   - Play Console accepts the registered upload-key certificate.
   - upload-key private material remains local.
   - the password is stored in the local secret store, never in Git.

3. `AAB_READY`
   - build `bundleRelease` from the recorded version.
   - sign the AAB with the currently registered Play upload key.
   - record the AAB SHA-256.

4. `PLAY_ACCEPTED`
   - upload the signed AAB to the intended Play track.
   - Play accepts versionCode/versionName.
   - record track and Play status.

5. `PLAY_SIGNED_APK_VERIFIED`
   - obtain the Play-signed universal APK for the accepted release.
   - verify package `app.ferietur`.
   - verify versionCode/versionName.
   - verify app-signing certificate SHA-256:
     `9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7`.
   - record APK SHA-256.

6. `GITHUB_RELEASE_PUBLISHED`
   - create/push the release tag.
   - publish the GitHub Release with the verified Play-signed APK, checksums,
     signing-certificate fingerprint and release record.
   - README must not claim the new public release before this succeeds.

7. `README_SYNCED`
   - update the release badge, download links, versionCode/versionName and APK SHA
     to the release that actually exists.
   - commit and push the README update.

8. `COMPLETE`
   - `release/current.env` records Play, GitHub Release and README as complete.
   - `main` is clean and tracks `origin/main`.

## Transaction rules

- `release/current.env` is the persistent non-secret source of truth.
- Every material release milestone updates this file.
- Never rotate or replace the app-signing key as part of a normal release.
- Upload-key replacement is independent of the app-signing key.
- Never store passwords, private keys, keystores, recovery codes, or secret values in Git.
- The latest source version and latest published release are separate concepts.
- A GitHub README download link is updated only after the corresponding GitHub Release exists.
- If a step fails, preserve the last successful state and resume from `NEXT_ACTION`; do not restart the release from memory.
