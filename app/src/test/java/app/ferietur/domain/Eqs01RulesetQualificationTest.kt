package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Eqs01RulesetQualificationTest {
    private val monday = LocalDate.of(2026, 8, 10)
    private val tuesday = monday.plusDays(1)

    @Test
    fun currentSnapshotAndTariffPackageExposeSameEqsRuleset() {
        assertEquals("2026.4", FERIETUR_RULESET_VERSION)
        assertEquals("2026.4", FerieturTariffs.DOK25_2026_2028_RULESET_VERSION)
        assertEquals(
            FERIETUR_RULESET_VERSION,
            FerieturTariffs.dok25_2026_2028.rulesetVersion,
        )
    }

    @Test
    fun productionRuntimeCarriesRuleset2026_4ThroughProvenance() {
        val result = FerieturTariffRuntimeCalculator.calculate(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
            dates = listOf(monday, tuesday),
            roster = mapOf(monday to "D1"),
            plans = mapOf(
                monday to listOf(
                    PlannedBlock(
                        kind = TimeKind.ACTIVE_WORK,
                        start = LocalTime.of(17, 0),
                        end = LocalTime.of(19, 0),
                        holidayWorkPlanRelation =
                            HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
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
        assertEquals("2026.4", runtime.rulesetVersion)
        assertTrue(runtime.provenance.isNotEmpty())
        assertTrue(runtime.provenance.all { it.rulesetVersion == "2026.4" })
        assertEquals(
            FerieturTariffs.DOK25_2026_2028_ID,
            runtime.provenance.single().tariffPackageId,
        )
    }
}
