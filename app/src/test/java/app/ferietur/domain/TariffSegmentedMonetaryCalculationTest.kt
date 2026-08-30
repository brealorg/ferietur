package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffSegmentedMonetaryCalculationTest {
    private val firstDate = LocalDate.of(2031, 6, 30)
    private val secondDate = LocalDate.of(2031, 7, 1)
    private val boundary = secondDate.atStartOfDay()

    @Test
    fun singleSliceCoordinatorIsMoneyAndLineEquivalentToCurrentProjectedCore() {
        val start = LocalDateTime.of(2026, 8, 10, 8, 0)
        val end = LocalDateTime.of(2026, 8, 10, 12, 0)
        val blocks = listOf(
            WorkBlock(start, start.plusHours(2), TimeKind.ACTIVE_WORK),
            WorkBlock(
                start.plusHours(2),
                start.plusHours(3),
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
                TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
            ),
        )
        val annualSalary = OsloSalaryTable2026.annualSalary(32)
        val rateSet = FerieturTariffRates.current
        val segment = TariffCalculationSegment(
            start = start.toLocalDate(),
            end = start.toLocalDate(),
            tariffPackage = FerieturTariffs.dok25_2026_2028,
            rateSet = rateSet,
            salaryTable = OsloSalaryTables.requireSupportedRange(start.toLocalDate(), start.toLocalDate()),
        )
        val plan = SegmentedTariffCalculationPlan(
            rulesetVersion = FerieturTariffs.dok25_2026_2028.rulesetVersion,
            tripStart = start,
            tripEnd = end,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            slices = listOf(
                TariffCalculationSlice(
                    segment = segment,
                    windowStart = start,
                    windowEnd = end,
                    dates = listOf(start.toLocalDate()),
                    annualSalary = annualSalary,
                    hourlyRate = TariffMath.hourlyRate(annualSalary, WeeklyBasis.HOURS_37_5, rateSet),
                    workBlocks = blocks.mapIndexed { index, block -> SegmentedWorkBlock(index, block) },
                ),
            ),
        )

        val expected = TripPlanEngine.calculatePreliminaryFromProjectedBlocks(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(start.toLocalDate()),
            roster = emptyMap(),
            blocks = blocks,
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = start,
            tripEnd = end,
            rateSet = rateSet,
        )
        val actual = success(plan)

        assertEquals(expected.lines, actual.lines)
        assertEquals(expected.knownAmount, actual.knownAmount)
        assertEquals(expected.paymentBasisAmount, actual.paymentBasisAmount)
        assertEquals(expected.alreadyCoveredByNormalRosterAmount, actual.alreadyCoveredByNormalRosterAmount)
        assertEquals(expected.excludedKnownRuleAmount, actual.excludedKnownRuleAmount)
        assertEquals(expected.applicableUnresolvedRuleIds, actual.applicableUnresolvedRuleIds)
    }

    @Test
    fun splitActiveWorkUsesEachSliceSalaryAndCountsStayAllowanceOnce() {
        val plan = splitPlan(
            tripStart = firstDate.atTime(8, 0),
            tripEnd = secondDate.atTime(10, 0),
            blocks = listOf(
                WorkBlock(firstDate.atTime(8, 0), firstDate.atTime(9, 0), TimeKind.ACTIVE_WORK),
                WorkBlock(secondDate.atTime(8, 0), secondDate.atTime(9, 0), TimeKind.ACTIVE_WORK),
            ),
        )

        val calculation = success(plan)
        val active = calculation.lineEntries.filter { it.line.id == "active" }
        val stay = calculation.lineEntries.filter { it.line.id == "stay-allowance" }

        assertEquals(listOf(0, 1), active.map { it.sliceIndex })
        assertEquals(listOf(BigDecimal("300.00"), BigDecimal("400.00")), active.map { it.line.amount })
        assertEquals(1, stay.size)
        assertEquals(BigDecimal("110.00"), stay.single().line.amount)
        assertEquals(BigDecimal("810.00"), calculation.paymentBasisAmount)
    }

    @Test
    fun knownOrdinaryTravelMayCrossBoundaryAndUsesEachSliceHourlyRate() {
        val plan = splitPlan(
            tripStart = firstDate.atTime(8, 0),
            tripEnd = secondDate.atTime(11, 0),
            blocks = listOf(
                WorkBlock(
                    firstDate.atTime(9, 0),
                    firstDate.atTime(10, 0),
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
                    TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
                ),
                WorkBlock(
                    secondDate.atTime(9, 0),
                    secondDate.atTime(10, 0),
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
                    TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
                ),
            ),
        )

        val calculation = success(plan)
        val travel = calculation.lineEntries.filter { it.line.id == "travel-without-responsibility" }

        assertEquals(listOf(0, 1), travel.map { it.sliceIndex })
        assertEquals(listOf(BigDecimal("300.00"), BigDecimal("400.00")), travel.map { it.line.amount })
        assertTrue(calculation.lineEntries.none { it.line.id == "travel-short-notice-overtime" })
    }

    @Test
    fun activeEventOnCrossBoundaryRestingWatchIsRoundedOnceAtEventPricingContext() {
        val watch = WorkBlock(
            firstDate.atTime(23, 0),
            secondDate.atTime(7, 0),
            TimeKind.RESTING_NIGHT_WATCH,
        )
        val event = WorkBlock(
            secondDate.atTime(2, 0),
            secondDate.atTime(2, 20),
            TimeKind.ACTIVE_EVENT_ON_RESTING,
        )
        val plan = splitPlan(
            tripStart = firstDate.atTime(22, 0),
            tripEnd = secondDate.atTime(8, 0),
            blocks = listOf(watch, event),
        )

        val calculation = success(plan)
        val activeOnResting = calculation.lineEntries.single { it.line.id == "active-on-resting" }

        assertEquals(TariffCalculationLineScope.PER_RESTING_WATCH, activeOnResting.scope)
        assertEquals(0, activeOnResting.watchSourceIndex)
        assertEquals(BigDecimal("300.00"), activeOnResting.line.amount)
        assertTrue(activeOnResting.line.detail.contains("20 min registrert"))
        assertTrue(activeOnResting.line.detail.contains("30 min betalt"))
    }

    @Test
    fun activeEventsAcrossDifferentPricingContextsStillFailClosedBeforeMoneyIsMerged() {
        val watch = WorkBlock(
            firstDate.atTime(23, 0),
            secondDate.atTime(7, 0),
            TimeKind.RESTING_NIGHT_WATCH,
        )
        val before = WorkBlock(
            firstDate.atTime(23, 40),
            firstDate.atTime(23, 50),
            TimeKind.ACTIVE_EVENT_ON_RESTING,
        )
        val after = WorkBlock(
            secondDate.atTime(0, 10),
            secondDate.atTime(0, 20),
            TimeKind.ACTIVE_EVENT_ON_RESTING,
        )
        val plan = splitPlan(
            tripStart = firstDate.atTime(22, 0),
            tripEnd = secondDate.atTime(8, 0),
            blocks = listOf(watch, before, after),
        )

        val failure = TariffSegmentedMonetaryCoordinator.calculate(
            plan = plan,
            fundingMode = FundingMode.VACATION_SEPARATE,
            roster = emptyMap(),
            weekendProfile = WeekendProfile.STANDARD,
        ) as TariffSegmentedMonetaryResult.Failure

        assertEquals(TariffSegmentedMonetaryFailureReason.WHOLE_TRIP_SCOPE_UNSAFE, failure.reason)
        assertEquals(TariffWholeTripScopeFailureReason.ACTIVE_EVENT_RATE_ALLOCATION_REQUIRED, failure.wholeTripScopeReason)
        assertEquals(0, failure.sourceIndex)
    }

    @Test
    fun shortOrUnresolvedTravelNoticeOnSplitTripFailsClosedUntilCrossRateAllocationExists() {
        val travel = WorkBlock(
            secondDate.atTime(8, 0),
            secondDate.atTime(9, 0),
            TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
        )
        val plan = splitPlan(
            tripStart = firstDate.atTime(8, 0),
            tripEnd = secondDate.atTime(10, 0),
            blocks = listOf(travel),
        )

        val failure = TariffSegmentedMonetaryCoordinator.calculate(
            plan = plan,
            fundingMode = FundingMode.VACATION_SEPARATE,
            roster = emptyMap(),
            weekendProfile = WeekendProfile.STANDARD,
        ) as TariffSegmentedMonetaryResult.Failure

        assertEquals(TariffSegmentedMonetaryFailureReason.SPLIT_TRAVEL_NOTICE_COORDINATION_REQUIRED, failure.reason)
        assertEquals(0, failure.sourceIndex)
    }

    private fun success(plan: SegmentedTariffCalculationPlan): SegmentedTariffMonetaryCalculation =
        (TariffSegmentedMonetaryCoordinator.calculate(
            plan = plan,
            fundingMode = FundingMode.VACATION_SEPARATE,
            roster = emptyMap(),
            weekendProfile = WeekendProfile.STANDARD,
        ) as TariffSegmentedMonetaryResult.Success).calculation

    private fun splitPlan(
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
        blocks: List<WorkBlock>,
    ): SegmentedTariffCalculationPlan {
        val tariff = TariffPackage(
            id = FerieturTariffs.DOK25_2026_2028_ID,
            label = "Testgrunnlag med kjent Dok. 25-ID",
            effectiveFrom = firstDate,
            effectiveTo = secondDate,
            rulesetVersion = FerieturTariffs.DOK25_2026_2028_RULESET_VERSION,
            sourceLabel = "test",
            sourcePageUrl = "https://example.invalid/tariff",
        )
        val firstRate = FerieturTariffRates.current.copy(
            id = "a4a5-rate-a",
            effectiveFrom = firstDate,
            effectiveTo = firstDate,
        )
        val secondRate = FerieturTariffRates.current.copy(
            id = "a4a5-rate-b",
            effectiveFrom = secondDate,
            effectiveTo = secondDate,
        )
        val firstSalary = SalaryTableDescriptor(
            id = "a4a5-salary-a",
            effectiveFrom = firstDate,
            verifiedThrough = firstDate,
            tariffPackageId = tariff.id,
            sourceLabel = "test",
            sourcePageUrl = "https://example.invalid/salary-a",
        )
        val secondSalary = SalaryTableDescriptor(
            id = "a4a5-salary-b",
            effectiveFrom = secondDate,
            verifiedThrough = secondDate,
            tariffPackageId = tariff.id,
            sourceLabel = "test",
            sourcePageUrl = "https://example.invalid/salary-b",
        )
        val firstAnnual = BigDecimal("585000")
        val secondAnnual = BigDecimal("780000")
        val firstSegment = TariffCalculationSegment(firstDate, firstDate, tariff, firstRate, firstSalary)
        val secondSegment = TariffCalculationSegment(secondDate, secondDate, tariff, secondRate, secondSalary)

        fun clipped(sliceStart: LocalDateTime, sliceEnd: LocalDateTime): List<SegmentedWorkBlock> =
            blocks.mapIndexedNotNull { index, source ->
                val start = maxOf(source.start, sliceStart)
                val end = minOf(source.end, sliceEnd)
                if (!end.isAfter(start)) null else SegmentedWorkBlock(index, source.copy(start = start, end = end))
            }

        return SegmentedTariffCalculationPlan(
            rulesetVersion = tariff.rulesetVersion,
            tripStart = tripStart,
            tripEnd = tripEnd,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            slices = listOf(
                TariffCalculationSlice(
                    segment = firstSegment,
                    windowStart = tripStart,
                    windowEnd = boundary,
                    dates = listOf(firstDate),
                    annualSalary = firstAnnual,
                    hourlyRate = BigDecimal("300.00"),
                    workBlocks = clipped(tripStart, boundary),
                ),
                TariffCalculationSlice(
                    segment = secondSegment,
                    windowStart = boundary,
                    windowEnd = tripEnd,
                    dates = listOf(secondDate),
                    annualSalary = secondAnnual,
                    hourlyRate = BigDecimal("400.00"),
                    workBlocks = clipped(boundary, tripEnd),
                ),
            ),
        )
    }
}
