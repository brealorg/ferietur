# Play App Signing — Ferietur

## Kritisk identitetskrav

Ferietur finnes allerede som direkte APK med pakkenavn `app.ferietur`. Google Play må derfor bruke **samme app-signing key** som den etablerte direkte APK-linjen for at Play-versjonen skal kunne oppdatere eksisterende installasjoner.

Forventet app-signing certificate SHA-256:

```text
9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7
```

I Play Console: velg **Provide a copy of your app signing key** / tilsvarende alternativ før noen Open testing- eller Production-release låser en annen identitet.

Den lokale app-signing keystoren er fortsatt privat og ligger utenfor kildekoden:

```text
${XDG_DATA_HOME:-$HOME/.local/share}/ferietur/signing/ferietur-release.p12
alias: ferietur-release
```

Ikke last opp `.p12` direkte. Følg den eksakte PEPK-flyten Play Console viser, med Console-versjonen av `pepk.jar` og encryption public key. Den krypterte PEPK-eksporten er sensitiv release-infrastruktur selv om den er kryptert og skal ikke legges i Git/release evidence/chat.

## Upload key

PLAY01A1 oppretter en separat, resettable Play upload key utenfor repoet. Denne signerer `.aab`-filen som lastes opp til Play. Upload key er **ikke** appens Android update identity.

Play skal etter konfigurasjon vise:

- App signing key certificate SHA-256 = `9bc0...52e7`
- Upload key certificate SHA-256 = fingerprinten som PLAY01A1 rapporterer

Ikke last opp AAB-en før dette skillet er forstått og app-signing key handoff er satt opp riktig.
