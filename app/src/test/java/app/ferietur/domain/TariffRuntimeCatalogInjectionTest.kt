package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffRuntimeCatalogInjectionTest {

    private val before =
        LocalDate.of(2027, 4, 30)

    private val boundary =
        LocalDate.of(2027, 5, 1)

    private val finalDay =
        LocalDate.of(2027, 5, 2)

    private fun syntheticSnapshot():
        TariffRuntimeCatalogSnapshot {

        val descriptor =
            SalaryTableDescriptor(
                id =
                    "salary-2027-a5a8-synthetic",
                effectiveFrom =
                    boundary,
                verifiedThrough =
                    LocalDate.of(
                        2028,
                        4,
                        30,
                    ),
                tariffPackageId =
                    FerieturTariffs
                        .DOK25_2026_2028_ID,
                sourceLabel =
                    "A5A8 syntetisk testlønn",
                sourcePageUrl =
                    "https://example.invalid/a5a8",
            )

        val period =
            SalaryTablePeriod(
                descriptor = descriptor,
                annualSalary = { step ->
                    OsloSalaryTable2026
                        .annualSalary(step)
                        .add(BigDecimal("10000"))
                },
            )

        return TariffRuntimeCatalogSnapshot(
            FerieturRuntimeCatalogSnapshots
                .current
                .payloads() +
                SalaryTableRuntimePayload(period),
        )
    }

    private fun boundaryPlans():
        Map<LocalDate, List<PlannedBlock>> =
        mapOf(
            before to listOf(
                PlannedBlock(
                    TimeKind.ACTIVE_WORK,
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                ),
            ),
            boundary to listOf(
                PlannedBlock(
                    TimeKind.ACTIVE_WORK,
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                ),
            ),
            finalDay to emptyList(),
        )

    @Test
    fun injectedGlobalViewMatchesProductionRuntimeExactly() {
        val first =
            LocalDate.of(2026, 8, 10)

        val second =
            first.plusDays(1)

        val dates =
            listOf(
                first,
                second,
            )

        val plans =
            mapOf(
                first to listOf(
                    PlannedBlock(
                        TimeKind.ACTIVE_WORK,
                        LocalTime.of(8, 0),
                        LocalTime.of(10, 0),
                    ),
                ),
                second to emptyList(),
            )

        val production =
            FerieturTariffRuntimeCalculator
                .calculate(
                    fundingMode =
                        FundingMode.VACATION_SEPARATE,
                    dates = dates,
                    roster = emptyMap(),
                    plans = plans,
                    salaryStep = 32,
                    weeklyBasis =
                        WeeklyBasis.HOURS_37_5,
                    weekendProfile =
                        WeekendProfile.STANDARD,
                    tripStart =
                        first.atTime(8, 0),
                    tripEnd =
                        second.atTime(12, 0),
                )

        val injected =
            FerieturTariffRuntimeCalculator
                .calculateWithCatalog(
                    catalog =
                        FerieturGlobalRuntimeCatalogView,
                    fundingMode =
                        FundingMode.VACATION_SEPARATE,
                    dates = dates,
                    roster = emptyMap(),
                    plans = plans,
                    salaryStep = 32,
                    weeklyBasis =
                        WeeklyBasis.HOURS_37_5,
                    weekendProfile =
                        WeekendProfile.STANDARD,
                    tripStart =
                        first.atTime(8, 0),
                    tripEnd =
                        second.atTime(12, 0),
                )

        assertEquals(
            production,
            injected,
        )
    }

    @Test
    fun isolatedRuntimeCalculatesAcrossSyntheticSalaryBoundary() {
        val result =
            FerieturTariffRuntimeCalculator
                .calculateWithCatalog(
                    catalog =
                        TariffRuntimeCatalogSnapshotView(
                            syntheticSnapshot(),
                        ),
                    fundingMode =
                        FundingMode.VACATION_SEPARATE,
                    dates =
                        listOf(
                            before,
                            boundary,
                            finalDay,
                        ),
                    roster = emptyMap(),
                    plans =
                        boundaryPlans(),
                    salaryStep = 32,
                    weeklyBasis =
                        WeeklyBasis.HOURS_37_5,
                    weekendProfile =
                        WeekendProfile.STANDARD,
                    tripStart =
                        before.atTime(8, 0),
                    tripEnd =
                        finalDay.atTime(10, 0),
                )

        assertTrue(
            result is
                TariffRuntimeCalculationResult.Success,
        )

        val calculation =
            (result as
                TariffRuntimeCalculationResult.Success)
                .calculation as
                TariffRuntimeCalculation
                    .SegmentedContexts

        assertEquals(
            listOf(
                "614600",
                "624600",
            ),
            calculation.provenance.map {
                it.annualSalary.toPlainString()
            },
        )

        assertEquals(
            listOf(
                OsloSalaryTable2026.tableId,
                "salary-2027-a5a8-synthetic",
            ),
            calculation.provenance.map {
                it.salaryTableId
            },
        )

        assertEquals(
            calculation.provenance[0]
                .tariffPackageId,
            calculation.provenance[1]
                .tariffPackageId,
        )

        assertEquals(
            calculation.provenance[0]
                .tariffRateSetId,
            calculation.provenance[1]
                .tariffRateSetId,
        )

        val activeLines =
            calculation.segmented
                .lineEntries
                .filter {
                    it.line.id == "active"
                }

        assertEquals(
            2,
            activeLines.size,
        )

        assertEquals(
            calculation.provenance.map {
                it.hourlyRate
            },
            activeLines.map {
                it.line.amount
            },
        )
    }

    @Test
    fun syntheticCatalogNeverBecomesProductionCatalog() {
        val production =
            FerieturTariffRuntimeCalculator
                .calculate(
                    fundingMode =
                        FundingMode.VACATION_SEPARATE,
                    dates =
                        listOf(
                            before,
                            boundary,
                            finalDay,
                        ),
                    roster = emptyMap(),
                    plans =
                        boundaryPlans(),
                    salaryStep = 32,
                    weeklyBasis =
                        WeeklyBasis.HOURS_37_5,
                    weekendProfile =
                        WeekendProfile.STANDARD,
                    tripStart =
                        before.atTime(8, 0),
                    tripEnd =
                        finalDay.atTime(10, 0),
                )

        assertTrue(
            production is
                TariffRuntimeCalculationResult.Failure,
        )

        val failure =
            production as
                TariffRuntimeCalculationResult.Failure

        assertEquals(
            TariffRuntimeCalculationFailureReason
                .SEGMENTATION_FAILED,
            failure.reason,
        )

        assertEquals(
            TariffSegmentationFailureReason
                .UNSUPPORTED_DATE,
            failure.segmentationReason,
        )

        assertEquals(
            boundary,
            failure.date,
        )

        assertNull(
            OsloSalaryTables
                .descriptorForDate(boundary),
        )

        assertNull(
            FerieturTariffResolver
                .resolveRange(
                    boundary,
                    boundary,
                ),
        )
    }
}
