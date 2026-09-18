# UPDATE01A1 — innebygd endringslogg og bekreftelse etter oppdatering

## Formål

Ferietur kan nå forklare vesentlige endringer i selve appen, i stedet for å være avhengig av
Play-beskrivelsen eller ekstern dokumentasjon.

## Innebygd endringslogg

`AppChangelog` inneholder versjonerte release notes med tre alvorlighetsnivåer:

- `NORMAL`
- `IMPORTANT`
- `CALCULATION_CHANGE`

Versjon 0.6.0 / build 54 markerer planbasis- og arbeidstidsendringene eksplisitt som
`CALCULATION_CHANGE`.

## Oppdateringsflyt

`AppInfoPreferences` lagrer `last_acknowledged_version_code`.

Når en eksisterende installasjon oppgraderes og det finnes release notes etter sist bekreftede
versjon, vises «Hva er nytt» én gang. Dialogen må bekreftes med «Forstått» før versjonskoden
lagres.

En fersk installasjon skal ikke presenteres som en oppdatering. Eksisterende installasjoner
gjenkjennes ved at den obligatoriske disclaimeren allerede er bekreftet; ferske installasjoner
seed-er gjeldende versjonskode uten oppdateringsdialog og viser den ordinære disclaimeren.

Hvis disclaimer-versjonen en gang senere økes samtidig som det finnes nye release notes, har
disclaimeren prioritet foran «Hva er nytt».

## Om Ferietur

«Om Ferietur» har en permanent «Hva er nytt»-rad som åpner release notes for gjeldende
versjonskode også etter at post-update-dialogen er bekreftet.

## Avgrensning

UPDATE01A1 endrer ikke tariffregler, beregningsformler, draft schema, snapshot-format eller
ruleset. Versjonsidentiteten løftes til 0.6.0 / build 54 slik at den eksisterende debug-installasjonen
kan kvalifiseres gjennom en ekte `adb install -r`-oppgradering fra 0.5.6-dev / build 53.

## A1R1 — Material 3-presentasjon

Første runtime-preview viste at en standard `AlertDialog` ble for høy og tett for fire
release-note-punkter; siste punkt ble visuelt avkuttet.

A1R1 beholder acknowledgement-modellen, men endrer presentasjonen:

- post-update bruker en egendefinert Material 3-dialog med begrenset høyde;
- innholdet scroller uavhengig av en fast footer med «Forstått»;
- alvorlighetsmerket ligger over tittelen, slik at tittelen får hele bredden;
- merkene er korte: `BEREGNING`, `VIKTIG`, `NYTT`;
- «Om Ferietur → Hva er nytt» åpner en egen fullskjermvisning med normal scrolling og
  egen tilbake-navigasjon i stedet for en dialog.

Ingen acknowledgement-, beregnings-, tariff- eller versjonssemantikk endres i A1R1.

## A1R2 — fullskjerm «Hva er nytt»

Runtime-review av R1 viste at release notes fortsatt ble opplevd som et dokument presset inn i
en modal flate. R2 fjerner dialogen helt.

Én gjenbrukbar `WhatsNewScreen` brukes i to moduser:

- `POST_UPDATE`: fullskjerm, system-Back konsumeres, én `LazyColumn` og fast bunnhandling
  «Forstått» som lagrer gjeldende `versionCode`;
- `ABOUT`: samme innhold og hierarki, men normal tilbake-navigasjon og ingen fast
  acknowledgement-handling.

Scaffoldets ordinære flow-bottom-bar og plan-FAB undertrykkes mens den obligatoriske
post-update-visningen er aktiv. Dette gjør visningen til én reell appflate også dersom appen
gjenopprettes fra en ikke-HOME-state.

Modal scrim, max-height-hack og dialog-scrolling er fjernet. Alvorlighetsmerkene `BEREGNING`,
`VIKTIG` og `NYTT` beholdes over hver tittel.

Acknowledgement-, fresh-install-, beregnings-, tariff-, schema-, snapshot- og ruleset-semantikk
er uendret.

