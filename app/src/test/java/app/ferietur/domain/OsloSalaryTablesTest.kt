package app.ferietur.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OsloSalaryTablesTest {
    private val mayFirst = LocalDate.of(2026, 5, 1)

    @Test
    fun earliestSupportedDateIsTypedEffectiveDate() {
        assertEquals(mayFirst, OsloSalaryTable2026.effectiveFromDate)
        assertEquals(mayFirst, OsloSalaryTables.earliestSupportedDate)
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
    fun effectiveDateAndLaterUseKnownTable() {
        val descriptor = OsloSalaryTables.descriptorForRange(
            mayFirst,
            mayFirst.plusDays(7),
        )
        requireNotNull(descriptor)
        assertEquals("oslo-salary-2026-05-01", descriptor.id)
        assertEquals(mayFirst, descriptor.effectiveFrom)
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
    fun unsupportedRangeCannotPassHardCalculationGuard() {
        assertThrows(IllegalArgumentException::class.java) {
            OsloSalaryTables.requireSupportedRange(
                mayFirst.minusDays(1),
                mayFirst,
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
}
