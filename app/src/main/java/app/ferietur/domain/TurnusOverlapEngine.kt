package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class TurnusOverlap(
    val actualMinutes: Long,
    val insideTurnusMinutes: Long,
    val outsideTurnusMinutes: Long,
)

object TurnusOverlapEngine {
    fun intervalFor(date: LocalDate, shift: ShiftDefinition): Pair<LocalDateTime, LocalDateTime>? {
        val start = shift.start ?: return null
        val end = shift.end ?: return null
        val startDateTime = LocalDateTime.of(date, start)
        val endDate = if (shift.crossesMidnight) date.plusDays(1) else date
        return startDateTime to LocalDateTime.of(endDate, end)
    }

    fun compare(
        actualStart: LocalDateTime,
        actualEnd: LocalDateTime,
        turnusStart: LocalDateTime,
        turnusEnd: LocalDateTime,
    ): TurnusOverlap {
        require(actualEnd.isAfter(actualStart))
        require(turnusEnd.isAfter(turnusStart))

        val actualMinutes = ChronoUnit.MINUTES.between(actualStart, actualEnd)
        val overlapStart = if (actualStart.isAfter(turnusStart)) actualStart else turnusStart
        val overlapEnd = if (actualEnd.isBefore(turnusEnd)) actualEnd else turnusEnd
        val overlapMinutes = if (overlapEnd.isAfter(overlapStart)) {
            ChronoUnit.MINUTES.between(overlapStart, overlapEnd)
        } else {
            0L
        }
        return TurnusOverlap(
            actualMinutes = actualMinutes,
            insideTurnusMinutes = overlapMinutes,
            outsideTurnusMinutes = actualMinutes - overlapMinutes,
        )
    }

    fun compare(
        date: LocalDate,
        actualStart: LocalTime,
        actualEnd: LocalTime,
        shift: ShiftDefinition,
    ): TurnusOverlap {
        val actualStartDateTime = LocalDateTime.of(date, actualStart)
        val actualEndDate = if (!actualEnd.isAfter(actualStart)) date.plusDays(1) else date
        val actualEndDateTime = LocalDateTime.of(actualEndDate, actualEnd)
        val turnus = requireNotNull(intervalFor(date, shift))
        return compare(actualStartDateTime, actualEndDateTime, turnus.first, turnus.second)
    }
}
