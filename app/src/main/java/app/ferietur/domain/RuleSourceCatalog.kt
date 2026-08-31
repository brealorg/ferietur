package app.ferietur.domain

enum class RuleSourceKind {
    TARIFF,
    STATUTE,
    LOCAL_SETTLEMENT,
}

data class RuleSourceBinding(
    val ruleId: String,
    val sourceKind: RuleSourceKind,
    val tariffPackageId: String?,
    val sections: List<String>,
    val sourceLabel: String,
)

object FerieturRuleSources {
    val bindings = listOf(
        tariff("D25_20_2", "20.2"),
        tariff("D25_20_3", "20.3"),
        tariff("D25_18_4", "18.4", "20.3"),
        tariff("D25_18_4_SHORT_NOTICE", "18.4", "13.2", "13.3", "13.7.1"),
        tariff("D25_18_4_NOTICE", "18.4"),
        tariff("D25_18_4_X13_7_3", "13.7.3", "18.4"),
        tariff("D25_20_3_SLEEP_PERMISSION", "20.3"),
        tariff("D25_20_4_RESTING", "20.4"),
        tariff("D25_12_1_1", "12.1.1"),
        tariff("D25_12_2_2", "12.2.2"),
        tariff("D25_12_2_3", "12.2.3"),
        tariff("D25_20_6", "20.6"),
        tariff("D25_20_6_EXACT_THRESHOLD", "20.6"),
        tariff("D25_20_4_ACTIVE", "20.4"),
        tariff("D25_8_9_X20", "8.9", "12.1.1", "12.2.2", "12.2.3", "20.3", "20.4"),
        tariff("D25_20_2_X12_13", "12.1.1", "12.2.2", "20.2"),
        tariff("D25_20_2_X13_7_3", "13.1", "13.7.3", "20.2"),
        RuleSourceBinding(
            ruleId = "PAYMENT_PROPOSAL",
            sourceKind = RuleSourceKind.LOCAL_SETTLEMENT,
            tariffPackageId = null,
            sections = emptyList(),
            sourceLabel = "Lokal avtale om oppgjør",
        ),
        RuleSourceBinding(
            ruleId = "AML_CONTROL",
            sourceKind = RuleSourceKind.STATUTE,
            tariffPackageId = null,
            sections = listOf("arbeidsmiljøloven kapittel 10"),
            sourceLabel = "Arbeidsmiljøloven kapittel 10",
        ),
    )

    private val byRuleId = bindings.associateBy { it.ruleId }

    init {
        require(byRuleId.size == bindings.size) { "Dupliserte regel-ID-er i regelkildematrisa." }

        val domainRuleIds = FerieturRules.rules.map { it.id }.toSet()
        val bindingRuleIds = bindings.map { it.ruleId }.toSet()
        require(domainRuleIds == bindingRuleIds) {
            val missing = domainRuleIds - bindingRuleIds
            val orphaned = bindingRuleIds - domainRuleIds
            "Regelkildematrisa er ikke komplett. Mangler=$missing, foreldreløse=$orphaned"
        }
    }

    fun forRule(ruleId: String): RuleSourceBinding =
        requireNotNull(byRuleId[ruleId]) { "Ukjent regel-ID: $ruleId" }

    private fun tariff(ruleId: String, vararg sections: String): RuleSourceBinding =
        RuleSourceBinding(
            ruleId = ruleId,
            sourceKind = RuleSourceKind.TARIFF,
            tariffPackageId = FerieturTariffs.DOK25_2026_2028_ID,
            sections = sections.toList(),
            sourceLabel = FerieturTariffs.dok25_2026_2028.sourceLabel,
        )
}
