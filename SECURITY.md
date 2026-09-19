# Sikkerhet

## Melde en sårbarhet

Har du funnet noe som kan true brukernes data eller appens integritet, meld det **privat**:

1. Bruk **Security → Report a vulnerability** i dette repositoryet (GitHub private vulnerability reporting), eller
2. send e-post til kontaktadressen i [personvernerklæringen](https://brealorg.github.io/ferietur/privacy/).

Ikke opprett en offentlig issue for sikkerhetsfeil. Beskriv hva som skjer, hvilken versjon det gjelder og hvordan feilen kan gjenskapes. Ikke legg ved ekte personopplysninger eller lønnsdata.

Du får svar så snart som mulig, normalt innen en uke. Ferietur vedlikeholdes av én person på fritiden, så det gis ingen garantert responstid og det finnes ingen dusørordning.

## Hva som regnes som en sårbarhet

Ferietur har ingen nettverkstillatelse, ingen konto og lagrer alt lokalt. Relevante funn er derfor typisk:

- en `.ferietur`-sikkerhetskopi eller annen importert fil som kan skade, overskrive eller lekke appens data
- data som havner utenfor appens private lagring uten at brukeren har valgt å dele dem
- at appen får tillatelser eller nettverkstilgang den ikke skal ha
- svakheter i signering, oppdateringsidentitet eller release-prosessen

Feil i selve beregningene er ikke sikkerhetsfeil. Meld dem som vanlig [issue](https://github.com/brealorg/ferietur/issues), uten personopplysninger.

## Støttede versjoner

Bare siste publiserte versjon får sikkerhetsrettinger. Oppdater til nyeste versjon før du melder en feil.

## Tidligere sikkerhetsrettinger

| Versjon | Retting |
| --- | --- |
| 0.6.1 | Tur-ID-er fra importerte sikkerhetskopier valideres før de brukes i filstier (SECURITY04). Se [docs/CODEREVIEW01_HARDENING.md](docs/CODEREVIEW01_HARDENING.md). |
