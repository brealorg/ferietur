#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

python3 - <<'PY'
from pathlib import Path

ui = Path('app/src/main/java/app/ferietur/ui/FerieturApp.kt').read_text(encoding='utf-8')
unit_test = Path('app/src/test/java/app/ferietur/ui/RosterTemplatePolicyTest.kt')
runtime_test = Path('app/src/androidTest/java/app/ferietur/UxFix01RuntimeTest.kt')
upgrade_test = Path('app/src/androidTest/java/app/ferietur/UxFix01UpgradeStateTest.kt')

def require(condition: bool, reason: str) -> None:
    if not condition:
        raise SystemExit(f'UXFIX01_CONTRACT=FAIL reason={reason}')

def between(start: str, end: str) -> str:
    begin = ui.index(start)
    finish = ui.index(end, begin)
    return ui[begin:finish]

require('For eksempel Høsttur 2026' in ui, 'neutral_trip_title_example_missing')
require('For eksempel Solgården' not in ui, 'old_site_specific_trip_title_example_remains')
require('KeyboardOptions(imeAction = ImeAction.Done)' in ui, 'trip_title_done_ime_action_missing')
require('onKeyboardAction = { keyboardController?.hide() }' in ui, 'trip_title_keyboard_hide_missing')

roster = between('private fun RosterDayEditorSheet(', 'private fun RosterTimeInputButton(')
require('enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)' in roster, 'roster_sheet_not_full_height_only')
require('sheetState = sheetState' in roster, 'roster_sheet_state_not_applied')
require('applyRosterTemplateToPrimaryDraft(workDrafts, template)' in roster, 'template_does_not_replace_primary_draft')
require('Trykk for å fylle inn vaktkode og klokkeslett.' in roster, 'template_copy_not_explicit')
require('Legg til en vakt til' not in roster, 'second_shift_add_action_remains')
require('workDrafts + draft' not in roster, 'template_append_bug_remains')

helper = between('internal fun applyRosterTemplateToPrimaryDraft(', 'private data class SettlementSummary(')
require('it[0] = replacement' in helper, 'primary_draft_replacement_missing')
require('drafts + replacement' not in helper, 'helper_appends_shift')
require(unit_test.is_file(), 'roster_template_policy_test_missing')
unit_text = unit_test.read_text(encoding='utf-8')
require('previouslyUsedTemplateReplacesPrimaryDraftWithCodeAndTimes' in unit_text, 'template_code_time_test_missing')
require('templateSelectionNeverAppendsAnotherShift' in unit_text, 'no_append_regression_test_missing')

plan_screen = between('private fun TripPlanScreen(', 'private fun CalculationScreen(')
require('onAddPeriod: (LocalDate) -> Unit' in plan_screen, 'trip_plan_add_callback_missing')
require('onAddPeriod = onAddPeriod' in plan_screen, 'day_card_add_callback_not_wired')
plan_card = between('private fun TripDayCard(', 'private fun PlanActivityRow(')
require(
    'Modifier.clickable(role = Role.Button) { onAddPeriod(date) }' in plan_card,
    'empty_day_accessible_button_click_missing',
)
require('Trykk på dagen eller «Legg til periode»' in plan_card, 'empty_day_guidance_not_updated')
require('val pendingPlanDate = mutableStateOf<LocalDate?>(null)' in ui, 'pending_plan_date_state_missing')
require('pendingPlanDate = date' in ui, 'clicked_day_date_not_preserved')
require('val suggestedDate = pendingPlanDate' in ui, 'period_editor_does_not_prefer_clicked_day')

fab = between('private fun PlanFabMenu(', 'private fun PlanPeriodEditorSheet(')
require('ExtendedFloatingActionButton' in fab, 'extended_add_period_fab_missing')
require('if (expanded) "Lukk" else "Legg til periode"' in fab, 'fab_label_missing')
require('val osloFabContainerColor = MaterialTheme.colorScheme.secondary' in fab, 'oslo_yellow_main_fab_missing')
require(fab.count('containerColor = MaterialTheme.colorScheme.secondary') >= 4, 'oslo_yellow_fab_actions_missing')
require(fab.count('color = MaterialTheme.colorScheme.onSecondary') >= 4, 'fab_action_text_contrast_missing')
require(fab.count('tint = MaterialTheme.colorScheme.onSecondary') >= 4, 'fab_action_icon_contrast_missing')
require('"Annet"' in fab, 'other_action_copy_not_corrected')

editor = between('private fun PlanPeriodEditorSheet(', 'private fun samePeriodCategory(')
require('enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)' in editor, 'period_sheet_not_full_height_only')
require('containerColor = MaterialTheme.colorScheme.surfaceContainerHigh' in editor, 'period_sheet_contrast_not_increased')
require('color = MaterialTheme.colorScheme.secondaryContainer' in editor, 'period_sheet_header_accent_missing')
require('containerColor = MaterialTheme.colorScheme.secondary' in editor, 'period_sheet_primary_action_not_oslo_yellow')
require('contentColor = MaterialTheme.colorScheme.onSecondary' in editor, 'period_sheet_primary_action_contrast_missing')

summary = between('private fun FinalSummaryScreen(', 'private fun FinalSummaryStatusRow(')
require('MethodSectionHeading("Dokumentasjon")' in summary, 'documentation_heading_missing')
require('MethodSectionHeading("Eksporter")' not in summary, 'old_export_heading_remains')
require('"Kort oppsummering"' in summary and '"Beløp og status · PDF"' in summary, 'short_pdf_action_not_self_explanatory')
require('"Full dokumentasjon"' in summary and '"Beregning, regler og kilder · PDF"' in summary, 'full_pdf_action_not_self_explanatory')
require('Icons.Rounded.PictureAsPdf' in summary, 'short_pdf_icon_missing')
require('Icons.Rounded.Description' in summary, 'full_document_icon_missing')
require('"Kunne ikke lage PDF"' in summary, 'pdf_error_copy_not_updated')
require('Oppsummering og eksport' not in ui, 'old_summary_export_copy_remains')
require('Oppsummering og dokumentasjon' in ui, 'summary_documentation_copy_missing')

timeline = between('private fun DayTimeline(', 'private fun RosterCodeTextField(')
require('if (blocks.isNotEmpty())' in timeline, 'empty_timeline_pointer_guard_missing')
require('if (blocks.isEmpty() || size.width' not in timeline, 'old_empty_timeline_consuming_handler_remains')
require('Modifier.pointerInput(date, blocks, plans)' in timeline, 'populated_timeline_hit_testing_missing')
require('debugLastPlanAddRequestDate' not in ui, 'temporary_preview_callback_diagnostic_remains')
require('plan-add-debug-' not in ui, 'temporary_preview_scaffold_diagnostic_remains')
require('testTag("workplan-day-$date")' in ui, 'stable_workplan_day_debug_tag_missing')
require('testTag("plan-period-editor-$initialDate")' in ui, 'stable_dated_editor_debug_tag_missing')
require('debugTestTag = "overview-nav-trip-plan"' in ui, 'stable_overview_navigation_debug_tag_missing')
require('Modifier.testTag("workplan-screen")' in ui, 'stable_workplan_screen_debug_tag_missing')

require(runtime_test.is_file(), 'uxfix_runtime_test_missing')
runtime_text = runtime_test.read_text(encoding='utf-8')
require('tripTitleShowsNeutralExample' in runtime_text, 'neutral_example_runtime_test_missing')
require('tappingEmptyWorkPlanDayOpensEditorForThatDate' in runtime_text, 'empty_day_runtime_test_missing')
require('__FERIETUR_UXFIX01__' in runtime_text, 'runtime_test_namespace_missing')
require(upgrade_test.is_file(), 'uxfix_upgrade_state_test_missing')
upgrade_text = upgrade_test.read_text(encoding='utf-8')
require('capturePreInstallState' in upgrade_text, 'pre_install_state_test_missing')
require('verifyPostInstallStateAndCleanup' in upgrade_text, 'post_install_state_test_missing')
require('.ferietur-uxfix01-upgrade-state' in upgrade_text, 'upgrade_sentinel_namespace_missing')

print('UXFIX01_01_NEUTRAL_TRIP_TITLE_EXAMPLE=PASS')
print('UXFIX01_02_FULL_HEIGHT_ROSTER_EDITOR=PASS')
print('UXFIX01_03_PREVIOUS_TEMPLATE_REPLACES_PRIMARY_SHIFT=PASS')
print('UXFIX01_04_EMPTY_WORKPLAN_DAY_CLICK=PASS')
print('UXFIX01_05_EXTENDED_OSLO_YELLOW_PERIOD_MENU=PASS')
print('UXFIX01_06_PERIOD_EDITOR_CONTRAST=PASS')
print('UXFIX01_07_DOCUMENTATION_ACTION_HIERARCHY=PASS')
print('UXFIX01_08_TRIP_TITLE_IME_DONE=PASS')
print('UXFIX01_09_DURABLE_STATE_AND_DEVICE_CONTRACTS=DEFINED')
print('UXFIX01_10_EMPTY_TIMELINE_PHYSICAL_TOUCH_FIX=PASS')
print('UXFIX01_11_TEMPORARY_PREVIEW_DIAGNOSTICS_REMOVED=PASS')
print('UXFIX01_12_STABLE_DEBUG_TESTABILITY_TAGS=PASS')
print('UXFIX01_CONTRACT=PASS')
PY
