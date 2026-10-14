package app.ferietur.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CostSplitInfoPolicyTest {
    private val texts = listOf(COST_SPLIT_INFO_SOURCE, COST_SPLIT_INFO_BODY, COST_SPLIT_INFO_DISCLAIMER)

    @Test
    fun costSplitInfoCitesHelseetatenAndKeepsRoutineAdvisory() {
        assertTrue(COST_SPLIT_INFO_SOURCE.contains("ref. 26/2867-3, 01.10.2026"))
        assertTrue(COST_SPLIT_INFO_BODY.contains("Reise, kost og losji dekkes av den som ønsker ledsaging"))
        assertTrue(COST_SPLIT_INFO_BODY.contains("veiledende"))
        assertTrue(COST_SPLIT_INFO_BODY.contains("sjekk med egen bydel"))
        assertTrue(COST_SPLIT_INFO_DISCLAIMER.contains("fastsetter ikke hvem som rettslig skal bære kostnaden"))
    }

    @Test
    fun costSplitInfoAvoidsUnresolvedAndOutOfScopeTopics() {
        texts.forEach { text ->
            assertFalse(text.contains("døgnkompensasjon", ignoreCase = true))
            assertFalse(text.contains("Personalhåndbok", ignoreCase = true))
        }
    }
}
