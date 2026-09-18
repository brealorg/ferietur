# UX02A — egen feriearbeidsplan og automatisk differanse

Ferietur skiller nå eksplisitt mellom tre datasett:

1. **Grunnturnus** — ordinær turnus hjemme.
2. **Feriearbeidsplan** — godkjent/varslet plan for ferieoppholdet.
3. **Faktisk arbeid** — det som faktisk ble arbeidet.

Punkt 20.2 avledes ved å sammenligne feriearbeidsplan mot faktisk arbeid. Grunnturnusen er ikke
klassifiseringsfasit.

Draft schema 9 legger til `holidayPlans`. Eksisterende `plans` beholder betydningen faktisk arbeid.
Pre-v9-drafts mangler dette faktum og migreres derfor med tom `holidayPlans` samt
`V9_HOLIDAY_WORK_PLAN_REVIEW_REQUIRED`; appen gjetter aldri feriearbeidsplan fra grunnturnus eller
faktisk arbeid.

`TripPlanEngine.deriveHolidayWorkPlanRelations()` splitter faktisk arbeid ved plangrensene. Dermed
blir eksempelvis planlagt 07:00–22:00 og faktisk 07:00–23:30 automatisk:

- 07:00–22:00 innenfor planen
- 22:00–23:30 utover planen

UX02B kobler dette inn i appflyten med eget feriearbeidsplansteg, kopi fra grunnturnus, faktisk
arbeid fra ferieplan og fjerning av den manuelle `I planen / Utover planen`-velgeren.

UX01A roster-templatefix og UX01D lokal backup beholdes.
