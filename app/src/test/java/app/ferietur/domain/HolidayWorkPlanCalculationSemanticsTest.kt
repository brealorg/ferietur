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

class HolidayWorkPlanCalculationSemanticsTest {
    private val monday = LocalDate.of(2026, 8, 10)
    private val salary = OsloSalaryTable2026.annualSalary(32)

    @Test
    fun approvedHolidayPlanDoesNotUseGroundRosterAsAutomaticPoint20_2Classifier() {
        val calculation = engineCalculation(HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED)

        assertFalse(calculation.lines.any { it.id == "active" })
        val open = calculation.lines.single { it.id == "holiday-work-plan-scope-open" }
        assertEquals(CalculationCertainty.OPEN, open.certainty)
        assertEquals(PaymentTreatment.OPEN, open.paymentTreatment)
        assertFalse(open.includedInKnownTotal)
        assertEquals(BigDecimal("0.00"), open.amount)
        assertTrue("D25_20_2_WORK_PLAN_SCOPE" in calculation.applicableUnresolvedRuleIds)
        assertTrue(open.explanation.contains("grunnturnusen"))
        assertTrue(open.explanation.contains("ikke som fasit"))
    }

    @Test
    fun lateOrUnapprovedHolidayPlanAlsoDoesNotAutomaticallyBecomePoint20_2Overtime() {
        val calculation = engineCalculation(HolidayWorkPlanStatus.NOT_APPROVED_OR_LATE)

        assertFalse(calculation.lines.any { it.id == "active" })
        assertTrue(calculation.lines.any { it.id == "holiday-work-plan-scope-open" })
        assertTrue("D25_20_2_WORK_PLAN_SCOPE" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun nullStatusPreservesLegacyPredecessorSemanticsDuringMigrationOnly() {
        val calculation = engineCalculation(null)
        val active = calculation.lines.single { it.id == "active" }

        val expected = calculation.hourlyRate
            .multiply(BigDecimal("2"))
            .multiply(BigDecimal("1.50"))
            .setScale(2, RoundingMode.HALF_UP)

        assertEquals(expected, active.amount)
        assertFalse(calculation.lines.any { it.id == "holiday-work-plan-scope-open" })
        assertFalse("D25_20_2_WORK_PLAN_SCOPE" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun productionRuntimeGatewayPropagatesExplicitHolidayPlanStatus() {
        val tuesday = monday.plusDays(1)
        val result = FerieturTariffRuntimeCalculator.calculate(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
            dates = listOf(monday, tuesday),
            roster = emptyMap(),
            plans = mapOf(
                monday to listOf(
                    PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                ),
                tuesday to emptyList(),
            ),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(monday, LocalTime.of(8, 0)),
            tripEnd = LocalDateTime.of(tuesday, LocalTime.of(8, 0)),
        )

        assertTrue(result is TariffRuntimeCalculationResult.Success)
        val calculation = (result as TariffRuntimeCalculationResult.Success).calculation
        assertTrue(calculation.lines.any { it.id == "holiday-work-plan-scope-open" })
        assertFalse(calculation.lines.any { it.id == "active" })
        assertTrue("D25_20_2_WORK_PLAN_SCOPE" in calculation.applicableUnresolvedRuleIds)
    }

    private fun engineCalculation(
        status: HolidayWorkPlanStatus?,
    ): PreliminaryCalculation =
        TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday),
            roster = emptyMap(),
            plans = mapOf(
                monday to listOf(
                    PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                ),
            ),
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(monday, LocalTime.of(8, 0)),
            tripEnd = LocalDateTime.of(monday, LocalTime.of(10, 0)),
            holidayWorkPlanStatus = status,
        )
}
