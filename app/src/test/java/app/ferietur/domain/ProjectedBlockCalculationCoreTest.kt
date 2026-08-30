package app.ferietur.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectedBlockCalculationCoreTest {
    private val monday = LocalDate.of(2026, 8, 10)

    @Test
    fun plannedAndProjectedEntryPointsAreExactlyEquivalentForRepresentativeTrip() {
        val dates = listOf(monday)
        val plans = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                PlannedBlock(
                    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
                    LocalTime.of(10, 0),
                    LocalTime.of(11, 0),
                    TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
                ),
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
                PlannedBlock(TimeKind.ACTIVE_EVENT_ON_RESTING, LocalTime.of(2, 0), LocalTime.of(2, 20)),
            ),
        )
        val tripStart = LocalDateTime.of(monday, LocalTime.of(8, 0))
        val tripEnd = LocalDateTime.of(monday.plusDays(1), LocalTime.of(7, 0))
        val annualSalary = OsloSalaryTable2026.annualSalary(32)

        val planned = TripPlanEngine.calculatePreliminary(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = emptyMap(),
            plans = plans,
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = tripStart,
            tripEnd = tripEnd,
        )
        val projected = TripPlanEngine.calculatePreliminaryFromProjectedBlocks(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = dates,
            roster = emptyMap(),
            blocks = TripPlanEngine.projectRange(dates, plans),
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = tripStart,
            tripEnd = tripEnd,
        )

        assertEquals(planned, projected)
    }

    @Test
    fun projectedCoreConsumesExactCrossMidnightBlocksWithoutDateReprojection() {
        val resting = WorkBlock(
            start = LocalDateTime.of(monday, LocalTime.of(23, 0)),
            end = LocalDateTime.of(monday.plusDays(1), LocalTime.of(7, 0)),
            kind = TimeKind.RESTING_NIGHT_WATCH,
        )
        val activeEvent = WorkBlock(
            start = LocalDateTime.of(monday.plusDays(1), LocalTime.of(2, 0)),
            end = LocalDateTime.of(monday.plusDays(1), LocalTime.of(2, 20)),
            kind = TimeKind.ACTIVE_EVENT_ON_RESTING,
        )

        val calculation = TripPlanEngine.calculatePreliminaryFromProjectedBlocks(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(monday),
            roster = emptyMap(),
            blocks = listOf(resting, activeEvent),
            annualSalary = OsloSalaryTable2026.annualSalary(32),
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = resting.start,
            tripEnd = resting.end,
        )

        val line = calculation.lines.single { it.id == "active-on-resting" }
        val eventEvidence = line.evidence.single { evidence ->
            evidence.start == activeEvent.start && evidence.end == activeEvent.end
        }
        val watchSummary = line.evidence.single { evidence ->
            evidence.start == resting.start && evidence.end == resting.end
        }
        assertEquals(activeEvent.start, eventEvidence.start)
        assertEquals(activeEvent.end, eventEvidence.end)
        assertEquals(20L, eventEvidence.minutes)
        assertEquals(30L, watchSummary.minutes)
        assertTrue(line.detail.contains("20 min registrert"))
        assertTrue(line.detail.contains("30 min betalt"))
    }

    @Test
    fun projectedCoreUsesTheInjectedRateSetForPayment() {
        val rateSet = FerieturTariffRates.dok25_2026_2028.copy(
            id = "test-projected-core-rate-set",
            chapter20ActiveMultiplier = BigDecimal("2.00"),
        )
        val block = WorkBlock(
            start = LocalDateTime.of(monday, LocalTime.of(8, 0)),
            end = LocalDateTime.of(monday, LocalTime.of(10, 0)),
            kind = TimeKind.ACTIVE_WORK,
        )
        val annualSalary = OsloSalaryTable2026.annualSalary(32)

        val calculation = TripPlanEngine.calculatePreliminaryFromProjectedBlocks(
            fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
            dates = listOf(monday),
            roster = emptyMap(),
            blocks = listOf(block),
            annualSalary = annualSalary,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = block.start,
            tripEnd = block.end,
            rateSet = rateSet,
        )

        val expected = calculation.hourlyRate
            .multiply(BigDecimal("2"))
            .multiply(BigDecimal("2.00"))
            .setScale(2, RoundingMode.HALF_UP)
        assertEquals(expected, calculation.lines.single { it.id == "active" }.amount)
    }

    @Test
    fun projectedCoreLeavesWholeTripStayAllowanceAtTheSuppliedTripScope() {
        val tripStart = LocalDateTime.of(monday, LocalTime.of(7, 0))
        val tripEnd = tripStart.plusDays(1).plusHours(6)

        val calculation = TripPlanEngine.calculatePreliminaryFromProjectedBlocks(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = listOf(monday, monday.plusDays(1)),
            roster = emptyMap(),
            blocks = emptyList(),
            annualSalary = OsloSalaryTable2026.annualSalary(32),
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = tripStart,
            tripEnd = tripEnd,
        )

        assertEquals(1, calculation.stayAllowanceDays)
        assertTrue(calculation.lines.any { it.id == "stay-allowance-exact-threshold-open" })
    }
}
