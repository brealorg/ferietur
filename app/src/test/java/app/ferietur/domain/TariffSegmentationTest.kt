package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffSegmentationTest {
    @Test
    fun currentVerified2026RangePlansAsOneSegment() {
        val result = FerieturTariffResolver.planSegments(
            LocalDate.of(2026, 8, 10),
            LocalDate.of(2026, 8, 16),
        )
        val success = result as TariffSegmentationResult.Success

        assertFalse(success.isSplit)
        assertFalse(success.changesRateSet)
        assertFalse(success.changesSalaryTable)
        assertEquals(FerieturTariffs.DOK25_2026_2028_RULESET_VERSION, success.rulesetVersion)
        assertEquals(FerieturTariffRates.DOK25_2026_2028_RATE_SET_ID, success.segments.single().rateSet.id)
        assertEquals(OsloSalaryTable2026.tableId, success.segments.single().salaryTable.id)
    }

    @Test
    fun sameRulesetMaySplitAcrossIndependentSalaryAndRateBoundaries() {
        val tariff = packageOf(
            id = "tariff-a",
            start = date(2030, 1, 1),
            end = date(2031, 12, 31),
            ruleset = "R1",
        )
        val rates = listOf(
            rateOf("rate-a", tariff.id, date(2030, 1, 1), date(2030, 12, 31)),
            rateOf("rate-b", tariff.id, date(2031, 1, 1), date(2031, 12, 31)),
        )
        val salaries = listOf(
            salaryOf("salary-a", tariff.id, date(2030, 1, 1), date(2030, 6, 30)),
            salaryOf("salary-b", tariff.id, date(2030, 7, 1), date(2031, 12, 31)),
        )
        val result = planner(listOf(tariff), rates, salaries).planRange(
            date(2030, 6, 29),
            date(2031, 1, 2),
        )
        val success = result as TariffSegmentationResult.Success

        assertTrue(success.isSplit)
        assertTrue(success.changesRateSet)
        assertTrue(success.changesSalaryTable)
        assertEquals("R1", success.rulesetVersion)
        assertEquals(3, success.segments.size)
        assertSegment(success.segments[0], date(2030, 6, 29), date(2030, 6, 30), "rate-a", "salary-a")
        assertSegment(success.segments[1], date(2030, 7, 1), date(2030, 12, 31), "rate-a", "salary-b")
        assertSegment(success.segments[2], date(2031, 1, 1), date(2031, 1, 2), "rate-b", "salary-b")
    }

    @Test
    fun semanticRulesetChangeFailsClosedInsteadOfAutoSplitting() {
        val first = packageOf("tariff-a", date(2030, 1, 1), date(2030, 12, 31), "R1")
        val second = packageOf("tariff-b", date(2031, 1, 1), date(2031, 12, 31), "R2")
        val rates = listOf(
            rateOf("rate-a", first.id, first.effectiveFrom, first.effectiveTo),
            rateOf("rate-b", second.id, second.effectiveFrom, second.effectiveTo),
        )
        val salaries = listOf(
            salaryOf("salary-a", first.id, first.effectiveFrom, first.effectiveTo),
            salaryOf("salary-b", second.id, second.effectiveFrom, second.effectiveTo),
        )

        val result = planner(listOf(first, second), rates, salaries).planRange(
            date(2030, 12, 31),
            date(2031, 1, 1),
        ) as TariffSegmentationResult.Failure

        assertEquals(TariffSegmentationFailureReason.SEMANTIC_RULESET_CHANGE, result.reason)
        assertEquals(date(2031, 1, 1), result.date)
    }

    @Test
    fun missingCoverageFailsClosedAtFirstUnsupportedDate() {
        val tariff = packageOf("tariff-a", date(2030, 1, 1), date(2030, 12, 31), "R1")
        val rate = rateOf("rate-a", tariff.id, tariff.effectiveFrom, tariff.effectiveTo)
        val salary = salaryOf("salary-a", tariff.id, date(2030, 1, 1), date(2030, 6, 30))

        val result = planner(listOf(tariff), listOf(rate), listOf(salary)).planRange(
            date(2030, 6, 30),
            date(2030, 7, 1),
        ) as TariffSegmentationResult.Failure

        assertEquals(TariffSegmentationFailureReason.UNSUPPORTED_DATE, result.reason)
        assertEquals(date(2030, 7, 1), result.date)
    }

    @Test
    fun incoherentSalaryTariffBindingFailsClosed() {
        val tariff = packageOf("tariff-a", date(2030, 1, 1), date(2030, 12, 31), "R1")
        val rate = rateOf("rate-a", tariff.id, tariff.effectiveFrom, tariff.effectiveTo)
        val salary = salaryOf("salary-a", "other-tariff", tariff.effectiveFrom, tariff.effectiveTo)

        val result = planner(listOf(tariff), listOf(rate), listOf(salary)).planRange(
            date(2030, 3, 1),
            date(2030, 3, 2),
        ) as TariffSegmentationResult.Failure

        assertEquals(TariffSegmentationFailureReason.INCOHERENT_SOURCES, result.reason)
        assertEquals(date(2030, 3, 1), result.date)
    }

    @Test
    fun invalidRangeIsTypedFailure() {
        val result = planner(emptyList(), emptyList(), emptyList()).planRange(
            date(2030, 2, 2),
            date(2030, 2, 1),
        ) as TariffSegmentationResult.Failure

        assertEquals(TariffSegmentationFailureReason.INVALID_RANGE, result.reason)
    }

    @Test
    fun productionResolverDoesNotRequireNextSalaryTableWhenTripEndsExactlyAtMidnight() {
        val result = FerieturTariffResolver.planSegments(
            LocalDateTime.of(2027, 4, 30, 20, 0),
            LocalDateTime.of(2027, 5, 1, 0, 0),
        )
        val success = result as TariffSegmentationResult.Success

        assertEquals(1, success.segments.size)
        assertEquals(LocalDate.of(2027, 4, 30), success.segments.single().start)
        assertEquals(LocalDate.of(2027, 4, 30), success.segments.single().end)
    }

    @Test
    fun productionResolverRequiresNewSalaryCoverageOneMinuteAfterMidnight() {
        val result = FerieturTariffResolver.planSegments(
            LocalDateTime.of(2027, 4, 30, 20, 0),
            LocalDateTime.of(2027, 5, 1, 0, 1),
        ) as TariffSegmentationResult.Failure

        assertEquals(TariffSegmentationFailureReason.UNSUPPORTED_DATE, result.reason)
        assertEquals(LocalDate.of(2027, 5, 1), result.date)
    }


    private fun planner(
        packages: List<TariffPackage>,
        rates: List<TariffRateSet>,
        salaries: List<SalaryTableDescriptor>,
    ): TariffSegmentPlanner = TariffSegmentPlanner(
        packageForDate = { day -> packages.singleOrNull { it.covers(day) } },
        rateSetForDate = { packageId, day ->
            rates.singleOrNull { it.tariffPackageId == packageId && it.covers(day) }
        },
        salaryTableForDate = { day -> salaries.singleOrNull { it.covers(day) } },
    )

    private fun packageOf(
        id: String,
        start: LocalDate,
        end: LocalDate,
        ruleset: String,
    ): TariffPackage = TariffPackage(
        id = id,
        label = id,
        effectiveFrom = start,
        effectiveTo = end,
        rulesetVersion = ruleset,
        sourceLabel = "test",
        sourcePageUrl = "https://example.invalid/$id",
    )

    private fun rateOf(
        id: String,
        packageId: String,
        start: LocalDate,
        end: LocalDate,
    ): TariffRateSet = FerieturTariffRates.dok25_2026_2028.copy(
        id = id,
        tariffPackageId = packageId,
        effectiveFrom = start,
        effectiveTo = end,
    )

    private fun salaryOf(
        id: String,
        packageId: String,
        start: LocalDate,
        end: LocalDate,
    ): SalaryTableDescriptor = SalaryTableDescriptor(
        id = id,
        effectiveFrom = start,
        verifiedThrough = end,
        tariffPackageId = packageId,
        sourceLabel = "test",
        sourcePageUrl = "https://example.invalid/$id",
    )

    private fun assertSegment(
        segment: TariffCalculationSegment,
        start: LocalDate,
        end: LocalDate,
        rateSetId: String,
        salaryTableId: String,
    ) {
        assertEquals(start, segment.start)
        assertEquals(end, segment.end)
        assertEquals(rateSetId, segment.rateSet.id)
        assertEquals(salaryTableId, segment.salaryTable.id)
    }

    private fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)
}
