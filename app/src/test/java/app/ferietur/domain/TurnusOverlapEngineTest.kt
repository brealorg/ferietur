package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TurnusOverlapEngineTest {
    private val date = LocalDate.of(2026, 8, 17)
    private val lv = requireNotNull(SolhaugenShiftCatalog.byCode("LV"))

    @Test
    fun actualSevenToTwentyThreeAgainstLongShiftSplitsFourHoursOutside() {
        val result = TurnusOverlapEngine.compare(date, LocalTime.of(7, 0), LocalTime.of(23, 0), lv)
        assertEquals(960, result.actualMinutes)
        assertEquals(720, result.insideTurnusMinutes)
        assertEquals(240, result.outsideTurnusMinutes)
    }

    @Test
    fun actualTenToTwentyIsFullyInsideLongShift() {
        val result = TurnusOverlapEngine.compare(date, LocalTime.of(10, 0), LocalTime.of(20, 0), lv)
        assertEquals(600, result.actualMinutes)
        assertEquals(600, result.insideTurnusMinutes)
        assertEquals(0, result.outsideTurnusMinutes)
    }
}
