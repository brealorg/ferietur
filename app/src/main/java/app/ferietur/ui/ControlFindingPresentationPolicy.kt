package app.ferietur.ui

/**
 * UXFIX02: compact presentation of the PILOT01-004 seven-day work-time finding.
 *
 * The finding text stays domain-owned (TripPlanEngine). This policy only reads the hours and the
 * window out of that sentence so the control step can show "138 t på sju dager" with the dates,
 * instead of repeating the finding's title as its own supporting text. If the sentence ever
 * changes shape, [sevenDayWindow] returns null and the caller falls back to the generic layout.
 */
internal object ControlFindingPresentationPolicy {
    const val SEVEN_DAY_TITLE = "Mer enn 48 timer i en sju-dagersperiode"

    data class SevenDayWindow(
        val hours: String,
        val start: String,
        val end: String,
    )

    // "11. august" and "kl. 06:00" contain ". ", so the window is anchored on the clock times.
    private val SENTENCE = Regex(
        "^Du har registrert (.+?) arbeid i løpet av sju dager, " +
            "fra (.+? kl\\. \\d{2}:\\d{2}) til (.+? kl\\. \\d{2}:\\d{2})\\.",
    )

    fun sevenDayWindow(detail: String): SevenDayWindow? {
        val match = SENTENCE.find(detail) ?: return null
        val (hours, start, end) = match.destructured
        return SevenDayWindow(hours = hours.trim(), start = start.trim(), end = end.trim())
    }
}
