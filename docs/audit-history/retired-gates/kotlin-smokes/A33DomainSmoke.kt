import app.ferietur.domain.FundingMode
import app.ferietur.domain.OsloSalaryTable2026
import app.ferietur.domain.PlannedBlock
import app.ferietur.domain.TimeKind
import app.ferietur.domain.TripPlanEngine
import app.ferietur.domain.WeeklyBasis
import app.ferietur.domain.WeekendProfile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun main() {
    val monday = LocalDate.of(2026, 8, 10)
    val base = mapOf(
        monday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(14, 30))),
    )
    val travel = mapOf(
        monday to listOf(PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(7, 0), LocalTime.of(11, 0))),
    )
    val overlaid = TripPlanEngine.overlayTravelOnPlan(listOf(monday), base, travel)
    val blocks = overlaid.getValue(monday)
    check(blocks.size == 2)
    check(blocks.any { it.kind == TimeKind.TRAVEL_WITH_RESPONSIBILITY && it.start == LocalTime.of(7, 0) && it.end == LocalTime.of(11, 0) })
    check(blocks.any { it.kind == TimeKind.ACTIVE_WORK && it.start == LocalTime.of(11, 0) && it.end == LocalTime.of(14, 30) })

    val saturday = LocalDate.of(2026, 8, 15)
    val roster = mapOf(saturday to "D")
    val plan = TripPlanEngine.planFromRoster(listOf(saturday), roster)
    val calc = TripPlanEngine.calculatePreliminary(
        fundingMode = FundingMode.TURNUS_PLUS_EXTERNAL,
        dates = listOf(saturday),
        roster = roster,
        plans = plan,
        annualSalary = OsloSalaryTable2026.annualSalary(32),
        weeklyBasis = WeeklyBasis.HOURS_35_5,
        weekendProfile = WeekendProfile.STANDARD,
        tripStart = LocalDateTime.of(saturday, LocalTime.of(7, 0)),
        tripEnd = LocalDateTime.of(saturday, LocalTime.of(20, 0)),
    )
    val weekend = calc.lines.single { it.id == "weekend" }
    check(weekend.includedInKnownTotal)
    check(weekend.amount.signum() > 0)
    check(calc.knownAmount >= weekend.amount)

    println("FERIETUR01_A33_DOMAIN_SMOKE=PASS")
}
