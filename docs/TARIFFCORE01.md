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
