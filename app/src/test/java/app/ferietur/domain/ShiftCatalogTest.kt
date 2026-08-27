package app.ferietur.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftCatalogTest {
    @Test
    fun longShiftIsTwelveHours() {
        val lv = requireNotNull(SolhaugenShiftCatalog.byCode("LV"))
        assertEquals(720, lv.durationMinutes())
        assertEquals(ShiftCategory.LONG_DAY, lv.category)
    }

    @Test
    fun nightShiftCrossesMidnight() {
        val n = requireNotNull(SolhaugenShiftCatalog.byCode("N"))
        assertTrue(n.crossesMidnight)
        assertEquals(585, n.durationMinutes())
    }

    @Test
    fun kitchenShiftIsExcludedFromTripCatalog() {
        val k6 = requireNotNull(SolhaugenShiftCatalog.byCode("K6"))
        assertTrue(k6.excludedFromTrip)
        assertFalse(SolhaugenShiftCatalog.tripRelevant.any { it.code == "K6" })
    }
}
