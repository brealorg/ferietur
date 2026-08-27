package app.ferietur.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeparateTripE2EPolicyTest {
    private val monday = LocalDate.of(2026, 8, 10)
    private val tuesday = monday.plusDays(1)
    private val salary = OsloSalaryTable2026.annualSalary(32)

    private fun calculate(roster: Map<LocalDate, String>): PreliminaryCalculation =
        TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(monday, tuesday),
            roster = roster,
            plans = mapOf(
                monday to listOf(
                    PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(15, 0), LocalTime.of(19, 0)),
                ),
            ),
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(monday, LocalTime.of(15, 0)),
            tripEnd = LocalDateTime.of(tuesday, LocalTime.of(15, 0)),
        )

    @Test
    fun separateTripUsesRegisteredPlanAndDoesNotSubtractRoster() {
        val withoutRoster = calculate(emptyMap())
        val withOverlappingRoster = calculate(mapOf(monday to "AL", tuesday to "D"))

        assertEquals(withoutRoster.paymentBasisAmount, withOverlappingRoster.paymentBasisAmount)
        assertEquals(0L, withOverlappingRoster.rosterUncoveredMinutes)
        assertFalse(withOverlappingRoster.lines.any { it.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER })

        val active = withOverlappingRoster.lines.single { it.id == "active" }
        val evening = withOverlappingRoster.lines.single { it.id == "evening-night" }
        val stay = withOverlappingRoster.lines.single { it.id == "stay-allowance" }
        assertEquals(withOverlappingRoster.hourlyRate.multiply(BigDecimal("4")).setScale(2, RoundingMode.HALF_UP), active.amount)
        assertEquals(BigDecimal("266.36"), evening.amount.setScale(2, RoundingMode.HALF_UP))
        assertEquals(BigDecimal("110.00"), stay.amount.setScale(2, RoundingMode.HALF_UP))
        assertTrue(active.explanation.contains("Grunnturnusen brukes ikke som sammenligningsgrunnlag"))
    }

    @Test
    fun separateTripFinalizesWithoutRosterGapConfirmation() {
        val calculation = calculate(mapOf(monday to "AL", tuesday to "D"))
        val snapshot = FinalizedTripSnapshotBuilder.build(
            title = "Separat oppdrag E2E",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.RESIDENT_OR_GUARDIAN,
            rosterComparisonMode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
            dates = listOf(monday, tuesday),
            roster = mapOf(monday to "AL", tuesday to "D"),
            plans = mapOf(monday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(15, 0), LocalTime.of(19, 0)))),
            salaryStep = 32,
            annualSalary = salary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = false,
            tripStart = LocalDateTime.of(monday, LocalTime.of(15, 0)),
            tripEnd = LocalDateTime.of(tuesday, LocalTime.of(15, 0)),
            settlement = SettlementSnapshot(calculation.paymentBasisAmount, calculation.paymentBasisAmount, true, ""),
        )

        assertTrue(snapshot.isFrameworkComplete)
        assertTrue(snapshot.isConfirmedDocumentBasis)
        assertEquals(RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER, snapshot.rosterComparisonMode)
        assertEquals(BigDecimal("0.00"), snapshot.calculation.alreadyCoveredByNormalRosterAmount.setScale(2))
    }
}
