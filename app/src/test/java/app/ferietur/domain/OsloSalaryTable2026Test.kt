package app.ferietur.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsloSalaryTable2026Test {
    @Test
    fun salaryTableContainsOfficial2026Step32AndVersionMetadata() {
        assertEquals("614600", OsloSalaryTable2026.annualSalary(32).toPlainString())
        assertEquals(1, OsloSalaryTable2026.minStep)
        assertEquals(80, OsloSalaryTable2026.maxStep)
        assertTrue(OsloSalaryTable2026.contains(1))
        assertFalse(OsloSalaryTable2026.contains(81))
        assertEquals(LocalDate.of(2026, 5, 1), OsloSalaryTable2026.effectiveFromDate)
        assertEquals(LocalDate.of(2027, 4, 30), OsloSalaryTable2026.verifiedThroughDate)
        assertEquals(FerieturTariffs.DOK25_2026_2028_ID, OsloSalaryTable2026.tariffPackageId)
    }
}
