package app.ferietur.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowChromePolicyTest {
    @Test
    fun parsesTurnusFlowProgress() {
        val progress = flowChromeProgress("4 av 9")!!
        assertEquals(4, progress.current)
        assertEquals(9, progress.total)
        assertEquals("Steg 4 av 9", progress.label)
        assertTrue(progress.fraction > 0.44f && progress.fraction < 0.45f)
    }

    @Test
    fun parsesSeparateFlowProgress() {
        val progress = flowChromeProgress("4 av 8")!!
        assertEquals("Steg 4 av 8", progress.label)
        assertEquals(0.5f, progress.fraction)
    }

    @Test
    fun rejectsInvalidProgress() {
        assertNull(flowChromeProgress(""))
        assertNull(flowChromeProgress("0 av 9"))
        assertNull(flowChromeProgress("10 av 9"))
    }
}
