package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffRuntimeCalculationTest {
    @Test
    fun currentSingleContextRuntimeIsExactlyEquivalentToExistingEngine() {
        val date = LocalDate.of(2026, 8, 10)
        val endDate = date.plusDays(1)
        val dates = listOf(date, endDate)
        val plans = mapOf(
            date to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                PlannedBlock(
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
                    LocalTime.of(10, 0),
                    LocalTime.of(11, 0),
                    TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
                ),
            ),
            endDate to emptyList(),
        )
        val start = date.atTime(8, 0)
        val end = endDate.atTime(12, 0)
        val annualSalary = OsloSalaryTable2026.annualSalary(32)

        val expected = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = emptyMap(),
            plans = plans,
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = start,
            tripEnd = end,
            rateSet = FerieturTariffRates.current,
        )

        val result = runtimeSuccess(
            dates = dates,
            plans = plans,
            tripStart = start,
            tripEnd = end,
        )
        val actual = result.calculation as TariffRuntimeCalculation.SingleContext

        assertEquals(TariffRuntimeCalculationMode.SINGLE_CONTEXT, actual.mode)
        assertEquals(expected, actual.preliminary)
        assertEquals(expected.paymentBasisAmount, actual.paymentBasisAmount)
        assertEquals(1, actual.provenance.size)
        assertEquals(FerieturTariffs.DOK25_2026_2028_ID, actual.provenance.single().tariffPackageId)
        assertEquals(FerieturTariffRates.DOK25_2026_2028_RATE_SET_ID, actual.provenance.single().tariffRateSetId)
        assertEquals(OsloSalaryTable2026.tableId, actual.provenance.single().salaryTableId)
        assertEquals(annualSalary, actual.provenance.single().annualSalary)
    }

    @Test
    fun runtimeUsesHalfOpenEffectiveDateWhenTripEndsExactlyAtMidnight() {
        val first = LocalDate.of(2027, 4, 30)
        val second = LocalDate.of(2027, 5, 1)
        val start = first.atTime(8, 0)
        val end = second.atStartOfDay()
        val dates = listOf(first, second)
        val plans = mapOf(
            first to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(9, 0))),
            second to emptyList(),
        )

        val result = runtimeSuccess(
            dates = dates,
            plans = plans,
            tripStart = start,
            tripEnd = end,
        )
        val calculation = result.calculation as TariffRuntimeCalculation.SingleContext

        assertEquals(LocalDate.of(2027, 4, 30), calculation.provenance.single().end)
        assertEquals(OsloSalaryTable2026.tableId, calculation.provenance.single().salaryTableId)
    }

    @Test
    fun runtimeFailsClosedOneMinuteIntoUnsupportedSalaryDate() {
        val first = LocalDate.of(2027, 4, 30)
        val second = LocalDate.of(2027, 5, 1)
        val result = FerieturTariffRuntimeCalculator.calculate(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(first, second),
            roster = emptyMap(),
            plans = mapOf(first to emptyList(), second to emptyList()),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = first.atTime(8, 0),
            tripEnd = second.atTime(0, 1),
        ) as TariffRuntimeCalculationResult.Failure

        assertEquals(TariffRuntimeCalculationFailureReason.SEGMENTATION_FAILED, result.reason)
        assertEquals(TariffSegmentationFailureReason.UNSUPPORTED_DATE, result.segmentationReason)
        assertEquals(second, result.date)
    }

    @Test
    fun runtimeFailsClosedOnUnintendedOverlapBeforeTariffMoneyIsCalculated() {
        val date = LocalDate.of(2026, 8, 10)
        val endDate = date.plusDays(1)
        val result = FerieturTariffRuntimeCalculator.calculate(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(date, endDate),
            roster = emptyMap(),
            plans = mapOf(
                date to listOf(
                    PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                    PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(9, 0), LocalTime.of(11, 0)),
                ),
                endDate to emptyList(),
            ),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = date.atTime(8, 0),
            tripEnd = endDate.atTime(12, 0),
        ) as TariffRuntimeCalculationResult.Failure

        assertEquals(TariffRuntimeCalculationFailureReason.WORK_BLOCK_OVERLAP, result.reason)
    }

    @Test
    fun alreadyVerifiedSplitPlanReturnsSegmentedRuntimeMoneyAndProvenance() {
        val plan = splitPlan(
            blocks = listOf(
                WorkBlock(firstDate.atTime(8, 0), firstDate.atTime(9, 0), TimeKind.ACTIVE_WORK),
                WorkBlock(secondDate.atTime(8, 0), secondDate.atTime(9, 0), TimeKind.ACTIVE_WORK),
            ),
        )

        val result = FerieturTariffRuntimeCalculator.calculatePlan(
            plan = plan,
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(firstDate, secondDate),
            roster = emptyMap(),
            weekendProfile = WeekendProfile.STANDARD,
        ) as TariffRuntimeCalculationResult.Success
        val calculation = result.calculation as TariffRuntimeCalculation.SegmentedContexts

        assertEquals(TariffRuntimeCalculationMode.SEGMENTED_CONTEXTS, calculation.mode)
        assertEquals(2, calculation.provenance.size)
        assertEquals(listOf("runtime-rate-a", "runtime-rate-b"), calculation.provenance.map { it.tariffRateSetId })
        assertEquals(listOf("runtime-salary-a", "runtime-salary-b"), calculation.provenance.map { it.salaryTableId })
        assertEquals(listOf(BigDecimal("300.00"), BigDecimal("400.00")), calculation.provenance.map { it.hourlyRate })
        assertEquals(BigDecimal("810.00"), calculation.paymentBasisAmount)
    }

    @Test
    fun segmentedRuntimeMapsUnsupportedShortNoticeCoordinationToTypedFailure() {
        val plan = splitPlan(
            blocks = listOf(
                WorkBlock(
                    secondDate.atTime(8, 0),
                    secondDate.atTime(9, 0),
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
                    TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
                ),
            ),
        )

        val result = FerieturTariffRuntimeCalculator.calculatePlan(
            plan = plan,
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(firstDate, secondDate),
            roster = emptyMap(),
            weekendProfile = WeekendProfile.STANDARD,
        ) as TariffRuntimeCalculationResult.Failure

        assertEquals(TariffRuntimeCalculationFailureReason.MONETARY_COORDINATION_FAILED, result.reason)
        assertEquals(TariffSegmentedMonetaryFailureReason.SPLIT_TRAVEL_NOTICE_COORDINATION_REQUIRED, result.monetaryReason)
        assertEquals(0, result.sourceIndex)
    }

    private fun runtimeSuccess(
        dates: List<LocalDate>,
        plans: Map<LocalDate, List<PlannedBlock>>,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): TariffRuntimeCalculationResult.Success =
        FerieturTariffRuntimeCalculator.calculate(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = emptyMap(),
            plans = plans,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = tripStart,
            tripEnd = tripEnd,
        ) as TariffRuntimeCalculationResult.Success

    private val firstDate = LocalDate.of(2031, 6, 30)
    private val secondDate = LocalDate.of(2031, 7, 1)
    private val boundary = secondDate.atStartOfDay()

    private fun splitPlan(blocks: List<WorkBlock>): SegmentedTariffCalculationPlan {
        val tariff = TariffPackage(
            id = FerieturTariffs.DOK25_2026_2028_ID,
            label = "Runtime test",
            effectiveFrom = firstDate,
            effectiveTo = secondDate,
            rulesetVersion = FerieturTariffs.DOK25_2026_2028_RULESET_VERSION,
            sourceLabel = "test",
            sourcePageUrl = "https://example.invalid/runtime-tariff",
        )
        val firstRate = FerieturTariffRates.current.copy(
            id = "runtime-rate-a",
            effectiveFrom = firstDate,
            effectiveTo = firstDate,
        )
        val secondRate = FerieturTariffRates.current.copy(
            id = "runtime-rate-b",
            effectiveFrom = secondDate,
            effectiveTo = secondDate,
        )
        val firstSalary = SalaryTableDescriptor(
            id = "runtime-salary-a",
            effectiveFrom = firstDate,
            verifiedThrough = firstDate,
            tariffPackageId = tariff.id,
            sourceLabel = "salary A",
            sourcePageUrl = "https://example.invalid/runtime-salary-a",
        )
        val secondSalary = SalaryTableDescriptor(
            id = "runtime-salary-b",
            effectiveFrom = secondDate,
            verifiedThrough = secondDate,
            tariffPackageId = tariff.id,
            sourceLabel = "salary B",
            sourcePageUrl = "https://example.invalid/runtime-salary-b",
        )

        fun clipped(sliceStart: LocalDateTime, sliceEnd: LocalDateTime): List<SegmentedWorkBlock> =
            blocks.mapIndexedNotNull { index, source ->
                val start = maxOf(source.start, sliceStart)
                val end = minOf(source.end, sliceEnd)
                if (!end.isAfter(start)) null else SegmentedWorkBlock(index, source.copy(start = start, end = end))
            }

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
                    workBlocks = clipped(firstDate.atTime(8, 0), boundary),
                ),
                TariffCalculationSlice(
                    segment = TariffCalculationSegment(secondDate, secondDate, tariff, secondRate, secondSalary),
                    windowStart = boundary,
                    windowEnd = secondDate.atTime(10, 0),
                    dates = listOf(secondDate),
                    annualSalary = BigDecimal("780000"),
                    hourlyRate = BigDecimal("400.00"),
                    workBlocks = clipped(boundary, secondDate.atTime(10, 0)),
                ),
            ),
        )
    }
}
