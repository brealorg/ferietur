package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDateRangePolicyTest {
    @Test
    fun inclusiveRangeKeepsThirtyOneDays() {
        val dates = TripDateRangePolicy.inclusiveDates(
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31),
        )
        assertEquals(31, dates.size)
        assertEquals(LocalDate.of(2026, 8, 31), dates.last())
    }

    @Test
    fun inclusiveRangeDoesNotTruncateAtThirtyTwoDays() {
        val dates = TripDateRangePolicy.inclusiveDates(
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 9, 1),
        )
        assertEquals(32, dates.size)
        assertEquals(LocalDate.of(2026, 9, 1), dates.last())
    }

    @Test
    fun multiMonthRangeKeepsEveryCalendarDayAndReturnTravel() {
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 9, 15)
        val dates = TripDateRangePolicy.inclusiveDates(start, end)
        assertEquals(46, dates.size)
        assertEquals(end, dates.last())

        val travel = mapOf(
            end to listOf(
                PlannedBlock(
                    TimeKind.TRAVEL_WITH_RESPONSIBILITY,
                    LocalTime.of(12, 0),
                    LocalTime.of(16, 0),
                ),
            ),
        )
        val overlaid = TripPlanEngine.overlayTravelOnPlan(
            dates = dates,
            basePlans = dates.associateWith { emptyList() },
            travelPlans = travel,
        )
        assertEquals(TimeKind.TRAVEL_WITH_RESPONSIBILITY, overlaid.getValue(end).single().kind)
    }

    @Test
    fun crossYearRangeKeepsEveryDate() {
        val start = LocalDate.of(2026, 12, 20)
        val end = LocalDate.of(2027, 1, 10)
        val dates = TripDateRangePolicy.inclusiveDates(start, end)
        assertEquals(22, dates.size)
        assertEquals(start, dates.first())
        assertEquals(end, dates.last())
    }

    @Test
    fun incompleteCoverageIsRejected() {
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 9, 1)
        val incomplete = TripDateRangePolicy.inclusiveDates(start, end).dropLast(1)

        val error = assertThrows(IllegalArgumentException::class.java) {
            TripDateRangePolicy.requireCompleteCoverage(incomplete, start, end)
        }
        assertTrue(error.message.orEmpty().contains("dekker ikke hele turperioden"))
    }
}
