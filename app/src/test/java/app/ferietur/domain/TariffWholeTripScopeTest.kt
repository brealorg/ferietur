package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffWholeTripScopeTest {
    @Test
    fun lineScopeCatalogCoversEveryCurrentCalculationLine() {
        val expected = setOf(
            "active",
            "active-on-resting",
            "evening-night",
            "holiday",
            "resting-evening-night",
            "resting-holiday",
            "resting-night",
            "resting-weekend",
            "stay-allowance",
            "stay-allowance-exact-threshold-open",
            "travel-night-sleep-open",
            "travel-notice-open",
            "travel-passive-evening-night",
            "travel-passive-holiday",
            "travel-passive-night",
            "travel-passive-weekend",
            "travel-responsibility-open",
            "travel-short-notice-133-open",
            "travel-short-notice-overtime",
            "travel-without-responsibility",
            "weekend",
        )

        assertEquals(expected, TariffCalculationLineScopes.knownLineIds)
        assertEquals(TariffCalculationLineScope.WHOLE_TRIP, TariffCalculationLineScopes.requireForLineId("stay-allowance"))
        assertEquals(TariffCalculationLineScope.PER_RESTING_WATCH, TariffCalculationLineScopes.requireForLineId("active-on-resting"))
        assertEquals(TariffCalculationLineScope.SEGMENT_LOCAL, TariffCalculationLineScopes.requireForLineId("active"))
    }

    @Test
    fun pureNumericSplitWithStableWholeTripPoliciesCanBeCoordinated() {
        val plan = splitPlan(
            firstRate = baseRate("rate-a", LocalDate.of(2030, 6, 30)),
            secondRate = baseRate("rate-b", LocalDate.of(2030, 7, 1)).copy(
                eveningNightFraction = BigDecimal("0.45"),
            ),
            firstHourlyRate = BigDecimal("300.00"),
            secondHourlyRate = BigDecimal("310.00"),
            blocks = emptyList(),
        )

        val result = TariffWholeTripScopeCoordinator.coordinate(plan) as TariffWholeTripScopeResult.Success

        assertEquals(BigDecimal("110"), result.scopePlan.stayAllowancePolicy.amountPerDay)
        assertEquals(360L, result.scopePlan.stayAllowancePolicy.remainderThresholdMinutes)
        assertEquals(120L, result.scopePlan.shortNoticePolicy.maxMinutes)
        assertTrue(result.scopePlan.restingWatchScopes.isEmpty())
    }

    @Test
    fun stayAllowancePolicyChangeFailsClosed() {
        val plan = splitPlan(
            firstRate = baseRate("rate-a", LocalDate.of(2030, 6, 30)),
            secondRate = baseRate("rate-b", LocalDate.of(2030, 7, 1)).copy(
                stayAllowancePerDay = BigDecimal("125"),
            ),
            blocks = emptyList(),
        )

        val failure = TariffWholeTripScopeCoordinator.coordinate(plan) as TariffWholeTripScopeResult.Failure

        assertEquals(TariffWholeTripScopeFailureReason.INCONSISTENT_STAY_ALLOWANCE_POLICY, failure.reason)
    }

    @Test
    fun shortNoticeCapOrRoundingChangeFailsClosed() {
        val plan = splitPlan(
            firstRate = baseRate("rate-a", LocalDate.of(2030, 6, 30)),
            secondRate = baseRate("rate-b", LocalDate.of(2030, 7, 1)).copy(
                shortNoticeMaxMinutes = 180L,
            ),
            blocks = emptyList(),
        )

        val failure = TariffWholeTripScopeCoordinator.coordinate(plan) as TariffWholeTripScopeResult.Failure

        assertEquals(TariffWholeTripScopeFailureReason.INCONSISTENT_SHORT_NOTICE_POLICY, failure.reason)
    }

    @Test
    fun restingWatchMayCrossBoundaryWhenAllActiveEventsBelongToOnePricingContext() {
        val watch = WorkBlock(
            LocalDateTime.of(2030, 6, 30, 23, 0),
            LocalDateTime.of(2030, 7, 1, 7, 0),
            TimeKind.RESTING_NIGHT_WATCH,
        )
        val event = WorkBlock(
            LocalDateTime.of(2030, 7, 1, 2, 0),
            LocalDateTime.of(2030, 7, 1, 2, 20),
            TimeKind.ACTIVE_EVENT_ON_RESTING,
        )
        val plan = splitPlan(
            firstRate = baseRate("rate-a", LocalDate.of(2030, 6, 30)),
            secondRate = baseRate("rate-b", LocalDate.of(2030, 7, 1)),
            firstHourlyRate = BigDecimal("300.00"),
            secondHourlyRate = BigDecimal("310.00"),
            blocks = listOf(watch, event),
        )

        val result = TariffWholeTripScopeCoordinator.coordinate(plan) as TariffWholeTripScopeResult.Success
        val scope = result.scopePlan.restingWatchScopes.single()

        assertEquals(0, scope.watchSourceIndex)
        assertEquals(listOf(1), scope.activeEventSourceIndices)
        assertEquals(setOf(1), scope.activeEventSliceIndexes)
        assertFalse(scope.crossesEffectiveBoundary)
    }

    @Test
    fun activeEventsInDifferentPriceContextsOnSameWatchFailClosedBeforeRoundingAllocation() {
        val watch = WorkBlock(
            LocalDateTime.of(2030, 6, 30, 23, 0),
            LocalDateTime.of(2030, 7, 1, 7, 0),
            TimeKind.RESTING_NIGHT_WATCH,
        )
        val before = WorkBlock(
            LocalDateTime.of(2030, 6, 30, 23, 40),
            LocalDateTime.of(2030, 6, 30, 23, 50),
            TimeKind.ACTIVE_EVENT_ON_RESTING,
        )
        val after = WorkBlock(
            LocalDateTime.of(2030, 7, 1, 0, 10),
            LocalDateTime.of(2030, 7, 1, 0, 20),
            TimeKind.ACTIVE_EVENT_ON_RESTING,
        )
        val plan = splitPlan(
            firstRate = baseRate("rate-a", LocalDate.of(2030, 6, 30)),
            secondRate = baseRate("rate-b", LocalDate.of(2030, 7, 1)),
            firstHourlyRate = BigDecimal("300.00"),
            secondHourlyRate = BigDecimal("310.00"),
            blocks = listOf(watch, before, after),
        )

        val failure = TariffWholeTripScopeCoordinator.coordinate(plan) as TariffWholeTripScopeResult.Failure

        assertEquals(TariffWholeTripScopeFailureReason.ACTIVE_EVENT_RATE_ALLOCATION_REQUIRED, failure.reason)
        assertEquals(0, failure.sourceIndex)
    }

    @Test
    fun activeEventsAcrossSlicesAreAllowedWhenEffectivePricingAndRoundingAreIdentical() {
        val watch = WorkBlock(
            LocalDateTime.of(2030, 6, 30, 23, 0),
            LocalDateTime.of(2030, 7, 1, 7, 0),
            TimeKind.RESTING_NIGHT_WATCH,
        )
        val before = WorkBlock(
            LocalDateTime.of(2030, 6, 30, 23, 40),
            LocalDateTime.of(2030, 6, 30, 23, 50),
            TimeKind.ACTIVE_EVENT_ON_RESTING,
        )
        val after = WorkBlock(
            LocalDateTime.of(2030, 7, 1, 0, 10),
            LocalDateTime.of(2030, 7, 1, 0, 20),
            TimeKind.ACTIVE_EVENT_ON_RESTING,
        )
        val firstRate = baseRate("rate-a", LocalDate.of(2030, 6, 30))
        val secondRate = baseRate("rate-b", LocalDate.of(2030, 7, 1))
        val plan = splitPlan(
            firstRate = firstRate,
            secondRate = secondRate,
            firstHourlyRate = BigDecimal("300.00"),
            secondHourlyRate = BigDecimal("300.0"),
            blocks = listOf(watch, before, after),
        )

        val result = TariffWholeTripScopeCoordinator.coordinate(plan) as TariffWholeTripScopeResult.Success
        val scope = result.scopePlan.restingWatchScopes.single()

        assertEquals(setOf(0, 1), scope.activeEventSliceIndexes)
        assertTrue(scope.crossesEffectiveBoundary)
    }

    private fun splitPlan(
        firstRate: TariffRateSet,
        secondRate: TariffRateSet,
        firstHourlyRate: BigDecimal = BigDecimal("300.00"),
        secondHourlyRate: BigDecimal = BigDecimal("310.00"),
        blocks: List<WorkBlock>,
    ): SegmentedTariffCalculationPlan {
        val tariff = tariffPackage()
        val firstSalary = salary("salary-a", tariff.id, LocalDate.of(2030, 6, 30))
        val secondSalary = salary("salary-b", tariff.id, LocalDate.of(2030, 7, 1))
        val firstSegment = TariffCalculationSegment(
            start = LocalDate.of(2030, 6, 30),
            end = LocalDate.of(2030, 6, 30),
            tariffPackage = tariff,
            rateSet = firstRate,
            salaryTable = firstSalary,
        )
        val secondSegment = TariffCalculationSegment(
            start = LocalDate.of(2030, 7, 1),
            end = LocalDate.of(2030, 7, 1),
            tariffPackage = tariff,
            rateSet = secondRate,
            salaryTable = secondSalary,
        )
        val boundary = LocalDateTime.of(2030, 7, 1, 0, 0)
        val tripStart = LocalDateTime.of(2030, 6, 30, 22, 0)
        val tripEnd = LocalDateTime.of(2030, 7, 1, 8, 0)

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
                    dates = listOf(LocalDate.of(2030, 6, 30)),
                    annualSalary = BigDecimal("585000"),
                    hourlyRate = firstHourlyRate,
                    workBlocks = clipped(tripStart, boundary),
                ),
                TariffCalculationSlice(
                    segment = secondSegment,
                    windowStart = boundary,
                    windowEnd = tripEnd,
                    dates = listOf(LocalDate.of(2030, 7, 1)),
                    annualSalary = BigDecimal("620000"),
                    hourlyRate = secondHourlyRate,
                    workBlocks = clipped(boundary, tripEnd),
                ),
            ),
        )
    }

    private fun tariffPackage(): TariffPackage = TariffPackage(
        id = "tariff-a",
        label = "Tariff A",
        effectiveFrom = LocalDate.of(2030, 1, 1),
        effectiveTo = LocalDate.of(2030, 12, 31),
        rulesetVersion = "R1",
        sourceLabel = "test",
        sourcePageUrl = "https://example.invalid/tariff",
    )

    private fun baseRate(id: String, date: LocalDate): TariffRateSet =
        FerieturTariffRates.dok25_2026_2028.copy(
            id = id,
            tariffPackageId = "tariff-a",
            effectiveFrom = date,
            effectiveTo = date,
        )

    private fun salary(id: String, tariffPackageId: String, date: LocalDate): SalaryTableDescriptor =
        SalaryTableDescriptor(
            id = id,
            effectiveFrom = date,
            verifiedThrough = date,
            tariffPackageId = tariffPackageId,
            sourceLabel = "test",
            sourcePageUrl = "https://example.invalid/$id",
        )
}
