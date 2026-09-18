# UX02B — feriearbeidsplan, faktisk arbeid og automatisk runtime-differanse

UX02A skilte datamodellen i `holidayPlans` og `plans`. UX02B gjør dette synlig i appflyten.

For Oslo/normal-turnus-modellen er rekkefølgen nå:

1. Grunnturnus
2. Feriearbeidsplan
3. Faktisk arbeid
4. Beregning

Grunnturnusen kan kopieres til feriearbeidsplanen som et redigerbart utgangspunkt, men er ikke
punkt-20.2-klassifikator. Når brukeren går videre fra feriearbeidsplanen og faktisk arbeid ennå
ikke er registrert, brukes feriearbeidsplanen som startpunkt for faktisk arbeid. Brukeren endrer
da bare det som faktisk ble annerledes.

`HolidayWorkPlanRelation` er fjernet fra sluttbrukereditoren. Runtime bruker
`runtimePlansForHolidayWorkPlanComparison()` og avleder relasjonen fra feriearbeidsplan mot
faktisk arbeid før tariffberegning, kontroll, presentasjon og finalisering.

Pre-v9-drafts kan ha faktisk arbeid, men ingen separat feriearbeidsplan. De sendes derfor til
Feriearbeidsplan-steget når de gjenopptas etter dette punktet. Ingen plan gjettes fra faktisk arbeid.

UX01A-turnusmalfiksen og UX01D lokal sikkerhetskopi beholdes.

## Preview-begrensning

Snapshot-format 6 fryser de avledede runtime-periodene og beregningsresultatet, men lagrer ennå
ikke den separate feriearbeidsplanen som eget audit-datasett. Før landing/release må UX02C ta
stilling til snapshot-/PDF-proveniens for dette nye faktagrunnlaget.
