import app.ferietur.domain.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun main() {
    val salary = OsloSalaryTable2026.annualSalary(32)
    val monday = LocalDate.of(2026, 8, 10)
    val mondayCalc = TripPlanEngine.calculatePreliminary(
        FundingMode.TURNUS_PLUS_EXTERNAL,
        listOf(monday),
        mapOf(monday to "D"),
        mapOf(monday to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)))),
        salary,
        WeeklyBasis.HOURS_35_5,
        WeekendProfile.STANDARD,
        LocalDateTime.of(monday, LocalTime.of(7, 0)),
        LocalDateTime.of(monday, LocalTime.of(22, 0)),
    )
    check(mondayCalc.lines.none { it.id == "outside-evening-night-open" })
    check("D25_20_2_X12_13" !in mondayCalc.applicableUnresolvedRuleIds)
    val mondayActive = mondayCalc.lines.single { it.id == "active" }
    check(mondayActive.amount == BigDecimal("3495.87"))
    check("12.1.1" in mondayActive.explanation)
    check("ikke utbetales for overtid" in mondayActive.explanation)
    check("12.2.2" in mondayActive.explanation)

    val may17 = LocalDate.of(2026, 5, 17)
    val holidayCalc = TripPlanEngine.calculatePreliminary(
        FundingMode.TURNUS_PLUS_EXTERNAL,
        listOf(may17),
        mapOf(may17 to "D"),
        mapOf(may17 to listOf(PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(17, 0)))),
        salary,
        WeeklyBasis.HOURS_35_5,
        WeekendProfile.STANDARD,
        LocalDateTime.of(may17, LocalTime.of(7, 0)),
        LocalDateTime.of(may17, LocalTime.of(17, 0)),
    )
    check(holidayCalc.lines.none { it.id == "holiday-overtime-open" })
    check("D25_20_2_X13_7_3" !in holidayCalc.applicableUnresolvedRuleIds)
    check(holidayCalc.lines.single { it.id == "active" }.amount == BigDecimal("998.82"))

    val rules = FerieturRules.rules.associateBy { it.id }
    check(rules.getValue("D25_20_2_X12_13").status == RuleStatus.IMPLEMENTED)
    check(rules.getValue("D25_20_2_X13_7_3").status == RuleStatus.IMPLEMENTED)
    println("A41_TARIFF_PRIORITY_DOMAIN_SMOKE=PASS")
}
