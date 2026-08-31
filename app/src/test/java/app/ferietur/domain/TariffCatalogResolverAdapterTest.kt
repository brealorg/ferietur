package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffCatalogResolverAdapterTest {

    private val boundary =
        LocalDate.of(2027, 5, 1)

    private val globalResolver =
        TariffCatalogResolverAdapter(
            FerieturGlobalRuntimeCatalogView,
        )

    private fun isolatedSnapshotWithNextSalary():
        TariffRuntimeCatalogSnapshot {
        val descriptor =
            SalaryTableDescriptor(
                id =
                    "salary-2027-a5a6-synthetic",
                effectiveFrom =
                    boundary,
                verifiedThrough =
                    LocalDate.of(2028, 4, 30),
                tariffPackageId =
                    FerieturTariffs
                        .DOK25_2026_2028_ID,
                sourceLabel =
                    "A5A6 syntetisk testlønn",
                sourcePageUrl =
                    "https://example.invalid/a5a6",
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

    @Test
    fun globalAdapterMatchesExistingResolverForCurrentRange() {
        val start =
            LocalDate.of(2026, 8, 10)

        val end =
            LocalDate.of(2026, 8, 16)

        val existing =
            requireNotNull(
                FerieturTariffResolver
                    .resolveRange(
                        start,
                        end,
                    ),
            )

        val adapted =
            requireNotNull(
                globalResolver.resolveRange(
                    start,
                    end,
                ),
            )

        assertEquals(
            existing,
            adapted,
        )

        assertEquals(
            OsloSalaryTable2026
                .annualSalary(32),
            globalResolver
                .annualSalaryForDate(
                    32,
                    start,
                ),
        )
    }

    @Test
    fun globalAdapterPreservesCurrentUnsupportedBoundary() {
        assertNull(
            FerieturTariffResolver
                .resolveRange(
                    boundary,
                    boundary,
                ),
        )

        assertNull(
            globalResolver.resolveDate(
                boundary,
            ),
        )

        assertNull(
            globalResolver
                .annualSalaryForDate(
                    32,
                    boundary,
                ),
        )
    }

    @Test
    fun snapshotAdapterResolvesSyntheticSalaryBoundary() {
        val adapter =
            TariffCatalogResolverAdapter(
                TariffRuntimeCatalogSnapshotView(
                    isolatedSnapshotWithNextSalary(),
                ),
            )

        val before =
            requireNotNull(
                adapter.resolveDate(
                    boundary.minusDays(1),
                ),
            )

        val after =
            requireNotNull(
                adapter.resolveDate(
                    boundary,
                ),
            )

        assertEquals(
            OsloSalaryTable2026.tableId,
            before.salaryTable.id,
        )

        assertEquals(
            "salary-2027-a5a6-synthetic",
            after.salaryTable.id,
        )

        assertEquals(
            "614600",
            adapter
                .annualSalaryForDate(
                    32,
                    boundary.minusDays(1),
                )
                ?.toPlainString(),
        )

        assertEquals(
            "624600",
            adapter
                .annualSalaryForDate(
                    32,
                    boundary,
                )
                ?.toPlainString(),
        )
    }

    @Test
    fun strictRangeRejectsSalaryBoundaryWhileSegmentPlannerAcceptsIt() {
        val adapter =
            TariffCatalogResolverAdapter(
                TariffRuntimeCatalogSnapshotView(
                    isolatedSnapshotWithNextSalary(),
                ),
            )

        val before =
            boundary.minusDays(1)

        assertNull(
            adapter.resolveRange(
                before,
                boundary,
            ),
        )

        assertTrue(
            adapter.planSegments(
                before,
                boundary,
            ) is TariffSegmentationResult.Success,
        )

        assertTrue(
            adapter.supportsSegmentedRange(
                before,
                boundary,
            ),
        )
    }

    @Test
    fun salaryBoundaryKeepsSamePackageAndRateSet() {
        val adapter =
            TariffCatalogResolverAdapter(
                TariffRuntimeCatalogSnapshotView(
                    isolatedSnapshotWithNextSalary(),
                ),
            )

        val before =
            requireNotNull(
                adapter.resolveDate(
                    boundary.minusDays(1),
                ),
            )

        val after =
            requireNotNull(
                adapter.resolveDate(
                    boundary,
                ),
            )

        assertEquals(
            before.tariffPackage.id,
            after.tariffPackage.id,
        )

        assertEquals(
            before.rateSet.id,
            after.rateSet.id,
        )

        assertFalse(
            before.salaryTable.id ==
                after.salaryTable.id,
        )
    }

    @Test
    fun isolatedAdapterDoesNotChangeGlobalResolver() {
        val isolated =
            TariffCatalogResolverAdapter(
                TariffRuntimeCatalogSnapshotView(
                    isolatedSnapshotWithNextSalary(),
                ),
            )

        assertTrue(
            isolated.resolveDate(boundary) != null,
        )

        assertNull(
            FerieturTariffResolver
                .resolveRange(
                    boundary,
                    boundary,
                ),
        )

        assertNull(
            OsloSalaryTables
                .descriptorForDate(
                    boundary,
                ),
        )
    }
}
