# Data safety — utkast til Play Console

Dette er et arbeidsgrunnlag. Svarene må kontrolleres mot den faktiske AAB-en og Play Consoles gjeldende spørsmål før innsending.

## Overordnet

- Samler appen inn eller deler appen noen av de oppførte brukerdataene? **Nei**.
- Data som brukeren registrerer behandles bare lokalt på enheten.
- Ingen `INTERNET`-permission.
- Ingen annonser.
- Ingen analytics/crash-reporting SDK.
- Ingen konto eller innlogging.

## Brukerinitiert eksport/deling

PDF-eksport og e-postkontakt skjer bare etter en eksplisitt brukerhandling. Google Plays Data safety-veiledning angir at lokal behandling ikke er «collection», og brukerinitiert overføring der brukeren forventer deling er unntatt fra «sharing»-deklarasjonen. Dette må likevel beskrives tydelig i personvernerklæringen.

## Sletting

- Lokale turer kan slettes i appen.
- Avinstallasjon fjerner private appdata.
- Android-backup/enhetsoverføring er deaktivert.
- Eksporterte PDF-er utenfor appens private område må slettes av brukeren der de er lagret.

## Personvernpolicy

Play Console URL: `https://brealorg.github.io/ferietur/privacy/`

Policyen må være live, offentlig, ikke-geoblokkert, ikke PDF og ikke en redigerbar dokumentlenke før Closed/Production submission.
