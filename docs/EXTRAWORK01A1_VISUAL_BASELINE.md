# EXTRAWORK01A1 — visuell baseline for arbeid utover avtalt tid

Denne slicen endrer bare presentasjonen på «Arbeid på turen». Beregningsmotor, planbasis,
tariffregler, persistence, snapshot-format og ruleset endres ikke.

Når vanlig grunnturnus gjelder, viser hvert dagskort to separate tidslinjer:

1. **Grunnturnus · avtalt tid**
2. **Arbeid på turen**

Arbeid som ligger innenfor grunnturnusen vises med ordinær primærfarge. Arbeid som ligger utenfor
den avtalte tiden vises rødt. På en fridag vil registrert sammenlignbart arbeid derfor være rødt.

Når arbeidsgiver faktisk har fastsatt en egen plan, brukes den samme presentasjonen, men
**Arbeidsgivers plan** er den avtalte baselinen. Den allerede kvalifiserte UX02-autodiffen avgjør
hvilke registrerte segmenter som ligger innenfor eller utover denne planen.

Visualiseringen er en forklaring av den valgte planbasisen. Den ber ikke arbeidstakeren avgjøre
«overtid» eller «merarbeid», og den endrer ikke tariffklassifiseringen. Hvilende natt og andre
særregler beholder sine etablerte perioder og beregningsregler.
