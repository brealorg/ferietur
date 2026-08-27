package app.ferietur.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsloSalaryTable2026Test {
    @Test
    fun salaryTableContainsOfficial2026Step32() {
        assertEquals("614600", OsloSalaryTable2026.annualSalary(32).toPlainString())
        assertEquals(1, OsloSalaryTable2026.minStep)
        assertEquals(80, OsloSalaryTable2026.maxStep)
        assertTrue(OsloSalaryTable2026.contains(1))
        assertFalse(OsloSalaryTable2026.contains(81))
    }
}
