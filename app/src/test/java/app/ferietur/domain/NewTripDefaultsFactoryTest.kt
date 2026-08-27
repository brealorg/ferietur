package app.ferietur.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewTripDefaultsFactoryTest {
    @Test
    fun newTripUsesClockDateAndBlankTitle() {
        val defaults = NewTripDefaultsFactory.create(
            Clock.fixed(Instant.parse("2026-08-25T08:00:00Z"), ZoneOffset.UTC),
        )
        assertEquals("", defaults.title)
        assertEquals(LocalDate.of(2026, 8, 25), defaults.startDate)
        assertEquals(LocalDate.of(2026, 8, 31), defaults.endDate)
        assertTrue(OsloSalaryTable2026.contains(defaults.provisionalSalaryStep))
    }

    @Test
    fun newTripNeverStartsBeforeEarliestSupportedSalaryDate() {
        val defaults = NewTripDefaultsFactory.create(
            Clock.fixed(Instant.parse("2026-04-20T08:00:00Z"), ZoneOffset.UTC),
        )
        assertEquals(OsloSalaryTables.earliestSupportedDate, defaults.startDate)
        assertEquals(defaults.startDate.plusDays(6), defaults.endDate)
    }
}
