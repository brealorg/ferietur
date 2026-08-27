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

class TripPlanEngineTest {
    private val monday = LocalDate.of(2026, 8, 10)

    @Test
    fun customLongDayAgainstLvKeepsExactTwelveHourOverlap() {
        val roster = mapOf(monday to "LV")
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(23, 0))))
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday),
            roster = roster,
            plans = plans,
            annualSalary = OsloSalaryTable2026.annualSalary(32),
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = LocalDateTime.of(monday, LocalTime.of(7, 0)),
            tripEnd = LocalDateTime.of(monday, LocalTime.of(23, 0)),
        )
        assertEquals(960, calculation.activeMinutes)
        assertEquals(720, calculation.activeInsideRosterMinutes)
        assertEquals(240, calculation.activeOutsideRosterMinutes)
    }

    @Test
    fun lvGetsFourHoursEveningAllowanceFromSeventeenToTwentyOne() {
        val roster = mapOf(monday to "LV")
        val plans = TripPlanEngine.planFromRoster(listOf(monday), roster)
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(9, 0)),
            LocalDateTime.of(monday, LocalTime.of(21, 0)),
        )
        assertEquals(240, calculation.eveningNightMinutes)
        val line = calculation.lines.first { it.id == "evening-night" }
        assertTrue(line.includedInKnownTotal)
        assertEquals(CalculationCertainty.CONFIRMED, line.certainty)
    }

    @Test
    fun n2GetsFortyPercentThroughShiftEndAfterSix() {
        val saturday = LocalDate.of(2026, 8, 15)
        val roster = mapOf(saturday to "N2")
        val plans = TripPlanEngine.planFromRoster(listOf(saturday), roster)
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(saturday),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(saturday, LocalTime.of(21, 45)),
            LocalDateTime.of(saturday.plusDays(1), LocalTime.of(7, 45)),
        )
        assertEquals(600, calculation.eveningNightMinutes)
        assertTrue(calculation.lines.first { it.id == "evening-night" }.evidence.any { it.note.contains("senest 08:00") })
    }

    @Test
    fun saturdayOrdinaryRosterGetsWeekendMinutes() {
        val saturday = LocalDate.of(2026, 8, 15)
        val roster = mapOf(saturday to "D")
        val plans = TripPlanEngine.planFromRoster(listOf(saturday), roster)
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(saturday),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(saturday, LocalTime.of(7, 0)),
            LocalDateTime.of(saturday, LocalTime.of(15, 0)),
        )
        assertEquals(480, calculation.weekendMinutes)
    }

    @Test
    fun restingNightProjectsEightHoursAndUsesUserFacingCalculationName() {
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0))))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(23, 0)),
            LocalDateTime.of(monday.plusDays(1), LocalTime.of(7, 0)),
        )
        assertEquals(480, calculation.restingNightMinutes)
        assertTrue(calculation.lines.any { it.title == "Hvilende nattevakt" })
    }

    @Test
    fun activeEventOnRestingNightMovesEarlyMorningEventToNextDay() {
        val blocks = listOf(
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            PlannedBlock(TimeKind.ACTIVE_EVENT_ON_RESTING, LocalTime.of(2, 0), LocalTime.of(2, 30)),
        )
        val projected = TripPlanEngine.projectDay(monday, blocks)
        val event = projected.first { it.kind == TimeKind.ACTIVE_EVENT_ON_RESTING }
        assertEquals(monday.plusDays(1), event.start.toLocalDate())
    }

    @Test
    fun stayAllowanceCountsStartedDayOnlyWhenRemainderExceedsSixHours() {
        val start = LocalDateTime.of(2026, 8, 10, 7, 0)
        assertEquals(6, TripPlanEngine.stayAllowanceDays(start, LocalDateTime.of(2026, 8, 16, 13, 0)))
        assertEquals(7, TripPlanEngine.stayAllowanceDays(start, LocalDateTime.of(2026, 8, 16, 13, 1)))
    }
}

// A3.1 regression tests live in their own class so the A3 baseline remains readable.
class TripPlanEngineA31Test {
    private val monday = LocalDate.of(2026, 8, 10)

    @Test
    fun travelWithResponsibilityCountsAsActiveWork() {
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0))))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(8, 0)),
            LocalDateTime.of(monday, LocalTime.of(10, 0)),
        )
        assertEquals(120, calculation.activeMinutes)
        assertEquals(120, calculation.activeOutsideRosterMinutes)
    }

    @Test
    fun travelWithoutResponsibilityOutsideRosterIsPaidAtOrdinaryHourlyRate() {
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(8, 0)),
            LocalDateTime.of(monday, LocalTime.of(10, 0)),
        )
        assertEquals(0, calculation.activeMinutes)
        val line = calculation.lines.first { it.id == "travel-without-responsibility" }
        assertEquals(CalculationCertainty.CONFIRMED, line.certainty)
        assertTrue(line.includedInKnownTotal)
        assertEquals(PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS, line.paymentTreatment)
        assertEquals(calculation.hourlyRate.multiply(BigDecimal("2.00")).setScale(2), line.amount)
        assertEquals(line.amount, calculation.paymentBasisAmount)
    }

    @Test
    fun travelWithoutResponsibilityInsideNormalRosterDoesNotCreateAdditionalPayment() {
        val roster = mapOf(monday to "D1")
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(8, 0)),
            LocalDateTime.of(monday, LocalTime.of(10, 0)),
        )
        assertFalse(calculation.lines.any { it.id == "travel-without-responsibility" })
        assertEquals(BigDecimal("0.00"), calculation.paymentBasisAmount)
    }

    @Test
    fun shortTripInsideLongRosterShiftHasNoArtificialRosterGap() {
        val roster = mapOf(monday to "LV")
        val plans = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(12, 0), LocalTime.of(14, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY),
            ),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(12, 0)),
            LocalDateTime.of(monday, LocalTime.of(14, 0)),
        )

        assertEquals(BigDecimal("0.00"), calculation.paymentBasisAmount)
        assertEquals(0L, calculation.rosterUncoveredMinutes)
        assertTrue(calculation.rosterUncoveredEvidence.isEmpty())
    }

    @Test
    fun travelWithoutResponsibilityPartlyOutsideRosterPaysOnlyUncoveredTime() {
        val roster = mapOf(monday to "D1")
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(13, 0), LocalTime.of(16, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(13, 0)),
            LocalDateTime.of(monday, LocalTime.of(16, 0)),
        )
        val line = calculation.lines.first { it.id == "travel-without-responsibility" }
        assertEquals(90L, line.evidence.sumOf { it.minutes })
        assertEquals(calculation.hourlyRate.multiply(BigDecimal("1.50")).setScale(2), line.amount)
    }

    @Test
    fun travelWithoutResponsibilityInSeparateTripModelPaysAllRegisteredTravelAtOrdinaryRate() {
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(8, 0)),
            LocalDateTime.of(monday, LocalTime.of(10, 0)),
        )
        val line = calculation.lines.first { it.id == "travel-without-responsibility" }
        assertEquals(calculation.hourlyRate.multiply(BigDecimal("2.00")).setScale(2), line.amount)
        assertTrue(line.explanation.contains("Varseltidspunktet registreres separat"))
    }

    @Test
    fun sleepAllowedNightTravelUsesPassiveOneThirdAndCountsAllNightAsWorktime() {
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED, LocalTime.of(23, 0), LocalTime.of(7, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(23, 0)),
            LocalDateTime.of(monday.plusDays(1), LocalTime.of(7, 0)),
        )
        val passive = calculation.lines.first { it.id == "travel-passive-night" }
        assertEquals(480L, passive.evidence.sumOf { it.minutes })
        assertEquals(
            calculation.hourlyRate.multiply(BigDecimal("8")).divide(BigDecimal("3"), 8, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP),
            passive.amount,
        )
        val evening = calculation.lines.first { it.id == "travel-passive-evening-night" }
        assertEquals(420L, evening.evidence.sumOf { it.minutes })
        assertFalse(calculation.applicableUnresolvedRuleIds.contains("D25_20_3_SLEEP_PERMISSION"))

        val findings = TripPlanEngine.controlFindings(TripPlanEngine.projectRange(listOf(monday), plans), 0)
        assertTrue(findings.first { it.title == "Samlet arbeidstid" }.detail.startsWith("8 t "))
        assertTrue(findings.any { it.title == "Passiv nattreise teller som arbeidstid" })
    }

    @Test
    fun passiveNightTravelGetsWeekendSupplementAtOneThirdWithoutDoubleCountingHoliday() {
        val saturday = LocalDate.of(2026, 8, 15)
        val plans = mapOf(saturday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED, LocalTime.of(23, 0), LocalTime.of(7, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(saturday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(saturday, LocalTime.of(23, 0)),
            LocalDateTime.of(saturday.plusDays(1), LocalTime.of(7, 0)),
        )

        assertEquals(480L, calculation.lines.first { it.id == "travel-passive-weekend" }.evidence.sumOf { it.minutes })
        assertFalse(calculation.lines.any { it.id == "travel-passive-holiday" })
    }

    @Test
    fun passiveNightTravelSeparatesHolidayFromWeekendSupplement() {
        val saturday = LocalDate.of(2026, 5, 16)
        val plans = mapOf(saturday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED, LocalTime.of(23, 0), LocalTime.of(7, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(saturday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(saturday, LocalTime.of(23, 0)),
            LocalDateTime.of(saturday.plusDays(1), LocalTime.of(7, 0)),
        )

        assertEquals(60L, calculation.lines.first { it.id == "travel-passive-weekend" }.evidence.sumOf { it.minutes })
        assertEquals(420L, calculation.lines.first { it.id == "travel-passive-holiday" }.evidence.sumOf { it.minutes })
    }

    @Test
    fun noSleepNightTravelStaysOrdinaryTravelAndDoesNotOpenSleepRule() {
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP, LocalTime.of(23, 0), LocalTime.of(1, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(23, 0)),
            LocalDateTime.of(monday.plusDays(1), LocalTime.of(1, 0)),
        )
        val line = calculation.lines.first { it.id == "travel-without-responsibility" }
        assertEquals(120L, line.evidence.sumOf { it.minutes })
        assertEquals(calculation.hourlyRate.multiply(BigDecimal("2.00")).setScale(2), line.amount)
        assertFalse(calculation.lines.any { it.id.startsWith("travel-passive-") })
        assertFalse(calculation.lines.any { it.id == "travel-night-sleep-open" })
        assertFalse(calculation.applicableUnresolvedRuleIds.contains("D25_20_3_SLEEP_PERMISSION"))
    }

    @Test
    fun unresolvedNightTravelIsHeldOpenInsteadOfAssumingOrdinaryTravel() {
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(23, 0), LocalTime.of(1, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(23, 0)),
            LocalDateTime.of(monday.plusDays(1), LocalTime.of(1, 0)),
        )
        val open = calculation.lines.first { it.id == "travel-night-sleep-open" }
        assertEquals(120L, open.evidence.sumOf { it.minutes })
        assertEquals(CalculationCertainty.OPEN, open.certainty)
        assertFalse(open.includedInKnownTotal)
        assertFalse(calculation.lines.any { it.id == "travel-without-responsibility" })
        assertTrue(calculation.applicableUnresolvedRuleIds.contains("D25_20_3_SLEEP_PERMISSION"))
    }

    @Test
    fun unresolvedTravelAcrossNightPaysOnlyKnownNonNightParts() {
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(21, 0), LocalTime.of(8, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(21, 0)),
            LocalDateTime.of(monday.plusDays(1), LocalTime.of(8, 0)),
        )
        assertEquals(180L, calculation.lines.first { it.id == "travel-without-responsibility" }.evidence.sumOf { it.minutes })
        assertEquals(480L, calculation.lines.first { it.id == "travel-night-sleep-open" }.evidence.sumOf { it.minutes })
    }

    @Test
    fun coveredRosterControlIsClippedToTripRange() {
        val friday = LocalDate.of(2026, 8, 14)
        val saturday = friday.plusDays(1)
        val roster = mapOf(friday to "LV", saturday to "LV")
        val plans = mapOf(friday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(friday, saturday),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(friday, LocalTime.of(8, 0)),
            LocalDateTime.of(saturday, LocalTime.of(10, 0)),
        )
        assertEquals(13 * 60L, calculation.rosterMinutes)
        assertEquals(240L, calculation.lines.first { it.id == "evening-night" }.evidence.sumOf { it.minutes })
        assertEquals(60L, calculation.lines.first { it.id == "weekend" }.evidence.sumOf { it.minutes })
    }

    @Test
    fun uncertainTravelRemainsExplicitlyOpen() {
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.TRAVEL_UNCERTAIN, LocalTime.of(8, 0), LocalTime.of(10, 0))))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(8, 0)),
            LocalDateTime.of(monday, LocalTime.of(10, 0)),
        )
        assertTrue(calculation.lines.any { it.id == "travel-responsibility-open" && it.certainty == CalculationCertainty.OPEN })
    }

    @Test
    fun travelWithResponsibilityCanDescribePartOfActiveWorkWithoutDoubleRegistration() {
        val plans = listOf(
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(6, 0), LocalTime.of(22, 0)),
            PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
        )
        val warnings = TripPlanEngine.planOverlapWarnings(monday, plans)
        assertTrue(warnings.isEmpty())

        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            mapOf(monday to plans),
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(6, 0)),
            LocalDateTime.of(monday, LocalTime.of(22, 0)),
        )
        assertEquals(960, calculation.activeMinutes)
    }

    @Test
    fun travelWithResponsibilityExtendingPastActiveWorkAddsOnlyUncoveredMinutes() {
        val plans = listOf(
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(17, 0), LocalTime.of(23, 30)),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            mapOf(monday to plans),
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(7, 0)),
            LocalDateTime.of(monday, LocalTime.of(23, 30)),
        )
        assertEquals(990, calculation.activeMinutes)
    }

    @Test
    fun activeEventInsideRestingNightIsNotReportedAsDoubleRegistration() {
        val warnings = TripPlanEngine.planOverlapWarnings(
            monday,
            listOf(
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
                PlannedBlock(TimeKind.ACTIVE_EVENT_ON_RESTING, LocalTime.of(2, 0), LocalTime.of(2, 30)),
            ),
        )
        assertTrue(warnings.isEmpty())
    }
}

class TripPlanEngineA34Test {
    private val monday = LocalDate.of(2026, 8, 10)

    @Test
    fun nineHourRestingNightShowsThreeHourPayEquivalent() {
        val plans = mapOf(monday to listOf(
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(22, 0), LocalTime.of(7, 0)),
        ))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(22, 0)),
            LocalDateTime.of(monday.plusDays(1), LocalTime.of(7, 0)),
        )
        val line = calculation.lines.single { it.id == "resting-night" }
        assertTrue(line.detail.contains("9 t arbeidstid"))
        assertTrue(line.detail.contains("3 t lønnsekvivalent"))
        assertEquals(BigDecimal("998.82"), line.amount.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun restingNightGetsNightAllowanceAtOneThird() {
        val plans = mapOf(monday to listOf(
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(22, 0), LocalTime.of(7, 0)),
        ))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(22, 0)),
            LocalDateTime.of(monday.plusDays(1), LocalTime.of(7, 0)),
        )
        val line = calculation.lines.single { it.id == "resting-evening-night" }
        assertEquals(BigDecimal("399.54"), line.amount.setScale(2, RoundingMode.HALF_UP))
        assertEquals(CalculationCertainty.CONFIRMED, line.certainty)
    }

    @Test
    fun restingWeekendAllowanceAlsoUsesOneThird() {
        val saturday = LocalDate.of(2026, 8, 15)
        val plans = mapOf(saturday to listOf(
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(22, 0), LocalTime.of(7, 0)),
        ))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(saturday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(saturday, LocalTime.of(22, 0)),
            LocalDateTime.of(saturday.plusDays(1), LocalTime.of(7, 0)),
        )
        val line = calculation.lines.single { it.id == "resting-weekend" }
        assertEquals(BigDecimal("229.74"), line.amount.setScale(2, RoundingMode.HALF_UP))
        assertEquals(CalculationCertainty.CONFIRMED, line.certainty)
    }

    @Test
    fun activeWorkOnRestingNightRoundsPerWatchAndPaysHourlyPlusFiftyPercent() {
        val plans = mapOf(monday to listOf(
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            PlannedBlock(TimeKind.ACTIVE_EVENT_ON_RESTING, LocalTime.of(1, 10), LocalTime.of(1, 22)),
            PlannedBlock(TimeKind.ACTIVE_EVENT_ON_RESTING, LocalTime.of(4, 5), LocalTime.of(4, 15)),
        ))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(23, 0)),
            LocalDateTime.of(monday.plusDays(1), LocalTime.of(7, 0)),
        )
        val line = calculation.lines.single { it.id == "active-on-resting" }
        assertTrue(line.detail.contains("22 min registrert"))
        assertTrue(line.detail.contains("30 min betalt"))
        assertEquals(BigDecimal("249.71"), line.amount.setScale(2, RoundingMode.HALF_UP))
        assertEquals(CalculationCertainty.CONFIRMED, line.certainty)
        assertTrue(line.includedInKnownTotal)
    }

    @Test
    fun fourteenActiveMinutesOnRestingNightRoundToZero() {
        val plans = mapOf(monday to listOf(
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            PlannedBlock(TimeKind.ACTIVE_EVENT_ON_RESTING, LocalTime.of(2, 0), LocalTime.of(2, 14)),
        ))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(23, 0)),
            LocalDateTime.of(monday.plusDays(1), LocalTime.of(7, 0)),
        )
        val line = calculation.lines.single { it.id == "active-on-resting" }
        assertEquals(BigDecimal.ZERO.setScale(2), line.amount.setScale(2, RoundingMode.HALF_UP))
        assertTrue(line.detail.contains("0 min betalt"))
    }
}

class TripPlanEngineA32Test {
    @Test
    fun controlPageDoesNotOwnDoubleRegistrationErrorsAndUsesNorwegianDates() {
        val date = LocalDate.of(2026, 8, 14)
        val blocks = listOf(
            PlannedBlock(TimeKind.ACTIVE_NIGHT_WATCH, LocalTime.of(21, 45), LocalTime.of(7, 45)),
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(15, 0)),
        ).flatMap { TripPlanEngine.projectDay(date, listOf(it)) }
        val findings = TripPlanEngine.controlFindings(blocks, 0)
        assertTrue(findings.none { it.title == "Samme tid er registrert to ganger" })
        assertTrue(findings.none { it.detail.contains("2026-08-") || it.detail.contains("T21:") })
    }
}

class TripPlanEngineA35Test {
    private val may17 = LocalDate.of(2026, 5, 17)

    @Test
    fun ordinaryWorkOnMay17GetsHolidaySupplementAndNotWeekendSupplementForSameMinutes() {
        val roster = mapOf(may17 to "D")
        val plans = TripPlanEngine.planFromRoster(listOf(may17), roster)
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(may17),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(may17, LocalTime.of(7, 0)),
            LocalDateTime.of(may17, LocalTime.of(15, 0)),
        )

        assertEquals(480, calculation.holidayMinutes)
        assertEquals(0, calculation.weekendMinutes)
        val holiday = calculation.lines.single { it.id == "holiday" }
        assertEquals(BigDecimal("3551.36"), holiday.amount.setScale(2, RoundingMode.HALF_UP))
        assertEquals(CalculationCertainty.CONFIRMED, holiday.certainty)
    }

    @Test
    fun restingNightOnHolidayGetsHolidaySupplementAtOneThird() {
        val plans = mapOf(may17 to listOf(
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(22, 0), LocalTime.of(7, 0)),
        ))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(may17),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(may17, LocalTime.of(22, 0)),
            LocalDateTime.of(may17.plusDays(1), LocalTime.of(7, 0)),
        )

        val holiday = calculation.lines.single { it.id == "resting-holiday" }
        assertEquals(BigDecimal("295.95"), holiday.amount.setScale(2, RoundingMode.HALF_UP))
        assertTrue(holiday.detail.contains("2 t hvilende"))
    }

    @Test
    fun outsideRosterWorkOnSpecialHolidayUsesPoint20_2InsteadOfCreating133OvertimeOpenAmount() {
        val roster = mapOf(may17 to "D")
        val plans = mapOf(may17 to listOf(
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(17, 0)),
        ))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(may17),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(may17, LocalTime.of(7, 0)),
            LocalDateTime.of(may17, LocalTime.of(17, 0)),
        )

        val active = calculation.lines.single { it.id == "active" }
        assertEquals(BigDecimal("998.82"), active.amount)
        assertFalse(calculation.lines.any { it.id == "holiday-overtime-open" })
        assertFalse("D25_20_2_X13_7_3" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun maundyThursdayInStandardTurnusGetsHolidaySupplementWithNonDuplicatedFormulaText() {
        val maundyThursday = LocalDate.of(2026, 4, 2)
        val roster = mapOf(maundyThursday to "D")
        val plans = TripPlanEngine.planFromRoster(listOf(maundyThursday), roster)
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(maundyThursday),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(maundyThursday, LocalTime.of(7, 0)),
            LocalDateTime.of(maundyThursday, LocalTime.of(15, 0)),
        )

        assertEquals(480, calculation.holidayMinutes)
        val holiday = calculation.lines.single { it.id == "holiday" }
        assertEquals("8 t × 443,92 kr", holiday.detail)
        assertTrue(holiday.explanation.contains("timelønn × 1 1/3"))
        assertFalse(holiday.detail.contains("1 1/3"))
        val audit = calculation.dayAudits.single()
        assertTrue(audit.holidayLabels.contains("Påske"))
        assertTrue(audit.contributions.any { it.lineId == "holiday" && it.minutes == 480L })
    }

    @Test
    fun dayBeforeStandardEasterWindowDoesNotGetHolidaySupplement() {
        val wednesday = LocalDate.of(2026, 4, 1)
        val block = WorkBlock(
            LocalDateTime.of(wednesday, LocalTime.of(7, 0)),
            LocalDateTime.of(wednesday, LocalTime.of(22, 0)),
            TimeKind.ACTIVE_WORK,
        )
        assertTrue(OsloHolidayCalendar.holidaySupplementSegments(block, WeeklyBasis.HOURS_35_5).isEmpty())
    }

    @Test
    fun easterMondayIsSpecialOvertimeDate() {
        val easterMonday = LocalDate.of(2026, 4, 6)
        assertEquals("2. påskedag", OsloHolidayCalendar.overtime133Dates(2026)[easterMonday])
    }

    @Test
    fun dayAuditShowsHolidayLabelAndHolidayContribution() {
        val roster = mapOf(may17 to "D")
        val plans = TripPlanEngine.planFromRoster(listOf(may17), roster)
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(may17),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(may17, LocalTime.of(7, 0)),
            LocalDateTime.of(may17, LocalTime.of(15, 0)),
        )

        val audit = calculation.dayAudits.single()
        assertTrue(audit.holidayLabels.contains("17. mai"))
        assertTrue(audit.contributions.any { it.lineId == "holiday" && it.minutes == 480L })
        assertEquals(BigDecimal("3551.36"), audit.knownSubtotal.setScale(2, RoundingMode.HALF_UP))
    }
}


class TripPlanEngineA39BIntegrityTest {
    private val monday = LocalDate.of(2026, 8, 10)
    private val salary = OsloSalaryTable2026.annualSalary(32)

    @Test
    fun normalRosterSupplementsAreDocumentedButExcludedFromAdditionalPaymentBasis() {
        val roster = mapOf(monday to "LV")
        val plans = TripPlanEngine.planFromRoster(listOf(monday), roster)
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            roster,
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(9, 0)),
            LocalDateTime.of(monday, LocalTime.of(21, 0)),
        )

        val evening = calculation.lines.single { it.id == "evening-night" }
        assertEquals(PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER, evening.paymentTreatment)
        assertEquals(evening.amount, calculation.alreadyCoveredByNormalRosterAmount)
        assertEquals(BigDecimal("110.00"), calculation.paymentBasisAmount)
        assertEquals(calculation.knownAmount, calculation.paymentBasisAmount.add(calculation.alreadyCoveredByNormalRosterAmount))
    }

    @Test
    fun point20_2EveningWorkDoesNotCreateChapter12CombinationQuestion() {
        val roster = mapOf(monday to "D")
        val plans = mapOf(monday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            roster,
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(7, 0)),
            LocalDateTime.of(monday, LocalTime.of(22, 0)),
        )

        val unresolved = FerieturRules.applicableUnresolvedRules(calculation).map { it.id }.toSet()
        assertTrue(unresolved.isEmpty())
        assertFalse(calculation.lines.any { it.id == "outside-evening-night-open" })
        val activeExplanation = calculation.lines.single { it.id == "active" }.explanation
        assertTrue(activeExplanation.contains("12.1.1"))
        assertTrue(activeExplanation.contains("ikke utbetales for overtid"))
    }

    @Test
    fun specialHolidayOutsideRosterUsesPoint20_2WithoutChapter13OpenRule() {
        val may17 = LocalDate.of(2026, 5, 17)
        val roster = mapOf(may17 to "D")
        val plans = mapOf(may17 to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(may17),
            roster,
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(may17, LocalTime.of(7, 0)),
            LocalDateTime.of(may17, LocalTime.of(22, 0)),
        )

        val unresolved = FerieturRules.applicableUnresolvedRules(calculation).map { it.id }.toSet()
        assertFalse("D25_20_2_X13_7_3" in unresolved)
        assertFalse(calculation.lines.any { it.id == "holiday-overtime-open" })
    }

    @Test
    fun visibleLineAmountsAreRoundedBeforeTheyAreSummed() {
        val saturday = LocalDate.of(2026, 8, 15)
        val plans = mapOf(saturday to listOf(
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
        ))
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(saturday),
            emptyMap(),
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(saturday, LocalTime.of(7, 0)),
            LocalDateTime.of(saturday.plusDays(1), LocalTime.of(7, 0)),
        )

        calculation.lines.forEach { line -> assertTrue("${line.id} has more than two decimals", line.amount.scale() <= 2) }
        val visibleSum = calculation.lines.filter { it.includedInKnownTotal }.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.amount) }.setScale(2)
        assertEquals(visibleSum, calculation.knownAmount)
        val paymentSum = calculation.lines.filter { it.includedInKnownTotal && it.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS }
            .fold(BigDecimal.ZERO) { acc, line -> acc.add(line.amount) }.setScale(2)
        assertEquals(paymentSum, calculation.paymentBasisAmount)
    }

    @Test
    fun dayAuditCentsAddBackToEveryDistributedLineExactly() {
        val tuesday = monday.plusDays(1)
        val plans = mapOf(
            monday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
            tuesday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.VACATION_SEPARATE,
            listOf(monday, tuesday),
            emptyMap(),
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(7, 0)),
            LocalDateTime.of(tuesday, LocalTime.of(22, 0)),
        )

        calculation.lines.filter { it.id != "stay-allowance" }.forEach { line ->
            val distributed = calculation.dayAudits.flatMap { it.contributions }.filter { it.lineId == line.id }
                .fold(BigDecimal.ZERO) { acc, contribution -> acc.add(contribution.amount) }.setScale(2)
            assertEquals("day audit mismatch for ${line.id}", line.amount.setScale(2), distributed)
        }
    }

    @Test
    fun workAfterTripEndIsReturnedAsBlockingRangeIssue() {
        val blocks = listOf(
            WorkBlock(
                LocalDateTime.of(monday, LocalTime.of(23, 0)),
                LocalDateTime.of(monday, LocalTime.of(23, 59)),
                TimeKind.ACTIVE_WORK,
            ),
        )
        val issues = TripPlanEngine.outsideTripRangeBlocks(
            blocks,
            LocalDateTime.of(monday, LocalTime.of(6, 0)),
            LocalDateTime.of(monday, LocalTime.of(23, 0)),
        )
        assertEquals(1, issues.size)
    }
}

class TripPlanEngineA39CR1PdfIntegrityTest {
    private val salary = OsloSalaryTable2026.annualSalary(32)

    @Test
    fun coveredTurnusSupplementsComeFromOriginalRosterEvenWhenTripWorkDoesNotCoverWholeShift() {
        val saturday = LocalDate.of(2026, 8, 15)
        val roster = mapOf(saturday to "N2")
        val plans = mapOf(
            saturday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            ),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(saturday),
            roster,
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(saturday, LocalTime.of(7, 0)),
            LocalDateTime.of(saturday.plusDays(1), LocalTime.of(7, 45)),
        )

        val evening = calculation.lines.single { it.id == "evening-night" }
        val weekend = calculation.lines.single { it.id == "weekend" }
        assertEquals(600L, evening.evidence.sumOf { it.minutes })
        assertEquals(600L, weekend.evidence.sumOf { it.minutes })
        assertEquals(PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER, evening.paymentTreatment)
        assertEquals(PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER, weekend.paymentTreatment)
    }

    @Test
    fun additionalActiveLineSeparatesWorkFromTravelWithResponsibility() {
        val day = LocalDate.of(2026, 8, 11)
        val roster = mapOf(day to "D1")
        val plans = mapOf(
            day to listOf(
                PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(6, 0), LocalTime.of(11, 0)),
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(11, 0), LocalTime.of(22, 0)),
            ),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(day),
            roster,
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(day, LocalTime.of(6, 0)),
            LocalDateTime.of(day, LocalTime.of(22, 0)),
        )

        assertEquals(450L, calculation.payableActiveWorkMinutes)
        assertEquals(60L, calculation.payableTravelWithResponsibilityMinutes)
        val active = calculation.lines.single { it.id == "active" }
        assertTrue(active.title.contains("reise"))
        assertTrue(active.detail.contains("1 t reise med ansvar"))
        assertTrue(active.source.contains("20.3"))
    }
}


class TripPlanEngineA39CR2HardeningTest {
    private val salary = OsloSalaryTable2026.annualSalary(32)

    @Test
    fun point20_2EveningMinutesDoNotStackChapter12EveningSupplement() {
        val monday = LocalDate.of(2026, 8, 10)
        val roster = mapOf(monday to "D")
        val plans = mapOf(
            monday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            roster,
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(7, 0)),
            LocalDateTime.of(monday, LocalTime.of(22, 0)),
        )

        assertFalse(calculation.lines.any { it.id == "outside-evening-night-open" })
        assertFalse("D25_20_2_X12_13" in calculation.applicableUnresolvedRuleIds)
        assertEquals(BigDecimal("3495.87"), calculation.lines.single { it.id == "active" }.amount)
    }

    @Test
    fun point20_2SaturdayMinutesDoNotStackChapter12EveningOrWeekendSupplements() {
        val saturday = LocalDate.of(2026, 8, 15)
        val roster = mapOf(saturday to "D")
        val plans = mapOf(
            saturday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(saturday),
            roster,
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(saturday, LocalTime.of(7, 0)),
            LocalDateTime.of(saturday, LocalTime.of(22, 0)),
        )

        assertFalse(calculation.lines.any { it.id == "outside-evening-night-open" })
        assertFalse(calculation.lines.any { it.id == "outside-weekend-open" })
        assertFalse("D25_20_2_X12_13" in calculation.applicableUnresolvedRuleIds)
    }

    @Test
    fun rosterGapEvidenceIdentifiesExactUnregisteredHourInsideNightShift() {
        val saturday = LocalDate.of(2026, 8, 15)
        val sunday = saturday.plusDays(1)
        val roster = mapOf(saturday to "N2")
        val plans = mapOf(
            saturday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            ),
            sunday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
        )
        val calculation = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(saturday, sunday),
            roster,
            plans,
            salary,
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(saturday, LocalTime.of(7, 0)),
            LocalDateTime.of(sunday, LocalTime.of(22, 0)),
        )

        assertEquals(60L, calculation.rosterUncoveredMinutes)
        val gap = calculation.rosterUncoveredEvidence.single()
        assertEquals(LocalDateTime.of(saturday, LocalTime.of(22, 0)), gap.start)
        assertEquals(LocalDateTime.of(saturday, LocalTime.of(23, 0)), gap.end)
    }
}
