# PILOT01 — real-world validation

## PILOT01-001 — passivtillegg og kildesporing for aktiv reise

Dato: 2026-08-31

PILOT01-001 bygger på Solgården-casen og korrigerer to
presentasjons-/sporbarhetsforhold uten å endre pengeberegningen.

### Arbeidsfortolkning

Ferietur bruker dagens 1:3-behandling av tillegg etter kapittel 12 ved arbeid
av passiv karakter som en arbeidsfortolkning.

Dette er ikke det samme som en `UNRESOLVED` beregningsregel:

- beløpet beregnes;
- beløpet inngår i betalingsgrunnlaget;
- dokumentgrunnlaget kan ferdigstilles;
- fortolkningen presenteres særskilt som fortsatt til avklaring.

Regel-ID `D25_8_9_X20` har derfor status
`WORKING_INTERPRETATION`.

### Aktiv reise

Når posten «Aktivt arbeid og reise med ansvar» faktisk inneholder reise med
ansvar, viser kilden også Dok. 25 punkt 20.3.

### Solgården monetary non-regression

- totalt betalingsgrunnlag: `51 307,42 kr`
- aktivt arbeid + reise med ansvar: `37 955,16 kr`
- hvilende nattevakt: `3 551,36 kr`
- kveld/natt på hvilende: `1 420,59 kr`
- helg på hvilende: `408,43 kr`
- ordinært kveld/natt: `4 794,48 kr`
- ordinær helg: `2 297,40 kr`
- døgngodtgjøring: `880,00 kr`

Ingen av disse beløpene endres av PILOT01-001.

Snapshot-format og codec endres ikke.

## PILOT01-002 — lønnstabelltekst i PDF

Device-verifikasjonen av PILOT01-001 avdekket at snapshotets
`salaryTableSourceLabel` allerede inneholder både «Lønnstabell» og en
maskinlesbar gyldighetsdato. PDF-presentasjonen la deretter til samme
informasjon på nytt.

PDF-en bruker nå bare tabellnavnet fra kildeetiketten og presenterer den
frosne effective-datoen separat med dokumentets vanlige norske
datoformatering.

For Oslo-tabellen blir resultatet:

`Lønnstabell: Oslo kommune · fra 1. mai 2026`

Dette er kun en presentasjonsendring. Lønnstabell-ID, årslønn,
gyldighetsperiode og beregning endres ikke.

## PILOT01-003 — visuell arbeidstidskontroll i PDF

Solgården-piloten viste at den eksisterende arbeidstidskontrollen var
matematisk forståelig, men presentasjonen kunne gi inntrykk av at en
23- eller 24-timers sammenhengende arbeidsperiode var likebetydende med
23 eller 24 timer aktivt arbeid.

PDF-en presenterer derfor lange sammenhengende arbeidsperioder som visuelle
kort når den fryste arbeidsplanen kan projiseres uten semantisk tap.

Kortene viser:

- samlet sammenhengende arbeidstid;
- start og slutt;
- en proporsjonal tidslinje;
- registrerte tidstyper og varighet for hver del;
- særskilt markering av hvilende nattevakt, aktivt arbeid og reise med ansvar.

For Solgården viser den siste perioden eksplisitt:

- 8 t hvilende nattevakt;
- 9 t aktivt arbeid;
- 7 t reise med ansvar;
- totalt 24 t sammenhengende arbeidstid.

Arbeidstidskontrollens regler, terskler og antall funn endres ikke.
Hvis PDF-presentasjonen ikke kan representere en spesiell
reise-/turnuskombinasjon sikkert, brukes den eksisterende tekstpresentasjonen
i stedet.

PILOT01-003 er dermed en dokument-/presentasjonsendring, ikke en endring i
beregning, tariff eller arbeidstidskontroll.

### PILOT01-003R1 — kompakt presentasjon av enkelttypeperioder

Pilotrenderingen viste at lange perioder som består av bare én registrert
tidstype ikke trenger samme visuelle plass som perioder som kombinerer flere
tidstyper.

Enkelttypeperioder presenteres derfor som kompakte rader med varighet,
tidsrom og tidstype. Sammensatte perioder beholder de store tidslinjekortene.

For Solgården betyr dette at de tre 15-timersperiodene med bare aktivt arbeid
vises kompakt, mens de tre 23-timersperiodene og den avsluttende
24-timersperioden fortsatt visualiseres med tidslinje og sammensetning.

Endringen påvirker kun PDF-presentasjonen. Arbeidstidskontroll, antall funn,
beregningslogikk, tariff og snapshotformat er uendret.

### PILOT01-003R2 — siste PDF-polering

Den endelige pilotrenderingen bruker mer direkte språk i
arbeidstidsseksjonen. Forklaringen sier nå at når en arbeidsperiode består av
flere registrerte tidstyper, vises de hver for seg.

Dato- og tidslinjen på de visuelle kortene bruker tydelige norske
forkortelser, punktum og pil, for eksempel:

`fre. 14. aug. 23:00 → lør. 15. aug. 22:00`

Dette er kun tekst- og presentasjonspolering. Beregning,
arbeidstidskontroll, tariff, antall funn og snapshotformat er uendret.

## PILOT01-004 — korrekt sju-dagersvindu for arbeidstid

Solgården-piloten avdekket at arbeidstidskontrollen sammenlignet samlet
arbeidstid i hele den viste turperioden direkte med 48 timer. Det gir feil
tidssemantikk når turen varer mer enn sju dager.

Arbeidstidskontrollen bruker nå maksimalt registrert arbeidstid innenfor et
sammenhengende 168-timersvindu.

Kontrollen er fortsatt et varsel og avgjør ikke om en konkret
arbeidstidsordning er lovlig. Teksten gjør eksplisitt oppmerksom på at
48-timersgrensen kan være underlagt gjennomsnittsberegning over åtte uker,
og at arbeidstid utenfor den registrerte turen må tas med i den samlede
vurderingen.

I Solgården-eksempelet er:

- samlet registrert arbeidstid i hele turen: 146 timer;
- høyeste registrerte arbeidstid i et sju-dagersvindu: 138 timer;
- antall REVIEW/CRITICAL-funn fortsatt 14.

PILOT01-004 endrer ikke lønnsberegning, tariff, betalingsgrunnlag,
snapshotformat eller reglene for kort hvile og lange arbeidsperioder.

