package app.ferietur.domain

import java.io.StringReader
import java.io.StringWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HolidayPlanAutoClassificationTest {
    private val monday = LocalDate.of(2026, 8, 10)
    private val tuesday = monday.plusDays(1)

    @Test
    fun exactActualMatchIsDerivedWithinPlanWithoutUserClassification() {
        val derived = derive(
            holiday = block(7, 0, 22, 0),
            actual = block(
                7, 0, 22, 0,
                relation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
            ),
        )
        assertEquals(1, derived.size)
        assertEquals(
            HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
            derived.single().holidayWorkPlanRelation,
        )
    }

    @Test
    fun actualExtensionBeyondPlanSplitsAutomaticallyAtPlanBoundary() {
        val derived = derive(
            holiday = block(7, 0, 22, 0),
            actual = block(7, 0, 23, 30),
        )
        assertEquals(2, derived.size)
        assertEquals(
            listOf(
                LocalTime.of(7, 0) to LocalTime.of(22, 0),
                LocalTime.of(22, 0) to LocalTime.of(23, 30),
            ),
            derived.map { it.start to it.end },
        )
        assertEquals(
            listOf(
                HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
                HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
            ),
            derived.map { it.holidayWorkPlanRelation },
        )
    }

    @Test
    fun beforeAndAfterPlanProducesThreeSegments() {
        val derived = derive(
            holiday = block(7, 0, 22, 0),
            actual = block(6, 0, 23, 0),
        )
        assertEquals(
            listOf(
                HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
                HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
                HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
            ),
            derived.map { it.holidayWorkPlanRelation },
        )
    }

    @Test
    fun overnightDifferenceKeepsEveryMinuteAndOnlyExtensionIsBeyond() {
        val holiday = mapOf(
            monday to listOf(
                PlannedBlock(
                    TimeKind.ACTIVE_NIGHT_WATCH,
                    LocalTime.of(21, 0),
                    LocalTime.of(7, 0),
                ),
            ),
            tuesday to emptyList(),
        )
        val actual = mapOf(
            monday to listOf(
                PlannedBlock(
                    TimeKind.ACTIVE_NIGHT_WATCH,
                    LocalTime.of(21, 0),
                    LocalTime.of(8, 0),
                ),
            ),
            tuesday to emptyList(),
        )

        val derived = TripPlanEngine.deriveHolidayWorkPlanRelations(
            listOf(monday, tuesday),
            actual,
            holiday,
        )
        val projected = TripPlanEngine.projectRange(listOf(monday, tuesday), derived)

        assertEquals(660L, projected.sumOf { ChronoUnit.MINUTES.between(it.start, it.end) })
        assertEquals(
            60L,
            projected.filter {
                it.holidayWorkPlanRelation == HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN
            }.sumOf { ChronoUnit.MINUTES.between(it.start, it.end) },
        )
    }

    @Test
    fun approvedPlanCalculationPricesOnlyDerivedBeyondMinutesUnderPoint20_2() {
        val holiday = mapOf(monday to listOf(block(7, 0, 22, 0)))
        val actual = mapOf(monday to listOf(block(7, 0, 23, 30)))
        val derived = TripPlanEngine.deriveHolidayWorkPlanRelations(
            listOf(monday),
            actual,
            holiday,
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday),
            roster = emptyMap(),
            plans = derived,
            annualSalary = OsloSalaryTable2026.annualSalary(32),
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = monday.atTime(7, 0),
            tripEnd = monday.atTime(23, 30),
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
        )
        val active = calculation.lines.single { it.id == "active" }
        val expected = calculation.hourlyRate
            .multiply(BigDecimal("1.5"))
            .multiply(BigDecimal("1.50"))
            .setScale(2, RoundingMode.HALF_UP)

        assertEquals(90L, active.evidence.sumOf { it.minutes })
        assertEquals(expected, active.amount)
        assertFalse(calculation.lines.any { it.id == "holiday-work-plan-scope-open" })
    }

    @Test
    fun schemaNinePersistsHolidayPlanSeparatelyFromActualWork() {
        val holiday = mapOf(monday to listOf(block(7, 0, 22, 0)))
        val actual = mapOf(monday to listOf(block(7, 0, 23, 30)))
        val draft = SavedTripDraft(
            id = "ux02a",
            updatedAtEpochMillis = 1L,
            screen = "TRIP_PLAN",
            title = "UX02A",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            startDate = monday,
            endDate = tuesday,
            startTime = LocalTime.of(6, 0),
            endTime = LocalTime.of(23, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = false,
            roster = emptyMap(),
            plans = actual,
            outboundArrival = monday.atTime(10, 0),
            returnDeparture = tuesday.atTime(18, 0),
            outboundTravelKind = null,
            returnTravelKind = null,
            settlementMode = "FULL_CALCULATION",
            settlementAmountText = "",
            settlementReason = "",
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
            holidayPlans = holiday,
        )

        val encoded = StringWriter().also { SavedTripDraftCodec.write(draft, it) }.toString()
        val restored = SavedTripDraftCodec.read(StringReader(encoded))

        assertTrue(encoded.contains("schemaVersion=10"))
        assertTrue(encoded.contains("holidayPlan."))
        assertEquals(actual, restored.plans)
        assertEquals(holiday, restored.holidayPlans)
    }

    @Test
    fun schemaEightMigrationDoesNotGuessHolidayPlan() {
        val current = StringWriter().also {
            SavedTripDraftCodec.write(
                SavedTripDraft(
                    id = "legacy-v8",
                    updatedAtEpochMillis = 1L,
                    screen = "TRIP_PLAN",
                    title = "Legacy v8",
                    employerKind = EmployerKind.OSLO_KOMMUNE,
                    payingParty = PayingParty.UNSPECIFIED,
                    rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
                    startDate = monday,
                    endDate = tuesday,
                    startTime = LocalTime.of(6, 0),
                    endTime = LocalTime.of(23, 0),
                    salaryStep = 32,
                    weeklyBasis = WeeklyBasis.HOURS_35_5,
                    weekendProfile = WeekendProfile.STANDARD,
                    payslipChecked = true,
                    rosterGapConfirmed = false,
                    roster = emptyMap(),
                    plans = mapOf(monday to listOf(block(7, 0, 23, 30))),
                    outboundArrival = monday.atTime(10, 0),
                    returnDeparture = tuesday.atTime(18, 0),
                    outboundTravelKind = null,
                    returnTravelKind = null,
                    settlementMode = "FULL_CALCULATION",
                    settlementAmountText = "",
                    settlementReason = "",
                    holidayWorkPlanStatus =
                        HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
                ),
                it,
            )
        }.toString()

        val v8 = current
            .replace("schemaVersion=10", "schemaVersion=8")
            .lineSequence()
            .filterNot { it.startsWith("holidayPlan.") }
            .joinToString("\n")

        val decoded = SavedTripDraftCodec.readDecoded(StringReader(v8))

        assertEquals(8, decoded.sourceSchemaVersion)
        assertTrue(decoded.draft.holidayPlans.isEmpty())
        assertTrue(
            SavedTripDraftMigrator.MIGRATION_V9_HOLIDAY_WORK_PLAN in
                decoded.draft.migrationHistory,
        )
    }

    @Test
    fun missingHolidayPlanCannotBeInterpretedAsAllBeyondPlan() {
        val result = runCatching {
            TripPlanEngine.deriveHolidayWorkPlanRelations(
                dates = listOf(monday),
                actualPlans = mapOf(monday to listOf(block(7, 0, 22, 0))),
                holidayPlans = emptyMap(),
            )
        }
        assertTrue(result.isFailure)
    }

    private fun derive(
        holiday: PlannedBlock,
        actual: PlannedBlock,
    ): List<PlannedBlock> =
        TripPlanEngine.deriveHolidayWorkPlanRelations(
            dates = listOf(monday),
            actualPlans = mapOf(monday to listOf(actual)),
            holidayPlans = mapOf(monday to listOf(holiday)),
        ).getValue(monday)

    private fun block(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        relation: HolidayWorkPlanRelation =
            HolidayWorkPlanRelation.NOT_CLARIFIED,
    ): PlannedBlock =
        PlannedBlock(
            kind = TimeKind.ACTIVE_WORK,
            start = LocalTime.of(startHour, startMinute),
            end = LocalTime.of(endHour, endMinute),
            holidayWorkPlanRelation = relation,
        )
    @Test
    fun runtimeBridgeLeavesSeparateTripModeUntouched() {
        val actual = mapOf(
            monday to listOf(
                PlannedBlock(
                    kind = TimeKind.ACTIVE_WORK,
                    start = LocalTime.of(7, 0),
                    end = LocalTime.of(15, 0),
                    holidayWorkPlanRelation = HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN,
                ),
            ),
        )

        val runtime = TripPlanEngine.runtimePlansForHolidayWorkPlanComparison(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(monday),
            actualPlans = actual,
            holidayPlans = emptyMap(),
        )

        assertEquals(actual, runtime)
    }

}
