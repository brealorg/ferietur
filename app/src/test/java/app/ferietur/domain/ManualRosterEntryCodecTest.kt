package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class ManualRosterEntryCodecTest {
    @Test
    fun roundTripKeepsUserDefinedCodesAndMultipleIntervals() {
        val shifts = listOf(
            RosterEntryCodec.manualWork("D1", LocalTime.of(7, 0), LocalTime.of(14, 30)),
            RosterEntryCodec.manualWork("K1", LocalTime.of(18, 0), LocalTime.of(20, 15)),
        )

        val encoded = RosterEntryCodec.encode(shifts)
        val restored = RosterEntryCodec.decode(encoded)

        assertTrue(RosterEntryCodec.isManual(encoded))
        assertEquals(listOf("D1", "K1"), restored.map { it.code })
        assertEquals(LocalTime.of(7, 0), restored[0].start)
        assertEquals(LocalTime.of(20, 15), restored[1].end)
    }

    @Test
    fun freeDayKeepsMandatoryLocalCodeAndWeeklyOffMeaning() {
        val encoded = RosterEntryCodec.encode(
            listOf(RosterEntryCodec.manualFree("F1", weeklyOff = true)),
        )
        val restored = RosterEntryCodec.decode(encoded).single()

        assertEquals("F1", restored.code)
        assertEquals(ShiftCategory.OFF, restored.category)
        assertTrue(restored.weeklyOff)
    }

    @Test
    fun legacyCatalogueValuesRemainReadableButAreNotManual() {
        val legacy = RosterEntryCodec.decode("D1").single()

        assertFalse(RosterEntryCodec.isManual("D1"))
        assertEquals("D1", legacy.code)
        assertEquals(LocalTime.of(7, 0), legacy.start)
    }

    @Test
    fun planFromRosterUsesEveryManualIntervalOnTheDay() {
        val date = LocalDate.of(2026, 8, 11)
        val roster = mapOf(
            date to RosterEntryCodec.encode(
                listOf(
                    RosterEntryCodec.manualWork("D", LocalTime.of(7, 0), LocalTime.of(14, 0)),
                    RosterEntryCodec.manualWork("K", LocalTime.of(18, 0), LocalTime.of(21, 0)),
                ),
            ),
        )

        val blocks = TripPlanEngine.planFromRoster(listOf(date), roster).getValue(date)

        assertEquals(2, blocks.size)
        assertEquals(LocalTime.of(7, 0), blocks[0].start)
        assertEquals(LocalTime.of(21, 0), blocks[1].end)
    }

    @Test
    fun freeDayTariffMeaningDoesNotDependOnLocalCode() {
        val codeF1ButOtherDay = RosterEntryCodec.manualFree("F1", weeklyOff = false)
        val arbitraryCodeButWeeklyOff = RosterEntryCodec.manualFree("X7", weeklyOff = true)

        assertFalse(codeF1ButOtherDay.weeklyOff)
        assertEquals("Annen fridag", codeF1ButOtherDay.label)
        assertTrue(arbitraryCodeButWeeklyOff.weeklyOff)
        assertEquals("Ukentlig fridag", arbitraryCodeButWeeklyOff.label)
    }

    @Test
    fun newManualEntriesRejectShiftCodesLongerThanThreeCharacters() {
        assertEquals(3, RosterEntryCodec.MAX_CODE_LENGTH)

        assertThrows(IllegalArgumentException::class.java) {
            RosterEntryCodec.manualWork(
                "D123",
                LocalTime.of(7, 0),
                LocalTime.of(15, 0),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            RosterEntryCodec.manualFree("F123", weeklyOff = false)
        }
    }

    @Test
    fun preR2ManualEntryWithLongCodeRemainsReadableForEditing() {
        val restored = RosterEntryCodec.decode(
            "MR1;W,vaktmeduvaliglangtnavn,07:00,15:00",
        ).single()

        assertEquals("vaktmeduvaliglangtnavn", restored.code)
        assertEquals(LocalTime.of(7, 0), restored.start)
        assertEquals(LocalTime.of(15, 0), restored.end)
    }
}
