package app.ferietur.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffPriorityPolicyTest {

    @Test
    fun point20_2InteractionRulesAreImplementedRatherThanOpen() {
        val rules = FerieturRules.rules.associateBy { it.id }

        assertEquals(RuleStatus.IMPLEMENTED, rules.getValue("D25_20_2_X12_13").status)
        assertEquals(RuleStatus.IMPLEMENTED, rules.getValue("D25_20_2_X13_7_3").status)
        assertTrue(rules.getValue("D25_20_2_X12_13").source.contains("12.1.1"))
        assertTrue(rules.getValue("D25_20_2_X13_7_3").source.contains("13.1"))
    }

    @Test
    fun onlyExplicitFactOrWordingGapsRemainGloballyUnresolved() {
        val unresolved = FerieturRules.rules.filter { it.status == RuleStatus.UNRESOLVED }.map { it.id }.toSet()

        assertEquals(
            setOf(
                "D25_18_4_NOTICE",
                "D25_18_4_X13_7_3",
                "D25_20_2_WORK_PLAN_SCOPE",
                "D25_20_3_TRAVEL_DUTY_STATUS",
                "D25_20_3_SLEEP_PERMISSION",
                "D25_20_6_EXACT_THRESHOLD",
            ),
            unresolved,
        )
        assertFalse("D25_20_2_X12_13" in unresolved)
        assertFalse("D25_20_2_X13_7_3" in unresolved)
        assertFalse("D25_8_9_X20" in unresolved)
        assertEquals(
            RuleStatus.WORKING_INTERPRETATION,
            FerieturRules.rules.single {
                it.id == "D25_8_9_X20"
            }.status,
        )
    }
}
