<div align="center">

<img src="assets/ferietur-icon.png" width="168" alt="Ferietur app-ikon">

# Ferietur

**Planlegging, beregning og dokumentasjon for ferieturer der ansatte følger med.**

[![Release](https://img.shields.io/badge/release-0.5.5-2A2859?style=flat-square)](https://github.com/brealorg/ferietur/releases/tag/v0.5.5)
![Android](https://img.shields.io/badge/Android-8.0%2B-F9C66B?style=flat-square&logo=android&logoColor=2A2859)
![Package](https://img.shields.io/badge/package-app.ferietur-2A2859?style=flat-square)
![Data](https://img.shields.io/badge/data-local_only-F9C66B?style=flat-square)

[**Last ned siste versjon**](https://github.com/brealorg/ferietur/releases/latest) · [Release notes](https://github.com/brealorg/ferietur/releases/tag/v0.5.5) · [Personvern](https://brealorg.github.io/ferietur/privacy/) · [Rapporter en feil](https://github.com/brealorg/ferietur/issues)

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

<!-- FERIETUR_SCREENSHOTS_START -->
## Skjermbilder

Ekte skjermbilder fra Ferietur på Android.

<p align="center">
  <img src="assets/screenshots/01-mine-turer.png" width="30%" alt="Mine turer med pågående tur og tur klar for eksport">
  <img src="assets/screenshots/02-turen.png" width="30%" alt="Turoppsett med datoer og reisetider">
  <img src="assets/screenshots/03-arbeidsplan.png" width="30%" alt="Arbeidsplan med arbeid, natt og reise">
</p>

<p align="center">
  <img src="assets/screenshots/04-beregning.png" width="30%" alt="Beregning med forklarbare beløpsposter">
  <img src="assets/screenshots/05-sporbart-grunnlag.png" width="30%" alt="Detaljert beregningsgrunnlag med timer, kilder og regler">
  <img src="assets/screenshots/06-kontroll.png" width="30%" alt="Kontroll av forhold som bør vurderes">
</p>

<p align="center">
  <img src="assets/screenshots/07-oppsummering.png" width="30%" alt="Oppsummering og dokumentasjon">
</p>
<!-- FERIETUR_SCREENSHOTS_END -->

## Typisk arbeidsflyt

1. **Opprett turen** – datoer, reiseforløp og grunnoppsett.
2. **Registrer arbeid og ansvar** – hva som faktisk skjer gjennom turen.
3. **Kontroller lønnsgrunnlaget** – blant annet mot nyere lønnsslipp.
4. **Se beregningen** – med synlige forutsetninger og regelgrunnlag.
5. **Lag PDF-dokumentasjon** – som grunnlag for videre kontroll og avklaring.

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

Siste publiserte produksjonsversjon er **Ferietur 0.5.5** (`versionCode 52`).

- [**Ferietur-0.5.5.apk**](https://github.com/brealorg/ferietur/releases/download/v0.5.5/Ferietur-0.5.5.apk)
- [Komplett Android-pakke](https://github.com/brealorg/ferietur/releases/download/v0.5.5/Ferietur-0.5.5-Android.zip)
- [Alle releases](https://github.com/brealorg/ferietur/releases)

**Krav:** Android 8.0 eller nyere (API 26+).

> Google Play-distribusjon er under klargjøring. Inntil videre er den signerte APK-en den publiserte distribusjonskanalen.

<details>
<summary><strong>Verifiser APK og signeringsidentitet</strong></summary>

### APK SHA-256

```text
999efa4bfea279fc0404e3043fc632f06b387de445b872741baa32df35d156a6
```

### Permanent signeringssertifikat SHA-256

```text
9bc0c2925d6bad3947cbcf6c237d6d085aebf5cb54f67170622ce02f8e8252e7
```

Checksum og sertifikatfingeravtrykk ligger også som egne filer i releasen.

</details>

## Release 0.5.5

`0.5.5` er en målrettet UX- og stabilitetsoppdatering basert på observasjoner fra førstegangsbrukere.

Blant endringene:

- enklere registrering i grunnturnus, inkludert komplette «Tidligere brukt»-vakter
- direkte trykk på tomme dager i arbeidsplanen
- tydeligere handlinger for perioder og PDF-dokumentasjon
- bedre kontrast og mer selvforklarende handlingsflater
- rettet fysisk touch-propagasjon i tom arbeidsplan
- oppdatert device-testharness

Produksjonskandidaten ble kvalifisert og deretter installert med samme permanente signeringssertifikat som `0.5.4`. Lagrede turer og utkast ble bevart gjennom oppgraderingen.

Se [release notes for 0.5.5](https://github.com/brealorg/ferietur/releases/tag/v0.5.5) for den publiserte releasepakken.

## Status

| Spor | Status |
|---|---|
| Signert Android APK | ✅ Publisert |
| Permanent update-signatur | ✅ Etablert |
| Lokal lagring / PDF | ✅ Produksjon |
| Google Play | 🚧 Under klargjøring |
| Kildekodepublisering | Ikke besluttet |

## Kildekode og lisens

Kildekoden er **ikke publisert som del av 0.5.5-utgivelsen**. Repositoryet har derfor foreløpig ingen åpen kildekode-lisens, og det gis ikke noen generell lisens til å kopiere, endre eller redistribuere prosjektets egen kildekode.

Kildekodepublisering og valg av programvarelisens behandles separat før eventuell kildekode legges ut.

## Tilbakemeldinger

Har du funnet en feil eller noe som er uklart, bruk [GitHub Issues](https://github.com/brealorg/ferietur/issues).

Når du rapporterer beregningsfeil, beskriv gjerne **hvilket oppsett du valgte, hvilke tidsperioder som ble registrert og hvilket resultat du forventet**. Ikke legg ved personopplysninger eller sensitive opplysninger fra faktiske brukere.
