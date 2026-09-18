package app.ferietur.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleSourceCatalogTest {
    @Test
    fun everyDomainRuleHasExactlyOneStructuredSourceBinding() {
        val domainRuleIds = FerieturRules.rules.map { it.id }.toSet()
        val bindings = FerieturRuleSources.bindings

        assertEquals(domainRuleIds.size, bindings.size)
        assertEquals(domainRuleIds, bindings.map { it.ruleId }.toSet())
    }

    @Test
    fun dok25RulesAreBoundToTheVersioned2026To2028TariffPackage() {
        val binding = FerieturRuleSources.forRule("D25_20_4_ACTIVE")

        assertEquals(RuleSourceKind.TARIFF, binding.sourceKind)
        assertEquals(FerieturTariffs.DOK25_2026_2028_ID, binding.tariffPackageId)
        assertEquals(listOf("20.4"), binding.sections)
    }

    @Test
    fun holidayWorkPlanScopeRuleKeepsPoint20_2AsCanonicalTariffBinding() {
        val binding = FerieturRuleSources.forRule("D25_20_2_WORK_PLAN_SCOPE")
        val domainRule = FerieturRules.rules.single { it.id == "D25_20_2_WORK_PLAN_SCOPE" }

        assertEquals(RuleSourceKind.TARIFF, binding.sourceKind)
        assertEquals(FerieturTariffs.DOK25_2026_2028_ID, binding.tariffPackageId)
        assertEquals(listOf("20.2"), binding.sections)
        assertTrue(domainRule.source.contains("EQS ID 53398"))
    }

    @Test
    fun travelDutyStatusRuleKeepsPoint20_3AsCanonicalTariffBinding() {
        val binding = FerieturRuleSources.forRule("D25_20_3_TRAVEL_DUTY_STATUS")
        val domainRule = FerieturRules.rules.single { it.id == "D25_20_3_TRAVEL_DUTY_STATUS" }

        assertEquals(RuleSourceKind.TARIFF, binding.sourceKind)
        assertEquals(FerieturTariffs.DOK25_2026_2028_ID, binding.tariffPackageId)
        assertEquals(listOf("20.3"), binding.sections)
        assertTrue(domainRule.source.contains("EQS ID 53398"))
    }

    @Test
    fun nonTariffRulesStayOutsideTheTariffPackage() {
        val settlement = FerieturRuleSources.forRule("PAYMENT_PROPOSAL")
        val aml = FerieturRuleSources.forRule("AML_CONTROL")

        assertNull(settlement.tariffPackageId)
        assertEquals(RuleSourceKind.LOCAL_SETTLEMENT, settlement.sourceKind)
        assertNull(aml.tariffPackageId)
        assertEquals(RuleSourceKind.STATUTE, aml.sourceKind)
        assertTrue(aml.sections.single().contains("kapittel 10"))
    }
}
