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

