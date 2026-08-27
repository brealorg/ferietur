package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class FinalizedTripSnapshotTest {
    @Test
    fun snapshotKeepsRosterCalculationSettlementAndSourcesTogether() {
        val date = LocalDate.of(2026, 8, 11)
        val nextDate = date.plusDays(1)
        val dates = listOf(date, nextDate)
        val roster = mapOf(date to "D1")
        val plans = mapOf(
            date to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(14, 30))),
        )
        val annualSalary = BigDecimal("614600")
        val calc = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            dates,
            roster,
            plans,
            annualSalary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(date, LocalTime.of(6, 0)),
            LocalDateTime.of(nextDate, LocalTime.of(6, 0)),
        )
        val snapshot = FinalizedTripSnapshotBuilder.build(
            title = "Testtur",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.RESIDENT_OR_GUARDIAN,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            dates = dates,
            roster = roster,
            plans = plans,
            salaryStep = 32,
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            tripStart = LocalDateTime.of(date, LocalTime.of(6, 0)),
            tripEnd = LocalDateTime.of(nextDate, LocalTime.of(6, 0)),
            settlement = SettlementSnapshot(calc.paymentBasisAmount, calc.paymentBasisAmount, true, ""),
            snapshotId = "11111111-1111-4111-8111-111111111111",
            createdAt = LocalDateTime.of(2026, 8, 21, 12, 34, 56),
        )

        assertEquals("11111111-1111-4111-8111-111111111111", snapshot.id)
        assertEquals(1, snapshot.roster.size)
        assertEquals("D1", snapshot.roster.single().code)
        assertEquals(calc.knownAmount, snapshot.calculation.knownAmount)
        assertEquals(calc.paymentBasisAmount, snapshot.settlement.proposedAmount)
        assertEquals(EmployerKind.OSLO_KOMMUNE, snapshot.employerKind)
        assertEquals(PayingParty.RESIDENT_OR_GUARDIAN, snapshot.payingParty)
        assertEquals("Oslo kommune – Dok. 25 2026–28, kapittel 20", snapshot.ruleBasis)
        assertTrue(snapshot.isRuleBasisConfirmed)
        assertTrue(snapshot.payslipChecked)
        assertFalse(snapshot.workBlocks.isEmpty())
        assertTrue(snapshot.unresolvedRules.isEmpty())
    }

    @Test
    fun snapshotRejectsWorkOutsideExactTripBounds() {
        val date = LocalDate.of(2026, 8, 18)
        val nextDate = date.plusDays(1)
        val dates = listOf(date, nextDate)
        val plans = mapOf(
            nextDate to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(6, 30), LocalTime.of(7, 30))),
        )
        val annualSalary = BigDecimal("614600")
        val tripStart = LocalDateTime.of(date, LocalTime.of(6, 0))
        val tripEnd = LocalDateTime.of(nextDate, LocalTime.of(6, 0))
        val calc = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            dates,
            emptyMap(),
            plans,
            annualSalary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            tripStart,
            tripEnd,
        )

        assertThrows(IllegalArgumentException::class.java) {
            FinalizedTripSnapshotBuilder.build(
                title = "Ugyldig sluttid",
                employerKind = EmployerKind.OSLO_KOMMUNE,
                payingParty = PayingParty.RESIDENT_OR_GUARDIAN,
                rosterComparisonMode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
                dates = dates,
                roster = emptyMap(),
                plans = plans,
                salaryStep = 32,
                annualSalary = annualSalary,
                weeklyBasis = WeeklyBasis.HOURS_35_5,
                weekendProfile = WeekendProfile.STANDARD,
                payslipChecked = true,
                tripStart = tripStart,
                tripEnd = tripEnd,
                settlement = SettlementSnapshot(calc.paymentBasisAmount, calc.paymentBasisAmount, true, ""),
            )
        }
    }

    @Test
    fun snapshotRejectsStaleSettlementAmount() {
        val date = LocalDate.of(2026, 8, 11)
        val nextDate = date.plusDays(1)
        val dates = listOf(date, nextDate)
        val roster = mapOf(date to "D1")
        val plans = mapOf(
            date to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(18, 0))),
        )
        val annualSalary = BigDecimal("614600")
        val tripStart = LocalDateTime.of(date, LocalTime.of(6, 0))
        val tripEnd = LocalDateTime.of(nextDate, LocalTime.of(6, 0))

        assertThrows(IllegalArgumentException::class.java) {
            FinalizedTripSnapshotBuilder.build(
                title = "Stale oppgjør",
                employerKind = EmployerKind.OSLO_KOMMUNE,
                payingParty = PayingParty.RESIDENT_OR_GUARDIAN,
                rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
                dates = dates,
                roster = roster,
                plans = plans,
                salaryStep = 32,
                annualSalary = annualSalary,
                weeklyBasis = WeeklyBasis.HOURS_35_5,
                weekendProfile = WeekendProfile.STANDARD,
                payslipChecked = true,
                tripStart = tripStart,
                tripEnd = tripEnd,
                settlement = SettlementSnapshot(BigDecimal("1.00"), BigDecimal("1.00"), true, ""),
            )
        }
    }

    @Test
    fun snapshotRequiresExplicitConfirmationWhenRosterHasUnregisteredTime() {
        val saturday = LocalDate.of(2026, 8, 15)
        val sunday = saturday.plusDays(1)
        val dates = listOf(saturday, sunday)
        val roster = mapOf(saturday to "N2")
        val plans = mapOf(
            saturday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            ),
            sunday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
        )
        val annualSalary = BigDecimal("614600")
        val tripStart = LocalDateTime.of(saturday, LocalTime.of(7, 0))
        val tripEnd = LocalDateTime.of(sunday, LocalTime.of(22, 0))
        val calc = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            dates,
            roster,
            plans,
            annualSalary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            tripStart,
            tripEnd,
        )
        assertEquals(60L, calc.rosterUncoveredMinutes)

        assertThrows(IllegalArgumentException::class.java) {
            FinalizedTripSnapshotBuilder.build(
                title = "Turnusgap",
                employerKind = EmployerKind.OSLO_KOMMUNE,
                payingParty = PayingParty.RESIDENT_OR_GUARDIAN,
                rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
                dates = dates,
                roster = roster,
                plans = plans,
                salaryStep = 32,
                annualSalary = annualSalary,
                weeklyBasis = WeeklyBasis.HOURS_35_5,
                weekendProfile = WeekendProfile.STANDARD,
                payslipChecked = true,
                rosterGapConfirmed = false,
                tripStart = tripStart,
                tripEnd = tripEnd,
                settlement = SettlementSnapshot(calc.paymentBasisAmount, calc.paymentBasisAmount, true, ""),
            )
        }

        val snapshot = FinalizedTripSnapshotBuilder.build(
            title = "Turnusgap",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.RESIDENT_OR_GUARDIAN,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            dates = dates,
            roster = roster,
            plans = plans,
            salaryStep = 32,
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = true,
            tripStart = tripStart,
            tripEnd = tripEnd,
            settlement = SettlementSnapshot(calc.paymentBasisAmount, calc.paymentBasisAmount, true, ""),
        )
        assertTrue(snapshot.rosterGapConfirmed)
    }

    @Test
    fun snapshotRejectsDayTripForChapter20HolidayStay() {
        val date = LocalDate.of(2026, 8, 14)
        val dates = listOf(date)
        val plans = mapOf(
            date to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)),
        )
        val annualSalary = BigDecimal("614600")
        val tripStart = LocalDateTime.of(date, LocalTime.of(8, 0))
        val tripEnd = LocalDateTime.of(date, LocalTime.of(10, 0))
        val calc = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            dates,
            emptyMap(),
            plans,
            annualSalary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            tripStart,
            tripEnd,
        )

        val error = assertThrows(IllegalArgumentException::class.java) {
            FinalizedTripSnapshotBuilder.build(
                title = "Dagstur",
                employerKind = EmployerKind.OSLO_KOMMUNE,
                payingParty = PayingParty.UNSPECIFIED,
                rosterComparisonMode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
                dates = dates,
                roster = emptyMap(),
                plans = plans,
                salaryStep = 32,
                annualSalary = annualSalary,
                weeklyBasis = WeeklyBasis.HOURS_35_5,
                weekendProfile = WeekendProfile.STANDARD,
                payslipChecked = true,
                tripStart = tripStart,
                tripEnd = tripEnd,
                settlement = SettlementSnapshot(calc.paymentBasisAmount, calc.paymentBasisAmount, true, ""),
            )
        }
        assertTrue(error.message!!.contains("gjelder ikke dagsturer"))
    }

    @Test
    fun unresolvedPaymentScenarioIsValidForConfirmedDocumentBasis() {
        val start = LocalDate.of(2026, 8, 14)
        val end = start.plusDays(1)
        val dates = listOf(start, end)
        val plans = mapOf(
            start to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(10, 0))),
        )
        val annualSalary = BigDecimal("614600")
        val tripStart = LocalDateTime.of(start, LocalTime.of(8, 0))
        val tripEnd = LocalDateTime.of(end, LocalTime.of(8, 0))
        val calc = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            dates,
            emptyMap(),
            plans,
            annualSalary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            tripStart,
            tripEnd,
        )
        val snapshot = FinalizedTripSnapshotBuilder.build(
            title = "Ikke avklart betaler",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER,
            dates = dates,
            roster = emptyMap(),
            plans = plans,
            salaryStep = 32,
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            tripStart = tripStart,
            tripEnd = tripEnd,
            settlement = SettlementSnapshot(calc.paymentBasisAmount, calc.paymentBasisAmount, true, ""),
        )

        assertTrue(snapshot.isFrameworkComplete)
        assertTrue(snapshot.isConfirmedDocumentBasis)
        assertEquals(PayingParty.UNSPECIFIED, snapshot.payingParty)
    }

}
