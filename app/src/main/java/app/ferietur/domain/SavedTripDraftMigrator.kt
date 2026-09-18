package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalTime

object SavedTripDraftMigrator {
    const val MIGRATION_V6_SCHEMA = "V6_SCHEMA"
    const val MIGRATION_V6_MANUAL_ROSTER = "V6_MANUAL_ROSTER_NORMALIZATION"
    const val MIGRATION_V6_REFINALIZE_LEGACY_SUMMARY = "V6_LEGACY_SUMMARY_REFINALIZATION_REQUIRED"
    const val MIGRATION_V6_A39A1_PLAN_REPAIR = "V6_A39A1_TRAVEL_ONLY_PLAN_REPAIR"
    const val MIGRATION_V7_WORK_PLAN_STATUS = "V7_HOLIDAY_WORK_PLAN_STATUS_REVIEW_REQUIRED"
    const val MIGRATION_V8_PERIOD_RELATION = "V8_PERIOD_RELATION_AND_TRAVEL_DUTY_REVIEW_REQUIRED"
    const val MIGRATION_V9_HOLIDAY_WORK_PLAN = "V9_HOLIDAY_WORK_PLAN_REVIEW_REQUIRED"
    const val MIGRATION_V10_WORK_PLAN_BASIS = "V10_EMPLOYER_WORK_PLAN_BASIS_REVIEW_REQUIRED"

    fun migrate(
        sourceSchemaVersion: Int,
        draft: SavedTripDraft,
    ): SavedTripDraft {
        if (sourceSchemaVersion >= SavedTripDraftCodec.SCHEMA_VERSION) return draft

        var migrated = draft
        val history = migrated.migrationHistory.toMutableSet()

        // Keep the existing v6 migration boundary exact. A genuine v6 draft may already contain
        // durable finalized snapshots and must not be treated as a pre-v6 legacy summary merely
        // because schema v7 adds a new editable-work-plan field.
        if (sourceSchemaVersion < 6) {
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
        }

        // Pre-v7 drafts were calculated from a grunnturnus comparison without a separately
        // recorded chapter-20 holiday work-plan status. Preserve all trip data and finalized
        // snapshots, but require the editable draft to be reviewed explicitly before later
        // EQS01 calculation semantics are applied.
        if (sourceSchemaVersion < 7) {
            migrated = migrated.copy(
                holidayWorkPlanStatus = HolidayWorkPlanStatus.NOT_CLARIFIED,
            )
            history += MIGRATION_V7_WORK_PLAN_STATUS
        }

        // Pre-v8 plan rows have no period-level relation to the holiday work plan and no
        // explicit on-duty/off-duty travel fact. The decoder defaults both facts to
        // NOT_CLARIFIED; record that review requirement without changing existing time.
        if (sourceSchemaVersion < 8) {
            history += MIGRATION_V8_PERIOD_RELATION
        }

        // Pre-v9 drafts have no separate factual holiday-work-plan dataset.
        // Never infer it from ground roster or actual work.
        if (sourceSchemaVersion < 9) {
            history += MIGRATION_V9_HOLIDAY_WORK_PLAN
        }

        if (sourceSchemaVersion < 10) {
            // Older drafts never recorded whether the employer actually set a
            // separate trip work plan. Never infer this from ground roster,
            // holidayPlans, or employee-entered work periods.
            history += MIGRATION_V10_WORK_PLAN_BASIS
        }

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
