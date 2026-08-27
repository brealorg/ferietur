package app.ferietur.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TripPlanOverlayTest {
    @Test
    fun travelReplacesOverlappingRosterWorkInsteadOfDoublingTime() {
        val date = LocalDate.of(2026, 8, 10)
        val base = mapOf(
            date to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(14, 30))),
        )
        val travel = mapOf(
            date to listOf(PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(7, 0), LocalTime.of(11, 0))),
        )

        val result = TripPlanEngine.overlayTravelOnPlan(listOf(date), base, travel).getValue(date)

        assertEquals(2, result.size)
        assertTrue(result.any { it.kind == TimeKind.TRAVEL_WITH_RESPONSIBILITY && it.start == LocalTime.of(7, 0) && it.end == LocalTime.of(11, 0) })
        assertTrue(result.any { it.kind == TimeKind.ACTIVE_WORK && it.start == LocalTime.of(11, 0) && it.end == LocalTime.of(14, 30) })
    }
}

class TripPlanProjectionA34R1Test {
    @Test
    fun manualTravelReclassifiesOverlappingActiveWork() {
        val date = LocalDate.of(2026, 8, 11)
        val result = TripPlanEngine.normalizeTravelClassification(
            date,
            listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
                PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(6, 0), LocalTime.of(10, 0)),
            ),
        )

        assertEquals(2, result.size)
        assertTrue(result.any { it.kind == TimeKind.TRAVEL_WITH_RESPONSIBILITY && it.start == LocalTime.of(6, 0) && it.end == LocalTime.of(10, 0) })
        assertTrue(result.any { it.kind == TimeKind.ACTIVE_WORK && it.start == LocalTime.of(10, 0) && it.end == LocalTime.of(22, 0) })
    }

    @Test
    fun overnightRestingWatchIsProjectedOnBothCalendarDays() {
        val saturday = LocalDate.of(2026, 8, 15)
        val sunday = saturday.plusDays(1)
        val plans = mapOf(
            saturday to listOf(
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(22, 0), LocalTime.of(7, 0)),
            ),
            sunday to emptyList(),
        )

        val saturdayView = TripPlanEngine.projectVisibleDay(saturday, plans)
        val sundayView = TripPlanEngine.projectVisibleDay(sunday, plans)

        assertEquals(1, saturdayView.size)
        assertEquals(LocalTime.of(22, 0), saturdayView.single().block.start.toLocalTime())
        assertEquals(LocalTime.MIDNIGHT, saturdayView.single().block.end.toLocalTime())
        assertTrue(saturdayView.single().continuesIntoNextDay)

        assertEquals(1, sundayView.size)
        assertEquals(LocalTime.MIDNIGHT, sundayView.single().block.start.toLocalTime())
        assertEquals(LocalTime.of(7, 0), sundayView.single().block.end.toLocalTime())
        assertTrue(sundayView.single().continuesFromPreviousDay)
        assertEquals(saturday, sundayView.single().sourceDate)
    }
}

class TripPlanComparisonIntegrityA39BTest {
    @Test
    fun changingComparisonModeDoesNotDestroyAnExistingActualWorkPlan() {
        val date = LocalDate.of(2026, 8, 11)
        val roster = mapOf(date to "D1")
        val actualPlan = mapOf(
            date to listOf(
                PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(6, 0), LocalTime.of(10, 0)),
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(10, 0), LocalTime.of(22, 0)),
            ),
        )

        val result = TripPlanEngine.adjustPlanForComparisonChange(
            dates = listOf(date),
            roster = roster,
            currentPlans = actualPlan,
            from = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
            to = RosterComparisonMode.USE_NORMAL_ROSTER,
        )

        assertEquals(actualPlan, result)
    }

    @Test
    fun changingTripDateRangePreservesExistingWorkAndLeavesNewDatesEmpty() {
        val first = LocalDate.of(2026, 8, 11)
        val second = first.plusDays(1)
        val third = first.plusDays(2)
        val actualPlan = mapOf(
            second to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
        )
        val roster = mapOf(first to "D1", second to "D", third to "F1")

        val result = TripPlanEngine.preservePlanForDateRange(
            dates = listOf(first, second, third),
            roster = roster,
            currentPlans = actualPlan,
            comparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
        )

        assertEquals(actualPlan.getValue(second), result.getValue(second))
        assertTrue(result.getValue(first).isEmpty())
        assertTrue(result.getValue(third).isEmpty())
    }

    @Test
    fun comparisonModeNeverSeedsOrDeletesActualTripPlan() {
        val date = LocalDate.of(2026, 8, 11)
        val roster = mapOf(date to "D1")
        val emptyPlan = mapOf(date to emptyList<PlannedBlock>())

        val useRoster = TripPlanEngine.adjustPlanForComparisonChange(
            dates = listOf(date),
            roster = roster,
            currentPlans = emptyPlan,
            from = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
            to = RosterComparisonMode.USE_NORMAL_ROSTER,
        )
        assertTrue(useRoster.getValue(date).isEmpty())

        val actualPlan = mapOf(date to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(12, 0), LocalTime.of(14, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val separate = TripPlanEngine.adjustPlanForComparisonChange(
            dates = listOf(date),
            roster = roster,
            currentPlans = actualPlan,
            from = RosterComparisonMode.USE_NORMAL_ROSTER,
            to = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
        )
        assertEquals(actualPlan, separate)
    }

    @Test
    fun copyingRosterIntoShortTripClipsToTripBounds() {
        val date = LocalDate.of(2026, 8, 13)
        val shift = SolhaugenShiftCatalog.byCode("LV")!!
        val result = TripPlanEngine.plannedBlocksForShiftWithinTrip(
            date = date,
            shift = shift,
            tripStart = date.atTime(12, 0),
            tripEnd = date.atTime(14, 0),
        )

        assertEquals(listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(12, 0), LocalTime.of(14, 0))), result)
    }
}
