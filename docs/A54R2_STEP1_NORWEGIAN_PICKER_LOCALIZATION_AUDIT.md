# A5.4R2 Step 1 Norwegian picker localization audit

Scope is deliberately narrow: Step 1 picker language and date-range selection colors only. No workflow, persistence, tariff, calculation, or PDF logic is changed.

## Runtime contract

- The DateRangePicker headline is owned by Ferietur and uses the same `nb-NO` range formatter as the Step 1 field, e.g. `11.–18. august 2026`.
- Material picker resources are composed with a Norwegian configuration so built-in picker labels and accessibility strings do not inherit an English device locale.
- The TimeInput remains 24-hour input mode, but its built-in labels are requested from Norwegian Material resources.
- Selected date endpoints use the approved Oslo-yellow semantic color; the in-range fill uses a translucent Oslo-yellow treatment instead of the dark brown selection strip.
- The two postponed lifecycle bugs (blank new trips saved as Sommerferie, and rotation returning to Home) are intentionally not part of this slice.

## Version

- app: `0.5.4-r2-a54r2`
- ruleset: unchanged `2026.3`
