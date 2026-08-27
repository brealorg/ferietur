package app.ferietur.ui

import app.ferietur.domain.TimeKind
import app.ferietur.domain.isTravelKind
import app.ferietur.domain.isTravelWithoutResponsibility

/**
 * Keeps the sleep-permission sub-state authoritative when the broader "Uten ansvar"
 * classification is already selected. Only the dedicated sleep selector should change
 * between Ja / Nei / Ikke avklart.
 */
internal fun travelWithoutResponsibilitySelection(current: TimeKind): TimeKind =
    if (current.isTravelWithoutResponsibility()) current else TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY

internal fun nightTravelSleepStatus(kind: TimeKind, overlapsNight: Boolean): String? = when (kind) {
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED -> "Søvntillatelse: Ja"
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP -> "Søvntillatelse: Nei"
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY -> if (overlapsNight) "Søvntillatelse: Ikke avklart" else null
    else -> null
}

/** Re-selecting the broad Reise category must not erase responsibility/sleep sub-state. */
internal fun periodTypeSelection(current: TimeKind, requested: TimeKind): TimeKind =
    if (current.isTravelKind() && requested.isTravelKind()) current else requested
