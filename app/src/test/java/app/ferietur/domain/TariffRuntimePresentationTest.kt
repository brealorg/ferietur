package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffRuntimePresentationTest {
    @Test
    fun singleContextPresentationPreservesExistingPreliminaryAuditExactly() {
        val date = LocalDate.of(2026, 8, 10)
        val endDate = date.plusDays(1)
        val dates = listOf(date, endDate)
        val plans = mapOf(
            date to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(10, 0)),
            ),
            endDate to emptyList(),
        )
        val start = date.atTime(8, 0)
        val end = endDate.atTime(12, 0)
        val runtime = FerieturTariffRuntimeCalculator.calculate(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = emptyMap(),
            plans = plans,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = start,
            tripEnd = end,
        ) as TariffRuntimeCalculationResult.Success
        val single = runtime.calculation as TariffRuntimeCalculation.SingleContext

        val presentation = TariffRuntimeCalculationPresentations.fromRuntime(
            calculation = single,
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = emptyMap(),
            plans = plans,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            tripStart = start,
            tripEnd = end,
        )

        assertEquals(single.preliminary.lines, presentation.lines)
        assertEquals(single.preliminary.knownAmount, presentation.knownAmount)
        assertEquals(single.preliminary.paymentBasisAmount, presentation.paymentBasisAmount)
        assertEquals(
            single.preliminary.alreadyCoveredByNormalRosterAmount,
            presentation.alreadyCoveredByNormalRosterAmount,
        )
        assertEquals(single.preliminary.excludedKnownRuleAmount, presentation.excludedKnownRuleAmount)
        assertEquals(single.preliminary.applicableUnresolvedRuleIds, presentation.applicableUnresolvedRuleIds)
        assertEquals(single.preliminary.dayAudits, presentation.dayAudits)
        assertEquals(single.preliminary.rosterMinutes, presentation.rosterMinutes)
        assertEquals(single.preliminary.rosterUncoveredMinutes, presentation.rosterUncoveredMinutes)
        assertEquals(single.preliminary.activeInsideRosterMinutes, presentation.activeInsideRosterMinutes)
        assertEquals(single.preliminary.restingNightMinutes, presentation.restingNightMinutes)
        assertEquals(single.preliminary.restingNightOutsideRosterMinutes, presentation.restingNightOutsideRosterMinutes)
        assertEquals(single.preliminary.hourlyRate, presentation.singleHourlyRateOrNull)
        assertNotNull(presentation.sharedControlRateSet)
        assertTrue(presentation.lineEntries.map { it.key }.toSet().size == presentation.lineEntries.size)
    }

    @Test
    fun segmentedPresentationKeepsDuplicateLineIdsAsDistinctUiEntries() {
        val plan = splitPlan(
            firstRate = FerieturTariffRates.current.copy(
                id = "presentation-rate-a",
                effectiveFrom = firstDate,
                effectiveTo = firstDate,
            ),
            secondRate = FerieturTariffRates.current.copy(
                id = "presentation-rate-b",
                effectiveFrom = secondDate,
                effectiveTo = secondDate,
            ),
        )
        val runtime = FerieturTariffRuntimeCalculator.calculatePlan(
            plan = plan,
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(firstDate, secondDate),
            roster = emptyMap(),
            weekendProfile = WeekendProfile.STANDARD,
        ) as TariffRuntimeCalculationResult.Success
        val segmented = runtime.calculation as TariffRuntimeCalculation.SegmentedContexts
        val plans = mapOf(
            firstDate to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(9, 0))),
            secondDate to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(9, 0))),
        )

        val presentation = TariffRuntimeCalculationPresentations.fromRuntime(
            calculation = segmented,
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(firstDate, secondDate),
            roster = emptyMap(),
            plans = plans,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            tripStart = plan.tripStart,
            tripEnd = plan.tripEnd,
        )

        val activeEntries = presentation.lineEntries.filter { it.line.id == "active" }
        assertEquals(2, activeEntries.size)
        assertEquals(2, activeEntries.map { it.key }.toSet().size)
        assertEquals(listOf(0, 1), activeEntries.map { it.tariffContextIndex })
        assertEquals(2, presentation.provenance.size)
        assertNull(presentation.singleHourlyRateOrNull)
        assertNotNull(presentation.sharedControlRateSet)
    }

    @Test
    fun segmentedPresentationRejectsOneSharedControlRateWhenControlParametersChange() {
        val firstRate = FerieturTariffRates.current.copy(
            id = "presentation-control-a",
            effectiveFrom = firstDate,
            effectiveTo = firstDate,
        )
        val secondRate = FerieturTariffRates.current.copy(
            id = "presentation-control-b",
            effectiveFrom = secondDate,
            effectiveTo = secondDate,
            travelSleepWindowStart = FerieturTariffRates.current.travelSleepWindowStart.plusHours(1),
        )
        val plan = splitPlan(firstRate, secondRate)
        val runtime = FerieturTariffRuntimeCalculator.calculatePlan(
            plan = plan,
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(firstDate, secondDate),
            roster = emptyMap(),
            weekendProfile = WeekendProfile.STANDARD,
        ) as TariffRuntimeCalculationResult.Success
        val plans = mapOf(
            firstDate to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(9, 0))),
            secondDate to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(9, 0))),
        )

        val presentation = TariffRuntimeCalculationPresentations.fromRuntime(
            calculation = runtime.calculation,
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(firstDate, secondDate),
            roster = emptyMap(),
            plans = plans,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            tripStart = plan.tripStart,
            tripEnd = plan.tripEnd,
        )

        assertNull(presentation.sharedControlRateSet)
    }

    private val firstDate = LocalDate.of(2031, 6, 30)
    private val secondDate = LocalDate.of(2031, 7, 1)
    private val boundary = secondDate.atStartOfDay()

    private fun splitPlan(
        firstRate: TariffRateSet,
        secondRate: TariffRateSet,
    ): SegmentedTariffCalculationPlan {
        val tariff = TariffPackage(
            id = FerieturTariffs.DOK25_2026_2028_ID,
            label = "Presentation test",
            effectiveFrom = firstDate,
            effectiveTo = secondDate,
            rulesetVersion = FerieturTariffs.DOK25_2026_2028_RULESET_VERSION,
            sourceLabel = "test",
            sourcePageUrl = "https://example.invalid/presentation-tariff",
        )
        val firstSalary = SalaryTableDescriptor(
            id = "presentation-salary-a",
            effectiveFrom = firstDate,
            verifiedThrough = firstDate,
            tariffPackageId = tariff.id,
            sourceLabel = "salary A",
            sourcePageUrl = "https://example.invalid/salary-a",
        )
        val secondSalary = SalaryTableDescriptor(
            id = "presentation-salary-b",
            effectiveFrom = secondDate,
            verifiedThrough = secondDate,
            tariffPackageId = tariff.id,
            sourceLabel = "salary B",
            sourcePageUrl = "https://example.invalid/salary-b",
        )
        val firstBlock = WorkBlock(firstDate.atTime(8, 0), firstDate.atTime(9, 0), TimeKind.ACTIVE_WORK)
        val secondBlock = WorkBlock(secondDate.atTime(8, 0), secondDate.atTime(9, 0), TimeKind.ACTIVE_WORK)

        return SegmentedTariffCalculationPlan(
            rulesetVersion = tariff.rulesetVersion,
            tripStart = firstDate.atTime(8, 0),
            tripEnd = secondDate.atTime(10, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            slices = listOf(
                TariffCalculationSlice(
                    segment = TariffCalculationSegment(firstDate, firstDate, tariff, firstRate, firstSalary),
                    windowStart = firstDate.atTime(8, 0),
                    windowEnd = boundary,
                    dates = listOf(firstDate),
                    annualSalary = BigDecimal("585000"),
                    hourlyRate = BigDecimal("300.00"),
                    workBlocks = listOf(SegmentedWorkBlock(0, firstBlock)),
                ),
                TariffCalculationSlice(
                    segment = TariffCalculationSegment(secondDate, secondDate, tariff, secondRate, secondSalary),
                    windowStart = boundary,
                    windowEnd = secondDate.atTime(10, 0),
                    dates = listOf(secondDate),
                    annualSalary = BigDecimal("780000"),
                    hourlyRate = BigDecimal("400.00"),
                    workBlocks = listOf(SegmentedWorkBlock(1, secondBlock)),
                ),
            ),
        )
    }
}
