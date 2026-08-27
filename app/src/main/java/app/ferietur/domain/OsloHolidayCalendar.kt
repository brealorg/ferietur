package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime


data class TariffTimeWindow(
    val id: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
)

data class TariffTimeSegment(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val title: String,
)

object OsloHolidayCalendar {
    fun holidaySupplementWindows(year: Int, basis: WeeklyBasis): List<TariffTimeWindow> {
        val easter = easterSunday(year)
        val longShiftWindow = basis == WeeklyBasis.HOURS_33_6
        val windows = mutableListOf<TariffTimeWindow>()

        windows += if (longShiftWindow) {
            TariffTimeWindow(
                id = "new-year-$year",
                title = "Nyttår",
                start = LocalDate.of(year - 1, 12, 31).atTime(12, 0),
                end = LocalDate.of(year, 1, 2).atTime(7, 0),
            )
        } else {
            TariffTimeWindow(
                id = "new-year-$year",
                title = "Nyttår",
                start = LocalDate.of(year - 1, 12, 31).atTime(12, 0),
                end = LocalDate.of(year, 1, 2).atStartOfDay(),
            )
        }

        windows += if (longShiftWindow) {
            TariffTimeWindow(
                id = "easter-$year",
                title = "Påske",
                start = easter.minusDays(4).atTime(13, 0),
                end = easter.plusDays(2).atTime(7, 0),
            )
        } else {
            TariffTimeWindow(
                id = "easter-$year",
                title = "Påske",
                start = easter.minusDays(3).atStartOfDay(),
                end = easter.plusDays(2).atStartOfDay(),
            )
        }

        windows += TariffTimeWindow(
            id = "may-1-$year",
            title = "1. mai",
            start = LocalDate.of(year, 5, 1).atStartOfDay(),
            end = LocalDate.of(year, 5, 2).atStartOfDay(),
        )
        windows += TariffTimeWindow(
            id = "may-17-$year",
            title = "17. mai",
            start = LocalDate.of(year, 5, 17).atStartOfDay(),
            end = LocalDate.of(year, 5, 18).atStartOfDay(),
        )

        val ascension = easter.plusDays(39)
        windows += if (longShiftWindow) {
            TariffTimeWindow(
                id = "ascension-$year",
                title = "Kristi himmelfart",
                start = ascension.minusDays(1).atTime(13, 0),
                end = ascension.plusDays(1).atTime(7, 0),
            )
        } else {
            TariffTimeWindow(
                id = "ascension-$year",
                title = "Kristi himmelfartsdag",
                start = ascension.atStartOfDay(),
                end = ascension.plusDays(1).atStartOfDay(),
            )
        }

        val pentecostSunday = easter.plusDays(49)
        windows += if (longShiftWindow) {
            TariffTimeWindow(
                id = "pentecost-$year",
                title = "Pinse",
                start = pentecostSunday.minusDays(1).atTime(12, 0),
                end = pentecostSunday.plusDays(2).atTime(7, 0),
            )
        } else {
            TariffTimeWindow(
                id = "pentecost-$year",
                title = "Pinse",
                start = pentecostSunday.minusDays(1).atTime(12, 0),
                end = pentecostSunday.plusDays(2).atStartOfDay(),
            )
        }

        windows += if (longShiftWindow) {
            TariffTimeWindow(
                id = "christmas-$year",
                title = "Jul",
                start = LocalDate.of(year, 12, 24).atTime(12, 0),
                end = LocalDate.of(year, 12, 27).atTime(7, 0),
            )
        } else {
            TariffTimeWindow(
                id = "christmas-$year",
                title = "Jul",
                start = LocalDate.of(year, 12, 24).atTime(12, 0),
                end = LocalDate.of(year, 12, 27).atStartOfDay(),
            )
        }

        return windows.sortedBy { it.start }
    }

    fun holidaySupplementSegments(block: WorkBlock, basis: WeeklyBasis): List<TariffTimeSegment> {
        val years = (block.start.year - 1)..(block.end.year + 1)
        return years.flatMap { holidaySupplementWindows(it, basis) }
            .distinctBy { it.id to it.start }
            .mapNotNull { window ->
                intersection(block.start, block.end, window.start, window.end)?.let { (start, end) ->
                    TariffTimeSegment(start, end, window.title)
                }
            }
            .sortedBy { it.start }
    }

    fun holidayLabelsForDate(date: LocalDate, basis: WeeklyBasis): List<String> {
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()
        return ((date.year - 1)..(date.year + 1)).flatMap { holidaySupplementWindows(it, basis) }
            .filter { intersection(start, end, it.start, it.end) != null }
            .map { it.title }
            .distinct()
    }

    fun overtime133Segments(block: WorkBlock): List<TariffTimeSegment> {
        val dates = mutableListOf<LocalDate>()
        var date = block.start.toLocalDate()
        val endDate = block.end.minusNanos(1).toLocalDate()
        while (!date.isAfter(endDate)) {
            dates += date
            date = date.plusDays(1)
        }
        return dates.mapNotNull { day ->
            val title = overtime133Dates(day.year)[day] ?: return@mapNotNull null
            val start = day.atStartOfDay()
            val end = day.plusDays(1).atStartOfDay()
            intersection(block.start, block.end, start, end)?.let { (segmentStart, segmentEnd) ->
                TariffTimeSegment(segmentStart, segmentEnd, title)
            }
        }
    }

    fun isOvertime133Date(date: LocalDate): Boolean = overtime133Dates(date.year).containsKey(date)

    fun isPublicHoliday(date: LocalDate): Boolean = date in publicHolidayDates(date.year)

    fun isDayBeforeSundayOrPublicHoliday(date: LocalDate): Boolean {
        val next = date.plusDays(1)
        return next.dayOfWeek.value == 7 || isPublicHoliday(next)
    }

    fun publicHolidayDates(year: Int): Set<LocalDate> {
        val easter = easterSunday(year)
        return setOf(
            LocalDate.of(year, 1, 1),
            easter.minusDays(3),
            easter.minusDays(2),
            easter,
            easter.plusDays(1),
            LocalDate.of(year, 5, 1),
            LocalDate.of(year, 5, 17),
            easter.plusDays(39),
            easter.plusDays(49),
            easter.plusDays(50),
            LocalDate.of(year, 12, 25),
            LocalDate.of(year, 12, 26),
        )
    }

    fun overtime133Dates(year: Int): Map<LocalDate, String> {
        val easter = easterSunday(year)
        val entries = linkedMapOf<LocalDate, String>()
        fun add(date: LocalDate, title: String) {
            entries[date] = title
        }

        add(LocalDate.of(year, 1, 1), "1. nyttårsdag")
        add(easter.minusDays(3), "Skjærtorsdag")
        add(easter.minusDays(2), "Langfredag")
        add(easter.minusDays(1), "Påskeaften")
        add(easter, "1. påskedag")
        add(easter.plusDays(1), "2. påskedag")
        add(LocalDate.of(year, 5, 1), "1. mai")
        add(LocalDate.of(year, 5, 17), "17. mai")
        add(easter.plusDays(39), "Kristi himmelfartsdag")
        add(easter.plusDays(48), "Pinseaften")
        add(easter.plusDays(49), "1. pinsedag")
        add(easter.plusDays(50), "2. pinsedag")
        add(LocalDate.of(year, 12, 24), "Julaften")
        add(LocalDate.of(year, 12, 25), "1. juledag")
        add(LocalDate.of(year, 12, 26), "2. juledag")
        add(LocalDate.of(year, 12, 31), "Nyttårsaften")
        return entries
    }

    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    private fun intersection(
        aStart: LocalDateTime,
        aEnd: LocalDateTime,
        bStart: LocalDateTime,
        bEnd: LocalDateTime,
    ): Pair<LocalDateTime, LocalDateTime>? {
        val start = if (aStart.isAfter(bStart)) aStart else bStart
        val end = if (aEnd.isBefore(bEnd)) aEnd else bEnd
        return if (end.isAfter(start)) start to end else null
    }
}
