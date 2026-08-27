import app.ferietur.domain.FindingSeverity
import app.ferietur.domain.OsloSalaryTable2026
import app.ferietur.domain.PlannedBlock
import app.ferietur.domain.TariffMath
import app.ferietur.domain.TimeKind
import app.ferietur.domain.TripPlanEngine
import app.ferietur.domain.WeeklyBasis
import app.ferietur.domain.WeekendProfile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

fun main() {
    val hourly = TariffMath.hourlyRate(OsloSalaryTable2026.annualSalary(32), WeeklyBasis.HOURS_35_5)
    check(hourly.compareTo(BigDecimal("332.94")) == 0)
    check(TariffMath.eveningNightRate(hourly).compareTo(BigDecimal("133.18")) == 0)
    check(TariffMath.weekendRate(hourly, WeekendProfile.STANDARD).compareTo(BigDecimal("76.58")) == 0)
    check(TariffMath.weekendRate(hourly, WeekendProfile.EXTENDED_30).compareTo(BigDecimal("110.00")) == 0)
    check(TariffMath.weekendRate(hourly, WeekendProfile.EXTENDED_35).compareTo(BigDecimal("135.00")) == 0)

    val date = LocalDate.of(2026, 8, 14)
    val blocks = TripPlanEngine.projectDay(
        date,
        listOf(
            PlannedBlock(TimeKind.ACTIVE_NIGHT_WATCH, LocalTime.of(21, 45), LocalTime.of(7, 45)),
        ),
    )
    val findings = TripPlanEngine.controlFindings(blocks, 0)
    check(findings.none { it.title == "Samme tid er registrert to ganger" })
    check(findings.all { !it.detail.contains("2026-08-") && !it.detail.contains("T21:") })

    println("FERIETUR01_A32_DOMAIN_SMOKE=PASS")
}
