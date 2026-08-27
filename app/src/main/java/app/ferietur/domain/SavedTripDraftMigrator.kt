package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalTime

object SavedTripDraftMigrator {
    const val MIGRATION_V6_SCHEMA = "V6_SCHEMA"
    const val MIGRATION_V6_MANUAL_ROSTER = "V6_MANUAL_ROSTER_NORMALIZATION"
    const val MIGRATION_V6_REFINALIZE_LEGACY_SUMMARY = "V6_LEGACY_SUMMARY_REFINALIZATION_REQUIRED"
    const val MIGRATION_V6_A39A1_PLAN_REPAIR = "V6_A39A1_TRAVEL_ONLY_PLAN_REPAIR"

    fun migrate(
        sourceSchemaVersion: Int,
        draft: SavedTripDraft,
    ): SavedTripDraft {
        if (sourceSchemaVersion >= SavedTripDraftCodec.SCHEMA_VERSION) return draft

        var migrated = draft
        val history = migrated.migrationHistory.toMutableSet()

        val manualRoster = migrated.roster.filterValues(RosterEntryCodec::isManual)
        if (manualRoster.size != migrated.roster.size) {
            migrated = migrated.copy(
                roster = manualRoster,
                rosterGapConfirmed = false,
            )
            history += MIGRATION_V6_MANUAL_ROSTER
        }

        val repairedPlan = repairA39A1TravelOnlyPlan(migrated)
        if (repairedPlan != null) {
            migrated = migrated.copy(
                plans = repairedPlan,
                rosterGapConfirmed = false,
            )
            history += MIGRATION_V6_A39A1_PLAN_REPAIR
        }

        if (migrated.screen == "SUMMARY") {
            migrated = migrated.copy(
                screen = "CONTROL",
                finalizedSnapshot = null,
            )
            history += MIGRATION_V6_REFINALIZE_LEGACY_SUMMARY
        }

        history += MIGRATION_V6_SCHEMA
        return migrated.copy(migrationHistory = history)
    }

    /**
     * One schema-bound migration for the characteristic A39A1 damaged shape.
     *
     * Unlike the retired runtime recovery, this does not branch on a trip title
     * or user-facing name. It is applied only while reading pre-v6 data and
     * records provenance in migrationHistory.
     */
    private fun repairA39A1TravelOnlyPlan(
        draft: SavedTripDraft,
    ): Map<LocalDate, List<PlannedBlock>>? {
        val start = LocalDate.of(2026, 8, 11)
        val end = LocalDate.of(2026, 8, 18)
        if (draft.startDate != start || draft.endDate != end) return null
        if (draft.startTime != LocalTime.of(6, 0) || draft.endTime != LocalTime.of(23, 0)) return null

        val savedBlocks = draft.plans.values.flatten()
        if (savedBlocks.isEmpty()) return null
        if (savedBlocks.any { !it.kind.isTravelKind() }) return null
        if (savedBlocks.count { it.kind.isTravelKind() } < 2) return null

        return linkedMapOf(
            start to listOf(
                PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(6, 0), LocalTime.of(11, 0)),
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(11, 0), LocalTime.of(22, 0)),
            ),
            start.plusDays(1) to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            ),
            start.plusDays(2) to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
            ),
            start.plusDays(3) to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            ),
            start.plusDays(4) to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            ),
            start.plusDays(5) to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            ),
            start.plusDays(6) to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(22, 0)),
                PlannedBlock(TimeKind.RESTING_NIGHT_WATCH, LocalTime.of(23, 0), LocalTime.of(7, 0)),
            ),
            end to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(7, 0), LocalTime.of(16, 0)),
                PlannedBlock(TimeKind.TRAVEL_WITH_RESPONSIBILITY, LocalTime.of(16, 0), LocalTime.of(23, 0)),
            ),
        )
    }
}
