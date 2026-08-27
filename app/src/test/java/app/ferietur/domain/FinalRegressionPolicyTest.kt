package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalRegressionPolicyTest {
    private val salary = OsloSalaryTable2026.annualSalary(32)

    @Test
    fun solgardenGoldenScenarioKeepsAcceptedPaymentBasisAndNoOpenCalculationRules() {
        val start = LocalDate.of(2026, 8, 11)
        val dates = (0..7).map { start.plusDays(it.toLong()) }
        val roster = solgardenRoster(start)
        val plans = solgardenPlans(start)
        val result = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = dates,
            roster = roster,
            plans = plans,
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(start, LocalTime.of(6, 0)),
            tripEnd = LocalDateTime.of(start.plusDays(7), LocalTime.of(23, 0)),
        )

        assertEquals(BigDecimal("43267.74"), result.paymentBasisAmount.setScale(2))
        assertEquals(60L, result.rosterUncoveredMinutes)
        assertTrue(result.applicableUnresolvedRuleIds.isEmpty())
        assertEquals(BigDecimal("38454.57"), result.lines.single { it.id == "active" }.amount.setScale(2))
        assertEquals(BigDecimal("2663.52"), result.lines.single { it.id == "resting-night" }.amount.setScale(2))
        assertEquals(BigDecimal("1065.44"), result.lines.single { it.id == "resting-evening-night" }.amount.setScale(2))
        assertEquals(BigDecimal("204.21"), result.lines.single { it.id == "resting-weekend" }.amount.setScale(2))
        assertEquals(BigDecimal("880.00"), result.lines.single { it.id == "stay-allowance" }.amount.setScale(2))
        assertFalse(result.lines.any { it.paymentTreatment == PaymentTreatment.OPEN })
    }

    @Test
    fun solgardenFinalizedSnapshotRemainsConfirmedAndVersionedForFinalExport() {
        val start = LocalDate.of(2026, 8, 11)
        val dates = (0..7).map { start.plusDays(it.toLong()) }
        val roster = solgardenRoster(start)
        val plans = solgardenPlans(start)
        val result = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            dates,
            roster,
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(start, LocalTime.of(6, 0)),
            LocalDateTime.of(start.plusDays(7), LocalTime.of(23, 0)),
        )
        val snapshot = FinalizedTripSnapshotBuilder.build(
            title = "Solgården",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.RESIDENT_OR_GUARDIAN,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            dates = dates,
            roster = roster,
            plans = plans,
            salaryStep = 32,
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = true,
            tripStart = LocalDateTime.of(start, LocalTime.of(6, 0)),
            tripEnd = LocalDateTime.of(start.plusDays(7), LocalTime.of(23, 0)),
            settlement = SettlementSnapshot(result.paymentBasisAmount, result.paymentBasisAmount, true, ""),
            createdAt = LocalDateTime.of(2026, 8, 22, 21, 45, 0),
        )

        assertTrue(snapshot.isConfirmedDocumentBasis)
        assertTrue(snapshot.unresolvedRules.isEmpty())
        assertEquals(BigDecimal("43267.74"), snapshot.calculation.paymentBasisAmount.setScale(2))
        assertEquals("2026.3", FERIETUR_RULESET_VERSION)
    }

    @Test
    fun passiveNightTravelThreeStateContractStaysStable() {
        val monday = LocalDate.of(2026, 8, 10)
        val dates = listOf(monday, monday.plusDays(1))
        val roster = mapOf(monday to "D1", monday.plusDays(1) to "LV")

        fun calculate(kind: TimeKind): PreliminaryCalculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = dates,
            roster = roster,
            plans = mapOf(
                monday to listOf(
                    PlannedBlock(
                        kind,
                        LocalTime.of(23, 0),
                        LocalTime.of(7, 0),
                        TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
                    ),
                ),
            ),
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(monday, LocalTime.of(22, 0)),
            tripEnd = LocalDateTime.of(monday.plusDays(1), LocalTime.of(8, 0)),
        )

        val yes = calculate(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED)
        val unknown = calculate(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY)
        val no = calculate(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP)

        assertEquals(BigDecimal("1308.59"), yes.paymentBasisAmount.setScale(2))
        assertTrue(yes.applicableUnresolvedRuleIds.isEmpty())
        assertEquals(BigDecimal("110.00"), unknown.paymentBasisAmount.setScale(2))
        assertTrue("D25_20_3_SLEEP_PERMISSION" in unknown.applicableUnresolvedRuleIds)
        assertEquals(BigDecimal("2773.52"), no.paymentBasisAmount.setScale(2))
        assertTrue(no.applicableUnresolvedRuleIds.isEmpty())

        val yesFindings = TripPlanEngine.controlFindings(
            TripPlanEngine.projectRange(dates, mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED, LocalTime.of(23, 0), LocalTime.of(7, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))),
            yes.applicableUnresolvedRuleIds.size,
            roster,
        )
        assertTrue(yesFindings.any { it.title == "Samlet arbeidstid" && it.detail.startsWith("8 t") })
    }

    @Test
    fun separateTripGoldenScenarioUsesOnlyRegisteredPlan() {
        val monday = LocalDate.of(2026, 8, 10)
        val tuesday = monday.plusDays(1)
        val dates = listOf(monday, tuesday)
        val plans = mapOf(
            monday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(15, 0), LocalTime.of(19, 0))),
        )
        val result = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = mapOf(monday to "AL", tuesday to "D"),
            plans = plans,
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(monday, LocalTime.of(15, 0)),
            tripEnd = LocalDateTime.of(tuesday, LocalTime.of(15, 0)),
        )

        assertEquals(BigDecimal("1708.12"), result.paymentBasisAmount.setScale(2))
        assertEquals(BigDecimal("1331.76"), result.lines.single { it.id == "active" }.amount.setScale(2))
        assertEquals(BigDecimal("266.36"), result.lines.single { it.id == "evening-night" }.amount.setScale(2))
        assertEquals(BigDecimal("110.00"), result.lines.single { it.id == "stay-allowance" }.amount.setScale(2))
        assertEquals(0L, result.rosterUncoveredMinutes)
        assertEquals(BigDecimal("0.00"), result.alreadyCoveredByNormalRosterAmount.setScale(2))
        assertFalse(result.lines.any { it.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER })
        assertTrue(result.applicableUnresolvedRuleIds.isEmpty())
    }

    private fun solgardenRoster(start: LocalDate): Map<LocalDate, String> = mapOf(
        start to "D1",
        start.plusDays(1) to "LV",
        start.plusDays(2) to "F1",
        start.plusDays(3) to "AL",
        start.plusDays(4) to "N2",
        start.plusDays(5) to "F2",
        start.plusDays(6) to "D",
        start.plusDays(7) to "D1",
    )

    private fun solgardenPlans(start: LocalDate): Map<LocalDate, List<PlannedBlock>> = mapOf(
        start to listOf(
            PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(6, 0), LocalTime.of(11, 0)),
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(11, 0), LocalTime.of(22, 0)),
        ),
        start.plusDays(1) to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
        start.plusDays(2) to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
        start.plusDays(3) to listOf(
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
        ),
        start.plusDays(4) to listOf(
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
        ),
        start.plusDays(5) to listOf(
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
        ),
        start.plusDays(6) to listOf(
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
        ),
        start.plusDays(7) to listOf(
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(16, 0)),
            PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(16, 0), LocalTime.of(23, 0)),
        ),
    )
}
