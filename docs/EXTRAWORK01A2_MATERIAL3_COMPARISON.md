# EXTRAWORK01A2 — Material 3-sammenligning av avtalt tid og ekstra arbeid

A2 er en ren presentasjonsslice over den kvalifiserte A1-modellen. Ingen beregningsregler,
planbasis, persistence, snapshot-format eller ruleset endres.

## Skjermnivå

«Arbeid på turen» viser én rolig legendeflate under introduksjonen:

- **Avtalt tid** — primærfarge.
- **Ekstra arbeid** — error/coral-accent.

Forklaringen gjentas ikke på hvert dagskort.

## Dagskort

Hvert kort beholder valgt planbasis i undertittelen, og bruker to kompakte, justerte spor:

1. **Avtalt tid**
2. **Arbeid på turen**

Begge deler samme 00–24-akse. Registrert arbeid som overlapper baselinen vises i
primærfarge. Segmenter utenfor baselinen vises med error/coral-accent.

Når dagen inneholder ekstra arbeid, vises én liten tonal chip:
`+ <tid> utenfor avtalt tid`.

Dette er bevisst ikke merket «overtid». Visualiseringen beskriver den faktiske
tidsrelasjonen; tariffmotoren avgjør økonomisk behandling.

## Planbasis

Ved `NORMAL_ROSTER_APPLIES` er grunnturnusen avtalt baseline.

Ved `EMPLOYER_SET_TRIP_PLAN` er arbeidsgivers fastsatte plan baseline, og UX02-autodiffen
leverer innenfor/utover-segmentene.

A2 endrer ingen beløp eller klassifiseringsregler.

## A2R1 — redigeringsaffordance

A2 erstattet den gamle interaktive `DayTimeline` med en ren Canvas-presentasjon. Dermed mistet
selve dagskortet den forventede «trykk for å redigere»-oppførselen.

A2R1 gjør hele dagskortet trykkbart når dagen peker på nøyaktig én underliggende registrert
periode. Tomme dager beholder «trykk for å legge til». Dager med flere underliggende perioder
beholder de individuelle aktivitetsradene som presise redigeringsmål, slik at appen aldri gjetter
hvilken periode brukeren ønsket å endre.

Ingen beregnings- eller lagringssemantikk endres.
