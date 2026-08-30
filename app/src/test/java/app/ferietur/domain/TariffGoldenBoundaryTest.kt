package app.ferietur.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffGoldenBoundaryTest {
    private val rates = FerieturTariffRates.dok25_2026_2028
    private val start = LocalDateTime.of(2026, 8, 10, 7, 0)

    @Test
    fun stayAllowanceBoundaryFiveFiftyNineExactSixAndSixOhOneIsFailClosedAtExactWordingGap() {
        val below = allowanceCalculation(start.plusDays(1).plusHours(5).plusMinutes(59))
        val exact = allowanceCalculation(start.plusDays(1).plusHours(6))
        val above = allowanceCalculation(start.plusDays(1).plusHours(6).plusMinutes(1))

        assertEquals(1, below.stayAllowanceDays)
        assertFalse("D25_20_6_EXACT_THRESHOLD" in below.applicableUnresolvedRuleIds)
        assertFalse(below.lines.any { it.id == "stay-allowance-exact-threshold-open" })

        assertEquals(1, exact.stayAllowanceDays)
        assertTrue("D25_20_6_EXACT_THRESHOLD" in exact.applicableUnresolvedRuleIds)
        val open = exact.lines.single { it.id == "stay-allowance-exact-threshold-open" }
        assertEquals(CalculationCertainty.OPEN, open.certainty)
        assertEquals(PaymentTreatment.OPEN, open.paymentTreatment)
        assertFalse(open.includedInKnownTotal)
        assertEquals("110.00", open.amount.toPlainString())

        assertEquals(2, above.stayAllowanceDays)
        assertFalse("D25_20_6_EXACT_THRESHOLD" in above.applicableUnresolvedRuleIds)
        assertFalse(above.lines.any { it.id == "stay-allowance-exact-threshold-open" })
        assertEquals("220.00", above.lines.single { it.id == "stay-allowance" }.amount.toPlainString())
    }

    @Test
    fun activeNightQuarterHourBoundariesRemainExactGoldenContract() {
        assertEquals(0, TariffMath.roundActiveNightMinutes(14, rates))
        assertEquals(30, TariffMath.roundActiveNightMinutes(15, rates))
        assertEquals(30, TariffMath.roundActiveNightMinutes(44, rates))
        assertEquals(60, TariffMath.roundActiveNightMinutes(45, rates))
    }

    @Test
    fun threeTenMinuteEventsOnSameRestingWatchAreSummedBeforeRounding() {
        val monday = LocalDate.of(2026, 8, 10)
        val plans = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
                PlannedBlock(TimeKind.ACTIVE_EVENT_ON_RESTING, LocalTime.of(1, 0), LocalTime.of(1, 10)),
                PlannedBlock(TimeKind.ACTIVE_EVENT_ON_RESTING, LocalTime.of(2, 0), LocalTime.of(2, 10)),
                PlannedBlock(TimeKind.ACTIVE_EVENT_ON_RESTING, LocalTime.of(3, 0), LocalTime.of(3, 10)),
            ),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(monday, monday.plusDays(1)),
            roster = emptyMap(),
            plans = plans,
            annualSalary = OsloSalaryTable2026.annualSalary(32),
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = monday.atTime(23, 0),
            tripEnd = monday.plusDays(1).atTime(7, 0),
            rateSet = rates,
        )

        val line = calculation.lines.single { it.id == "active-on-resting" }
        assertTrue(line.detail.contains("30 min registrert"))
        assertTrue(line.detail.contains("30 min betalt"))
        assertEquals(BigDecimal("249.71"), line.amount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun activeNightRoundingIsPerWatchNotAcrossWholeTrip() {
        val monday = LocalDate.of(2026, 8, 10)
        val tuesday = monday.plusDays(1)
        fun watch() = listOf(
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            PlannedBlock(TimeKind.ACTIVE_EVENT_ON_RESTING, LocalTime.of(2, 0), LocalTime.of(2, 10)),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(monday, tuesday, tuesday.plusDays(1)),
            roster = emptyMap(),
            plans = mapOf(monday to watch(), tuesday to watch()),
            annualSalary = OsloSalaryTable2026.annualSalary(32),
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = monday.atTime(23, 0),
            tripEnd = tuesday.plusDays(1).atTime(7, 0),
            rateSet = rates,
        )

        val line = calculation.lines.single { it.id == "active-on-resting" }
        assertTrue(line.detail.contains("20 min registrert"))
        assertTrue(line.detail.contains("0 min betalt"))
        assertEquals(BigDecimal("0.00"), line.amount.setScale(2, RoundingMode.HALF_UP))
    }

    private fun allowanceCalculation(end: LocalDateTime): PreliminaryCalculation {
        val dates = generateSequence(start.toLocalDate()) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end.toLocalDate()) }
            .toList()
        return TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = emptyMap(),
            plans = emptyMap(),
            annualSalary = OsloSalaryTable2026.annualSalary(32),
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = start,
            tripEnd = end,
            rateSet = rates,
        )
    }
}
