package app.ferietur.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class Step1PickerPolicyTest {
    @Test
    fun sameMonthRangeUsesCompactNorwegianLabel() {
        assertEquals(
            "11.–18. august 2026",
            tripDateRangeLabel(LocalDate.of(2026, 8, 11), LocalDate.of(2026, 8, 18)),
        )
    }

    @Test
    fun crossMonthRangeKeepsBothMonthNames() {
        assertEquals(
            "30. august–2. september 2026",
            tripDateRangeLabel(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 9, 2)),
        )
    }

    @Test
    fun crossYearRangeKeepsBothYears() {
        assertEquals(
            "30. desember 2026–2. januar 2027",
            tripDateRangeLabel(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2)),
        )
    }
    @Test
    fun pickerHeadlineUsesNorwegianCompactRange() {
        val start = LocalDate.of(2026, 8, 11).toEpochDay() * 86_400_000L
        val end = LocalDate.of(2026, 8, 18).toEpochDay() * 86_400_000L
        assertEquals("11.–18. august 2026", tripDateRangePickerHeadline(start, end))
    }

    @Test
    fun pickerHeadlineGuidesWhenOnlyStartIsSelected() {
        val start = LocalDate.of(2026, 8, 11).toEpochDay() * 86_400_000L
        assertEquals("11. august 2026 – velg sluttdato", tripDateRangePickerHeadline(start, null))
    }

}
