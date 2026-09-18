# PLANBASIS01C — snapshot- og PDF-proveniens for planbasis

PLANBASIS01C lukker hullet der utkastet visste hvem som hadde fastsatt arbeidsplanen, men den
ferdigstilte beregningen mistet dette faktumet.

## Snapshot-format 7

Et ferdigstilt snapshot fryser nå:

- `workPlanBasis`
- `employerWorkPlanBlocks` når arbeidsgiver faktisk har fastsatt en egen plan
- eksisterende `holidayWorkPlanStatus`

Format 1–6 leses fortsatt. Eldre snapshots får `NOT_CLARIFIED` og tom arbeidsgiverplan. Ferietur
infererer ikke planbasis fra grunnturnus, registrert arbeid eller gammel planstatus.

## Ferdigstilling

`NORMAL_ROSTER_APPLIES` fryser grunnturnus som planbasis og tømmer ferieplanstatusen i snapshotet.

`EMPLOYER_SET_TRIP_PLAN` krever en ikke-tom, gyldig arbeidsgiverplan og avklart planstatus før
snapshotet kan bygges. Den faktiske planen fryses i snapshotet sammen med planbasis.

## Oppsummering

Normal-roster-sporet viser:

- **Planbasis — Vanlig grunnturnus gjelder**

Arbeidsgiverplan-sporet viser:

- **Planbasis — Arbeidsgiver har fastsatt egen plan**
- **Arbeidsgivers plan — <planstatus>**

Eldre ferdigstillinger viser eksplisitt at planbasis ikke ble lagret.

## PDF

Kort og full PDF viser eksplisitt planbasis. Full PDF dokumenterer også den frosne
arbeidsgiverplanen når den finnes, og skiller den fra «Arbeid på turen».

Ingen tariff- eller beløpsregler endres i denne slicen.

## C1 — PDF-proveniens og forklaring

Runtime-review av format-7-PDF-en avdekket to presentasjonsrester:

- forklaringen til arbeid utenfor grunnturnusen sa fortsatt at selve arbeidsplanen måtte avklares,
  selv om `NORMAL_ROSTER_APPLIES` allerede var fryst som planbasis;
- seksjonen «Grunnlaget som er brukt» listet ikke den nye planbasisen.

C1 gjør den detaljerte forklaringen snapshot-bevisst. Når format-7-snapshotet eksplisitt sier
`NORMAL_ROSTER_APPLIES`, beskriver PDF-en dette som den registrerte planbasisen. Juridisk
forsiktighet beholdes ved å presisere at eventuell gjennomsnittsberegning eller annen
arbeidstidsordning fortsatt må vurderes særskilt med arbeidsgiver.

«Grunnlaget som er brukt» inkluderer nå `Planbasis`. Ved arbeidsgiverfastsatt turplan inkluderer
seksjonen også `Arbeidsgivers planstatus`.

Eldre snapshots med `NOT_CLARIFIED` får fortsatt den konservative teksten. C1 endrer ingen
beregning, snapshot-modell, codec, formatversjon eller regelsett.
