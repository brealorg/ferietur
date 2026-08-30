package app.ferietur.domain

import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffSegmentCalculationTest {
    @Test
    fun tripEndAtMidnightDoesNotConsumeTheNewEffectiveDate() {
        val range = TariffEffectiveDateRange.forTrip(
            LocalDateTime.of(2030, 6, 30, 23, 0),
            LocalDateTime.of(2030, 7, 1, 0, 0),
        )

        assertEquals(LocalDate.of(2030, 6, 30), range?.start)
        assertEquals(LocalDate.of(2030, 6, 30), range?.end)

        val oneMinuteLater = TariffEffectiveDateRange.forTrip(
            LocalDateTime.of(2030, 6, 30, 23, 0),
            LocalDateTime.of(2030, 7, 1, 0, 1),
        )
        assertEquals(LocalDate.of(2030, 7, 1), oneMinuteLater?.end)
    }

    @Test
    fun invalidTripRangeHasNoEffectiveDateRange() {
        val instant = LocalDateTime.of(2030, 7, 1, 0, 0)
        assertNull(TariffEffectiveDateRange.forTrip(instant, instant))
        assertNull(TariffEffectiveDateRange.forTrip(instant, instant.minusMinutes(1)))
    }

    @Test
    fun crossMidnightBlockIsClippedAtEffectiveBoundaryWithoutLostMinutes() {
        val fixture = splitFixture()
        val original = WorkBlock(
            start = LocalDateTime.of(2030, 6, 30, 23, 0),
            end = LocalDateTime.of(2030, 7, 1, 7, 0),
            kind = TimeKind.ACTIVE_NIGHT_WATCH,
        )

        val result = fixture.builder.build(
            segmentation = fixture.segmentation,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            tripStart = LocalDateTime.of(2030, 6, 30, 22, 0),
            tripEnd = LocalDateTime.of(2030, 7, 1, 8, 0),
            workBlocks = listOf(original),
        ) as TariffCalculationPlanResult.Success

        val plan = result.plan
        assertTrue(plan.isSplit)
        assertEquals(listOf(LocalDateTime.of(2030, 7, 1, 0, 0)), plan.boundaryInstants)
        assertEquals(2, plan.slices.size)

        val first = plan.slices[0].workBlocks.single()
        val second = plan.slices[1].workBlocks.single()
        assertEquals(0, first.sourceIndex)
        assertEquals(0, second.sourceIndex)
        assertEquals(LocalDateTime.of(2030, 6, 30, 23, 0), first.block.start)
        assertEquals(LocalDateTime.of(2030, 7, 1, 0, 0), first.block.end)
        assertEquals(LocalDateTime.of(2030, 7, 1, 0, 0), second.block.start)
        assertEquals(LocalDateTime.of(2030, 7, 1, 7, 0), second.block.end)

        val splitMinutes = plan.slices.sumOf { slice ->
            slice.workBlocks.sumOf { Duration.between(it.block.start, it.block.end).toMinutes() }
        }
        assertEquals(Duration.between(original.start, original.end).toMinutes(), splitMinutes)
    }

    @Test
    fun clippingPreservesKindAndTravelNoticeProvenance() {
        val fixture = splitFixture()
        val travel = WorkBlock(
            start = LocalDateTime.of(2030, 6, 30, 23, 30),
            end = LocalDateTime.of(2030, 7, 1, 0, 30),
            kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            travelNoticeStatus = TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
        )

        val plan = (fixture.builder.build(
            segmentation = fixture.segmentation,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            tripStart = LocalDateTime.of(2030, 6, 30, 23, 0),
            tripEnd = LocalDateTime.of(2030, 7, 1, 1, 0),
            workBlocks = listOf(travel),
        ) as TariffCalculationPlanResult.Success).plan

        assertEquals(2, plan.slices.size)
        plan.slices.forEach { slice ->
            val clipped = slice.workBlocks.single()
            assertEquals(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP, clipped.block.kind)
            assertEquals(TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY, clipped.block.travelNoticeStatus)
            assertEquals(0, clipped.sourceIndex)
        }
    }

    @Test
    fun eachSliceUsesItsOwnSalaryTableAndRateSetForHourlyRate() {
        val fixture = splitFixture()
        val plan = (fixture.builder.build(
            segmentation = fixture.segmentation,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            tripStart = LocalDateTime.of(2030, 6, 30, 12, 0),
            tripEnd = LocalDateTime.of(2030, 7, 1, 12, 0),
            workBlocks = emptyList(),
        ) as TariffCalculationPlanResult.Success).plan

        assertEquals(BigDecimal("585000"), plan.slices[0].annualSalary)
        assertEquals(BigDecimal("300.00"), plan.slices[0].hourlyRate)
        assertEquals(BigDecimal("620000"), plan.slices[1].annualSalary)
        assertEquals(BigDecimal("310.00"), plan.slices[1].hourlyRate)
    }

    @Test
    fun missingSalaryForOneSegmentFailsClosed() {
        val fixture = splitFixture(
            salaryLookup = mapOf("salary-a" to BigDecimal("585000")),
        )
        val result = fixture.builder.build(
            segmentation = fixture.segmentation,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            tripStart = LocalDateTime.of(2030, 6, 30, 12, 0),
            tripEnd = LocalDateTime.of(2030, 7, 1, 12, 0),
            workBlocks = emptyList(),
        ) as TariffCalculationPlanResult.Failure

        assertEquals(TariffCalculationPlanFailureReason.SALARY_NOT_AVAILABLE, result.reason)
        assertEquals(LocalDate.of(2030, 7, 1), result.date)
    }

    @Test
    fun workBlockOutsideTripFailsClosedInsteadOfBeingSilentlyTrimmed() {
        val fixture = splitFixture()
        val result = fixture.builder.build(
            segmentation = fixture.segmentation,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            tripStart = LocalDateTime.of(2030, 6, 30, 22, 0),
            tripEnd = LocalDateTime.of(2030, 7, 1, 8, 0),
            workBlocks = listOf(
                WorkBlock(
                    start = LocalDateTime.of(2030, 6, 30, 21, 59),
                    end = LocalDateTime.of(2030, 6, 30, 23, 0),
                    kind = TimeKind.ACTIVE_WORK,
                ),
            ),
        ) as TariffCalculationPlanResult.Failure

        assertEquals(TariffCalculationPlanFailureReason.WORK_BLOCK_OUTSIDE_TRIP, result.reason)
    }

    @Test
    fun segmentationMustMatchTheDatesActuallyOccupiedByTheTrip() {
        val fixture = splitFixture()
        val result = fixture.builder.build(
            segmentation = fixture.segmentation,
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_37_5,
            tripStart = LocalDateTime.of(2030, 6, 30, 23, 0),
            tripEnd = LocalDateTime.of(2030, 7, 1, 0, 0),
            workBlocks = emptyList(),
        ) as TariffCalculationPlanResult.Failure

        assertEquals(TariffCalculationPlanFailureReason.SEGMENT_RANGE_MISMATCH, result.reason)
    }

    private data class Fixture(
        val segmentation: TariffSegmentationResult.Success,
        val builder: TariffCalculationSliceBuilder,
    )

    private fun splitFixture(
        salaryLookup: Map<String, BigDecimal> = mapOf(
            "salary-a" to BigDecimal("585000"),
            "salary-b" to BigDecimal("620000"),
        ),
    ): Fixture {
        val tariff = TariffPackage(
            id = "tariff-a",
            label = "Tariff A",
            effectiveFrom = LocalDate.of(2030, 1, 1),
            effectiveTo = LocalDate.of(2030, 12, 31),
            rulesetVersion = "R1",
            sourceLabel = "test",
            sourcePageUrl = "https://example.invalid/tariff-a",
        )
        val firstRate = FerieturTariffRates.dok25_2026_2028.copy(
            id = "rate-a",
            tariffPackageId = tariff.id,
            effectiveFrom = LocalDate.of(2030, 1, 1),
            effectiveTo = LocalDate.of(2030, 6, 30),
        )
        val secondRate = FerieturTariffRates.dok25_2026_2028.copy(
            id = "rate-b",
            tariffPackageId = tariff.id,
            effectiveFrom = LocalDate.of(2030, 7, 1),
            effectiveTo = LocalDate.of(2030, 12, 31),
            weeklyDivisors = FerieturTariffRates.dok25_2026_2028.weeklyDivisors +
                (WeeklyBasis.HOURS_37_5 to 2000),
        )
        val firstSalary = SalaryTableDescriptor(
            id = "salary-a",
            effectiveFrom = LocalDate.of(2030, 1, 1),
            verifiedThrough = LocalDate.of(2030, 6, 30),
            tariffPackageId = tariff.id,
            sourceLabel = "test",
            sourcePageUrl = "https://example.invalid/salary-a",
        )
        val secondSalary = SalaryTableDescriptor(
            id = "salary-b",
            effectiveFrom = LocalDate.of(2030, 7, 1),
            verifiedThrough = LocalDate.of(2030, 12, 31),
            tariffPackageId = tariff.id,
            sourceLabel = "test",
            sourcePageUrl = "https://example.invalid/salary-b",
        )
        val segmentation = TariffSegmentationResult.Success(
            rulesetVersion = tariff.rulesetVersion,
            segments = listOf(
                TariffCalculationSegment(
                    start = LocalDate.of(2030, 6, 30),
                    end = LocalDate.of(2030, 6, 30),
                    tariffPackage = tariff,
                    rateSet = firstRate,
                    salaryTable = firstSalary,
                ),
                TariffCalculationSegment(
                    start = LocalDate.of(2030, 7, 1),
                    end = LocalDate.of(2030, 7, 1),
                    tariffPackage = tariff,
                    rateSet = secondRate,
                    salaryTable = secondSalary,
                ),
            ),
        )
        val builder = TariffCalculationSliceBuilder { _, tableId -> salaryLookup[tableId] }
        return Fixture(segmentation, builder)
    }
}
