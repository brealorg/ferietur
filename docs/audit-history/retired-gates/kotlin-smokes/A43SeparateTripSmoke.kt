import app.ferietur.domain.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun main() {
    val monday = LocalDate.of(2026, 8, 10)
    val tuesday = monday.plusDays(1)
    val salary = OsloSalaryTable2026.annualSalary(32)

    val unresolved = TripPlanEngine.calculatePreliminary(
        FundingMode.TURNUS_PLUS_EXTERNAL,
        listOf(monday, tuesday),
        emptyMap(),
        mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(15, 0), LocalTime.of(17, 0), TravelNoticeStatus.NOT_CLARIFIED),
            ),
        ),
        salary,
        WeeklyBasis.HOURS_35_5,
        WeekendProfile.STANDARD,
        LocalDateTime.of(monday, LocalTime.of(15, 0)),
        LocalDateTime.of(tuesday, LocalTime.of(15, 0)),
    )
    val open = unresolved.lines.single { it.id == "travel-notice-open" }
    check(open.amount == unresolved.hourlyRate.setScale(2, RoundingMode.HALF_UP))
    check(!open.includedInKnownTotal)
    check(unresolved.paymentBasisAmount == BigDecimal("775.88"))

    fun separate(roster: Map<LocalDate, String>) = TripPlanEngine.calculatePreliminary(
        FundingMode.VACATION_SEPARATE,
        listOf(monday, tuesday),
        roster,
        mapOf(monday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(15, 0), LocalTime.of(19, 0)))),
        salary,
        WeeklyBasis.HOURS_35_5,
        WeekendProfile.STANDARD,
        LocalDateTime.of(monday, LocalTime.of(15, 0)),
        LocalDateTime.of(tuesday, LocalTime.of(15, 0)),
    )
    val noRoster = separate(emptyMap())
    val withRoster = separate(mapOf(monday to "AL", tuesday to "D"))
    check(noRoster.paymentBasisAmount == withRoster.paymentBasisAmount)
    check(withRoster.rosterUncoveredMinutes == 0L)
    check(withRoster.lines.none { it.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER })
    println("A43_SEPARATE_TRIP_E2E_SMOKE=PASS")
}
