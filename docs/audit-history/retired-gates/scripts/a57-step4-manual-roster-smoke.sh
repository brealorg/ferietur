#!/usr/bin/env bash
set -u
set -o pipefail

UI="app/src/main/java/app/ferietur/ui/FerieturApp.kt"
SHIFT="app/src/main/java/app/ferietur/domain/ShiftCatalog.kt"
ENGINE="app/src/main/java/app/ferietur/domain/TripPlanEngine.kt"
SNAP="app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt"
DRAFT="app/src/main/java/app/ferietur/domain/SavedTripDraft.kt"
TEST="app/src/test/java/app/ferietur/domain/ManualRosterEntryCodecTest.kt"

fail() {
    echo "A57_STEP4_MANUAL_ROSTER_SMOKE=FAIL reason=$1"
    exit 1
}

for f in "$UI" "$SHIFT" "$ENGINE" "$SNAP" "$DRAFT" "$TEST"; do
    [[ -f "$f" ]] || fail "missing_$f"
done

! grep -Fq 'defaultRoster(' "$UI" || fail "default_roster_function_or_call_remains"
grep -Fq 'mutableStateOf(emptyMap<LocalDate, String>())' "$UI" || fail "new_trip_roster_not_empty"
grep -Fq 'val newRoster = emptyMap<LocalDate, String>()' "$UI" || fail "start_new_trip_roster_not_empty"
grep -Fq 'oldRoster[date]?.let { date to it }' "$UI" || fail "range_change_does_not_preserve_only_existing_manual_days"
grep -Fq 'filterValues(RosterEntryCodec::isManual)' "$UI" || fail "legacy_roster_not_cleared_on_resume"

grep -Fq 'Legg inn vaktene som står i grunnturnusen din.' "$UI" || fail "approved_intro_missing"
grep -Fq 'Text("Legg til vakter")' "$UI" || fail "extended_fab_missing"
grep -Fq 'private fun RosterDayEditorSheet(' "$UI" || fail "manual_day_editor_missing"
grep -Fq 'label = { Text("Vaktkode") }' "$UI" || fail "mandatory_shift_code_field_missing"
grep -Fq 'Obligatorisk · maks ${RosterEntryCodec.MAX_CODE_LENGTH} tegn' "$UI" || fail "mandatory_shift_code_affordance_missing"
grep -Fq 'title = "Arbeidsvakt"' "$UI" || fail "work_choice_missing"
grep -Fq 'title = "Fri"' "$UI" || fail "free_choice_missing"
grep -Fq 'title = "Ukentlig fridag"' "$UI" || fail "weekly_off_tariff_meaning_missing"

# Formatting-independent text check. The original A57 gate incorrectly required
# Text("Tidligere brukt" to be on one source line, while Compose formats this
# as Text( newline "Tidligere brukt", ...).
grep -Fq '"Tidligere brukt"' "$UI" || fail "local_reuse_templates_missing"

grep -Fq 'Text("Legg til en vakt til")' "$UI" || fail "multiple_shifts_same_day_missing"
grep -Fq 'private fun RosterTimeInputButton(' "$UI" || fail "manual_time_picker_missing"
grep -Fq 'RosterEntryCodec.manualWork(' "$UI" || fail "manual_work_encoding_missing"
grep -Fq 'RosterEntryCodec.manualFree(' "$UI" || fail "manual_free_encoding_missing"
grep -Fq 'FlowScreen.ROSTER -> rosterComplete' "$UI" || fail "incomplete_roster_does_not_block_next"
grep -Fq 'TripPlanEngine.hasRosterOverlap(roster)' "$UI" || fail "roster_overlap_gate_missing"
grep -Fq '"Vakter overlapper"' "$UI" || fail "roster_overlap_message_missing"

! grep -Fq 'Langvakt og natt beholdes som sine reelle intervaller' "$UI" || fail "removed_long_night_copy_remains"
! grep -Fq 'K6 er skjult for tur' "$UI" || fail "removed_k6_copy_remains"
! grep -Fq 'private fun ShiftPickerSheet(' "$UI" || fail "legacy_catalog_picker_remains"
! grep -Fq 'SolhaugenShiftCatalog.tripRelevant' "$UI" || fail "site_specific_catalog_still_exposed_in_ui"

grep -Fq 'object RosterEntryCodec' "$SHIFT" || fail "roster_entry_codec_missing"
grep -Fq 'fun manualWork(code: String, start: LocalTime, end: LocalTime)' "$SHIFT" || fail "manual_work_factory_missing"
grep -Fq 'fun manualFree(code: String, weeklyOff: Boolean)' "$SHIFT" || fail "manual_free_factory_missing"
grep -Fq 'val weeklyOff: Boolean = false' "$SHIFT" || fail "weekly_off_domain_flag_missing"
grep -Fq 'ShiftDefinition("F1", null, null, ShiftCategory.OFF, "Ukentlig fridag", weeklyOff = true)' "$SHIFT" || fail "legacy_f1_weekly_off_compatibility_missing"

grep -Fq 'RosterEntryCodec.decode(roster[date]).flatMap(::plannedBlocksForShift)' "$ENGINE" || fail "multi_interval_plan_from_roster_missing"
grep -Fq 'fun hasRosterOverlap(roster: Map<LocalDate, String>): Boolean' "$ENGINE" || fail "cross_day_roster_overlap_check_missing"
grep -Fq 'it.category == ShiftCategory.OFF && it.weeklyOff' "$ENGINE" || fail "weekly_off_rule_still_code_based"
! grep -Fq 'return shift.code == "F1"' "$ENGINE" || fail "hardcoded_f1_tariff_semantics_remain"
! grep -Fq 'SolhaugenShiftCatalog.byCode' "$ENGINE" || fail "engine_still_depends_on_site_catalog"

grep -Fq 'RosterEntryCodec.decode(roster[date]).map { shift ->' "$SNAP" || fail "snapshot_multi_roster_decode_missing"
grep -Fq 'code = shift.code' "$SNAP" || fail "snapshot_exposes_wire_token"

grep -Fq 'const val SCHEMA_VERSION = 5' "$DRAFT" || fail "draft_schema_not_bumped"
grep -Fq 'roundTripKeepsUserDefinedCodesAndMultipleIntervals' "$TEST" || fail "manual_roster_roundtrip_test_missing"
grep -Fq 'planFromRosterUsesEveryManualIntervalOnTheDay' "$TEST" || fail "multi_interval_engine_test_missing"

echo "A57_STEP4_MANUAL_ROSTER_SMOKE=PASS"
