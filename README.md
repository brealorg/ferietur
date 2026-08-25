# Ferietur

Ferietur er et Android-verktøy for planlegging av bemannede ferieturer og for å lage et forklarbart lønns- og betalingsgrunnlag.

Prosjektet er uavhengig og er ikke en offisiell app fra Oslo kommune eller andre offentlige myndigheter.

## Last ned

Siste publiserte versjon er **Ferietur 0.5.4** (`versionCode 51`).

- [GitHub Release v0.5.4](https://github.com/brealorg/ferietur/releases/tag/v0.5.4)
- [Ferietur-0.5.4.apk](https://github.com/brealorg/ferietur/releases/download/v0.5.4/Ferietur-0.5.4.apk)
- [Komplett Android-pakke](https://github.com/brealorg/ferietur/releases/download/v0.5.4/Ferietur-0.5.4-Android.zip)

### APK-integritet

```text
SHA-256
38ae4bd3dac217104c27979594436413b01e69914e1236c722a66a86e1b57d7d
```

Permanent signeringssertifikat:

```text
SHA-256
9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7
```

Checksum og sertifikatfingeravtrykk ligger også som egne filer i releasen.

## Hva appen gjør

Ferietur hjelper med å strukturere en ferietur, registrere arbeid og relevante perioder og bygge opp et etterprøvbart beregningsgrunnlag. Appen kan lagre turer og utkast lokalt og eksportere PDF med beregnings- og regelinformasjon.

Målet er at beregningen skal være forståelig og kontrollerbar, ikke bare ende i et sluttbeløp.

## Viktig om beregningene

Ferietur er et **planleggings- og beregningsverktøy**. Resultatet erstatter ikke gjeldende tariffavtale, arbeidsgivers vurdering eller ordinær lønnskontroll.

Kontroller alltid:

- lønnsopplysninger mot en nyere lønnsslipp
- at satsene og reglene fortsatt gjelder for den aktuelle perioden
- at registrert arbeidstid og reise samsvarer med det som faktisk er avtalt og gjennomført

## Personvern og lagring

- turer og utkast lagres lokalt på enheten
- appen har ingen nettverkstillatelse
- Android-backup og enhetsoverføring er deaktivert
- eksporterte PDF-er deles bare når brukeren selv velger å dele dem

## Plattform

- Android 8.0 eller nyere (API 26+)
- direkte APK-distribusjon
- pakkenavn: `app.ferietur`

Ved installasjon utenfor Google Play kan Android be om tillatelse til å installere apper fra den aktuelle nettleseren eller filbehandleren.

## Release 0.5.4

0.5.4 er den første offentlig distribuerte, permanent signerte produksjonsutgaven. Releasekandidaten ble oppgradert til final versjon på reell Android-enhet med samme permanente signeringssertifikat. Durable appdata og utkast ble kontrollert før og etter oppgraderingen, og runtime-kontraktene for kaldstart, disclaimer, system-tilbake, rotasjon, draft recovery og FileProvider-deling passerte.

Se [release-notatene](https://github.com/brealorg/ferietur/releases/tag/v0.5.4) for detaljer og nedlastbare verifikasjonsfiler.

## Kildekode og lisens

Kildekoden er **ikke publisert som del av 0.5.4-utgivelsen**. Repositoryet har derfor foreløpig ingen åpen kildekode-lisens, og det gis ikke noen generell lisens til å kopiere, endre eller redistribuere prosjektets egen kildekode.

En eventuell publisering av kildekode og valg av programvarelisens behandles separat før kildekoden legges ut.
