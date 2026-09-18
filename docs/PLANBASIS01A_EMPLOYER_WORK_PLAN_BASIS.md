# PLANBASIS01A — hvem fastsatte arbeidsplanen?

## Modellfeilen som korrigeres

UX02 gjorde en separat feriearbeidsplan teknisk mulig, men UI/runtime forutsatte i praksis at en
slik plan fantes for turer med vanlig grunnturnus. Det er ikke et trygt premiss. En praktisk
arbeidsfordeling som ansatte lager under reisen er ikke automatisk det samme som en arbeidsplan
arbeidsgiver har fastsatt/godkjent.

PLANBASIS01 gjør derfor eksistensen av **arbeidsgivers plan** til et eksplisitt faktum.

## `TripWorkPlanBasis`

- `NORMAL_ROSTER_APPLIES`: arbeidsgiver har ikke fastsatt en egen plan for turen; grunnturnusen er
  beregningsbaseline.
- `EMPLOYER_SET_TRIP_PLAN`: arbeidsgiver har fastsatt en egen arbeidsplan for turen; denne kan
  brukes som baseline når status/varsling behandles etter eksisterende regler.
- `NOT_CLARIFIED`: appen skal ikke gjette.

Dette er et annet spørsmål enn `HolidayWorkPlanStatus`. Statusfeltet beskriver godkjenning/varsling
**når en egen arbeidsgiverplan faktisk finnes**.

## Runtime

For `NORMAL_ROSTER_APPLIES` aktiveres den etablerte grunnturnus-sammenligningen. Arbeid utenfor
grunnturnusen kan da behandles etter den eksisterende punkt-20.2-modellen.

For `EMPLOYER_SET_TRIP_PLAN` beholdes UX02-autodiffen mellom arbeidsgivers registrerte plan og
registrert arbeid på turen.

For `NOT_CLARIFIED` beholdes fail-closed: appen lager ikke et betalingsbeløp ved å gjette hvilken
plan som gjelder.

## Migrering

Draft schema 10 lagrer `workPlanBasis`. Drafts fra schema 9 og eldre settes til `NOT_CLARIFIED`
med `V10_EMPLOYER_WORK_PLAN_BASIS_REVIEW_REQUIRED`. Selv eksisterende `holidayPlans` brukes ikke
til å inferere at arbeidsgiver faktisk fastsatte planen, fordi UX02B kunne ha fått brukeren til å
registrere en praktisk plan under det tidligere premisset.

## Neste slice

PLANBASIS01B kobler faktumet inn i UI og flyt:

- spør eksplisitt hvilken plan arbeidsgiver faktisk har fastsatt;
- viser feriearbeidsplansteget bare når `EMPLOYER_SET_TRIP_PLAN` er valgt;
- lar `NORMAL_ROSTER_APPLIES` gå direkte fra grunnturnus til arbeid på turen;
- sender migrerte turer til avklaring i stedet for å gjette;
- erstatter det misvisende navnet **Faktisk arbeid** med en enklere brukerflate for arbeid på turen.
