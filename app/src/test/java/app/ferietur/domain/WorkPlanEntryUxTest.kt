package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkPlanEntryUxTest {
    private val monday = LocalDate.of(2026, 8, 10)
    private val tuesday = monday.plusDays(1)

    @Test
    fun groundRosterCanSeedHolidayPlanAsEditableNeutralStartingPoint() {
        val roster = mapOf(
            monday to RosterEntryCodec.encode(
                listOf(RosterEntryCodec.manualWork("D", LocalTime.of(7, 0), LocalTime.of(15, 0))),
            ),
            tuesday to RosterEntryCodec.encode(
                listOf(RosterEntryCodec.manualWork("A", LocalTime.of(14, 30), LocalTime.of(22, 0))),
            ),
        )

        val seeded = TripPlanEngine.seedPlanFromRosterForEditing(
            dates = listOf(monday, tuesday),
            roster = roster,
            existingPlans = emptyMap(),
            tripStart = LocalDateTime.of(monday, LocalTime.of(6, 0)),
            tripEnd = LocalDateTime.of(tuesday, LocalTime.of(23, 0)),
        )

        val work = seeded.values.flatten()
        assertEquals(2, work.size)
        assertTrue(work.all {
            it.holidayWorkPlanRelation == HolidayWorkPlanRelation.NOT_CLARIFIED
        })
    }

    @Test
    fun groundRosterSeedRefusesToOverwriteExistingHolidayPlanWork() {
        val existing = mapOf(
            monday to listOf(
                PlannedBlock(
                    kind = TimeKind.ACTIVE_WORK,
                    start = LocalTime.of(10, 0),
                    end = LocalTime.of(12, 0),
                ),
            ),
        )

        val result = runCatching {
            TripPlanEngine.seedPlanFromRosterForEditing(
                dates = listOf(monday),
                roster = emptyMap(),
                existingPlans = existing,
                tripStart = LocalDateTime.of(monday, LocalTime.of(0, 0)),
                tripEnd = LocalDateTime.of(tuesday, LocalTime.of(0, 0)),
            )
        }

        assertTrue(result.isFailure)
    }

    @Test
    fun actualWorkCanStartAsCopyOfHolidayPlanWithoutInventingRelation() {
        val holiday = mapOf(
            monday to listOf(
                PlannedBlock(
                    kind = TimeKind.ACTIVE_WORK,
                    start = LocalTime.of(7, 0),
                    end = LocalTime.of(22, 0),
                ),
            ),
            tuesday to emptyList(),
        )

        val actual = TripPlanEngine.seedActualWorkFromHolidayPlan(
            dates = listOf(monday, tuesday),
            holidayPlans = holiday,
        )

        val copied = actual.getValue(monday).single()
        assertEquals(LocalTime.of(7, 0), copied.start)
        assertEquals(LocalTime.of(22, 0), copied.end)
        assertEquals(
            HolidayWorkPlanRelation.NOT_CLARIFIED,
            copied.holidayWorkPlanRelation,
        )
    }

    @Test
    fun runtimeBridgeDerivesWithinAndBeyondFromHolidayPlanVsActual() {
        val holiday = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            ),
        )
        val actual = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(23, 30)),
            ),
        )

        val runtime = TripPlanEngine.runtimePlansForHolidayWorkPlanComparison(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday),
            actualPlans = actual,
            holidayPlans = holiday,
        ).getValue(monday)

        assertEquals(2, runtime.size)
        assertEquals(
            HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
            runtime[0].holidayWorkPlanRelation,
        )
        assertEquals(
            HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
            runtime[1].holidayWorkPlanRelation,
        )
        assertEquals(LocalTime.of(22, 0), runtime[1].start)
        assertEquals(LocalTime.of(23, 30), runtime[1].end)
    }

    @Test
    fun missingHolidayPlanClearsStaleManualRelationAndFailsClosed() {
        val actual = mapOf(
            monday to listOf(
                PlannedBlock(
                    kind = TimeKind.ACTIVE_WORK,
                    start = LocalTime.of(7, 0),
                    end = LocalTime.of(22, 0),
                    holidayWorkPlanRelation =
                        HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
                ),
            ),
        )

        val runtime = TripPlanEngine.runtimePlansForHolidayWorkPlanComparison(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday),
            actualPlans = actual,
            holidayPlans = emptyMap(),
        )

        assertEquals(
            HolidayWorkPlanRelation.NOT_CLARIFIED,
            runtime.getValue(monday).single().holidayWorkPlanRelation,
        )
    }
}
