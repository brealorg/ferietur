# A4.0C content coherence audit

A4.0C audits user-facing copy across the main Compose UI, calculation-line explanations and PDF export after the A4.0/A4.0A travel-model changes. The goal is to keep language aligned with the calculation model instead of discovering semantic drift through screenshots after each UI slice.

## Canonical terms

- **Grunnturnus**: the stored roster used as comparison basis when `USE_NORMAL_ROSTER` is selected. It is not silently copied into the actual trip work plan.
- **Betalingsgrunnlag**: the amount produced by the calculation engine and included in the payment basis. Open possible additions are not silently included.
- **Betalingsforslag**: the amount documented as the proposed settlement. It normally equals the payment basis; an already agreed alternative amount can be documented without changing the calculation.
- **Betalingsscenario**: the selected payer/refunder used in the payment proposal and documentation. It does not change the calculation and does not establish legal liability.
- **Grunnturnus – ikke i betalingsgrunnlaget**: ordinary roster supplements shown only as control information. They do not compete visually or semantically with the amount that comes in addition.
- **Må avklares**: a rule/input uncertainty that can affect the calculated amount. It does not determine the payment scenario.

## Findings fixed

1. The app previously mixed `vanlig turnus`, `opprinnelig turnus`, `original turnus` and `grunnturnus`. User-facing UI/PDF now use **grunnturnus** for the comparison object.
2. Payer selection previously used imperative/legal-sounding copy such as “skal betale” / “skal dekke”. The UI now asks which party the **betalingsforslag** should be set up against and explicitly states that the payment scenario neither changes the calculation nor determines legal liability. The PDF disclaimer is aligned with the same boundary.
3. The Settlement screen previously invited users to “bli enige om et kompromiss”. It now only documents an **already agreed** alternative amount and keeps the rule-based calculation unchanged.
4. Calculation-line copy for ordinary roster supplements previously said that who should cover the amount would be decided later, even though those lines are explicitly outside the trip payment basis. Their explanations now say that they belong to the grunnturnus and are shown only for control.
5. The active-work explanation in the UI still overstated Dok. 25 point 20.2 as if “outside the stored roster” were tariff wording. It now separates the app's comparison model from the tariff wording, matching the hardened PDF explanation.
6. Day-by-day expanded cards previously re-promoted ordinary roster supplements as full rows next to actual trip-payment rows. They now show only payment/open rows; ordinary-roster amounts are summarized as secondary control information and referred back to the dedicated turnus control.
7. Travel-without-responsibility copy still said payment had to be clarified even after A4.0 implemented the point 18.4 calculation assumption. The copy now says the time is calculated under the travel-time rules while named exceptions still require control.
8. An internal slice name (“A4.0”) leaked into a user-facing night-travel warning. It has been removed.
9. Open-rule control copy previously said unresolved rules could affect “who should pay”. It now correctly says they can affect the calculated amount; payment scenario is independent.
10. Singular/plural wording for one unresolved rule is corrected.

## Enforcement

A4.0C adds policy tests for the semantic boundaries above and a static content smoke that rejects known stale phrases (`kompromiss`, imperative payer wording, internal slice leakage, and the old “ALLEREDE DEKKET” label in the UI).

No tariff arithmetic is changed by this slice.
