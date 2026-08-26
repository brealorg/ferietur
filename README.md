<div align="center">

<img src="assets/ferietur-icon.png" width="168" alt="Ferietur app-ikon">

# Ferietur

**Planlegging, beregning og dokumentasjon for ferieturer der ansatte følger med.**

[![Release](https://img.shields.io/badge/release-0.5.4-2A2859?style=flat-square)](https://github.com/brealorg/ferietur/releases/tag/v0.5.4)
![Android](https://img.shields.io/badge/Android-8.0%2B-F9C66B?style=flat-square&logo=android&logoColor=2A2859)
![Package](https://img.shields.io/badge/package-app.ferietur-2A2859?style=flat-square)
![Data](https://img.shields.io/badge/data-local_only-F9C66B?style=flat-square)

[**Last ned siste versjon**](https://github.com/brealorg/ferietur/releases/latest) · [Release notes](https://github.com/brealorg/ferietur/releases/tag/v0.5.4) · [Personvern](https://brealorg.github.io/ferietur/privacy/) · [Rapporter en feil](https://github.com/brealorg/ferietur/issues)

</div>

> [!IMPORTANT]
> **Ferietur er et uavhengig planleggings- og beregningsverktøy.** Appen er ikke utviklet av, godkjent av eller en offisiell tjeneste fra Oslo kommune eller andre offentlige myndigheter. Beregningene erstatter heller ikke gjeldende tariffavtale, arbeidsgivers vurdering eller ordinær lønnskontroll.

## Hvorfor Ferietur?

Når en person er avhengig av at ansatte blir med for å kunne reise på ferie, blir en vanlig ferietur fort også et spørsmål om **turnus, reisetid, arbeidstid, natt, tillegg, ansvar og betaling**.

Ferietur samler disse opplysningene i én strukturert turplan og bygger et beregningsgrunnlag som kan **forklares, kontrolleres og dokumenteres**.

Målet er ikke bare å produsere et sluttbeløp, men å gjøre det synlig **hva som er beregnet, hvorfor og på hvilket grunnlag**.

## Hva appen gjør

- planlegger turen dag for dag
- registrerer reise, arbeid og relevante tidsperioder
- håndterer arbeid på dag, kveld og natt, inkludert hvilende nattevakt og aktivt arbeid
- bygger opp et forklarbart lønns- og betalingsgrunnlag
- støtter ulike oppsett for turnus og separat oppdrag
- lagrer turer og utkast lokalt på enheten
- lager PDF med beregning, forutsetninger og regelinformasjon
- lar brukeren gå tilbake og kontrollere grunnlaget før noe ferdigstilles

## Typisk arbeidsflyt

1. **Opprett turen** – datoer, reiseforløp og grunnoppsett.
2. **Registrer arbeid og ansvar** – hva som faktisk skjer gjennom turen.
3. **Kontroller lønnsgrunnlaget** – blant annet mot nyere lønnsslipp.
4. **Se beregningen** – med synlige forutsetninger og regelgrunnlag.
5. **Eksporter PDF** – som dokumentasjon og grunnlag for videre avklaring.

## Viktig om beregningene

Ferietur er laget for å gjøre kompliserte beregninger mer etterprøvbare, men appen kan ikke vite om lokale avtaler, nye tariffendringer eller arbeidsgivers konkrete vurdering endrer resultatet.

Kontroller derfor alltid:

- lønnsopplysninger mot en nyere lønnsslipp
- at satsene og reglene fortsatt gjelder for den aktuelle perioden
- at arbeidstid, reise og ansvar er registrert slik de faktisk er avtalt og gjennomført
- at beregningsgrunnlaget samsvarer med gjeldende tariff- og arbeidsrettslige regler

## Personvern

Ferietur er laget for lokal bruk:

- turer og utkast lagres lokalt på enheten
- appen har ingen nettverkstillatelse
- Android-backup og enhetsoverføring er deaktivert
- eksporterte PDF-er deles bare når brukeren selv velger å dele dem
- det kreves ingen konto eller innlogging

[**Les personvernerklæringen**](https://brealorg.github.io/ferietur/privacy/)

## Last ned

Siste publiserte produksjonsversjon er **Ferietur 0.5.4** (`versionCode 51`).

- [**Ferietur-0.5.4.apk**](https://github.com/brealorg/ferietur/releases/download/v0.5.4/Ferietur-0.5.4.apk)
- [Komplett Android-pakke](https://github.com/brealorg/ferietur/releases/download/v0.5.4/Ferietur-0.5.4-Android.zip)
- [Alle releases](https://github.com/brealorg/ferietur/releases)

**Krav:** Android 8.0 eller nyere (API 26+).

> Google Play-distribusjon er under klargjøring. Inntil videre er den signerte APK-en den publiserte distribusjonskanalen.

<details>
<summary><strong>Verifiser APK og signeringsidentitet</strong></summary>

### APK SHA-256

```text
38ae4bd3dac217104c27979594436413b01e69914e1236c722a66a86e1b57d7d
```

### Permanent signeringssertifikat SHA-256

```text
9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7
```

Checksum og sertifikatfingeravtrykk ligger også som egne filer i releasen.

</details>

## Release 0.5.4

`0.5.4` er den første offentlig distribuerte, permanent signerte produksjonsutgaven av Ferietur.

Før publisering ble blant annet dette verifisert på reell Android-enhet:

- oppgradering fra release candidate til final med samme permanente signeringssertifikat
- kontinuitet i lagrede appdata og utkast gjennom oppgraderingen
- kaldstart
- disclaimer og lagring av aksept
- system-tilbake
- rotasjon og state retention
- gjenoppretting av lagrede utkast
- PDF-deling via Android FileProvider
- crash/ANR-scan etter sluttkvalifisering

Se [release notes for 0.5.4](https://github.com/brealorg/ferietur/releases/tag/v0.5.4) for den publiserte releasepakken.

## Status

| Spor | Status |
|---|---|
| Signert Android APK | ✅ Publisert |
| Permanent update-signatur | ✅ Etablert |
| Lokal lagring / PDF | ✅ Produksjon |
| Google Play | 🚧 Under klargjøring |
| Kildekodepublisering | Ikke besluttet |

## Kildekode og lisens

Kildekoden er **ikke publisert som del av 0.5.4-utgivelsen**. Repositoryet har derfor foreløpig ingen åpen kildekode-lisens, og det gis ikke noen generell lisens til å kopiere, endre eller redistribuere prosjektets egen kildekode.

Kildekodepublisering og valg av programvarelisens behandles separat før eventuell kildekode legges ut.

## Tilbakemeldinger

Har du funnet en feil eller noe som er uklart, bruk [GitHub Issues](https://github.com/brealorg/ferietur/issues).

Når du rapporterer beregningsfeil, beskriv gjerne **hvilket oppsett du valgte, hvilke tidsperioder som ble registrert og hvilket resultat du forventet**. Ikke legg ved personopplysninger eller sensitive opplysninger fra faktiske brukere.
