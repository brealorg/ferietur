package app.ferietur.domain

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class TariffMathTest {
    @Test
    fun restingNightKeepsFullWorkTimeButOneThirdPayEquivalent() {
        val result = TariffMath.restingNight(480)
        assertEquals(480, result.workTimeMinutes)
        assertEquals("160.00000000", result.payEquivalentMinutes.toPlainString())
    }

    @Test
    fun activeNightRoundingMatchesDok25Threshold() {
        assertEquals(0, TariffMath.roundActiveNightMinutes(14))
        assertEquals(30, TariffMath.roundActiveNightMinutes(15))
        assertEquals(30, TariffMath.roundActiveNightMinutes(44))
        assertEquals(60, TariffMath.roundActiveNightMinutes(45))
    }

    @Test
    fun eveningNightRateIsFortyPercent() {
        assertEquals("133.18", TariffMath.eveningNightRate(BigDecimal("332.94")).toPlainString())
    }

    @Test
    fun weekendRateUsesHigherOfPercentageAndMinimum() {
        assertEquals("76.58", TariffMath.weekendRate(BigDecimal("332.94"), WeekendProfile.STANDARD).toPlainString())
        assertEquals("73.00", TariffMath.weekendRate(BigDecimal("200"), WeekendProfile.STANDARD).toPlainString())
    }
}
