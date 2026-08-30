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
