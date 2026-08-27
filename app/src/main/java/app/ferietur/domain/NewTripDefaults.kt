package app.ferietur.domain

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

data class NewTripDefaults(
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    /**
     * These are only editor start values. Ferietur does not treat them as
     * confirmed until the user completes the payslip confirmation.
     */
    val provisionalSalaryStep: Int,
    val provisionalWeeklyBasis: WeeklyBasis,
    val provisionalWeekendProfile: WeekendProfile,
)

object NewTripDefaultsFactory {
    fun current(): NewTripDefaults = create(Clock.systemDefaultZone())

    fun create(clock: Clock): NewTripDefaults {
        val today = LocalDate.now(clock)
        val start = maxOf(today, OsloSalaryTables.earliestSupportedDate)
        return NewTripDefaults(
            title = "",
            startDate = start,
            endDate = start.plusDays(6),
            startTime = LocalTime.of(7, 0),
            endTime = LocalTime.of(20, 0),
            provisionalSalaryStep = 32,
            provisionalWeeklyBasis = WeeklyBasis.HOURS_35_5,
            provisionalWeekendProfile = WeekendProfile.STANDARD,
        )
    }
}
