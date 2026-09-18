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

class RestingNightHolidaySemanticsTest {
    private val monday = LocalDate.of(2026, 8, 10)
    private val tuesday = monday.plusDays(1)
    private val salary = OsloSalaryTable2026.annualSalary(32)

    @Test
    fun explicitHolidayPlanPaysRegisteredRestingNightAtOneThirdRegardlessOfGroundRoster() {
        val noRoster = calculate(roster = emptyMap())
        val overlappingRoster = calculate(roster = mapOf(monday to "N2"))

        val first = noRoster.lines.single { it.id == "resting-night" }
        val second = overlappingRoster.lines.single { it.id == "resting-night" }
        val expected = noRoster.hourlyRate
            .multiply(BigDecimal("8"))
            .divide(BigDecimal("3"), 8, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)

        assertEquals(expected, first.amount)
        assertEquals(first.amount, second.amount)
        assertEquals(480L, first.evidence.sumOf { it.minutes })
        assertEquals(480L, second.evidence.sumOf { it.minutes })
        assertTrue(second.explanation.contains("Grunnturnusen vises bare som sammenligning"))
    }

    @Test
    fun restingNightPoint20_4DoesNotDependOnHolidayPlanRelation() {
        fun amount(relation: HolidayWorkPlanRelation): BigDecimal =
            calculate(relation = relation).lines.single { it.id == "resting-night" }.amount

        assertEquals(
            amount(HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN),
            amount(HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN),
        )
        assertEquals(
            amount(HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN),
            amount(HolidayWorkPlanRelation.NOT_CLARIFIED),
        )
    }

    @Test
    fun explicitHolidayPlanUsesSameRegisteredWatchForPassiveSupplements() {
        val saturday = LocalDate.of(2026, 8, 15)
        val calculation = calculate(
            date = saturday,
            roster = mapOf(saturday to "N2"),
        )

        val resting = calculation.lines.single { it.id == "resting-night" }
        val evening = calculation.lines.single { it.id == "resting-evening-night" }
        val weekend = calculation.lines.single { it.id == "resting-weekend" }

        assertEquals(480L, resting.evidence.sumOf { it.minutes })
        assertEquals(480L, evening.evidence.sumOf { it.minutes })
        assertTrue(weekend.evidence.sumOf { it.minutes } > 0)
        assertTrue(evening.explanation.contains("Grunnturnusen brukes ikke"))
        assertTrue(weekend.explanation.contains("Grunnturnusen brukes ikke"))
    }

    @Test
    fun activeEventInsideRestingWatchStillUsesSpecificPoint20_4Rule() {
        val plans = mapOf(
            monday to listOf(
                restingBlock(),
                PlannedBlock(
                    kind = TimeKind.ACTIVE_EVENT_ON_RESTING,
                    start = LocalTime.of(2, 0),
                    end = LocalTime.of(2, 20),
                ),
            ),
            tuesday to emptyList(),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday, tuesday),
            roster = mapOf(monday to "N2"),
            plans = plans,
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(monday, LocalTime.of(22, 0)),
            tripEnd = LocalDateTime.of(tuesday, LocalTime.of(8, 0)),
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
        )

        val active = calculation.lines.single { it.id == "active-on-resting" }
        assertTrue(active.amount > BigDecimal.ZERO)
        assertTrue(active.source.contains("20.4"))
        assertTrue(active.detail.contains("30 min"))
    }

    @Test
    fun legacyNullStatusRetainsGroundRosterPredecessorBehavior() {
        val plans = mapOf(
            monday to listOf(restingBlock()),
            tuesday to emptyList(),
        )
        val roster = mapOf(monday to "N2")
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday, tuesday),
            roster = roster,
            plans = plans,
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(monday, LocalTime.of(22, 0)),
            tripEnd = LocalDateTime.of(tuesday, LocalTime.of(8, 0)),
            holidayWorkPlanStatus = null,
        )

        val overlap = TripPlanEngine.overlapWithRoster(restingBlock().toWorkBlock(monday), roster)
        assertTrue(overlap > 0)
        val line = calculation.lines.firstOrNull { it.id == "resting-night" }
        if (overlap == 480L) {
            assertTrue(line == null || line.amount.compareTo(BigDecimal.ZERO) == 0)
        } else {
            assertTrue(line != null)
            assertTrue(line!!.evidence.sumOf { it.minutes } < 480L)
        }
    }

    @Test
    fun productionRuntimeUsesExplicitPoint20_4RestingNightSemantics() {
        val result = FerieturTariffRuntimeCalculator.calculate(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
            dates = listOf(monday, tuesday),
            roster = mapOf(monday to "N2"),
            plans = mapOf(
                monday to listOf(restingBlock()),
                tuesday to emptyList(),
            ),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(monday, LocalTime.of(22, 0)),
            tripEnd = LocalDateTime.of(tuesday, LocalTime.of(8, 0)),
        )

        assertTrue(result is TariffRuntimeCalculationResult.Success)
        val runtime = (result as TariffRuntimeCalculationResult.Success).calculation
        val line = runtime.lines.single { it.id == "resting-night" }
        assertEquals(480L, line.evidence.sumOf { it.minutes })
        assertFalse(runtime.applicableUnresolvedRuleIds.contains("D25_20_2_WORK_PLAN_SCOPE"))
    }

    private fun calculate(
        date: LocalDate = monday,
        roster: Map<LocalDate, String> = emptyMap(),
        relation: HolidayWorkPlanRelation = HolidayWorkPlanRelation.NOT_CLARIFIED,
    ): PreliminaryCalculation {
        val next = date.plusDays(1)
        return TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(date, next),
            roster = roster,
            plans = mapOf(
                date to listOf(restingBlock(relation)),
                next to emptyList(),
            ),
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(date, LocalTime.of(22, 0)),
            tripEnd = LocalDateTime.of(next, LocalTime.of(8, 0)),
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
        )
    }

    private fun restingBlock(
        relation: HolidayWorkPlanRelation = HolidayWorkPlanRelation.NOT_CLARIFIED,
    ): PlannedBlock =
        PlannedBlock(
            kind = TimeKind.RESTING_NIGHT_WATCH,
            start = LocalTime.of(23, 0),
            end = LocalTime.of(7, 0),
            holidayWorkPlanRelation = relation,
        )
}
