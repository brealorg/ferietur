package app.ferietur.ui

import app.ferietur.domain.RosterComparisonMode
import app.ferietur.domain.SavedTripDraft

enum class TripLibraryLifecycle {
    IN_PROGRESS,
    SUMMARY_READY,
}

data class TripLibraryProgress(
    val currentStep: Int,
    val totalSteps: Int,
)

fun savedTripLifecycle(saved: SavedTripDraft): TripLibraryLifecycle =
    if (saved.screen == "SUMMARY") TripLibraryLifecycle.SUMMARY_READY else TripLibraryLifecycle.IN_PROGRESS

fun savedTripProgress(saved: SavedTripDraft): TripLibraryProgress {
    val usesRoster = saved.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER
    val order = if (usesRoster) {
        listOf("TRIP", "METHOD", "PAY", "ROSTER", "TRIP_PLAN", "CALCULATION", "SETTLEMENT", "CONTROL", "SUMMARY")
    } else {
        listOf("TRIP", "METHOD", "PAY", "TRIP_PLAN", "CALCULATION", "SETTLEMENT", "CONTROL", "SUMMARY")
    }
    val normalized = if (saved.screen == "TRAVEL") "TRIP_PLAN" else saved.screen
    val current = (order.indexOf(normalized).takeIf { it >= 0 } ?: 0) + 1
    return TripLibraryProgress(currentStep = current, totalSteps = order.size)
}

fun copyTitle(title: String): String {
    val base = title.trim().ifBlank { "Ferietur" }
    return if (base.startsWith("Kopi av ")) base else "Kopi av $base"
}

fun copiedTripDraft(source: SavedTripDraft, newId: String, now: Long): SavedTripDraft = source.copy(
    id = newId,
    updatedAtEpochMillis = now,
    screen = "TRIP",
    title = copyTitle(source.title),
    payslipChecked = false,
    rosterGapConfirmed = false,
    settlementMode = "FULL_CALCULATION",
    settlementAmountText = "",
    settlementReason = "",
    finalizedSnapshot = null,
    finalizationHistory = emptyList(),
    migrationHistory = emptySet(),
)
