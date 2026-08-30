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
