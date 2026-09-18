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

class HolidayWorkPlanPeriodSemanticsTest {
    private val monday = LocalDate.of(2026, 8, 10)
    private val tuesday = monday.plusDays(1)
    private val salary = OsloSalaryTable2026.annualSalary(32)

    @Test
    fun approvedWithinPlanActiveWorkGetsOrdinarySupplementWithoutAddingBaseSalaryAgain() {
        val calculation = calculate(
            listOf(
                PlannedBlock(
                    kind = TimeKind.ACTIVE_WORK,
                    start = LocalTime.of(16, 0),
                    end = LocalTime.of(19, 0),
                    holidayWorkPlanRelation = HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
                ),
            ),
        )

        assertFalse(calculation.lines.any { it.id == "active" })
        assertFalse(calculation.lines.any { it.id == "holiday-work-plan-scope-open" })
        assertFalse("D25_20_2_WORK_PLAN_SCOPE" in calculation.applicableUnresolvedRuleIds)

        val evening = calculation.lines.single { it.id == "evening-night" }
        assertEquals(120L, evening.evidence.sumOf { it.minutes })
        assertEquals(PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS, evening.paymentTreatment)
        assertTrue(evening.amount > BigDecimal.ZERO)
        assertTrue(evening.explanation.contains("innenfor feriearbeidsplanen"))
    }

    @Test
    fun approvedBeyondPlanActiveWorkUsesPoint20_2WithoutGroundRosterClassifier() {
        val calculation = calculate(
            listOf(
                PlannedBlock(
                    kind = TimeKind.ACTIVE_WORK,
                    start = LocalTime.of(8, 0),
                    end = LocalTime.of(10, 0),
                    holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
                ),
            ),
        )

        val active = calculation.lines.single { it.id == "active" }
        val expected = calculation.hourlyRate
            .multiply(BigDecimal("2"))
            .multiply(BigDecimal("1.50"))
            .setScale(2, RoundingMode.HALF_UP)

        assertEquals(expected, active.amount)
        assertTrue(active.title.contains("feriearbeidsplanen"))
        assertEquals(120L, active.evidence.sumOf { it.minutes })
        assertTrue(active.explanation.contains("Grunnturnusen brukes ikke som klassifiseringsfasit"))
        assertFalse(calculation.lines.any { it.id == "holiday-work-plan-scope-open" })
        assertFalse("D25_20_2_WORK_PLAN_SCOPE" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun approvedMixedPeriodsPriceBeyondKeepUnknownOpenAndUseWithinForOrdinarySupplements() {
        val calculation = calculate(
            listOf(
                PlannedBlock(
                    kind = TimeKind.ACTIVE_WORK,
                    start = LocalTime.of(16, 0),
                    end = LocalTime.of(18, 0),
                    holidayWorkPlanRelation = HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
                ),
                PlannedBlock(
                    kind = TimeKind.ACTIVE_WORK,
                    start = LocalTime.of(18, 0),
                    end = LocalTime.of(20, 0),
                    holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
                ),
                PlannedBlock(
                    kind = TimeKind.ACTIVE_WORK,
                    start = LocalTime.of(20, 0),
                    end = LocalTime.of(21, 0),
                    holidayWorkPlanRelation = HolidayWorkPlanRelation.NOT_CLARIFIED,
                ),
            ),
        )

        val active = calculation.lines.single { it.id == "active" }
        assertEquals(120L, active.evidence.sumOf { it.minutes })

        val open = calculation.lines.single { it.id == "holiday-work-plan-scope-open" }
        assertEquals(60L, open.evidence.sumOf { it.minutes })
        assertEquals(CalculationCertainty.OPEN, open.certainty)
        assertEquals(PaymentTreatment.OPEN, open.paymentTreatment)
        assertTrue("D25_20_2_WORK_PLAN_SCOPE" in calculation.applicableUnresolvedRuleIds)

        val evening = calculation.lines.single { it.id == "evening-night" }
        assertEquals(60L, evening.evidence.sumOf { it.minutes })
        assertEquals(PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS, evening.paymentTreatment)
    }

    @Test
    fun lateOrUnapprovedPlanDoesNotTurnBeyondRelationIntoAutomaticPoint20_2Money() {
        val calculation = calculate(
            blocks = listOf(
                PlannedBlock(
                    kind = TimeKind.ACTIVE_WORK,
                    start = LocalTime.of(8, 0),
                    end = LocalTime.of(10, 0),
                    holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
                ),
            ),
            status = HolidayWorkPlanStatus.NOT_APPROVED_OR_LATE,
        )

        assertFalse(calculation.lines.any { it.id == "active" })
        val open = calculation.lines.single { it.id == "holiday-work-plan-scope-open" }
        assertEquals(120L, open.evidence.sumOf { it.minutes })
        assertTrue("D25_20_2_WORK_PLAN_SCOPE" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun approvedExplicitRelationsProduceSamePaymentRegardlessOfStoredGroundRoster() {
        val blocks = listOf(
            PlannedBlock(
                kind = TimeKind.ACTIVE_WORK,
                start = LocalTime.of(16, 0),
                end = LocalTime.of(18, 0),
                holidayWorkPlanRelation = HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
            ),
            PlannedBlock(
                kind = TimeKind.ACTIVE_WORK,
                start = LocalTime.of(18, 0),
                end = LocalTime.of(20, 0),
                holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
            ),
        )

        val withoutRoster = calculate(blocks, roster = emptyMap())
        val withRoster = calculate(blocks, roster = mapOf(monday to "D1"))

        assertEquals(withoutRoster.paymentBasisAmount, withRoster.paymentBasisAmount)
        assertEquals(
            withoutRoster.lines.single { it.id == "active" }.amount,
            withRoster.lines.single { it.id == "active" }.amount,
        )
        assertEquals(
            withoutRoster.lines.single { it.id == "evening-night" }.amount,
            withRoster.lines.single { it.id == "evening-night" }.amount,
        )
    }

    @Test
    fun productionRuntimePricesExplicitBeyondPlanRelationAfterB1FailClosedBridge() {
        val result = FerieturTariffRuntimeCalculator.calculate(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
            dates = listOf(monday, tuesday),
            roster = mapOf(monday to "D1", tuesday to "LV"),
            plans = mapOf(
                monday to listOf(
                    PlannedBlock(
                        kind = TimeKind.ACTIVE_WORK,
                        start = LocalTime.of(18, 0),
                        end = LocalTime.of(20, 0),
                        holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
                    ),
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
        val runtime = (result as TariffRuntimeCalculationResult.Success).calculation
        assertTrue(runtime.lines.any { it.id == "active" && it.title.contains("feriearbeidsplanen") })
        assertFalse(runtime.lines.any { it.id == "holiday-work-plan-scope-open" })
        assertFalse("D25_20_2_WORK_PLAN_SCOPE" in runtime.applicableUnresolvedRuleIds)
    }

    private fun calculate(
        blocks: List<PlannedBlock>,
        status: HolidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
        roster: Map<LocalDate, String> = emptyMap(),
    ): PreliminaryCalculation =
        TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday),
            roster = roster,
            plans = mapOf(monday to blocks),
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(monday, LocalTime.of(0, 0)),
            tripEnd = LocalDateTime.of(monday, LocalTime.of(23, 0)),
            holidayWorkPlanStatus = status,
        )
}
