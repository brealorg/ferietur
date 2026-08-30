package app.ferietur.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffResolutionTest {
    @Test
    fun resolverReturnsOneCoherentTariffRateAndSalaryContext() {
        val start = LocalDate.of(2026, 8, 10)
        val end = LocalDate.of(2026, 8, 16)
        val resolved = FerieturTariffResolver.resolveRange(start, end)
        requireNotNull(resolved)

        assertEquals(FerieturTariffs.DOK25_2026_2028_ID, resolved.tariffPackage.id)
        assertEquals(FerieturTariffRates.DOK25_2026_2028_RATE_SET_ID, resolved.rateSet.id)
        assertEquals(OsloSalaryTable2026.tableId, resolved.salaryTable.id)
        assertEquals(resolved.tariffPackage.id, resolved.rateSet.tariffPackageId)
        assertEquals(resolved.tariffPackage.id, resolved.salaryTable.tariffPackageId)
    }

    @Test
    fun resolverIsInclusiveAtCurrentVerifiedEdgesAndFailsClosedOutsideThem() {
        val first = LocalDate.of(2026, 5, 1)
        val last = LocalDate.of(2027, 4, 30)

        assertTrue(FerieturTariffResolver.supportsRange(first, first))
        assertTrue(FerieturTariffResolver.supportsRange(last, last))
        assertFalse(FerieturTariffResolver.supportsRange(first.minusDays(1), first))
        assertFalse(FerieturTariffResolver.supportsRange(last, last.plusDays(1)))
        assertNull(FerieturTariffResolver.resolveRange(last, last.plusDays(1)))
    }
}
