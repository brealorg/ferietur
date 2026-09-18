# FERIETUR01 domain contract — A4.2

The application keeps the following concepts separate.

1. **Employer relationship** — who is the employer for the trip.
2. **Payment scenario** — the payer/refunder selected for the proposal and PDF. This is documentation metadata: it does not change tariff arithmetic and is not a legal determination of liability.
3. **Roster comparison** — whether the normal roster is used as a comparison basis.
4. **Work plan** — what actually happened, including travel, active work, night watch, resting night watch and free time.
5. **Rule-based calculation** — what the implemented rules calculate from the work plan.
6. **Payment proposal** — the amount documented for the proposed settlement. It normally follows the calculated payment basis; an already agreed alternative amount can be documented without changing the calculation.

A payment scenario is not inferred to be the employer. A roster-comparison choice is not used to infer either employer or payment scenario. The app does not determine who is legally liable for the cost.

## Employer and rule basis

When `EmployerKind.OSLO_KOMMUNE` is selected, the explicit rule basis is Oslo kommune Dok. 25 2026–28, chapter 20.

When `EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED` is selected, the employer relationship and rule basis are not treated as finally clarified. The app may preserve and display a preliminary calculation for documentation, but must not present it as a confirmed total employer cost.

Legacy saved trips created before A3.9A contain no reliable employer/payer field. They are migrated without guessing: employer and payer become `UNSPECIFIED`, while the previous calculation choice is preserved only as roster-comparison mode.

## Roster comparison

`USE_NORMAL_ROSTER` compares trip work with the stored normal roster and allows the calculation engine to distinguish work inside and outside that roster.

`DO_NOT_USE_NORMAL_ROSTER` treats the trip work plan separately and does not use the normal roster as a comparison basis.

These choices do not establish who is the employer or who is legally liable for payment.

## Travel

`TRAVEL_WITH_RESPONSIBILITY` is a more precise description of active work while travelling with responsibility for the resident. It may overlap an active-work period in the editor. Those overlapping minutes are counted once in salary/work-time totals. Travel that extends beyond an active-work period contributes only the uncovered minutes to active work.

`TRAVEL_WITHOUT_RESPONSIBILITY` and `TRAVEL_UNCERTAIN` remain distinct because their pay treatment is not identical to active travel.

## Resting night watch

A resting night watch is work time minute-for-minute, while its base pay is calculated in the ratio 1:3. Thus a 9-hour resting watch corresponds to 3 hours of base-pay equivalent.

Night, Saturday/Sunday and holiday/high-day allowances for passive work follow the same 1:3 ratio under Dok. 25 point 8.9.

Active work during the resting watch is a separate component under Dok. 25 point 20.4. Actual active minutes are summed per resting watch and rounded to the nearest half hour: 14 minutes or less are discarded, 15 minutes or more round up to the next half hour. The rounded time is paid at hourly rate + 50%.

## Holiday/high-day calculation

Point 12.2.3 is modeled as explicit time windows. The windows differ between 33.6-hour schedules and 35.5/37.5-hour schedules plus point 8.2.2.

Movable Easter, Ascension and Pentecost dates are calculated from the calendar year. Ordinary service in an applicable window receives 1 1/3 hourly wage per worked hour as an additional supplement.

Saturday/Sunday allowance is removed from minutes that already receive point 12.2.3 holiday/high-day supplement, because point 12.2.2 excludes work that is compensated under another provision by more than 50 percent.

## Point-20.2 applicability — EQS01B1

The stored normal roster is a comparison and control source. It is no longer allowed to decide by itself which registered periods receive Dok. 25 point 20.2 compensation.

Oslo kommune EQS ID 53398 states that the work plan for a holiday stay may differ from the ordinary roster without the change itself creating overtime when the plan is changed with at least 14 days notice and otherwise satisfies the working-time rules. `HolidayWorkPlanStatus` therefore enters the production runtime explicitly.

EQS01B1 is deliberately fail-closed. The current work-period model cannot yet distinguish, period by period, work that belongs to the approved holiday work plan from work performed beyond that plan. For `TURNUS_PLUS_EXTERNAL` calculations with an explicit holiday-work-plan status, Ferietur therefore does **not** convert minutes outside the stored normal roster to hourly wage + 50 percent. Instead it emits unresolved rule `D25_20_2_WORK_PLAN_SCOPE` and keeps the uncertain amount outside the payment basis.

The nullable runtime parameter exists only as a migration/predecessor compatibility bridge while EQS01B is under development. The Android app runtime always supplies the explicit status. EQS01B2 must introduce the period-level duty/work-plan relation before point 20.2 can again be priced automatically.

The existing rule-priority behavior for minutes that are eventually proven to fall under point 20.2 remains conceptually separate: ordinary-service supplements must not be inferred or stacked merely because a period lies outside the old ground roster.

## Payment proposal

The rule-based **payment basis** and the **payment proposal** are separate concepts. The payment basis is the amount produced by the calculation engine. The payment proposal normally mirrors that amount. If the parties have already agreed another settlement amount, the app can document that amount and the reason for the difference without rewriting the work plan, rule-based calculation or open-rule status.

Employer relationship, payment scenario and payment proposal are independent data. The payment scenario is descriptive, does not change the calculated payment basis and does not determine legal liability.

## Work-time control

Work-time warnings remain separate from payment. A lower agreed payment amount does not make a work-time warning disappear and does not constitute approval of the work-time arrangement.

## Travel without supervision responsibility — A4.2

Dok. 25 point 20.3 refers to point 18.4 for travel time. Travel without active supervision responsibility is therefore kept separate from active work. In the normal-roster comparison model, ordinary travel outside the stored ground roster is paid at ordinary hourly rate. In the separate-trip model, registered ordinary travel is paid at ordinary hourly rate.

The point-18.4 notice fact is explicit input on each travel period: **known by previous day**, **not known by previous day**, or **not clarified**. Ordinary travel pay is not withheld while the notice fact is open. When short notice is confirmed, the app adds the overtime supplement for up to two actual hours of ordinary travel required outside ordinary working time. The app applies the two-hour cap once across the current trip so UI block splitting cannot multiply it, then applies point-13.3 rounding to each rate band.

Point 13.2 supplies the ordinary overtime rates: 50 percent during 07.00–20.00 and 100 percent during 20.00–07.00, on Sundays and public holidays, with the day-before rule applied after the end of ordinary work where the stored roster gives that evidence. An explicit `F1` maps to the weekly day off in point 13.7.1 and therefore receives 100 percent. `F2` is not promoted to that status without another tariff basis.

On dates listed in point 13.7.3, the app does not infer the employee-specific eligibility condition from the trip alone. It includes the confirmed ordinary overtime rate and exposes only a possible difference up to 133 1/3 percent as an open rule when that date is actually hit.

For travel without supervision responsibility that overlaps 23.00–07.00, the work plan also records whether the employee had permission to sleep. **Yes** uses point 20.3 passive-character treatment: worktime time-for-time, base pay 1:3 and applicable passive-work supplements 1:3. **No** uses ordinary travel treatment under point 18.4. **Not clarified** keeps the affected night travel outside the payment basis and exposes an unresolved sleep-permission rule. Passive night minutes do not also receive the point-18.4 short-notice overtime treatment.

## PILOT01-001 — passivtillegg som arbeidsfortolkning under avklaring

PILOT01-001 skiller mellom reelle uavklarte beregningshull og
arbeidsfortolkninger som Ferietur faktisk bruker i beregningen.

`D25_8_9_X20` har status `WORKING_INTERPRETATION`.

Ferietur bruker fortsatt 1:3-behandlingen av kapittel-12-tillegg ved arbeid av
passiv karakter som arbeidsfortolkning. Beløpene er med i kjent
betalingsgrunnlag og behandles derfor ikke som åpne beløpsposter.

Fortolkningen vises når en eller flere av disse linjene faktisk forekommer:

- `resting-evening-night`
- `resting-weekend`
- `resting-holiday`
- `travel-passive-evening-night`
- `travel-passive-weekend`
- `travel-passive-holiday`

Dette påvirker ikke `applicableUnresolvedRuleIds` eller snapshotets
`unresolvedRules`.

Kildebindingen dekker Dok. 25 punkt 8.9, 12.1.1, 12.2.2, 12.2.3, 20.3 og
20.4.

PILOT01-001 endrer ikke divisor, tilleggssatser, lønnstabell, tariffpakke,
rate-set, regelsettversjon eller pengeberegning.
## EQS01B2B1 — feriearbeidsplan per aktiv periode

Når arbeidsgiver er Oslo kommune og feriearbeidsplanen er registrert som godkjent og varslet
minst 14 dager før, bruker Ferietur den eksplisitte perioderelasjonen som klassifiseringsgrunnlag:

- `WITHIN_HOLIDAY_WORK_PLAN`: planlagt ordinær tjeneste. Grunnlønn legges ikke til på nytt.
  Relevante ordinære kapittel-12-tillegg kan inngå i betalingsgrunnlaget.
- `BEYOND_HOLIDAY_WORK_PLAN`: aktiv arbeidstid/reise med ansvar som behandles etter Dok. 25
  punkt 20.2 med timelønn + 50 prosent.
- `NOT_CLARIFIED`: punkt 20.2 holdes åpent og beløpet prises ikke positivt.

Den lagrede grunnturnusen er fortsatt tilgjengelig som kontroll- og sammenligningsinformasjon,
men brukes ikke som positiv punkt-20.2-klassifikator når den eksplisitte ferieplanmodellen er aktiv.

`NOT_APPROVED_OR_LATE` betyr fortsatt ikke automatisk overtid. Ferietur holder aktiv tid åpen i
den tilstanden.

B2B1 endrer ikke reise uten tilsynsansvar (`TravelDutyStatus`) eller hvilende nattevakt.
Disse områdene kvalifiseres separat i B2B2/B2C.
## EQS01B2B2 — vaktstatus under reise uten tilsynsansvar

For `TURNUS_PLUS_EXTERNAL` med eksplisitt feriearbeidsplanstatus brukes `TravelDutyStatus` som
faktagrunnlag for reise uten tilsynsansvar.

- `ON_DUTY`: ordinære reisedeler teller som arbeidstid. Ved godkjent feriearbeidsplan bruker
  perioden også `HolidayWorkPlanRelation`; innenfor planen er den planlagt tjeneste, mens utover
  planen kan behandles etter punkt 20.2.
- `OFF_DUTY`: ordinære reisedeler behandles etter punkt 18.4 med reisetidsbetaling og teller ikke
  som arbeidstid.
- `NOT_CLARIFIED`: ordinær reisetid prises ikke positivt; `D25_20_3_TRAVEL_DUTY_STATUS` holdes åpen.

Søvntillatelse kl. 23–07 er fortsatt et separat faktum. Reise med søvntillatelse behandles etter
punkt 20.3 som passiv arbeidstid og teller som arbeidstid time for time.

Grunnturnusen beholdes som sammenligningsinformasjon og legacy-fallback for historiske perioder,
men er ikke positiv vaktstatusklassifikator i den eksplisitte B2B2-modellen.

B2B2 endrer ikke hvilende nattevakt. Det området kvalifiseres i EQS01B2C.
## EQS01B2C — hvilende nattevakt under ferieopphold
Dok. 25 punkt 20.4 er en spesifikk ferieoppholdsregel for nattevakt mellom kl. 23:00 og 07:00.
Nattevakten skal normalt innrettes som arbeid av passiv karakter. Tiden regnes som arbeidstid
time for time, mens én time passiv vakt betales med 1/3 timelønn. Kapittel-12-tillegg på passivt
arbeid følger 1:3 etter punkt 8.9.

Når den eksplisitte feriearbeidsplanmodellen er aktiv, bruker Ferietur derfor den registrerte
`RESTING_NIGHT_WATCH` direkte som kilde til punkt 20.4. Den lagrede grunnturnusen avgjør ikke
om den registrerte ferie-nattevakten får 1:3-behandling.

`HolidayWorkPlanRelation` brukes ikke som et ekstra betalingsvilkår for hvilende natt. Punkt 20.4
regulerer nattevakten særskilt under oppholdet. Aktivt arbeid inne i den hvilende vakten følger
fortsatt den egne punkt-20.4-regelen med +50 prosent og avrunding per vakt.

En null `HolidayWorkPlanStatus` beholder den gamle grunnturnusavgrensningen kun for
legacy/predecessor-kompatibilitet. Appens produksjonsruntime sender eksplisitt status.
## EQS01C — ruleset 2026.4 og samlet kvalifisering

Ruleset `2026.4` markerer den samlede beregningssemantiske endringen fra EQS01A/B:

- feriearbeidsplanstatus er eksplisitt og kreves før Oslo-finalisering;
- aktiv tid klassifiseres per periode mot feriearbeidsplanen, ikke mot grunnturnusen;
- arbeid utover godkjent feriearbeidsplan behandles etter Dok. 25 punkt 20.2;
- ordinære kapittel-12-tillegg hentes fra planlagt tjeneste i feriearbeidsplanen;
- reise uten tilsynsansvar skiller eksplisitt mellom på vakt, ikke på vakt og uavklart;
- hvilende nattevakt under ferieopphold behandles direkte etter punkt 20.4 i den eksplisitte modellen;
- grunnturnusen beholdes som sammenlignings-/kontrollgrunnlag og legacy-fallback, ikke som
  positiv klassifiseringsfasit for de nye ferieplansemantikkene.

Persistensformatene for draft/snapshot er allerede kvalifisert i EQS01B2A. Ruleset-bumpen endrer
ikke snapshotformatet; nye finaliseringer fryser `2026.4`, mens historiske snapshots beholder den
ruleset-versjonen de ble opprettet med.
