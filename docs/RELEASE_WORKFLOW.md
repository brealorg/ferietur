# Ferietur release transaction

`release/current.env` records observed milestones. This runbook defines the procedure;
neither file independently proves the current state of Google Play or GitHub Releases.

## Shared release identity

- Canonical repository: `brealorg/ferietur`; local checkout: `$HOME/dev/ferietur/app`.
- Read saved state and current local/remote status before resuming.
- Keep versionName, versionCode, qualified source and artifact hashes explicit.
- Record only completed checks as PASS. Keep failed/unverified stages separate.
- Git source pushes, GitHub Releases, Play rollout and README updates are different events.

## Direct APK track

1. Verify the qualified source/version and the unsigned release artifact.
2. Align the APK, then sign with the existing `ferietur-release` alias in
   `$HOME/.local/share/ferietur/signing/ferietur-release.p12`.
   Use 1Password item **Ferietur - keystore password**. Never store its value in Git.
3. Verify signature, certificate, package `app.ferietur`, version, non-debuggable
   status and APK alignment. Record the exact signed APK hash and source reference.
4. Test the actual signed APK through an in-place update on the agreed device.
   Do not uninstall, clear data or use a downgrade workaround. An install/start
   check does not prove data continuity or manual UX acceptance; record those separately.
5. After runtime acceptance and publication authorization, tag the intended source
   and publish the verified APK, checksums and release record to GitHub Releases.
6. Update README download links, version and hash only after those assets exist.

Direct APK signing is available with the original Ferietur release key. Waiting for
an upload-key reset or production-access review is not a technical prerequisite for
this track. The temporary Play-signed-universal-APK detour is not mandatory.

## Google Play track

1. Read the actual Play upload-key status. A scheduled reset is neither canceled nor
   active merely because credentials have been found locally.
2. Existing v2 keystore: `$HOME/.local/share/ferietur/play-upload/ferietur-play-upload-v2.p12`.
   Credential item: **Ferietur - Google Play upload key** in 1Password. Verification
   with that correct item is still required; release-key success does not prove it.
3. v3 reset candidate: `$HOME/.local/share/ferietur/play-upload/ferietur-play-upload-v3.p12`.
   Its password was stored in GNOME Secrets with attributes `application=ferietur`,
   `purpose=play-upload-v3`. Successful retrieval and private-key use are separate checks.
4. Use only the key Play actually accepts. Do not cancel/reset/replace keys implicitly.
5. Build and sign the AAB, record its hash and source, and upload to the agreed track.
   Check versionCode availability. Distinguish accepted upload, processing, review,
   production access and actual rollout.
6. Check app-signing identity on distributed APKs before cross-channel update claims.

## Coordination and recovery

- Both tracks share recorded source/version but may have different external blockers.
- Keep Play status pending until actual Play evidence changes it, even if GitHub is ready.
- Mark the whole release COMPLETE only when the agreed tracks and README are reconciled.
- The former credential-loss diagnosis was incorrect: the correct Ferietur release
  credential was found in 1Password and successfully used to sign 0.6.0.
- A saved status is a dated record, not fresh remote verification. Reconcile newer evidence.
- Never create a new repository or modify app-signing identity during an ordinary release.
- Never record passwords/private keys in state files, logs or chat. Certificate hashes
  and non-secret credential-item names are safe identifiers, not credentials.
- On failure, preserve completed milestones and actual error output; resume rather than
  repeating signing, rebuilding, rotating keys or inventing another release procedure.
