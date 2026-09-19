package app.ferietur.domain

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockChangePolicyTest {
    @Test
    fun nightAcrossSpringChangeIsOneHourShorterThanRegistered() {
        val periods = ClockChangePolicy.affectedPeriods(
            listOf(block(LocalDateTime.of(2026, 3, 28, 22, 0), LocalDateTime.of(2026, 3, 29, 8, 0))),
        )

        val period = periods.single()
        assertTrue(period.change.movesForward)
        assertEquals(LocalDateTime.of(2026, 3, 29, 2, 0), period.change.localTimeBefore)
        assertEquals(LocalDateTime.of(2026, 3, 29, 3, 0), period.change.localTimeAfter)
        assertEquals(600L, period.registeredMinutes)
        assertEquals(540L, period.elapsedMinutes)
    }

    @Test
    fun nightAcrossAutumnChangeIsOneHourLongerThanRegistered() {
        val period = ClockChangePolicy.affectedPeriods(
            listOf(block(LocalDateTime.of(2026, 10, 24, 22, 0), LocalDateTime.of(2026, 10, 25, 8, 0))),
        ).single()

        assertEquals(-60L, period.change.shiftMinutes)
        assertEquals(600L, period.registeredMinutes)
        assertEquals(660L, period.elapsedMinutes)
    }

    @Test
    fun periodsThatOnlyTouchTheChangeAreNotReported() {
        val startsAfterChange = block(LocalDateTime.of(2026, 3, 29, 3, 0), LocalDateTime.of(2026, 3, 29, 8, 0))
        val endsAtChange = block(LocalDateTime.of(2026, 3, 28, 22, 0), LocalDateTime.of(2026, 3, 29, 2, 0))
        val ordinarySummerNight = block(LocalDateTime.of(2026, 8, 1, 22, 0), LocalDateTime.of(2026, 8, 2, 8, 0))

        assertTrue(
            ClockChangePolicy.affectedPeriods(
                listOf(startsAfterChange, endsAtChange, ordinarySummerNight),
            ).isEmpty(),
        )
    }

    @Test
    fun startInsideTheSkippedHourIsReported() {
        val period = ClockChangePolicy.affectedPeriods(
            listOf(block(LocalDateTime.of(2026, 3, 29, 2, 30), LocalDateTime.of(2026, 3, 29, 8, 0))),
        ).single()

        assertEquals(330L, period.registeredMinutes)
        assertEquals(270L, period.elapsedMinutes)
    }

    @Test
    fun engineReportsReviewFindingWithoutChangingOtherFindings() {
        val crossing = listOf(block(LocalDateTime.of(2026, 3, 28, 22, 0), LocalDateTime.of(2026, 3, 29, 8, 0)))
        val sameShiftOneWeekEarlier = listOf(block(LocalDateTime.of(2026, 3, 21, 22, 0), LocalDateTime.of(2026, 3, 22, 8, 0)))

        val crossingFindings = TripPlanEngine.controlFindings(crossing, unresolvedRuleCount = 0)
        val baselineFindings = TripPlanEngine.controlFindings(sameShiftOneWeekEarlier, unresolvedRuleCount = 0)

        val clockFinding = crossingFindings.single { it.title.contains("sommertid") }
        assertEquals(FindingSeverity.REVIEW, clockFinding.severity)
        assertTrue(clockFinding.detail.contains("10 t"))
        assertTrue(clockFinding.detail.contains("9 t"))
        assertTrue(baselineFindings.none { it.title.contains("sommertid") || it.title.contains("normaltid") })
        assertEquals(baselineFindings.size + 1, crossingFindings.size)
    }

    @Test
    fun identicalBlocksAreReportedOnce() {
        val night = block(LocalDateTime.of(2026, 10, 24, 22, 0), LocalDateTime.of(2026, 10, 25, 8, 0))
        assertEquals(1, ClockChangePolicy.controlFindings(listOf(night, night)).size)
    }

    private fun block(start: LocalDateTime, end: LocalDateTime): WorkBlock =
        WorkBlock(start = start, end = end, kind = TimeKind.ACTIVE_WORK)
}
