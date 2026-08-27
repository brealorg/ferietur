package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OsloHolidayCalendarTest {
    @Test
    fun easterSunday2026IsAprilFifth() {
        assertEquals(LocalDate.of(2026, 4, 5), OsloHolidayCalendar.easterSunday(2026))
    }

    @Test
    fun standardTurnusEasterWindowRunsFromMaundyThursdayToTuesdayMidnight() {
        val windows = OsloHolidayCalendar.holidaySupplementWindows(2026, WeeklyBasis.HOURS_35_5)
        val easter = windows.single { it.id == "easter-2026" }
        assertEquals(LocalDateTime.of(2026, 4, 2, 0, 0), easter.start)
        assertEquals(LocalDateTime.of(2026, 4, 7, 0, 0), easter.end)
    }

    @Test
    fun thirtyThreeSixEasterWindowUsesExtendedTariffWindow() {
        val windows = OsloHolidayCalendar.holidaySupplementWindows(2026, WeeklyBasis.HOURS_33_6)
        val easter = windows.single { it.id == "easter-2026" }
        assertEquals(LocalDateTime.of(2026, 4, 1, 13, 0), easter.start)
        assertEquals(LocalDateTime.of(2026, 4, 7, 7, 0), easter.end)
    }

    @Test
    fun standardChristmasWindowStartsAtNoonChristmasEve() {
        val block = WorkBlock(
            LocalDateTime.of(2026, 12, 24, 11, 0),
            LocalDateTime.of(2026, 12, 24, 13, 0),
            TimeKind.ACTIVE_WORK,
        )
        val segments = OsloHolidayCalendar.holidaySupplementSegments(block, WeeklyBasis.HOURS_35_5)
        assertEquals(1, segments.size)
        assertEquals(LocalTime.NOON, segments.single().start.toLocalTime())
        assertEquals(LocalTime.of(13, 0), segments.single().end.toLocalTime())
        assertEquals("Jul", segments.single().title)
    }

    @Test
    fun seventeenthMayIsBothHolidaySupplementAndSpecialOvertimeDate() {
        val date = LocalDate.of(2026, 5, 17)
        assertTrue(OsloHolidayCalendar.holidayLabelsForDate(date, WeeklyBasis.HOURS_35_5).contains("17. mai"))
        assertTrue(OsloHolidayCalendar.overtime133Dates(2026).containsKey(date))
        assertFalse(OsloHolidayCalendar.overtime133Dates(2026).containsKey(LocalDate.of(2026, 5, 18)))
    }
}
