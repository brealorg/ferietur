package app.ferietur.domain

import java.io.StringReader
import java.io.StringWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkPlanBasisSemanticsTest {
    private val monday = LocalDate.of(2026, 8, 10)
    private val dates = listOf(monday)
    private val annualSalary = OsloSalaryTable2026.annualSalary(32)

    @Test
    fun normalRosterBasisUsesGroundRosterAndDoesNotRequireEmployerTripPlan() {
        val roster = mapOf(
            monday to RosterEntryCodec.encode(
                listOf(RosterEntryCodec.manualWork("D", LocalTime.of(7, 0), LocalTime.of(15, 0))),
            ),
        )
        val actual = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            ),
        )
        val runtime = TripPlanEngine.runtimePlansForWorkPlanBasis(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            workPlanBasis = TripWorkPlanBasis.NORMAL_ROSTER_APPLIES,
            dates = dates,
            actualPlans = actual,
            holidayPlans = emptyMap(),
        )
        val effectiveStatus = TripPlanEngine.calculationHolidayWorkPlanStatusForBasis(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            workPlanBasis = TripWorkPlanBasis.NORMAL_ROSTER_APPLIES,
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
        )

        assertNull(effectiveStatus)
        val calculation = calculate(roster, runtime, effectiveStatus)
        val active = calculation.lines.single { it.id == "active" }
        val expected = calculation.hourlyRate
            .multiply(BigDecimal("7.0"))
            .multiply(BigDecimal("1.50"))
            .setScale(2, RoundingMode.HALF_UP)

        assertEquals(420L, active.evidence.sumOf { it.minutes })
        assertEquals(expected, active.amount)
        assertFalse(calculation.lines.any { it.id == "holiday-work-plan-scope-open" })
    }

    @Test
    fun employerTripPlanBasisUsesPlanVsRecordedWorkNotGroundRoster() {
        val roster = mapOf(
            monday to RosterEntryCodec.encode(
                listOf(RosterEntryCodec.manualWork("D", LocalTime.of(7, 0), LocalTime.of(15, 0))),
            ),
        )
        val employerPlan = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            ),
        )
        val actual = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(23, 0)),
            ),
        )
        val runtime = TripPlanEngine.runtimePlansForWorkPlanBasis(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            workPlanBasis = TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN,
            dates = dates,
            actualPlans = actual,
            holidayPlans = employerPlan,
        )
        val effectiveStatus = TripPlanEngine.calculationHolidayWorkPlanStatusForBasis(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            workPlanBasis = TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN,
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
        )

        val calculation = calculate(roster, runtime, effectiveStatus)
        val active = calculation.lines.single { it.id == "active" }

        assertEquals(60L, active.evidence.sumOf { it.minutes })
        assertFalse(calculation.lines.any { it.id == "holiday-work-plan-scope-open" })
    }

    @Test
    fun unresolvedBasisFailsClosedInsteadOfGuessingGroundRosterOrTripPlan() {
        val roster = mapOf(
            monday to RosterEntryCodec.encode(
                listOf(RosterEntryCodec.manualWork("D", LocalTime.of(7, 0), LocalTime.of(15, 0))),
            ),
        )
        val actual = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            ),
        )
        val runtime = TripPlanEngine.runtimePlansForWorkPlanBasis(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            workPlanBasis = TripWorkPlanBasis.NOT_CLARIFIED,
            dates = dates,
            actualPlans = actual,
            holidayPlans = emptyMap(),
        )
        val effectiveStatus = TripPlanEngine.calculationHolidayWorkPlanStatusForBasis(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            workPlanBasis = TripWorkPlanBasis.NOT_CLARIFIED,
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
        )

        val calculation = calculate(roster, runtime, effectiveStatus)
        assertFalse(calculation.lines.any { it.id == "active" })
        assertTrue(calculation.lines.any { it.id == "holiday-work-plan-scope-open" })
        assertTrue("D25_20_2_WORK_PLAN_SCOPE" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun schemaTenRoundTripsEmployerPlanBasis() {
        val draft = draft(TripWorkPlanBasis.NORMAL_ROSTER_APPLIES)
        val encoded = StringWriter().also { SavedTripDraftCodec.write(draft, it) }.toString()
        val restored = SavedTripDraftCodec.read(StringReader(encoded))

        assertTrue(encoded.contains("schemaVersion=10"))
        assertTrue(encoded.contains("workPlanBasis=NORMAL_ROSTER_APPLIES"))
        assertEquals(TripWorkPlanBasis.NORMAL_ROSTER_APPLIES, restored.workPlanBasis)
    }

    @Test
    fun schemaNineMigrationDoesNotInferEmployerPlanBasisEvenWhenHolidayPlanExists() {
        val current = StringWriter().also {
            SavedTripDraftCodec.write(
                draft(
                    basis = TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN,
                    holidayPlans = mapOf(
                        monday to listOf(
                            PlannedBlock(
                                TimeKind.ACTIVE_WORK,
                                LocalTime.of(7, 0),
                                LocalTime.of(22, 0),
                            ),
                        ),
                    ),
                ),
                it,
            )
        }.toString()
        val schemaNine = current
            .replace("schemaVersion=10", "schemaVersion=9")
            .lineSequence()
            .filterNot { it.startsWith("workPlanBasis=") }
            .joinToString("\n")

        val decoded = SavedTripDraftCodec.readDecoded(StringReader(schemaNine))

        assertEquals(9, decoded.sourceSchemaVersion)
        assertEquals(TripWorkPlanBasis.NOT_CLARIFIED, decoded.draft.workPlanBasis)
        assertTrue(decoded.draft.holidayPlans.values.flatten().isNotEmpty())
        assertTrue(
            SavedTripDraftMigrator.MIGRATION_V10_WORK_PLAN_BASIS in
                decoded.draft.migrationHistory,
        )
    }

    private fun calculate(
        roster: Map<LocalDate, String>,
        plans: Map<LocalDate, List<PlannedBlock>>,
        status: HolidayWorkPlanStatus?,
    ): PreliminaryCalculation =
        TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = dates,
            roster = roster,
            plans = plans,
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = monday.atTime(7, 0),
            tripEnd = monday.atTime(23, 0),
            holidayWorkPlanStatus = status,
        )

    private fun draft(
        basis: TripWorkPlanBasis,
        holidayPlans: Map<LocalDate, List<PlannedBlock>> = emptyMap(),
    ): SavedTripDraft =
        SavedTripDraft(
            id = "planbasis01a",
            updatedAtEpochMillis = 1L,
            screen = "METHOD",
            title = "Planbasis",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            startDate = monday,
            endDate = monday,
            startTime = LocalTime.of(7, 0),
            endTime = LocalTime.of(23, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = false,
            roster = emptyMap(),
            plans = emptyMap(),
            outboundArrival = monday.atTime(10, 0),
            returnDeparture = monday.atTime(18, 0),
            outboundTravelKind = null,
            returnTravelKind = null,
            settlementMode = "FULL_CALCULATION",
            settlementAmountText = "",
            settlementReason = "",
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
            workPlanBasis = basis,
            holidayPlans = holidayPlans,
        )
}
