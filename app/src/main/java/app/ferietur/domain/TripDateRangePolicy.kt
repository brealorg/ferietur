package app.ferietur.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Canonical calendar-date coverage for a trip.
 *
 * Ferietur's UI, roster and plan maps use one source date for every calendar
 * day in the selected inclusive date range. Never truncate this list silently.
 */
object TripDateRangePolicy {
    fun inclusiveDates(start: LocalDate, end: LocalDate): List<LocalDate> {
        if (end.isBefore(start)) return listOf(start)
        val dayCount = ChronoUnit.DAYS.between(start, end)
        require(dayCount <= Int.MAX_VALUE.toLong() - 1L) {
            "Datoperioden er for lang."
        }
        return (0L..dayCount).map(start::plusDays)
    }

    fun hasCompleteCoverage(
        dates: List<LocalDate>,
        start: LocalDate,
        end: LocalDate,
    ): Boolean = !end.isBefore(start) && dates == inclusiveDates(start, end)

    fun requireCompleteCoverage(
        dates: List<LocalDate>,
        start: LocalDate,
        end: LocalDate,
    ) {
        require(hasCompleteCoverage(dates, start, end)) {
            "Datolisten dekker ikke hele turperioden $start–$end."
        }
    }
}
