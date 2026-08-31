# TARIFFCORE01 — versjonert tariff- og lønnsgrunnlag

## A1: eksplisitt katalog og kildebinding

A1 introduserer en egen, effektivdatert tariffkatalog uten å endre dagens
beregningsformler eller beløp.

### Tariffpakke

`FerieturTariffs` registrerer Dok. 25 2026–28 som en eksplisitt pakke:

- id: `oslo-dok25-2026-2028`
- avtaleperiode: 01.05.2026–30.04.2028
- Ferietur-regelsett: `2026.3`
- kilde: Oslo kommunes offisielle jobb-/tariffside

En tariffpakke kan bare løses for datoer innen sin uttrykkelige avtaleperiode.
Det hindrer at dagens Dok. 25-identitet brukes implisitt etter 30.04.2028.

### Strukturert regelkildematrise

`FerieturRuleSources` binder hver eksisterende `DomainRule` til én strukturert
kilde. Dok. 25-regler får:

- tariffpakke-ID
- eksplisitte punktreferanser
- kildeetikett

`PAYMENT_PROPOSAL` og `AML_CONTROL` holdes bevisst utenfor tariffpakken.

A1 endrer ikke `DomainRule`-serialiseringen og krever derfor ingen endring i
`FinalizedTripSnapshotCodec`.

### Lønnstabell

`SalaryTableDescriptor` har nå:

- `effectiveFrom`
- `verifiedThrough`
- `tariffPackageId`
- kildeetikett og kilde-URL

Den innebygde tabellen fra 01.05.2026 er verifisert i Ferietur bare gjennom
30.04.2027. Dok. 25 punkt 1.5 krever forhandlinger om eventuell
lønnsregulering for andre avtaleår, og tariffavtalen kan tidligst utløpe
01.05.2027 dersom partene ikke blir enige. Ferietur skal derfor ikke anta at
2026-lønnstabellen fortsatt er riktig fra 01.05.2027.

Dette er en fail-closed endring for fremtidige turer: etter 30.04.2027 må en ny
verifisert lønnstabell legges til før beregning kan ferdigstilles.

### Ikke endret i A1

- ingen tariffbeløp er endret
- ingen beregningsformel er endret
- ingen `TripPlanEngine`-logikk er flyttet
- ingen PDF-layout er endret
- ingen snapshot-/draft-schema er endret
- ingen UI-layout er endret

## A1 checkpoint

A1 stoppet bevisst før snapshot-formatet og beregningskonstantene ble endret.

## A2: fryst tariffproveniens og versjonert satssett

A2 gjør tariffgrunnlaget til en del av selve beregningsidentiteten uten å endre
2026-beløpene.

### Snapshot-format v2

`FinalizedTripSnapshot` fryser nå i tillegg:

- `tariffPackageId`
- `tariffRateSetId`

Produksjonsflyten løser tariffpakken for turperioden, løser satssettet fra
pakken og krever at lønnstabellen tilhører samme tariffpakke før både
beregning og ferdigstilling. Arbeidstidskontrollen bruker det samme valgte
satssettet. Builderen validerer også at regelsett, tariffpakke og satssett hører
sammen.

`FinalizedTripSnapshotCodec` skriver format v2. Format v1 beholdes som eksplisitt
lesestøtte. For eksisterende v1-snapshots med regelsett `2026.3` og
`oslo-salary-2026-05-01` utledes den nå eksplisitte 2026–28-pakken og satssettet.
2026–28-pakken har sin egen uforanderlige regelsett-ID; migreringen er derfor
ikke koblet til hva en fremtidig appversjon kaller sitt aktive regelsett.
Andre ukjente v1-kombinasjoner merkes som legacy-ukjent i stedet for å få en
oppdiktet moderne identitet.

Draft-schemaet endres ikke: snapshot-payloaden er allerede selvversjonert.

### `TariffRateSet`

Beregningskonstanter som tidligere lå direkte i `TariffMath` og
`TripPlanEngine` er samlet i satssettet
`oslo-dok25-2026-2028-rates-2026.1`, blant annet:

- timeverksdivisorer
- 40 prosent kveld/natt
- lørdags-/søndagssatser og minstebeløp
- 1 1/3 helge-/høytidstillegg
- timelønn + 50 prosent etter kapittel 20
- 1:3 ved arbeid av passiv karakter
- 110 kroner døgngodtgjøring
- seks-timersgrensen for påbegynt døgn
- avrunding av aktiv tid under hvilende nattevakt
- to-timersgrense og halvtimesavrunding ved kortvarslet reise
- 50/100-prosentbånd og tilhørende klokkeslett
- kveld/natt- og nattreisevinduer

`TripPlanEngine.calculatePreliminary` tar nå et eksplisitt `TariffRateSet` med
dagens satssett som kompatibilitetsdefault. Produksjonsflyten sender inn
satssettet som er løst fra turens datoer, og `FinalizedTripSnapshotBuilder`
bruker alltid det satssettet som faktisk fryses i snapshoten. Tekstlige
regnestykker, forklaringer og evidensnotater henter også satser, forhold og
klokkeslett fra satssettet, slik at de ikke kan bli hengende igjen på gamle
2026-konstanter mens beløpet bruker nye verdier.

### PDF og historisk etterprøvbarhet

PDF-eksporten løser satssettet fra `tariffRateSetId` som ligger i den fryste
snapshoten. Helgesats og tariffavhengige forklaringer kommer dermed fra samme
satssett som beregningen, ikke fra appens til enhver tid aktive satssett. Fullt
grunnlag skriver også ut både tariffpakke-ID og satssett-ID sammen med
regelsett-, app- og lønnstabellmetadata.

### Kompatibilitetsmål

A2 skal gi identiske beregningsresultater for dagens 2026-satssett. Endringen
er en eierskaps- og proveniensendring, ikke en tariffendring. Eksisterende
`TariffMathTest`- og `TripPlanEngineTest`-scenarioer fungerer derfor som
regresjonsport, i tillegg til egne satssett- og v1-codec-tester.

### Fortsatt bevisst utsatt

A2 introduserer ikke flere samtidige effektivdaterte satssett inne i samme
Dok. 25-pakke og splitter ikke en tur over en satsgrense. Det kommer i neste
slice sammen med golden-scenarioer for tariff-/lønnstabellgrenser.

## Neste slice

A3 skal etablere golden-scenarioer og effektivdatert sats-/lønnstabelloppløsning
slik at en fremtidig tariff- eller lønnsgrense kan innføres uten å omskrive
historiske snapshots.

## A3: effektivdaterte underkataloger og golden-grenser

A3 gjør to ting samtidig: den gjør neste sats-/lønnstabelloppdatering til en
katalogendring, og den låser de mest feilutsatte randverdiene som eksplisitte
testkontrakter.

### Satssett har egen gyldighetsperiode

`TariffPackage` eier ikke lenger én enkelt `rateSetId`. Det var for rigid for en
avtaleperiode der satser kan bli revidert uten at selve Dok. 25-identiteten
endres. `TariffRateSet` har nå egne `effectiveFrom`/`effectiveTo`, og
`TariffRateSetCatalog` kan inneholde flere ikke-overlappende satssett i samme
tariffpakke.

For én beregning krever Ferietur fortsatt at ett og samme satssett dekker hele
turperioden. En tur som krysser en satsgrense stoppes fail-closed inntil motoren
har eksplisitt split-rate-støtte. Dette er bevisst bedre enn å bruke startdatoens
sats på hele turen.

### Lønnstabellkatalogen er gjort generell

`SalaryTableCatalog` og `SalaryTablePeriod` skiller katalogmekanismen fra
`OsloSalaryTables`. Neste offisielle lønnstabell kan dermed legges til som en ny
periode med egen descriptor og oppslagsfunksjon uten å omskrive kataloglogikken.

Som for satssett blandes ikke to lønnstabeller automatisk i samme beregning.
Kryssing av lønnstabellgrense stopper inntil split-rate-beregning er implementert.

### Én samlet resolver

`FerieturTariffResolver` løser tariffpakke, satssett og lønnstabell som én
konsistent `ResolvedTariffContext`. UI, kontrollberegning og ferdigstilling bruker
dermed samme beslutning og krever at lønnstabellen tilhører tariffpakken.

Dette fjerner en tidligere klasse av feil der tariffperioden kunne være støttet,
mens lønnstabellen var utløpt eller satssettet ble valgt via en separat kodevei.

### Golden-grenser

A3 låser blant annet:

- tariffens start- og sluttdato
- den verifiserte lønnstabellens start- og sluttdato
- at fremtidige sats- og lønnstabellgrenser ikke blandes over én tur
- 14/15 og 44/45 minutter ved aktivt arbeid under hvilende nattevakt
- at flere aktive hendelser summeres per hvilende vakt før avrunding
- at avrunding skjer per vakt, ikke over hele reisen
- 5:59 / 6:00 / 6:01 ved døgngodtgjøringens seks-timersgrense

### Nøyaktig seks timer er nå eksplisitt åpent

Punkt 20.6 beskriver resttid **over** seks timer som fullt døgn og resttid
**mindre enn** seks timer som uten godtgjøring. Ordlyden angir ikke nøyaktig seks
timer. Tidligere falt denne verdien stilltiende på siden uten ekstradøgn.

A3 beholder dette som kjent minimumsbeløp, men gjør nøyaktig seks timers resttid
til regel `D25_20_6_EXACT_THRESHOLD` med status `UNRESOLVED`. Et mulig ekstra
døgn vises separat som åpent beløp og legges ikke inn i kjent betalingsgrunnlag.
Dermed blir randverdien synlig uten at appen later som om tariffen har sagt mer
enn den faktisk gjør.

### Fortsatt bevisst utsatt

A3 beregner ikke én tur med to forskjellige lønnstabeller eller satssett. Neste
arkitekturslice kan implementere eksplisitt segmentering dersom dette er ønsket.
Historiske snapshots trenger ingen migrering: de beholder allerede fryst
`tariffRateSetId`, lønnstabell-ID og beregningsresultat.


## A4A1: hybrid segmenteringsplan uten beregningsaktivering

A4A1 etablerer beslutningsmotoren for turer som senere skal kunne krysse en
effektivdato uten å gjette. Den eksisterende produksjonsberegningen er fortsatt
strengt én tariffkontekst per tur; ingen synlig beregningsatferd endres i denne
slicen.

`TariffSegmentPlanner` bygger maksimale, sammenhengende segmenter der samme
tariffpakke, satssett og lønnstabell gjelder. En ren numerisk endring kan derfor
planlegges som flere segmenter så lenge alle segmentene bruker samme
`rulesetVersion`. Uavhengige grenser i satssett og lønnstabell kan ligge på
forskjellige datoer.

Dersom `rulesetVersion` skifter under turen, returneres
`SEMANTIC_RULESET_CHANGE` i stedet for en automatisk split. Dette er den
fail-closed skillet mellom:

- sats-/lønnstabellendring: kan segmenteres automatisk, og
- semantisk regelendring: må implementeres og valideres eksplisitt.

Manglende kildekning og inkonsistent binding mellom tariffpakke, satssett og
lønnstabell gir egne typede feil. `SalaryTableCatalog` kan nå også slå opp
årslønn for én konkret dato; dette er nødvendig når neste slice skal beregne
hvert segment med riktig lønnstabell.

### Ikke aktivert ennå

A4A1 endrer ikke `FinalizedTripSnapshot`, codec, PDF, UI eller
`TripPlanEngine.calculatePreliminary`. `FerieturTariffResolver.resolveRange`
beholder den gamle single-context-porten, mens `planSegments` er den nye
forberedende API-en. Neste slice kan dermed implementere selve
segmentberegningen uten å blande planleggingslogikk og beregningsrefaktor i ett
stort risikosteg.

## A4A2: tariffaktive tidsvinduer og klippede beregningsslicer

A4A2 aktiverer fortsatt ikke fler-satsberegning i produksjonen. Den gjør i
stedet neste nødvendige sikkerhetssteg: én planlagt effektivdatogrense kan nå
oversettes til eksakte, sammenhengende tidsvinduer med korrekt lønnstabell,
satssett, årslønn og timelønn for hvert vindu.

### Slutttid behandles som eksklusiv ved midnatt

Tariffsegmentering skal følge faktisk tid, ikke bare skjemaets inkluderende
sluttdato. `TariffEffectiveDateRange` behandler derfor turintervallet som
`[start, slutt)`. En tur som slutter nøyaktig 1. mai kl. 00:00 bruker ikke
1. mai-satsen bare fordi sluttdatoen heter 1. mai. Går turen til 00:01, er den
nye datoen derimot faktisk tatt i bruk og må ha gyldig tariff-/lønnskilde.

Dette låses med eksplisitte tester rundt 00:00/00:01.

### Arbeidsblokker klippes uten å miste proveniens

`TariffCalculationSliceBuilder` tar ferdig projiserte `WorkBlock`-intervaller
og klipper dem ved segmentgrensene. En nattevakt 23:00–07:00 over en
midnattsgrense blir dermed 23:00–00:00 og 00:00–07:00, men begge delene beholder:

- opprinnelig `sourceIndex`
- `TimeKind`
- reisevarselstatus
- eksakte tidsgrenser

Motoren konverterer ikke de klippede intervallene tilbake til `PlannedBlock`.
Det er bevisst, fordi en slik rundtur kunne mistet eierskap for aktive hendelser
under hvilende natt eller andre perioder som opprinnelig krysser midnatt.

Arbeidsperioder som allerede ligger utenfor selve turen avvises i stedet for å
bli stilltiende trimmet bort. Den eksisterende valideringen skal fortsatt gjøre
slike feil synlige for brukeren.

### Lønn og timelønn fryses per segment

Hver slice slår opp årslønn via den konkrete `salaryTable.id` som
segmentplanleggeren valgte, og beregner timelønn med akkurat segmentets
`TariffRateSet`. Dette betyr at en fremtidig lønnstabell og en separat
satsendring kan ligge på ulike datoer uten at startdatoens tall lekker over
hele reisen.

### Fortsatt ikke summert som ferdig lønnsberegning

A4A2 bygger sikre input-slicer, men kaller ikke dagens
`TripPlanEngine.calculatePreliminary` én gang per slice. Det ville vært feil for
regler som gjelder turen eller vakten samlet, blant annet:

- døgngodtgjøring for hele ferieoppholdets varighet
- to-timersgrensen for kortvarslet reise, som gjelder reisen samlet
- avrunding av aktivt arbeid per hvilende nattevakt dersom selve vakten krysser
  en effektivdatogrense

Neste slice skal derfor først trekke beregningskjernen ned på projiserte
`WorkBlock`-inputs og eksplisitt skille segmentlokale beløpsposter fra slike
globale/per-vakt-regler. Først deretter kan segmentresultater summeres uten
å doble rettigheter eller avrunding.

## A4A3: beregningskjerne for ferdig projiserte arbeidsintervaller

A4A3 er en ren strukturell refaktor. Den eksisterende
`TripPlanEngine.calculatePreliminary` projiserer fortsatt `PlannedBlock` til
`WorkBlock` på samme måte som før, men delegerer deretter til
`calculatePreliminaryFromProjectedBlocks`.

Den nye inngangen tar ferdig projiserte `WorkBlock`-intervaller direkte. Dette
er nødvendig for split-rate-arbeidet fordi A4A2 allerede har klippet
kryss-midnatt-intervaller ved effektivdatogrensen og bevart deres tidsart,
reisevarselstatus og kildeproveniens. De intervallene skal ikke konverteres
frem og tilbake via `PlannedBlock`, siden det kan endre eierskap eller dato for
aktive hendelser under hvilende nattevakt.

### Paritet er kontrakten

A4A3 endrer ikke beregningsregler. En representativ tur kjøres både gjennom den
opprinnelige planbaserte inngangen og den nye projiserte inngangen og skal gi
identisk `PreliminaryCalculation`. Egne tester låser også at:

- en aktiv hendelse kl. 02:00 under en kryss-midnatt hvilende vakt beholder sitt
  eksakte `LocalDateTime` og avrundes som før;
- et eksplisitt injisert satssett brukes i den projiserte kjernen;
- døgngodtgjøring fortsatt bruker den oppgitte hele turperioden og ikke
  automatisk blir gjort segmentlokal.

Det siste er bevisst. `calculatePreliminaryFromProjectedBlocks` er fremdeles en
**whole-trip-kjerne**. Den skal derfor ikke kalles én gang per tariffslice og
summeres ukritisk. Døgngodtgjøring, den felles kortvarselsgrensen og avrunding
av aktive hendelser per hvilende vakt må først løftes til eksplisitt koordinert
scope før produksjonsmessig fler-satsberegning kan aktiveres.

### Neste slice

A4A4 skal splitte beregningsansvaret i segmentlokale poster og koordinerte
whole-trip/per-vakt-poster. Først da kan A4A2-slicene beregnes med forskjellige
satssett/lønnstabeller og slås sammen uten dobbel godtgjøring eller dobbel
avrunding.

## A4A4: eksplisitt scope for hel-tur- og per-vakt-regler

A4A4 aktiverer fortsatt ikke summert fler-satsberegning. Den gjør i stedet
scope-eierskapet eksplisitt slik at neste monetære koordinator ikke kan behandle
alle beregningslinjer som om de var segmentlokale.

`TariffCalculationLineScopes` klassifiserer hver eksisterende beregningslinje som:

- `SEGMENT_LOCAL`: kan senere beregnes med slicens egen lønn og sats;
- `WHOLE_TRIP`: må koordineres én gang for hele reisen;
- `PER_RESTING_WATCH`: må koordineres én gang per hvilende nattevakt.

Kildekontrakten sammenligner de faktiske linje-ID-ene i beregningskjernen med
scope-katalogen. En ny beregningslinje kan dermed ikke introduseres uten at dens
scope samtidig klassifiseres.

### Døgngodtgjøring og kortvarselsgrense må være invariant

Døgngodtgjøringen beregnes for ferieoppholdet samlet. Dersom satsen per døgn
eller seks-timersgrensen endres midt i turen, stopper A4A4 fail-closed. Ferietur
har da ingen dokumentert regel for hvordan et helt døgn som krysser
virkningsdatoen skal prises.

Tilsvarende gjelder kortvarslet reise. To-timersgrensen og
avrundingssteget er scope for reisen samlet og må være de samme på alle slicer.
Selve overtidsprosenten kan senere prises lokalt etter at den globale tidsmengden
er fordelt kronologisk.

### Aktivt arbeid under hvilende natt

A4A4 rekonstruerer opprinnelige arbeidsintervaller fra A4A2s klippede
`sourceIndex`-fragmenter. En hvilende nattevakt kan krysse en effektivdatogrense
uten å være et problem i seg selv.

Dersom aktive hendelser under den samme vakten berører to forskjellige
pris-/avrundingskontekster, stopper motoren derimot med
`ACTIVE_EVENT_RATE_ALLOCATION_REQUIRED`. Punkt 20.4 krever avrunding per vakt;
appen skal ikke finne på hvordan den avrundede betalte tiden fordeles mellom to
ulike timelønner eller multiplikatorer.

Hvis hendelsene bare ligger i én kontekst, eller to slicer faktisk har identisk
timelønn, multiplikator og avrundingsregel, er scope-et entydig og kan gå videre.

### Fortsatt ikke produksjonsaktivert

A4A4 beregner ingen nye beløp, endrer ingen snapshot og brukes ikke av UI/PDF.
Neste slice kan bruke scope-planen til å koordinere hel-tur-reglene og deretter
slå sammen segmentlokale beløp uten dobbel døgngodtgjøring, dobbel
kortvarselsgrense eller dobbel avrunding per hvilende vakt.

## A4A5: første segmenterte monetære koordinator

A4A5 er første slice som faktisk kan beregne og summere beløp over en ren
sats-/lønnstabellgrense. Den er fortsatt en domenemotor og er ikke koblet til
UI, snapshot eller PDF ennå.

`TariffSegmentedMonetaryCoordinator` bygger resultatet fra tre disjunkte scope:

- `SEGMENT_LOCAL`: beregnes én gang per A4A2-slice med akkurat slicens
  lønnstabell, årslønn, timelønn og `TariffRateSet`;
- `WHOLE_TRIP`: døgngodtgjøring beregnes én gang for hele turen, etter at A4A4
  har bevist at sats og resttidsgrense er invariant;
- `PER_RESTING_WATCH`: aktivt arbeid under hvilende natt rekonstrueres fra
  original `sourceIndex` og avrundes én gang per opprinnelig vakt. A4A4 må på
  forhånd ha bevist at alle aktive hendelser på vakten har én entydig
  pris-/avrundingskontekst.

### Enkelt-slice-paritet

For en tur med bare én tariffkontekst delegerer koordinatoren direkte til den
etablerte projiserte beregningskjernen. Linjer, kjent beløp, betalingsgrunnlag,
åpne beløp og uavklarte regel-ID-er skal derfor være identiske med dagens
`PreliminaryCalculation`.

### Faktisk split-prising

Ved flere slicer kjøres bare `SEGMENT_LOCAL`-linjene per slice. Dermed kan for
eksempel én time aktivt arbeid før en virkningsdato prises med gammel timelønn
og én time etter datoen med ny timelønn, uten at døgngodtgjøring eller
nattavrunding dobles.

Linjer beholdes foreløpig separat per tariffkontekst i
`TariffScopedCalculationLine`, med `sliceIndex` eller `watchSourceIndex` som
proveniens. A4A5 forsøker ikke å slå to forskjellige satser sammen til én
presentasjonslinje.

### Kort/uavklart reisevarsel er fortsatt fail-closed ved split

Punkt 18.4 bruker en felles grense for turen, mens punkt 13.3 avrunder
overtidsgrunnlaget. Dersom en split-tur inneholder reise uten tilsynsansvar med
`NOT_KNOWN_BY_PREVIOUS_DAY` eller `NOT_CLARIFIED`, returnerer A4A5 derfor
`SPLIT_TRAVEL_NOTICE_COORDINATION_REQUIRED`.

Dette er bevisst konservativt. Ordinær reise som var kjent senest dagen i
forveien kan allerede prises segmentlokalt med forskjellige timelønner. En egen
senere slice må definere hvordan den felles kortvarselsmengden og avrundede
overtidsbånd fordeles når de faktisk berører flere priskontekster.

### Ikke runtime-aktivert ennå

A4A5 introduserer en reell segmentert monetær domeneberegning, men
`FerieturApp` bruker fortsatt dagens single-context runtime-path. Snapshot v2,
PDF og UI endres ikke i denne slicen. Neste steg kan først kvalifisere A4A5 og
deretter koble resolver → slice builder → monetær koordinator inn i en eksplisitt
runtime-orchestrator med bakoverkompatibel snapshot-proveniens for flere
kontekster.

## A4A6 — runtime calculation orchestrator

A4A6 introduces a runtime-ready domain gateway without wiring it into Compose,
PDF export, or finalized snapshots yet.

`FerieturTariffRuntimeCalculator.calculate(...)` now owns the complete branch:

1. validate the trip interval, chapter-20 scope, date coverage, outside-trip
   blocks, and unintended overlap;
2. resolve effective dates with the half-open `[tripStart, tripEnd)` policy;
3. build frozen calculation slices with the exact salary table and rate set;
4. preserve the existing `PreliminaryCalculation` path byte-for-behavior for a
   single tariff context;
5. delegate real cross-context money to the qualified A4A5 coordinator;
6. return typed failures instead of falling back to a guessed tariff context.

The runtime result deliberately does not flatten a multi-context trip into
`PreliminaryCalculation`. That legacy type contains one hourly rate and one
implicit tariff context. A4A6 instead exposes common money/line properties plus
an ordered `TariffRuntimeProvenanceSlice` list containing the exact tariff
package, ruleset, rate set, salary table, annual salary, and hourly rate used in
each effective-date segment.

This provenance list is the input contract for the next persistence slice. Until
snapshot schema v3 (or an equivalent backward-compatible representation) exists,
the Compose flow remains on the current single-context path and cannot silently
finalize a multi-context result with only one provenance identity.

## A4A7 — multi-context finalized snapshot provenance

A4A7 upgrades finalized snapshot persistence from format v2 to v3 so one
finished trip can freeze every tariff/salary context that actually contributed
to the calculation.

`FinalizedTripSnapshot` now stores an ordered `tariffContexts` list. Each entry
contains the occupied effective-date range together with:

- tariff package ID;
- ruleset version;
- tariff rate-set ID;
- salary-table ID and effective date;
- salary source label;
- frozen annual salary;
- frozen hourly rate.

The existing scalar tariff/salary fields remain as the primary/first context for
backward compatibility with the still-single-context UI/PDF surface. A snapshot
with multiple contexts is therefore identifiable explicitly through
`hasMultipleTariffContexts`; it must not be rendered through the old single-rate
presentation path until that surface is migrated.

### Codec v3 and legacy reads

New snapshots are encoded as format v3. The codec still accepts:

- v1 snapshots, where tariff package/rate-set identity is inferred only for the
  known 2026 ruleset + salary-table combination;
- v2 snapshots, where the one stored tariff package/rate-set identity is used
  directly.

Both legacy formats are upgraded in memory to one synthetic frozen tariff
context covering the effective half-open trip date range. Their already-stored
annual salary and hourly rate are reused; no newer tariff or salary data is
consulted during decode.

### Runtime bridge

`FinalizedTariffContextSnapshot.fromRuntime(...)` and
`FinalizedTariffContextSnapshots.fromRuntime(...)` provide a lossless bridge
from A4A6 runtime provenance to persistence provenance. This slice intentionally
does not wire the Compose calculation path yet and does not flatten a segmented
runtime result into `PreliminaryCalculation`.

The next slice must define the persisted calculation payload for a segmented
runtime result before Compose can finalize multi-context trips safely.

## A4A8 — persisted segmented calculation payload

A4A8 upgrades finalized snapshot persistence from format v3 to v4. The tariff
context list from A4A7 remains unchanged; the new part is that the calculation
payload itself can now represent either the legacy single-context calculation
or a genuinely segmented result.

`FinalizedCalculationPayload` has two modes:

- `PRELIMINARY`: stores the complete existing `PreliminaryCalculation` without
  changing any field or monetary semantics;
- `SEGMENTED_CONTEXTS`: stores the ordered scoped calculation lines from A4A5,
  including each line's `SEGMENT_LOCAL`, `WHOLE_TRIP`, or
  `PER_RESTING_WATCH` ownership plus `sliceIndex` / `watchSourceIndex`
  provenance and the exact unresolved-rule ID set.

The segmented payload derives known amount, payment basis, amount already
covered by normal roster, and excluded/open amount directly from its frozen
lines. It intentionally has `preliminaryOrNull = null`: Ferietur does not invent
one synthetic hourly rate for a result that actually used multiple tariff or
salary contexts.

### Current UI/PDF compatibility boundary

`FinalizedTripSnapshot.calculation` remains as a compatibility accessor for the
current single-context UI/PDF code. It returns the stored preliminary
calculation when one exists and fails closed for a segmented snapshot. Compose
runtime is still not allowed to finalize a segmented result until those
presentation surfaces are migrated to the common payload API.

### Codec v4 and legacy reads

New snapshots write format v4 and persist the tagged calculation payload.
The codec still reads v1, v2, and v3:

- v1/v2 keep synthesizing one frozen tariff context and wrap their existing
  calculation as `PRELIMINARY`;
- v3 keeps its explicit tariff-context list and wraps its existing
  `PreliminaryCalculation` as `PRELIMINARY`;
- v4 can round-trip either mode, including multiple scoped calculation lines
  without any fake single hourly rate.

`FinalizedCalculationPayload.fromRuntime(...)` is the lossless bridge from the
A4A6 runtime result. A single runtime result preserves its exact
`PreliminaryCalculation`; a segmented runtime result freezes A4A5's scoped line
entries and unresolved-rule set.

The outer `SavedTripDraft` schema remains version 6 because finalized snapshots
are still stored as an opaque codec payload. UI, PDF, calculation money, and the
runtime gateway are unchanged in this slice.


## A4A9 — finalization and presentation bridge

A4A9 migrates the **finalized** summary/PDF surface away from the legacy
assumption that every finished calculation has one `PreliminaryCalculation` and
one hourly rate. It still does not switch the live Compose calculation flow to
the segmented runtime gateway.

`FinalizedCalculationPresentation` is derived only from frozen snapshot data.
For a `PRELIMINARY` payload it exposes the already-stored lines, day audits,
roster metrics and monetary totals unchanged. For `SEGMENTED_CONTEXTS` it uses
the frozen scoped lines from snapshot v4, rebuilds day audits from their frozen
evidence, and reconstructs roster/control metrics from the snapshot's frozen
roster rows and work blocks. It never re-runs salary or tariff money logic.

### Per-line tariff provenance in presentation

Segment-local lines retain their `sliceIndex` and are presented with the
`TariffRateSet` frozen for that context. A PDF therefore cannot explain a line
calculated after an effective-date boundary using the primary/first period's
sats. Whole-trip or per-resting-watch lines only receive one context when that
context is unambiguous; otherwise the already-frozen line explanation is used
instead of inventing one rate set.

The full PDF lists every frozen tariff/lønn context for a segmented snapshot:
occupied period, salary source/table, annual salary, hourly rate, rate-set ID and
tariff-package ID. The single-context PDF ordering and content remain on the
qualified legacy path.

### Runtime-safe finalization contract

`FinalizedTripSnapshotBuilder.buildFromRuntime(...)` can now freeze an A4A6
runtime result without collapsing its provenance or calculation payload. It
revalidates the trip interval, projected work-block bounds/overlap, settlement
amount, context coverage and unresolved rules before building snapshot v4.

Control findings are still whole-trip presentation data. Finalization therefore
requires all runtime contexts to agree on the numeric time/control parameters
used by that copy (travel sleep window, passive-work divisor and short-notice
limit). If they do not, finalization fails closed until an explicit
multi-context control policy exists.

### Summary/PDF compatibility boundary

`FinalSummaryScreen` and `PdfExporter` now consume `snapshot.presentation`
rather than `snapshot.calculation`. A segmented finalized snapshot can therefore
be rendered without requesting a fake single hourly rate. The summary adds a
compact `Tariffgrunnlag — N perioder` status only when more than one frozen
context exists.

The live Compose calculation/finalization flow remains on the existing
single-context path in A4A9. A4A10 may wire the A4A6 runtime gateway into the
flow only after this presentation/finalization bridge has qualified.

## A4A10 — Compose runtime wiring

The live calculation flow now enters through `FerieturTariffRuntimeCalculator`
instead of requiring one tariff/salary context for the entire trip.

- the trip-date gate uses the half-open effective-date segment planner;
- single-context trips keep the qualified existing calculation path;
- supported pure rate/salary-table boundaries use the segmented monetary path;
- unsupported semantic or whole-trip allocation cases remain fail-closed;
- live calculation rows use stable presentation keys so the same line ID can
  occur in multiple tariff slices without LazyColumn key collisions;
- the live rules sheet shows every effective salary/hourly-rate context rather
  than inventing one blended hourly rate;
- control/finalization requires one coherent shared worktime-control parameter
  set across contexts;
- finalization now calls `FinalizedTripSnapshotBuilder.buildFromRuntime`, so the
  v4 calculation payload and all tariff contexts are frozen from the exact live
  runtime result.

`TariffRuntimeCalculationPresentations` supplies the common live UI surface for
single and segmented calculations. `TripPlanEngine.buildWorktimeAudit` provides
non-monetary roster/worktime metrics for a segmented live result without
re-running money through one arbitrary tariff rate set.


## A4A10R1 – segmented PDF presentation hardening

Device qualification of Scenario B exposed two presentation-only defects while
the segmented monetary calculation itself remained correct.

Segment-local PDF line titles now derive their date label from frozen
calculation evidence rather than from the tariff/lønn context period.
Tariff/lønn-perioden remains separate provenance under
`Tariff- og lønnskontekster`.

The full-PDF `Opprettet / beregning-ID` traceability metadata is rendered in a
fixed footer on the final page, outside the flowing content area, so it cannot
create a metadata-only trailing page.

Short-PDF ordering, calculation formulas, runtime segmentation, snapshot v4 and
Compose calculation flow are unchanged.

## A5A1 – tariff update manifest and activation contract

A5 introduces a control-plane lifecycle for future tariff and salary updates.
The first slice deliberately does not change runtime calculation catalogs.

A tariff update is not synonymous with a new tariff package. For example, the
Dok. 25 2026–28 agreement package remains effective beyond the currently
verified Oslo salary table window. A later salary table may therefore target
the existing tariff package while adding only a new immutable salary-table
component.

`TariffUpdateManifest` records:

- stable update identity;
- target tariff-package identity and ruleset version;
- effective window;
- immutable component IDs affected by the update;
- exact source-document identity, publication date and SHA-256 fingerprint;
- explicit source-to-component bindings;
- verification state;
- explicit `CANDIDATE` or `ACTIVE` lifecycle state.

An ACTIVE manifest is invalid unless every component has explicit source
provenance, source fingerprints are canonical SHA-256 values and verification
confirms that the source hashes were checked.

`TariffUpdateManifestCatalog` provides an explicit activation boundary:
candidate manifests remain visible to the update workflow but cannot be
returned through active lookup.

A5A1 is intentionally not wired into `FerieturTariffResolver`,
`FerieturTariffs`, `FerieturTariffRates` or `OsloSalaryTables`. Runtime
activation and component-to-catalog coherence are separate later gates.

No calculation formula, tariff value, salary value, snapshot format, PDF or UI
behavior changes in A5A1.

## A5A2 – component coherence validation

A5A2 adds a second fail-closed layer to the tariff-update control plane.

A structurally valid manifest is not sufficient for activation. Every referenced
immutable component must also exist in a known component registry, belong to
the manifest's target tariff package and cover the manifest's effective window.

The update component registry models tariff packages, rulesets, rate sets,
salary tables and rule-source bindings as typed immutable identities.

The current runtime catalogs are mirrored read-only into
`FerieturTariffUpdateComponents.currentRuntime`. This mirror does not alter
`FerieturTariffResolver` or make candidate components available to calculation.

The coherence validator rejects:

- unknown target tariff packages;
- ruleset-version mismatch;
- manifest periods outside the target agreement;
- unknown component IDs;
- components belonging to another tariff package;
- components that do not cover the full manifest window.

The registry itself also refuses components whose effective period lies outside
their owning tariff package.

This preserves the distinction between "known to the update workflow" and
"available to runtime".

## A5A3 – qualified activation gate

A5A3 combines the two independent update-control layers into one final
qualification decision.

A manifest is qualified for a later runtime-registration step only when:

1. it is explicitly marked `ACTIVE`;
2. A5A1 structural, provenance and source-hash validation passes;
3. A5A2 component identity, package ownership and effective-window coherence
   passes.

The result is typed as either `Qualified` or `Rejected`. Rejected results retain
the exact A5A1/A5A2 failure categories.

`Qualified` deliberately means only "eligible for later runtime registration".
A5A3 does not mutate `FerieturTariffs`, `FerieturTariffRates`,
`OsloSalaryTables`, `FerieturTariffResolver` or the calculation runtime.

This preserves a hard boundary between approval of update material and actual
availability to live calculations.

## A5A4 – runtime registration dry-run

A5A4 adds an explicit add-only dry-run between A5A3 qualification and any
future change to live runtime catalogs.

A qualified component is classified as either `ADD` or `ALREADY_PRESENT`.
Reusing an existing immutable kind/ID with different metadata is rejected.

The projected post-registration component set is validated before mutation for:

- tariff-package overlap and gaps;
- rate-set overlap and gaps inside a tariff package;
- salary-table overlap and gaps inside a tariff package;
- incomplete registration of a completely new tariff package.

A new tariff package must arrive together with package, ruleset, rate set,
salary table and tariff rule-source bindings.

A contiguous salary-only update inside an existing package is explicitly
supported. This is the expected update shape for a future salary table during
the existing Dok. 25 2026–28 agreement period.

A5A4 still performs no live runtime mutation.

## A5A5 – isolated typed runtime catalog snapshot

A5A5 materializes an approved A5A4 dry-run into a fully typed runtime catalog
snapshot without connecting it to the live resolver.

The update workflow now distinguishes immutable metadata from the actual typed
runtime payload:

- `TariffPackage`;
- ruleset/package payload;
- `TariffRateSet`;
- `SalaryTablePeriod`;
- tariff `RuleSourceBinding` payloads.

Every `ADD` action in the A5A4 plan must have exactly one matching typed
payload. The typed payload must regenerate the exact immutable component
metadata approved by the dry-run. Missing, duplicate, unplanned or mismatching
payloads fail closed.

The projected typed component set must also match A5A4
`projectedComponents` exactly before a snapshot can be constructed.

The snapshot exposes isolated date resolution for tariff package, rate set,
salary table and annual salary.

A synthetic boundary test proves:

- 30 April 2027 resolves the existing salary table and annual salary 614600;
- 1 May 2027 resolves a synthetic next salary table and annual salary 624600;
- both dates remain in the same Dok. 25 package and rate set;
- the global `OsloSalaryTables` catalog remains unchanged and still does not
  support 1 May 2027.

A5A5 therefore validates future runtime behavior without activating future data
in the application.
