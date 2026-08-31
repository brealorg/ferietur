package app.ferietur.domain

enum class RuleStatus {
    IMPLEMENTED,
    WORKING_INTERPRETATION,
    UNRESOLVED,
    WARNING_ONLY,
}

data class DomainRule(
    val id: String,
    val title: String,
    val source: String,
    val status: RuleStatus,
)

object FerieturRules {
    val rules = listOf(
        DomainRule("D25_20_2", "Arbeid utover den ordinære arbeidstiden på ferieopphold", "Dok. 25 2026–28, punkt 20.2", RuleStatus.IMPLEMENTED),
        DomainRule("D25_20_3", "Reise med ansvar for beboeren", "Dok. 25 2026–28, punkt 20.3", RuleStatus.IMPLEMENTED),
        DomainRule("D25_18_4", "Reisetid uten tilsynsansvar", "Dok. 25 2026–28, punkt 18.4 og 20.3", RuleStatus.IMPLEMENTED),
        DomainRule("D25_18_4_SHORT_NOTICE", "Kort varsel om tjenestereisen", "Dok. 25 2026–28, punkt 18.4, 13.2, 13.3 og 13.7.1", RuleStatus.IMPLEMENTED),
        DomainRule("D25_18_4_NOTICE", "Når arbeidstakeren fikk vite om reisen", "Dok. 25 2026–28, punkt 18.4", RuleStatus.UNRESOLVED),
        DomainRule("D25_18_4_X13_7_3", "Særskilt overtidsprosent ved kort varsel på helge-/høytidsdag", "Dok. 25 2026–28, punkt 13.7.3 og 18.4", RuleStatus.UNRESOLVED),
        DomainRule("D25_20_3_SLEEP_PERMISSION", "Søvntillatelse ved reise mellom kl. 23:00 og 07:00", "Dok. 25 2026–28, punkt 20.3", RuleStatus.UNRESOLVED),
        DomainRule("D25_20_4_RESTING", "Hvilende nattevakt", "Dok. 25 2026–28, punkt 20.4", RuleStatus.IMPLEMENTED),
        DomainRule("D25_12_1_1", "Kveld- og nattillegg", "Dok. 25 2026–28, punkt 12.1.1", RuleStatus.IMPLEMENTED),
        DomainRule("D25_12_2_2", "Lørdags- og søndagstillegg", "Dok. 25 2026–28, punkt 12.2.2", RuleStatus.IMPLEMENTED),
        DomainRule("D25_12_2_3", "Helge- og høytidsdagstillegg", "Dok. 25 2026–28, punkt 12.2.3", RuleStatus.IMPLEMENTED),
        DomainRule("D25_20_6", "Døgngodtgjøring ved ferieopphold", "Dok. 25 2026–28, punkt 20.6", RuleStatus.IMPLEMENTED),
        DomainRule("D25_20_6_EXACT_THRESHOLD", "Nøyaktig seks timers resttid ved døgngodtgjøring", "Dok. 25 2026–28, punkt 20.6", RuleStatus.UNRESOLVED),
        DomainRule("D25_20_4_ACTIVE", "Aktivt arbeid under hvilende nattevakt", "Dok. 25 2026–28, punkt 20.4", RuleStatus.IMPLEMENTED),
        DomainRule("D25_8_9_X20", "Tillegg etter kapittel 12 ved arbeid av passiv karakter", "Dok. 25 2026–28, punkt 8.9, 12.1.1, 12.2.2, 12.2.3, 20.3 og 20.4", RuleStatus.WORKING_INTERPRETATION),
        DomainRule("D25_20_2_X12_13", "Kapittel 12-tillegg stables ikke på de samme timene som kompenseres etter punkt 20.2", "Dok. 25 2026–28, punkt 12.1.1, 12.2.2 og 20.2", RuleStatus.IMPLEMENTED),
        DomainRule("D25_20_2_X13_7_3", "Punkt 20.2 brukes som særregel for arbeidstid ut over ordinær arbeidstid under ferieopphold", "Dok. 25 2026–28, punkt 13.1, 13.7.3 og 20.2", RuleStatus.IMPLEMENTED),
        DomainRule("PAYMENT_PROPOSAL", "Betalingsforslaget holdes adskilt fra beregningen", "Lokal avtale om oppgjør", RuleStatus.IMPLEMENTED),
        DomainRule("AML_CONTROL", "Arbeidstidskontrollen varsler, men stopper ikke beregningen", "Arbeidsmiljøloven kapittel 10", RuleStatus.WARNING_ONLY),
    )

    fun applicableUnresolvedRules(calculation: PreliminaryCalculation): List<DomainRule> =
        applicableUnresolvedRules(calculation.applicableUnresolvedRuleIds)

    fun applicableUnresolvedRules(ruleIds: Set<String>): List<DomainRule> =
        rules.filter { rule ->
            rule.status == RuleStatus.UNRESOLVED && rule.id in ruleIds
        }

    private val workingInterpretationLineIdsByRule: Map<String, Set<String>> =
        mapOf(
            "D25_8_9_X20" to setOf(
                "resting-evening-night",
                "resting-weekend",
                "resting-holiday",
                "travel-passive-evening-night",
                "travel-passive-weekend",
                "travel-passive-holiday",
            ),
        )

    /**
     * Returns interpretations that Ferietur actually used in the monetary
     * calculation, but which the app does not present as finally clarified.
     *
     * These are deliberately separate from [applicableUnresolvedRules]:
     * working interpretations remain part of the calculated payment basis.
     */
    fun applicableWorkingInterpretationRules(
        lines: List<CalculationLine>,
    ): List<DomainRule> {
        val lineIds = lines.mapTo(mutableSetOf()) { it.id }

        return rules.filter { rule ->
            rule.status == RuleStatus.WORKING_INTERPRETATION &&
                workingInterpretationLineIdsByRule[
                    rule.id
                ].orEmpty().any { lineId ->
                    lineId in lineIds
                }
        }
    }
}

