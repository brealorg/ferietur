import app.ferietur.domain.CalculationCertainty
import app.ferietur.domain.FundingMode
import app.ferietur.domain.OsloSalaryTable2026
import app.ferietur.domain.PlannedBlock
import app.ferietur.domain.TariffMath
import app.ferietur.domain.TimeKind
import app.ferietur.domain.TripPlanEngine
import app.ferietur.domain.WeeklyBasis
import app.ferietur.domain.WeekendProfile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun main() {
    val date = LocalDate.of(2026, 8, 10)
    val salary = OsloSalaryTable2026.annualSalary(32)
    val hourly = TariffMath.hourlyRate(salary, WeeklyBasis.HOURS_35_5)
    check(TariffMath.weekendRate(hourly, WeekendProfile.EXTENDED_30).compareTo(java.math.BigDecimal("110")) == 0)
    check(TariffMath.weekendRate(hourly, WeekendProfile.EXTENDED_35).compareTo(java.math.BigDecimal("135")) == 0)

    val responsible = mapOf(date to listOf(PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0))))
    val responsibleResult = TripPlanEngine.calculatePreliminary(
        FundingMode.TURNUS_PLUS_EXTERNAL,
        listOf(date),
        emptyMap(),
        responsible,
        salary,
        WeeklyBasis.HOURS_35_5,
        WeekendProfile.STANDARD,
        LocalDateTime.of(date, LocalTime.of(8, 0)),
        LocalDateTime.of(date, LocalTime.of(10, 0)),
    )
    check(responsibleResult.activeMinutes == 120L)

    val noResponsibility = mapOf(date to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0))))
    val noResponsibilityResult = TripPlanEngine.calculatePreliminary(
        FundingMode.TURNUS_PLUS_EXTERNAL,
        listOf(date),
        emptyMap(),
        noResponsibility,
        salary,
        WeeklyBasis.HOURS_35_5,
        WeekendProfile.STANDARD,
        LocalDateTime.of(date, LocalTime.of(8, 0)),
        LocalDateTime.of(date, LocalTime.of(10, 0)),
    )
    check(noResponsibilityResult.activeMinutes == 0L)
    check(noResponsibilityResult.lines.first { it.id == "travel-without-responsibility-open" }.certainty == CalculationCertainty.OPEN)

    val overlap = TripPlanEngine.planOverlapWarnings(
        date,
        listOf(
            PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(6, 0), LocalTime.of(22, 0)),
            PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
        ),
    )
    check(overlap.isNotEmpty())

    println("FERIETUR01_A31_DOMAIN_SMOKE=PASS")
}
