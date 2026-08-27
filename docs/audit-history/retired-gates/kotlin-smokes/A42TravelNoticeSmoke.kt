import app.ferietur.domain.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun main() {
    val date = LocalDate.of(2026, 8, 10)
    val salary = OsloSalaryTable2026.annualSalary(32)
    fun calculate(status: TravelNoticeStatus) = TripPlanEngine.calculatePreliminary(
        FundingMode.TURNUS_PLUS_EXTERNAL,
        listOf(date),
        emptyMap(),
        mapOf(date to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0), status))),
        salary,
        WeeklyBasis.HOURS_35_5,
        WeekendProfile.STANDARD,
        LocalDateTime.of(date, LocalTime.of(8, 0)),
        LocalDateTime.of(date, LocalTime.of(10, 0)),
    )

    val known = calculate(TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY)
    check(known.lines.none { it.id == "travel-short-notice-overtime" })
    check(known.lines.none { it.id == "travel-notice-open" })

    val unknown = calculate(TravelNoticeStatus.NOT_CLARIFIED)
    check(unknown.lines.any { it.id == "travel-notice-open" })
    check("D25_18_4_NOTICE" in unknown.applicableUnresolvedRuleIds)

    val short = calculate(TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY)
    val overtime = short.lines.single { it.id == "travel-short-notice-overtime" }
    check(overtime.detail.contains("50 %"))
    check(overtime.amount == short.hourlyRate.setScale(2, RoundingMode.HALF_UP))
    check(short.lines.single { it.id == "travel-without-responsibility" }.amount == short.hourlyRate.multiply(BigDecimal("2")).setScale(2, RoundingMode.HALF_UP))
    println("A42_TRAVEL_NOTICE_DOMAIN_SMOKE=PASS")
}
