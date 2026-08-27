import app.ferietur.domain.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun main() {
    val salary = OsloSalaryTable2026.annualSalary(32)
    val start = LocalDate.of(2026, 8, 11)
    val dates = (0..7).map { start.plusDays(it.toLong()) }
    val roster = mapOf(
        start to "D1", start.plusDays(1) to "LV", start.plusDays(2) to "F1", start.plusDays(3) to "AL",
        start.plusDays(4) to "N2", start.plusDays(5) to "F2", start.plusDays(6) to "D", start.plusDays(7) to "D1",
    )
    val plans = mapOf(
        start to listOf(
            PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(6, 0), LocalTime.of(11, 0)),
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(11, 0), LocalTime.of(22, 0)),
        ),
        start.plusDays(1) to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
        start.plusDays(2) to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0))),
        start.plusDays(3) to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)), PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0))),
        start.plusDays(4) to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)), PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0))),
        start.plusDays(5) to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)), PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0))),
        start.plusDays(6) to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)), PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0))),
        start.plusDays(7) to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(16, 0)), PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(16, 0), LocalTime.of(23, 0))),
    )
    val solgarden = TripPlanEngine.calculatePreliminary(
        FundingMode.TURNUS_PLUS_EXTERNAL, dates, roster, plans, salary, WeeklyBasis.HOURS_35_5, WeekendProfile.STANDARD,
        LocalDateTime.of(start, LocalTime.of(6, 0)), LocalDateTime.of(start.plusDays(7), LocalTime.of(23, 0)),
    )
    check(solgarden.paymentBasisAmount.setScale(2) == BigDecimal("43267.74"))
    check(solgarden.rosterUncoveredMinutes == 60L)
    check(solgarden.applicableUnresolvedRuleIds.isEmpty())

    val monday = LocalDate.of(2026, 8, 10)
    val nightDates = listOf(monday, monday.plusDays(1))
    val nightRoster = mapOf(monday to "D1", monday.plusDays(1) to "LV")
    fun night(kind: TimeKind) = TripPlanEngine.calculatePreliminary(
        FundingMode.TURNUS_PLUS_EXTERNAL,
        nightDates,
        nightRoster,
        mapOf(monday to listOf(PlannedBlock(kind, LocalTime.of(23, 0), LocalTime.of(7, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY))),
        salary,
        WeeklyBasis.HOURS_35_5,
        WeekendProfile.STANDARD,
        LocalDateTime.of(monday, LocalTime.of(22, 0)),
        LocalDateTime.of(monday.plusDays(1), LocalTime.of(8, 0)),
    )
    check(night(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED).paymentBasisAmount.setScale(2) == BigDecimal("1308.59"))
    check(night(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY).paymentBasisAmount.setScale(2) == BigDecimal("110.00"))
    check(night(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP).paymentBasisAmount.setScale(2) == BigDecimal("2773.52"))

    val separate = TripPlanEngine.calculatePreliminary(
        FundingMode.VACATION_SEPARATE,
        listOf(monday, monday.plusDays(1)),
        mapOf(monday to "AL", monday.plusDays(1) to "D"),
        mapOf(monday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(15, 0), LocalTime.of(19, 0)))),
        salary,
        WeeklyBasis.HOURS_35_5,
        WeekendProfile.STANDARD,
        LocalDateTime.of(monday, LocalTime.of(15, 0)),
        LocalDateTime.of(monday.plusDays(1), LocalTime.of(15, 0)),
    )
    check(separate.paymentBasisAmount.setScale(2) == BigDecimal("1708.12"))
    check(separate.alreadyCoveredByNormalRosterAmount.setScale(2) == BigDecimal("0.00"))
    check(separate.rosterUncoveredMinutes == 0L)
    check(separate.lines.none { it.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER })

    check(FERIETUR_APP_VERSION.isNotBlank())
    check(FERIETUR_RULESET_VERSION == "2026.3")
    println("A44_FINAL_REGRESSION_DOMAIN_SMOKE=PASS")
}
