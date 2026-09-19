# Gjennomregnede eksempler

Disse eksemplene viser for hånd hvordan Ferietur kommer fram til enkeltbeløp. Hvert eksempel er
også en automatisk test i kildekoden, slik at tallene her ikke kan skli fra hverandre uten at
bygget feiler. Du trenger ikke kunne lese kode for å kontrollere dem – bare en kalkulator og
Dokument 25.

Eksemplene gjelder **tariffpakken Dok. 25 for 2026–2028** med lønnstabellen fra 1. mai 2026.
De viser enkeltposter, ikke en hel tur. Finner du en feil i regnestykkene eller i tolkningen,
meld den som [issue](https://github.com/brealorg/ferietur/issues).

## Felles forutsetninger

| Størrelse | Verdi | Kommentar |
| --- | --- | --- |
| Lønnstrinn | 32 | Årslønn 614 600 kr i lønnstabellen fra 01.05.2026 |
| Arbeidsuke | 35,5 t | Timeverksdivisor 1846 |
| Timelønn | 614 600 / 1846 = 332,936… → **332,94 kr** | Avrundes til hele øre |
| Kveld-/nattillegg | 40 % av timelønn = 133,176 → **133,18 kr/t** | Kl. 17–06; for nattevakt til vakten slutter, senest kl. 08 |
| Lørdags-/søndagstillegg | 23 % av timelønn = 76,576… → **76,58 kr/t** | Minst 73 kr/t; prosentsatsen er høyest her |
| Hvilende nattevakt | 1/3 | Arbeidstid time for time, betaling for en tredel |
| Aktivt arbeid under hvilende vakt | timelønn + 50 % | Summeres per vakt og avrundes til hele halvtimer |
| Døgngodtgjøring | 110 kr per døgn | Påbegynt døgn teller når resttiden er over 6 timer |

Avrunding: Satser avrundes til hele øre (vanlig avrunding, 5 rundes opp). Beløp regnes med
full presisjon underveis og avrundes til hele øre til slutt.

## Eksempel 1 – Hvilende nattevakt, mandag kl. 22.00–07.00

Dok. 25 punkt 20.4. Test: `TripPlanEngineTest.nineHourRestingNightShowsThreeHourPayEquivalent`.

1. Vakten varer 9 timer. Alle 9 timer teller som arbeidstid.
2. Lønnsekvivalent: 9 t / 3 = 3 t.
3. Beløp: 3 t × 332,94 kr = **998,82 kr**.

## Eksempel 2 – Kveld- og nattillegg på den samme vakten

Dok. 25 punkt 8.9, 12.1.1 og 20.4. Test: `TripPlanEngineTest.restingNightGetsNightAllowanceAtOneThird`.

1. Tillegget gjelder fra kl. 17. For nattevakt løper det til vakten slutter (senest kl. 08),
   så hele vakten kl. 22.00–07.00 gir tillegg: 9 timer.
2. Hvilende vakt betales med en tredel: 9 t / 3 = 3 t.
3. Beløp: 3 t × 133,18 kr = **399,54 kr**.

Eksempel 1 og 2 gjelder samme vakt og legges sammen: 998,82 + 399,54 = 1 398,36 kr.

## Eksempel 3 – Helgetillegg, hvilende nattevakt lørdag kl. 22.00 til søndag kl. 07.00

Dok. 25 punkt 8.9, 12.2.2 og 20.4. Test: `TripPlanEngineTest.restingWeekendAllowanceAlsoUsesOneThird`.

1. Hele vakten ligger mellom lørdag kl. 00 og søndag kl. 24: 9 timer.
2. Hvilende vakt betales med en tredel: 9 t / 3 = 3 t.
3. Sats: 23 % av 332,94 = 76,58 kr/t. Minstesatsen på 73 kr/t er lavere og brukes ikke.
4. Beløp: 3 t × 76,58 kr = **229,74 kr**.

## Eksempel 4 – Tre korte utrykninger under én hvilende nattevakt

Dok. 25 punkt 20.4. Test: `TariffGoldenBoundaryTest.threeTenMinuteEventsOnSameRestingWatchAreSummedBeforeRounding`.

Hvilende vakt mandag kl. 23.00–07.00 med aktivt arbeid kl. 01.00–01.10, 02.00–02.10 og 03.00–03.10.

1. Aktivt arbeid summeres per vakt før avrunding: 10 + 10 + 10 = 30 minutter.
2. Avrunding til hele halvtimer: under 15 minutter rundes ned, 15 minutter eller mer rundes opp.
   30 minutter blir 30 minutter. (Hver utrykning for seg ville blitt rundet ned til 0.)
3. Sats: timelønn + 50 % = 332,94 × 1,5 = 499,41 kr/t.
4. Beløp: 0,5 t × 499,41 kr = 249,705 → **249,71 kr**.

Test for selve avrundingsgrensene: `TariffGoldenBoundaryTest.activeNightQuarterHourBoundariesRemainExactGoldenContract`
(14 min → 0, 15 min → 30, 44 min → 30, 45 min → 60).

## Eksempel 5 – Døgngodtgjøring rundt 6-timersgrensen

Dok. 25 punkt 20.6. Test:
`TariffGoldenBoundaryTest.stayAllowanceBoundaryFiveFiftyNineExactSixAndSixOhOneIsFailClosedAtExactWordingGap`.

Turen starter mandag 10. august 2026 kl. 07.00.

| Turen slutter | Varighet | Døgn | Beløp |
| --- | --- | --- | --- |
| tirsdag kl. 12.59 | 1 døgn + 5 t 59 min | 1 | 110,00 kr |
| tirsdag kl. 13.00 | 1 døgn + nøyaktig 6 t | 1, og et **åpent punkt** på 110,00 kr | 110,00 kr bekreftet |
| tirsdag kl. 13.01 | 1 døgn + 6 t 1 min | 2 | 220,00 kr |

Ordlyden sier «mer enn» seks timer og regulerer ikke nøyaktig seks timer. Ferietur gjetter ikke:
det ekstra døgnet vises som et åpent punkt som må avklares, og holdes utenfor den bekreftede summen.

## Slik kontrollerer du et eksempel mot appen

Legg inn en tur med samme datoer og klokkeslett i Ferietur, velg lønnstrinn 32 og 35,5 timers
uke, og åpne «Detaljert beregningsgrunnlag». Beløpsposten skal vise samme timer, sats og beløp
som her.
