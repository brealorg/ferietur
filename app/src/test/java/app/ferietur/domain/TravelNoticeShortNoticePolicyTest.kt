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

class TravelNoticeShortNoticePolicyTest {
    private val salary = OsloSalaryTable2026.annualSalary(32)
    private val monday = LocalDate.of(2026, 8, 10)

    private fun calculate(
        date: LocalDate = monday,
        block: PlannedBlock,
        roster: Map<LocalDate, String> = emptyMap(),
        fundingMode: FundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
    ): PreliminaryCalculation = TripPlanEngine.calculatePreliminary(
        fundingMode = fundingMode,
        dates = listOf(date),
        roster = roster,
        plans = mapOf(date to listOf(block)),
        annualSalary = salary,
        weeklyBasis = WeeklyBasis.HOURS_35_5,
        weekendProfile = WeekendProfile.STANDARD,
        tripStart = block.toWorkBlock(date).start,
        tripEnd = block.toWorkBlock(date).end,
    )

    @Test
    fun knownByPreviousDayKeepsOnlyOrdinaryTravelPay() {
        val calculation = calculate(
            block = PlannedBlock(
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            ),
        )

        assertTrue(calculation.lines.any { it.id == "travel-without-responsibility" })
        assertFalse(calculation.lines.any { it.id == "travel-short-notice-overtime" })
        assertFalse(calculation.lines.any { it.id == "travel-notice-open" })
        assertFalse("D25_18_4_NOTICE" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun notClarifiedKeepsOrdinaryPayAndOpensNoticeRule() {
        val calculation = calculate(
            block = PlannedBlock(
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                TravelNoticeStatus.NOT_CLARIFIED,
            ),
        )

        val base = calculation.lines.single { it.id == "travel-without-responsibility" }
        val open = calculation.lines.single { it.id == "travel-notice-open" }
        assertEquals(calculation.hourlyRate.multiply(BigDecimal("2")).setScale(2, RoundingMode.HALF_UP), base.amount)
        assertEquals(calculation.hourlyRate.setScale(2, RoundingMode.HALF_UP), open.amount)
        assertFalse(open.includedInKnownTotal)
        assertEquals(BigDecimal("0.00"), calculation.paymentBasisAmount.subtract(base.amount).setScale(2))
        assertTrue(open.detail.contains("mulig tillegg"))
        assertTrue("D25_18_4_NOTICE" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun shortNoticeWeekdayDaytimeAddsFiftyPercentForUpToTwoHours() {
        val calculation = calculate(
            block = PlannedBlock(
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
            ),
        )

        val overtime = calculation.lines.single { it.id == "travel-short-notice-overtime" }
        assertEquals(120L, overtime.evidence.sumOf { it.minutes })
        assertTrue(overtime.detail.contains("50 %"))
        assertEquals(calculation.hourlyRate.setScale(2, RoundingMode.HALF_UP), overtime.amount)
    }

    @Test
    fun shortNoticeAtNightUsesHundredPercent() {
        val calculation = calculate(
            block = PlannedBlock(
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
                LocalTime.of(21, 0),
                LocalTime.of(23, 0),
                TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
            ),
        )

        val overtime = calculation.lines.single { it.id == "travel-short-notice-overtime" }
        assertTrue(overtime.detail.contains("100 %"))
        assertEquals(
            calculation.hourlyRate.multiply(BigDecimal("2")).setScale(2, RoundingMode.HALF_UP),
            overtime.amount,
        )
    }

    @Test
    fun shortNoticeThreeHourJourneyCapsOvertimeTreatmentAtTwoActualHours() {
        val calculation = calculate(
            block = PlannedBlock(
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                LocalTime.of(8, 0),
                LocalTime.of(11, 0),
                TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
            ),
        )

        val base = calculation.lines.single { it.id == "travel-without-responsibility" }
        val overtime = calculation.lines.single { it.id == "travel-short-notice-overtime" }
        assertEquals(180L, base.evidence.sumOf { it.minutes })
        assertEquals(120L, overtime.evidence.sumOf { it.minutes })
        assertTrue(overtime.detail.startsWith("2 t reisetid"))
    }

    @Test
    fun shortNoticeOvertimeSupplementRoundsUpToCommencedHalfHour() {
        val calculation = calculate(
            block = PlannedBlock(
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                LocalTime.of(8, 0),
                LocalTime.of(9, 10),
                TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
            ),
        )

        val overtime = calculation.lines.single { it.id == "travel-short-notice-overtime" }
        assertEquals(70L, overtime.evidence.sumOf { it.minutes })
        assertTrue(overtime.detail.contains("1 t 30 min tilleggsgrunnlag"))
        val expected = calculation.hourlyRate.multiply(BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP)
        assertEquals(expected, overtime.amount)
    }

    @Test
    fun multipleShortNoticeBlocksShareOneTwoHourCapForTheTrip() {
        val tuesday = monday.plusDays(1)
        val plans = mapOf(
            monday to listOf(
                PlannedBlock(
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 30),
                    TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
                ),
            ),
            tuesday to listOf(
                PlannedBlock(
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 30),
                    TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
                ),
            ),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday, tuesday),
            roster = emptyMap(),
            plans = plans,
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = monday.atTime(8, 0),
            tripEnd = tuesday.atTime(9, 30),
        )

        val base = calculation.lines.single { it.id == "travel-without-responsibility" }
        val overtime = calculation.lines.single { it.id == "travel-short-notice-overtime" }
        assertEquals(180L, base.evidence.sumOf { it.minutes })
        assertEquals(120L, overtime.evidence.sumOf { it.minutes })
    }

    @Test
    fun explicitWeeklyDayOffF1UsesHundredPercentButF2DoesNot() {
        fun rateFor(code: String): String {
            val calculation = calculate(
                block = PlannedBlock(
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                    LocalTime.of(8, 0),
                    LocalTime.of(10, 0),
                    TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
                ),
                roster = mapOf(monday to code),
            )
            return calculation.lines.single { it.id == "travel-short-notice-overtime" }.detail
        }

        assertTrue(rateFor("F1").contains("100 %"))
        assertTrue(rateFor("F2").contains("50 %"))
    }

    @Test
    fun dayBeforeSundayAfterRosterEndUsesHundredPercent() {
        val saturday = LocalDate.of(2026, 8, 15)
        val calculation = calculate(
            date = saturday,
            block = PlannedBlock(
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                LocalTime.of(15, 0),
                LocalTime.of(17, 0),
                TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
            ),
            roster = mapOf(saturday to "D1"),
        )

        assertTrue(calculation.lines.single { it.id == "travel-short-notice-overtime" }.detail.contains("100 %"))
    }

    @Test
    fun specialHolidayKeepsConfirmedOvertimeAndOpensOnlyPossible133Difference() {
        val may1 = LocalDate.of(2026, 5, 1)
        val calculation = calculate(
            date = may1,
            block = PlannedBlock(
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
            ),
        )

        assertTrue(calculation.lines.single { it.id == "travel-short-notice-overtime" }.detail.contains("100 %"))
        val open = calculation.lines.single { it.id == "travel-short-notice-133-open" }
        assertTrue(open.amount > BigDecimal.ZERO)
        assertFalse(open.includedInKnownTotal)
        assertTrue("D25_18_4_X13_7_3" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun passiveSleepNightDoesNotUseShortNoticeRuleForSameNightMinutes() {
        val calculation = calculate(
            block = PlannedBlock(
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED,
                LocalTime.of(23, 0),
                LocalTime.of(7, 0),
                TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
            ),
            fundingMode = FundingMode.VACATION_SEPARATE,
        )

        assertTrue(calculation.lines.any { it.id == "travel-passive-night" })
        assertFalse(calculation.lines.any { it.id == "travel-short-notice-overtime" })
        assertFalse(calculation.lines.any { it.id == "travel-notice-open" })
        assertFalse("D25_18_4_NOTICE" in calculation.applicableUnresolvedRuleIds)
    }
    @Test
    fun unresolvedNoticePossibleAmountRespectsSharedTwoHourCap() {
        val tuesday = monday.plusDays(1)
        val plans = mapOf(
            monday to listOf(
                PlannedBlock(
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                    LocalTime.of(8, 0),
                    LocalTime.of(10, 0),
                    TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
                ),
            ),
            tuesday to listOf(
                PlannedBlock(
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                    LocalTime.of(8, 0),
                    LocalTime.of(10, 0),
                    TravelNoticeStatus.NOT_CLARIFIED,
                ),
            ),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday, tuesday),
            roster = emptyMap(),
            plans = plans,
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = monday.atTime(8, 0),
            tripEnd = tuesday.atTime(10, 0),
        )

        val open = calculation.lines.single { it.id == "travel-notice-open" }
        assertEquals(BigDecimal("0.00"), open.amount)
        assertTrue(open.detail.contains("mulig beløpsendring"))
    }

}
