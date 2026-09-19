package app.ferietur.domain

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * TIME01: Ferietur calculates with the wall-clock times the user registers ([LocalDateTime]).
 * It has no time-zone model. Two situations can therefore make the registered duration differ
 * from the time that actually passed:
 *
 *  1. a period crosses the Norwegian change to or from daylight saving time, and
 *  2. departure and arrival are registered in the local time of two different time zones.
 *
 * Situation 1 can be detected and is reported as a control finding. Situation 2 cannot be
 * detected from the trip data, so the app asks the user to register every time in Norwegian time.
 * This policy never changes a calculated amount; it only makes the deviation visible.
 */
object ClockChangePolicy {
    val NORWEGIAN_ZONE: ZoneId = ZoneId.of("Europe/Oslo")

    const val REGISTER_IN_NORWEGIAN_TIME_NOTE: String =
        "Alle klokkeslett regnes som norsk tid. Er reisemålet i en annen tidssone, " +
            "regner du avreise og ankomst om til norsk tid før du registrerer dem."

    data class ClockChange(
        /** Local time at the instant the clocks are changed, e.g. 02:00 (spring) or 03:00 (autumn). */
        val localTimeBefore: LocalDateTime,
        /** Local time right after the change, e.g. 03:00 (spring) or 02:00 (autumn). */
        val localTimeAfter: LocalDateTime,
        /** Positive when clocks move forward (time is skipped), negative when they move back. */
        val shiftMinutes: Long,
    ) {
        val movesForward: Boolean get() = shiftMinutes > 0
    }

    data class AffectedPeriod(
        val start: LocalDateTime,
        val end: LocalDateTime,
        val change: ClockChange,
        val registeredMinutes: Long,
        val elapsedMinutes: Long,
    )

    fun clockChangesWithin(
        start: LocalDateTime,
        end: LocalDateTime,
        zone: ZoneId = NORWEGIAN_ZONE,
    ): List<ClockChange> {
        if (!end.isAfter(start)) return emptyList()
        val rules = zone.rules
        val startInstant = start.atZone(zone).toInstant()
        val endInstant = end.atZone(zone).toInstant()
        val changes = mutableListOf<ClockChange>()
        // A start time inside the skipped hour (e.g. 02:30 on the spring date) does not exist.
        // java.time resolves it to after the change, so report that change explicitly.
        rules.getTransition(start)?.takeIf { it.isGap }?.let { gap ->
            changes += ClockChange(gap.dateTimeBefore, gap.dateTimeAfter, gap.duration.toMinutes())
        }
        // nextTransition is strictly after startInstant: a period that starts exactly when the
        // clocks have been changed does not cross the change.
        var transition = rules.nextTransition(startInstant)
        while (transition != null && transition.instant.isBefore(endInstant)) {
            changes += ClockChange(
                localTimeBefore = transition.dateTimeBefore,
                localTimeAfter = transition.dateTimeAfter,
                shiftMinutes = transition.duration.toMinutes(),
            )
            transition = rules.nextTransition(transition.instant)
        }
        return changes
    }

    fun affectedPeriods(
        blocks: List<WorkBlock>,
        zone: ZoneId = NORWEGIAN_ZONE,
    ): List<AffectedPeriod> =
        blocks
            .sortedBy { it.start }
            .flatMap { block ->
                clockChangesWithin(block.start, block.end, zone).map { change ->
                    val registered = java.time.Duration.between(block.start, block.end).toMinutes()
                    AffectedPeriod(
                        start = block.start,
                        end = block.end,
                        change = change,
                        registeredMinutes = registered,
                        elapsedMinutes = registered - change.shiftMinutes,
                    )
                }
            }
            .distinctBy { Triple(it.start, it.end, it.change.localTimeBefore) }

    fun controlFindings(
        blocks: List<WorkBlock>,
        zone: ZoneId = NORWEGIAN_ZONE,
    ): List<ControlFinding> =
        affectedPeriods(blocks, zone).map { period ->
            val change = period.change
            ControlFinding(
                severity = FindingSeverity.REVIEW,
                title = if (change.movesForward) {
                    "Arbeidsperiode krysser overgangen til sommertid"
                } else {
                    "Arbeidsperiode krysser overgangen til normaltid"
                },
                detail = "${dateLabel(change.localTimeBefore)} stilles klokken fra kl. ${clock(change.localTimeBefore)} " +
                    "til kl. ${clock(change.localTimeAfter)}. Ferietur regner med klokkeslettene slik de er registrert: " +
                    "perioden ${dateTimeLabel(period.start)}–${dateTimeLabel(period.end)} er beregnet som " +
                    "${minutesLabel(period.registeredMinutes)}, mens det faktisk gikk ${minutesLabel(period.elapsedMinutes)}. " +
                    "Kontroller varigheten, og juster klokkeslettene dersom beregningen skal følge faktisk medgått tid.",
            )
        }

    private val norwegian: Locale = Locale.forLanguageTag("nb-NO")
    private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormat = DateTimeFormatter.ofPattern("EEEE d. MMMM yyyy", norwegian)
    private val shortDateTimeFormat = DateTimeFormatter.ofPattern("d.M. 'kl.' HH:mm", norwegian)

    private fun clock(value: LocalDateTime): String = value.format(clockFormat)

    private fun dateLabel(value: LocalDateTime): String =
        value.format(dateFormat).replaceFirstChar { it.titlecase(norwegian) }

    private fun dateTimeLabel(value: LocalDateTime): String = value.format(shortDateTimeFormat)

    private fun minutesLabel(minutes: Long): String {
        val hours = minutes / 60
        val remainder = minutes % 60
        return if (remainder == 0L) "$hours t" else "$hours t $remainder min"
    }
}
