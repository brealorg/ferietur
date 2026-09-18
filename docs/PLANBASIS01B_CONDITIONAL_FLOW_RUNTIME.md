# PLANBASIS01B — betinget arbeidsplanflyt

PLANBASIS01A innførte det eksplisitte faktumet `TripWorkPlanBasis`. PLANBASIS01B gjør dette
synlig og avgjør hvilken beregningsbaseline appen faktisk bruker.

For Oslo kommune med «Vanlig turnus beholdes» spør Ferietur først:

**Hvilken arbeidsplan har arbeidsgiver fastsatt?**

## Vanlig grunnturnus gjelder

Velges når arbeidsgiver ikke har fastsatt en egen arbeidsplan for turen. En arbeidsfordeling de
ansatte lager seg imellom på ferieoppholdet blir ikke automatisk en arbeidsgiverplan.

Flyten blir:

1. Grunnturnus
2. Reise
3. Arbeid på turen
4. Beregning

Runtime bruker den etablerte grunnturnussammenligningen for punkt 20.2. Det separate
`holidayPlans`-datasettet ignoreres som tariffbaseline.

## Arbeidsgiver har fastsatt egen plan

Velges bare når arbeidsgiver faktisk har fastsatt/godkjent en arbeidsplan for turen.

Flyten blir:

1. Grunnturnus
2. Arbeidsgivers arbeidsplan
3. Reise
4. Arbeid på turen
5. Beregning

UX02-autodiffen beholdes og sammenligner arbeidsgivers plan mot registrert arbeid på turen.

## Ikke avklart

Nye og migrerte turer får ingen baseline ved gjetning. Normal-turnussporet kan ikke forlates fra
metodesiden før bruker har valgt om arbeidsgiver faktisk fastsatte en egen plan.

Schema 10, ruleset 2026.4 og snapshot-format 6 endres ikke i denne preview-slicen. Snapshot/PDF
må fortsatt få eksplisitt planbasis-proveniens før landing.
