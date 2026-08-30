package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OsloSalaryTablesTest {
    private val mayFirst = LocalDate.of(2026, 5, 1)
    private val verifiedThrough = LocalDate.of(2027, 4, 30)

    @Test
    fun supportedWindowUsesTypedVersionMetadata() {
        assertEquals(mayFirst, OsloSalaryTable2026.effectiveFromDate)
        assertEquals(mayFirst, OsloSalaryTables.earliestSupportedDate)
        assertEquals(verifiedThrough, OsloSalaryTable2026.verifiedThroughDate)
        assertEquals(verifiedThrough, OsloSalaryTables.latestSupportedDate)
    }

    @Test
    fun dayBeforeEffectiveDateIsUnsupported() {
        assertNull(OsloSalaryTables.descriptorForDate(mayFirst.minusDays(1)))
        assertFalse(
            OsloSalaryTables.supportsRange(
                mayFirst.minusDays(1),
                mayFirst.plusDays(1),
            ),
        )
    }

    @Test
    fun effectiveDateAndLaterUseKnownTableInsideVerifiedWindow() {
        val descriptor = OsloSalaryTables.descriptorForRange(
            mayFirst,
            mayFirst.plusDays(7),
        )
        requireNotNull(descriptor)
        assertEquals("oslo-salary-2026-05-01", descriptor.id)
        assertEquals(mayFirst, descriptor.effectiveFrom)
        assertEquals(verifiedThrough, descriptor.verifiedThrough)
        assertEquals(FerieturTariffs.DOK25_2026_2028_ID, descriptor.tariffPackageId)
        assertEquals(
            "614600",
            OsloSalaryTables.annualSalaryForRange(
                32,
                mayFirst,
                mayFirst.plusDays(7),
            )?.toPlainString(),
        )
    }

    @Test
    fun dayAfterVerifiedWindowIsUnsupportedUntilNewTableIsAdded() {
        assertNull(OsloSalaryTables.descriptorForDate(verifiedThrough.plusDays(1)))
        assertFalse(
            OsloSalaryTables.supportsRange(
                verifiedThrough,
                verifiedThrough.plusDays(1),
            ),
        )
    }

    @Test
    fun unsupportedRangeCannotPassHardCalculationGuard() {
        assertThrows(IllegalArgumentException::class.java) {
            OsloSalaryTables.requireSupportedRange(
                mayFirst.minusDays(1),
                mayFirst,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            OsloSalaryTables.requireSupportedRange(
                verifiedThrough,
                verifiedThrough.plusDays(1),
            )
        }
    }

    @Test
    fun validRangePassesHardCalculationGuard() {
        assertTrue(
            OsloSalaryTables.requireSupportedRange(
                mayFirst,
                mayFirst.plusMonths(1),
            ).id.isNotBlank(),
        )
    }
    @Test
    fun genericCatalogAcceptsFutureTableButRefusesToBlendOneTripAcrossBoundary() {
        val firstDescriptor = SalaryTableDescriptor(
            id = "salary-a",
            effectiveFrom = LocalDate.of(2030, 5, 1),
            verifiedThrough = LocalDate.of(2031, 4, 30),
            tariffPackageId = "tariff-x",
            sourceLabel = "A",
            sourcePageUrl = "https://example.invalid/a",
        )
        val secondDescriptor = SalaryTableDescriptor(
            id = "salary-b",
            effectiveFrom = LocalDate.of(2031, 5, 1),
            verifiedThrough = LocalDate.of(2032, 4, 30),
            tariffPackageId = "tariff-x",
            sourceLabel = "B",
            sourcePageUrl = "https://example.invalid/b",
        )
        val catalog = SalaryTableCatalog(
            listOf(
                SalaryTablePeriod(firstDescriptor) { BigDecimal("700000") },
                SalaryTablePeriod(secondDescriptor) { BigDecimal("710000") },
            ),
        )

        assertEquals("salary-a", catalog.descriptorForDate(firstDescriptor.verifiedThrough)?.id)
        assertEquals("salary-b", catalog.descriptorForDate(secondDescriptor.effectiveFrom)?.id)
        assertEquals("700000", catalog.annualSalaryForDate(1, firstDescriptor.verifiedThrough)?.toPlainString())
        assertEquals("710000", catalog.annualSalaryForDate(1, secondDescriptor.effectiveFrom)?.toPlainString())
        assertEquals("710000", catalog.annualSalaryForRange(1, secondDescriptor.effectiveFrom, secondDescriptor.effectiveFrom)?.toPlainString())
        assertNull(catalog.descriptorForRange(firstDescriptor.verifiedThrough, secondDescriptor.effectiveFrom))
    }


    @Test
    fun currentSalaryCanBeResolvedByFrozenTableId() {
        assertEquals(
            OsloSalaryTable2026.annualSalary(32),
            OsloSalaryTables.annualSalaryForTable(32, OsloSalaryTable2026.tableId),
        )
        assertNull(OsloSalaryTables.annualSalaryForTable(32, "unknown-table"))
    }

}
