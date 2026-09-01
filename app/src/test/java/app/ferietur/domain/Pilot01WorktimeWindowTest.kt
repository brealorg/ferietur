package app.ferietur.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Pilot01WorktimeWindowTest {

    private val title =
        "Mer enn 48 timer i en sju-dagersperiode"

    @Test
    fun totalTripAboveFortyEightDoesNotWarnWhenNoSevenDayWindowExceedsFortyEight() {
        val start = LocalDate.of(2026, 9, 1)

        val blocks =
            (0L until 10L).map { offset ->
                val day = start.plusDays(offset)

                WorkBlock(
                    start = day.atTime(8, 0),
                    end = day.atTime(14, 0),
                    kind = TimeKind.ACTIVE_WORK,
                )
            }

        // 60 hours in the whole shown trip, but only 42 hours in any
        // continuous seven-day window.
        val findings =
            TripPlanEngine.controlFindings(
                blocks = blocks,
                unresolvedRuleCount = 0,
            )

        assertFalse(
            findings.any {
                it.title == title
            },
        )

        assertEquals(
            "60 t i den viste perioden.",
            findings.single {
                it.title == "Samlet arbeidstid"
            }.detail,
        )
    }

    @Test
    fun exactlyFortyEightHoursDoesNotTriggerSevenDayWarning() {
        val start = LocalDate.of(2026, 9, 1)

        val blocks =
            (0L until 6L).map { offset ->
                val day = start.plusDays(offset)

                WorkBlock(
                    start = day.atTime(8, 0),
                    end = day.atTime(16, 0),
                    kind = TimeKind.ACTIVE_WORK,
                )
            }

        val findings =
            TripPlanEngine.controlFindings(
                blocks = blocks,
                unresolvedRuleCount = 0,
            )

        assertFalse(
            findings.any {
                it.title == title
            },
        )
    }

    @Test
    fun fortyNineHoursInsideSevenDaysTriggersReview() {
        val start = LocalDate.of(2026, 9, 1)

        val blocks =
            (0L until 7L).map { offset ->
                val day = start.plusDays(offset)

                WorkBlock(
                    start = day.atTime(8, 0),
                    end = day.atTime(15, 0),
                    kind = TimeKind.ACTIVE_WORK,
                )
            }

        val finding =
            TripPlanEngine.controlFindings(
                blocks = blocks,
                unresolvedRuleCount = 0,
            ).single {
                it.title == title
            }

        assertEquals(
            FindingSeverity.REVIEW,
            finding.severity,
        )
        assertTrue(
            finding.detail.contains("49 t"),
        )
        assertTrue(
            finding.detail.contains("åtte uker"),
        )
        assertTrue(
            finding.detail.contains(
                "betyr ikke dette varselet automatisk",
            ),
        )
        assertTrue(
            finding.detail.contains(
                "arbeid før og etter turen",
            ),
        )
    }

    @Test
    fun solgardenUsesMaximumSevenDayWindowInsteadOfWholeTripTotal() {
        fun dt(
            day: Int,
            hour: Int,
        ): LocalDateTime =
            LocalDateTime.of(
                2026,
                8,
                day,
                hour,
                0,
            )

        val blocks = listOf(
            WorkBlock(
                dt(11, 7),
                dt(11, 15),
                TimeKind.ACTIVE_WORK,
            ),
            WorkBlock(
                dt(12, 7),
                dt(12, 22),
                TimeKind.ACTIVE_WORK,
            ),
            WorkBlock(
                dt(13, 7),
                dt(13, 22),
                TimeKind.ACTIVE_WORK,
            ),
            WorkBlock(
                dt(14, 7),
                dt(14, 22),
                TimeKind.ACTIVE_WORK,
            ),
            WorkBlock(
                dt(14, 23),
                dt(15, 7),
                TimeKind.RESTING_NIGHT_WATCH,
            ),
            WorkBlock(
                dt(15, 7),
                dt(15, 22),
                TimeKind.ACTIVE_WORK,
            ),
            WorkBlock(
                dt(15, 23),
                dt(16, 7),
                TimeKind.RESTING_NIGHT_WATCH,
            ),
            WorkBlock(
                dt(16, 7),
                dt(16, 22),
                TimeKind.ACTIVE_WORK,
            ),
            WorkBlock(
                dt(16, 23),
                dt(17, 7),
                TimeKind.RESTING_NIGHT_WATCH,
            ),
            WorkBlock(
                dt(17, 7),
                dt(17, 22),
                TimeKind.ACTIVE_WORK,
            ),
            WorkBlock(
                dt(17, 23),
                dt(18, 7),
                TimeKind.RESTING_NIGHT_WATCH,
            ),
            WorkBlock(
                dt(18, 7),
                dt(18, 16),
                TimeKind.ACTIVE_WORK,
            ),
            WorkBlock(
                dt(18, 16),
                dt(18, 23),
                TimeKind.TRAVEL_WITH_RESPONSIBILITY,
            ),
        )

        val findings =
            TripPlanEngine.controlFindings(
                blocks = blocks,
                unresolvedRuleCount = 0,
            )

        val sevenDay =
            findings.single {
                it.title == title
            }

        assertTrue(
            sevenDay.detail.contains("138 t"),
        )
        assertFalse(
            sevenDay.detail.contains("146 t"),
        )

        val reviewCount =
            findings.count {
                it.severity == FindingSeverity.REVIEW ||
                    it.severity == FindingSeverity.CRITICAL
            }

        // 1 seven-day finding + 6 short-rest findings +
        // 7 long-period findings.
        assertEquals(
            14,
            reviewCount,
        )
    }
}
