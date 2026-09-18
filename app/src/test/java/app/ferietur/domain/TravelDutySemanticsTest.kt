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

class TravelDutySemanticsTest {
    private val monday = LocalDate.of(2026, 8, 10)
    private val salary = OsloSalaryTable2026.annualSalary(32)

    @Test
    fun offDutyTravelPaysFullOrdinaryTravelRegardlessOfGroundRosterAndDoesNotCountAsWorktime() {
        val block = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(10, 0),
            travelNoticeStatus = TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            travelDutyStatus = TravelDutyStatus.OFF_DUTY,
        )
        val withoutRoster = calculate(block, emptyMap())
        val withRoster = calculate(block, mapOf(monday to "D1"))
        val expected = withoutRoster.hourlyRate.multiply(BigDecimal("2")).setScale(2, RoundingMode.HALF_UP)

        assertEquals(expected, withoutRoster.lines.single { it.id == "travel-without-responsibility" }.amount)
        assertEquals(expected, withRoster.lines.single { it.id == "travel-without-responsibility" }.amount)
        assertFalse(withRoster.lines.any { it.id == "active" })
        assertFalse(withRoster.lines.any { it.id == "travel-duty-status-open" })

        val findings = TripPlanEngine.controlFindings(
            TripPlanEngine.projectRange(listOf(monday), mapOf(monday to listOf(block))),
            0,
            mapOf(monday to "D1"),
        )
        assertTrue(findings.first { it.title == "Samlet arbeidstid" }.detail.startsWith("0 t "))
    }

    @Test
    fun onDutyWithinPlanTravelCountsAsWorktimeWithoutSeparateTravelBasePay() {
        val block = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            start = LocalTime.of(17, 0),
            end = LocalTime.of(19, 0),
            holidayWorkPlanRelation = HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
            travelDutyStatus = TravelDutyStatus.ON_DUTY,
        )
        val calculation = calculate(block)

        assertFalse(calculation.lines.any { it.id == "travel-without-responsibility" })
        assertFalse(calculation.lines.any { it.id == "active" })
        assertEquals(120L, calculation.lines.single { it.id == "evening-night" }.evidence.sumOf { it.minutes })

        val findings = TripPlanEngine.controlFindings(
            TripPlanEngine.projectRange(listOf(monday), mapOf(monday to listOf(block))),
            0,
        )
        assertTrue(findings.first { it.title == "Samlet arbeidstid" }.detail.startsWith("2 t "))
    }

    @Test
    fun onDutyBeyondPlanTravelUsesPoint20_2() {
        val block = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(10, 0),
            holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
            travelDutyStatus = TravelDutyStatus.ON_DUTY,
        )
        val calculation = calculate(block)
        val active = calculation.lines.single { it.id == "active" }
        val expected = calculation.hourlyRate.multiply(BigDecimal("2")).multiply(BigDecimal("1.50")).setScale(2, RoundingMode.HALF_UP)

        assertEquals(expected, active.amount)
        assertEquals(120L, active.evidence.sumOf { it.minutes })
        assertTrue(active.evidence.all { it.note.contains("på vakt") })
        assertFalse(calculation.lines.any { it.id == "travel-without-responsibility" })
    }

    @Test
    fun unresolvedDutyStatusFailsClosedForOrdinaryTravel() {
        val block = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(10, 0),
            travelNoticeStatus = TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            travelDutyStatus = TravelDutyStatus.NOT_CLARIFIED,
        )
        val calculation = calculate(block)

        assertFalse(calculation.lines.any { it.id == "travel-without-responsibility" })
        assertFalse(calculation.lines.any { it.id == "active" })
        val open = calculation.lines.single { it.id == "travel-duty-status-open" }
        assertEquals(120L, open.evidence.sumOf { it.minutes })
        assertEquals(PaymentTreatment.OPEN, open.paymentTreatment)
        assertFalse(open.includedInKnownTotal)
        assertTrue("D25_20_3_TRAVEL_DUTY_STATUS" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun offDutySleepAllowedNightStillUsesPassiveSpecialRuleAndCountsAsWorktime() {
        val block = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED,
            start = LocalTime.of(23, 0),
            end = LocalTime.of(7, 0),
            travelNoticeStatus = TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            travelDutyStatus = TravelDutyStatus.OFF_DUTY,
        )
        val calculation = calculate(block)

        assertTrue(calculation.lines.any { it.id == "travel-passive-night" })
        assertFalse(calculation.lines.any { it.id == "travel-without-responsibility" })
        assertFalse("D25_20_3_TRAVEL_DUTY_STATUS" in calculation.applicableUnresolvedRuleIds)

        val findings = TripPlanEngine.controlFindings(
            TripPlanEngine.projectRange(listOf(monday), mapOf(monday to listOf(block))),
            0,
        )
        assertTrue(findings.first { it.title == "Samlet arbeidstid" }.detail.startsWith("8 t "))
    }

    @Test
    fun lateOrUnapprovedOnDutyBeyondRelationStillFailsClosed() {
        val block = PlannedBlock(
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            start = LocalTime.of(8, 0),
            end = LocalTime.of(10, 0),
            holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
            travelDutyStatus = TravelDutyStatus.ON_DUTY,
        )
        val calculation = calculate(block, status = HolidayWorkPlanStatus.NOT_APPROVED_OR_LATE)

        assertFalse(calculation.lines.any { it.id == "active" })
        assertTrue(calculation.lines.any { it.id == "holiday-work-plan-scope-open" })
        assertTrue("D25_20_2_WORK_PLAN_SCOPE" in calculation.applicableUnresolvedRuleIds)
    }

    private fun calculate(
        block: PlannedBlock,
        roster: Map<LocalDate, String> = emptyMap(),
        status: HolidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
    ): PreliminaryCalculation = TripPlanEngine.calculatePreliminary(
        fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
        dates = listOf(monday),
        roster = roster,
        plans = mapOf(monday to listOf(block)),
        annualSalary = salary,
        weeklyBasis = WeeklyBasis.HOURS_35_5,
        weekendProfile = WeekendProfile.STANDARD,
        tripStart = LocalDateTime.of(monday, block.start),
        tripEnd = block.toWorkBlock(monday).end,
        holidayWorkPlanStatus = status,
    )
}
