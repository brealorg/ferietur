package app.ferietur.domain

import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.net.URLDecoder
import java.net.URLEncoder

enum class ShiftCategory {
    DAY,
    LONG_DAY,
    EVENING,
    NIGHT,
    OFF,
    EXCLUDED
}

data class ShiftDefinition(
    val code: String,
    val start: LocalTime?,
    val end: LocalTime?,
    val category: ShiftCategory,
    val label: String,
    val excludedFromTrip: Boolean = false,
    val weeklyOff: Boolean = false,
) {
    val crossesMidnight: Boolean
        get() = start != null && end != null && !end.isAfter(start)

    fun durationMinutes(): Long {
        val from = start ?: return 0
        val to = end ?: return 0
        val raw = ChronoUnit.MINUTES.between(from, to)
        return if (raw > 0) raw else raw + 24 * 60
    }
}

/**
 * Legacy development catalogue. Kept only so pre-A57 drafts and regression tests can still be
 * decoded. New roster registration never exposes or depends on these site-specific codes.
 */
object SolhaugenShiftCatalog {
    val all = listOf(
        ShiftDefinition("D1", LocalTime.of(7, 0), LocalTime.of(14, 30), ShiftCategory.DAY, "Dagvakt"),
        ShiftDefinition("D", LocalTime.of(7, 0), LocalTime.of(15, 0), ShiftCategory.DAY, "Dagvakt"),
        ShiftDefinition("D2", LocalTime.of(7, 30), LocalTime.of(15, 0), ShiftCategory.DAY, "Dagvakt"),
        ShiftDefinition("D2H", LocalTime.of(7, 30), LocalTime.of(14, 45), ShiftCategory.DAY, "Dagvakt"),
        ShiftDefinition("DCO", LocalTime.of(8, 0), LocalTime.of(15, 0), ShiftCategory.DAY, "Dagvakt"),
        ShiftDefinition("LV", LocalTime.of(9, 0), LocalTime.of(21, 0), ShiftCategory.LONG_DAY, "Langvakt"),
        ShiftDefinition("AL2", LocalTime.of(13, 0), LocalTime.of(21, 0), ShiftCategory.EVENING, "Aftenvakt"),
        ShiftDefinition("AL", LocalTime.of(13, 0), LocalTime.of(22, 0), ShiftCategory.EVENING, "Aftenvakt"),
        ShiftDefinition("AL1", LocalTime.of(13, 0), LocalTime.of(21, 30), ShiftCategory.EVENING, "Aftenvakt"),
        ShiftDefinition("AV4", LocalTime.of(14, 30), LocalTime.of(21, 0), ShiftCategory.EVENING, "Aftenvakt"),
        ShiftDefinition("A", LocalTime.of(14, 30), LocalTime.of(22, 0), ShiftCategory.EVENING, "Aftenvakt"),
        ShiftDefinition("A1", LocalTime.of(14, 30), LocalTime.of(21, 30), ShiftCategory.EVENING, "Aftenvakt"),
        ShiftDefinition("N", LocalTime.of(21, 30), LocalTime.of(7, 15), ShiftCategory.NIGHT, "Nattevakt"),
        ShiftDefinition("N2", LocalTime.of(21, 45), LocalTime.of(7, 45), ShiftCategory.NIGHT, "Nattevakt"),
        ShiftDefinition("NVH", LocalTime.of(21, 45), LocalTime.of(7, 30), ShiftCategory.NIGHT, "Nattevakt"),
        ShiftDefinition("F1", null, null, ShiftCategory.OFF, "Ukentlig fridag", weeklyOff = true),
        ShiftDefinition("F2", null, null, ShiftCategory.OFF, "Ekstra ukefridag"),
        ShiftDefinition("K6", LocalTime.of(8, 30), LocalTime.of(15, 30), ShiftCategory.EXCLUDED, "Kjøkkenvakt", excludedFromTrip = true),
    )

    val tripRelevant = all.filterNot { it.excludedFromTrip }

    fun byCode(code: String): ShiftDefinition? = all.firstOrNull { it.code == code }
}

/**
 * A57 manual-roster wire format. SavedTripDraft deliberately keeps its Map<LocalDate, String>
 * shape so old drafts remain readable, while each value can now carry one or more user-defined
 * intervals. The user's local shift code is data, never a lookup key with Oslo-wide semantics.
 */
object RosterEntryCodec {
    private const val PREFIX = "MR1"
    const val MAX_CODE_LENGTH = 3

    fun isManual(value: String): Boolean = value.startsWith("$PREFIX;")

    fun decode(value: String?): List<ShiftDefinition> {
        if (value.isNullOrBlank()) return emptyList()
        if (!isManual(value)) return SolhaugenShiftCatalog.byCode(value)?.let(::listOf).orEmpty()
        return value.split(';').drop(1).mapNotNull(::decodeSegment)
    }

    fun encode(shifts: List<ShiftDefinition>): String {
        require(shifts.isNotEmpty()) { "Roster day must contain at least one registration" }
        require(shifts.none { it.excludedFromTrip }) { "Excluded legacy shifts cannot be stored as manual roster data" }
        require(shifts.count { it.category == ShiftCategory.OFF } in 0..1) { "Only one free-day registration is allowed" }
        require(shifts.none { it.category == ShiftCategory.OFF } || shifts.size == 1) { "Free day cannot be combined with work shifts" }
        val segments = shifts.map { shift ->
            if (shift.category == ShiftCategory.OFF) {
                listOf("F", encodeCode(shift.code), if (shift.weeklyOff) "1" else "0").joinToString(",")
            } else {
                val start = requireNotNull(shift.start)
                val end = requireNotNull(shift.end)
                listOf("W", encodeCode(shift.code), start.toString(), end.toString()).joinToString(",")
            }
        }
        return (listOf(PREFIX) + segments).joinToString(";")
    }

    fun manualWork(code: String, start: LocalTime, end: LocalTime): ShiftDefinition {
        val cleanCode = code.trim()
        require(cleanCode.isNotBlank()) { "Vaktkode er obligatorisk" }
        require(cleanCode.length <= MAX_CODE_LENGTH) { "Vaktkode kan være maks $MAX_CODE_LENGTH tegn" }
        val category = inferredWorkCategory(start, end)
        return ShiftDefinition(
            code = cleanCode,
            start = start,
            end = end,
            category = category,
            label = categoryLabel(category),
        )
    }

    fun manualFree(code: String, weeklyOff: Boolean): ShiftDefinition {
        val cleanCode = code.trim()
        require(cleanCode.isNotBlank()) { "Vaktkode er obligatorisk" }
        require(cleanCode.length <= MAX_CODE_LENGTH) { "Vaktkode kan være maks $MAX_CODE_LENGTH tegn" }
        return ShiftDefinition(
            code = cleanCode,
            start = null,
            end = null,
            category = ShiftCategory.OFF,
            label = if (weeklyOff) "Ukentlig fridag" else "Annen fridag",
            weeklyOff = weeklyOff,
        )
    }

    private fun decodeSegment(segment: String): ShiftDefinition? {
        val parts = segment.split(',')
        return runCatching {
            when (parts.firstOrNull()) {
                "W" -> {
                    require(parts.size == 4)
                    restoreWork(
                        decodeCode(parts[1]),
                        LocalTime.parse(parts[2]),
                        LocalTime.parse(parts[3]),
                    )
                }
                "F" -> {
                    require(parts.size == 3)
                    restoreFree(decodeCode(parts[1]), parts[2] == "1")
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun restoreWork(code: String, start: LocalTime, end: LocalTime): ShiftDefinition {
        val cleanCode = code.trim()
        require(cleanCode.isNotBlank()) { "Vaktkode er obligatorisk" }
        val category = inferredWorkCategory(start, end)
        return ShiftDefinition(
            code = cleanCode,
            start = start,
            end = end,
            category = category,
            label = categoryLabel(category),
        )
    }

    private fun restoreFree(code: String, weeklyOff: Boolean): ShiftDefinition {
        val cleanCode = code.trim()
        require(cleanCode.isNotBlank()) { "Vaktkode er obligatorisk" }
        return ShiftDefinition(
            code = cleanCode,
            start = null,
            end = null,
            category = ShiftCategory.OFF,
            label = if (weeklyOff) "Ukentlig fridag" else "Annen fridag",
            weeklyOff = weeklyOff,
        )
    }

    private fun inferredWorkCategory(start: LocalTime, end: LocalTime): ShiftCategory {
        val raw = ChronoUnit.MINUTES.between(start, end)
        val duration = if (raw > 0) raw else raw + 24 * 60
        return when {
            !end.isAfter(start) -> ShiftCategory.NIGHT
            duration >= 10 * 60 -> ShiftCategory.LONG_DAY
            !start.isBefore(LocalTime.NOON) -> ShiftCategory.EVENING
            else -> ShiftCategory.DAY
        }
    }

    private fun categoryLabel(category: ShiftCategory): String = when (category) {
        ShiftCategory.DAY -> "Dagvakt"
        ShiftCategory.LONG_DAY -> "Langvakt"
        ShiftCategory.EVENING -> "Aftenvakt"
        ShiftCategory.NIGHT -> "Nattevakt"
        ShiftCategory.OFF -> "Fri"
        ShiftCategory.EXCLUDED -> "Arbeidsvakt"
    }

    private fun encodeCode(code: String): String = URLEncoder.encode(code, Charsets.UTF_8.name())

    private fun decodeCode(encoded: String): String = URLDecoder.decode(encoded, Charsets.UTF_8.name())
}
