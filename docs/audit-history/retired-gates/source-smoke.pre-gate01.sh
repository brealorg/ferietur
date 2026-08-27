#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
grep -Fq 'version "9.3.1"' build.gradle.kts
grep -Fq 'VERSION=9.7.1' tools/gradle.sh
grep -Fq 'Jeg har kontrollert lønnstrinn og lørdags-/søndagssats mot en nyere lønnsslipp' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Checkbox(checked = payslipChecked' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'overlayTravelOnPlan' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt

grep -Fq 'Du kan endre, legge til og slette perioder fritt.' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Legg til periode' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'PeriodTypeMenu' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Hvilende nattevakt' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'rememberBottomSheetState(' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'initialValue = SheetValue.Hidden' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Betalingsforslag' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Beregnet betalingsgrunnlag' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Samme tid er registrert to ganger' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'DateTimeFormatter.ofPattern("HH:mm")' app/src/main/java/app/ferietur/ui/FerieturApp.kt
if grep -Fq 'TopAppBar(' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=PINNED_TOP_APP_BAR_REINTRODUCED"
    exit 90
fi
if grep -R -Fq 'tariffkryss' app/src/main/java; then
    echo "STOP=STALE_TARIFF_CROSSING_JARGON"
    exit 91
fi
if grep -R -Fq 'Arbeidstidsgrunnlag' app/src/main/java; then
    echo "STOP=STALE_WORK_BASIS_JARGON"
    exit 92
fi
if grep -R -Fq 'Dok. 25 §' app/src/main/java; then
    echo "STOP=STALE_SECTION_SYMBOL_SOURCE_LABEL"
    exit 93
fi
if grep -Fq 'import androidx.compose.foundation.layout.weight' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=STALE_COMPOSE_WEIGHT_IMPORT"
    exit 94
fi
grep -Fq 'assertTrue(line.includedInKnownTotal)' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'assertEquals(CalculationCertainty.CONFIRMED, line.certainty)' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'travelReplacesOverlappingRosterWorkInsteadOfDoublingTime' app/src/test/java/app/ferietur/domain/TripPlanOverlayTest.kt
grep -Fq 'weekendRateUsesHigherOfPercentageAndMinimum' app/src/test/java/app/ferietur/domain/TariffMathTest.kt
grep -Fq 'overlapIsIntentional' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'travelAnnotatesActive' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'normalizeTravelClassification' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'projectVisibleDay' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'continuesFromPreviousDay' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'overnightRestingWatchIsProjectedOnBothCalendarDays' app/src/test/java/app/ferietur/domain/TripPlanOverlayTest.kt
grep -Fq 'manualTravelReclassifiesOverlappingActiveWork' app/src/test/java/app/ferietur/domain/TripPlanOverlayTest.kt
grep -Fq 'id = "active-on-resting"' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq '30 min betalt' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'resting-evening-night' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'resting-weekend' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'lønnsekvivalent' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'Kapittel 12-tillegg stables ikke på de samme timene som kompenseres etter punkt 20.2' app/src/main/java/app/ferietur/domain/Rules.kt
grep -Fq 'timeKindContainerColor' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'coloredBlocks' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'holidaySupplementWindows' app/src/main/java/app/ferietur/domain/OsloHolidayCalendar.kt
grep -Fq 'Helge- og høytidsdagstillegg' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
if grep -Fq 'holiday-overtime-open' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt; then
    echo "STOP=STALE_HOLIDAY_OVERTIME_OPEN_RULE"
    exit 127
fi
grep -Fq 'Dag-for-dag kontroll' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'DayCalculationAuditCard' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'ordinaryWorkOnMay17GetsHolidaySupplementAndNotWeekendSupplementForSameMinutes' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'standardTurnusEasterWindowRunsFromMaundyThursdayToTuesdayMidnight' app/src/test/java/app/ferietur/domain/OsloHolidayCalendarTest.kt
if grep -R -Fq 'active-on-resting-open' app/src/main/java; then
    echo "STOP=STALE_ACTIVE_RESTING_OPEN_RULE"
    exit 95
fi
if grep -R -Fq 'Kombinasjon av 50 prosent og andre tillegg' app/src/main/java; then
    echo "STOP=STALE_UNCLEAR_50_PERCENT_RULE_LABEL"
    exit 96
fi
if grep -R -Fq --exclude='source-smoke.sh' 'kotlinc' tools; then
    echo "STOP=TOOLS_REQUIRE_KOTLINC"
    exit 97
fi

grep -Fq 'detail = "${minutesLabel(holidayMinutes)} × ${moneyRate(rate)}"' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'Satsen som vises i regnestykket er allerede dette tillegget' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'maundyThursdayInStandardTurnusGetsHolidaySupplementWithNonDuplicatedFormulaText' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'dayBeforeStandardEasterWindowDoesNotGetHolidaySupplement' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'easterMondayIsSpecialOvertimeDate' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
if grep -Fq '· 1 1/3 timelønn i tillegg' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt; then
    echo "STOP=STALE_DOUBLE_MULTIPLIER_HOLIDAY_FORMULA_TEXT"
    exit 98
fi

grep -Fq 'FlowScreen.SUMMARY' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Oppsummering og eksport' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Lag og del kort oppsummering' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Lag og del fullt beregningsgrunnlag' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'FinalizedTripSnapshotBuilder' app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt
grep -Fq 'Grunnturnus' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Dag for dag' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Arbeidstid som bør vurderes' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'FileProvider' app/src/main/AndroidManifest.xml
grep -Fq 'snapshotKeepsRosterCalculationSettlementAndSourcesTogether' app/src/test/java/app/ferietur/domain/FinalizedTripSnapshotTest.kt
if grep -R -Eq 'rememberModalBottomSheetState|Icons\.Rounded\.(ArrowBack|HelpOutline)|Locale\("nb", "NO"\)|headlineContent[[:space:]]*=' app/src/main/java; then
    echo "STOP=DEPRECATED_SOURCE_API_REINTRODUCED"
    exit 99
fi
grep -Fq 'jniLibs.keepDebugSymbols += "**/libandroidx.graphics.path.so"' app/build.gradle.kts
grep -Fq 'private val NORWEGIAN_LOCALE = Locale.forLanguageTag("nb-NO")' app/src/main/java/app/ferietur/export/PdfExporter.kt
if grep -Fq 'NumberFormat.getCurrencyInstance(norwegian)' app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=PDF_EXPORTER_STALE_LOCALE_SCOPE"
    exit 100
fi

grep -Fq 'Slik er beløpet satt sammen' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Arbeidstid som bør vurderes' app/src/main/java/app/ferietur/export/PdfExporter.kt
if grep -Fq 'Kan enkelte høytidsdager gi høyere overtidsbetaling enn 50 prosent?' app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=PDF_STILL_EXPOSES_RESOLVED_HOLIDAY_PRIORITY_AS_OPEN"
    exit 128
fi
if grep -Fq 'Full beregningsspesifikasjon' app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=STALE_PDF_TECHNICAL_HEADING"
    exit 101
fi
grep -Fq 'data class SavedTripDraft' app/src/main/java/app/ferietur/domain/SavedTripDraft.kt
grep -Fq 'object SavedTripDraftCodec' app/src/main/java/app/ferietur/domain/SavedTripDraft.kt
grep -Fq 'class TripDraftStore' app/src/main/java/app/ferietur/data/TripDraftStore.kt
grep -Fq 'Utkast lagres automatisk. Kopier endrer ikke originalen.' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'LaunchedEffect(' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'delay(250)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'roundTripKeepsEditableTripState' app/src/test/java/app/ferietur/domain/SavedTripDraftCodecTest.kt
grep -Fq 'private enum class DraftSaveState' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'DraftSaveState.SAVED -> "Lagret"' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'DraftSaveState.SAVING -> "Lagrer…"' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'DraftSaveState.ERROR -> "Ikke lagret"' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'contentDescription = if (saveUi.state == DraftSaveState.ERROR) "Prøv å lagre på nytt" else "Lagre nå"' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'ScreenHeader("Turoversikt", "", onBack)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq '"Fortsett der du slapp"' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq '"Sist arbeidssteg:' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq '"Oppsummering og eksport"' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'tripOverviewOpen = true' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'runCatching { draftStore.save(draft) }' app/src/main/java/app/ferietur/ui/FerieturApp.kt

grep -Fq 'enum class EmployerKind' app/src/main/java/app/ferietur/domain/TripModels.kt
grep -Fq 'enum class PayingParty' app/src/main/java/app/ferietur/domain/TripModels.kt
grep -Fq 'enum class RosterComparisonMode' app/src/main/java/app/ferietur/domain/TripModels.kt
grep -Fq 'const val SCHEMA_VERSION = 4' app/src/main/java/app/ferietur/domain/SavedTripDraft.kt
grep -Fq 'schemaOneMigratesWithoutGuessingEmployerOrPayer' app/src/test/java/app/ferietur/domain/SavedTripDraftCodecTest.kt
grep -Fq 'rosterComparisonIsIndependentFromEmployerAndPayer' app/src/test/java/app/ferietur/domain/TripFrameworkTest.kt
grep -Fq 'EmployerKind.UNSPECIFIED' app/src/main/java/app/ferietur/domain/SavedTripDraft.kt
grep -Fq 'payingParty = payingParty' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'rosterComparisonMode = rosterComparisonMode' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Arbeidsgiverforholdet må avklares' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Foreløpig regnegrunnlag' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Regler appen bruker' app/src/main/java/app/ferietur/export/PdfExporter.kt
if grep -Fq 'Ved et separat privat oppdrag' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt; then
    echo "STOP=ROSTER_MODE_STILL_IMPLIES_PRIVATE_EMPLOYMENT"
    exit 102
fi
if grep -Fq 'Vanlig turnus er ikke brukt som betalingsgrunnlag' app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=PDF_STILL_CONFLATES_ROSTER_WITH_PAYMENT"
    exit 103
fi

grep -Fq 'ScreenHeader("Lønn og betaling", stepLabel, onBack)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Hva skjer med den vanlige lønnen din?' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Jeg beholder lønnen for vaktene i turnusen' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Hvem skal betalingsforslaget settes opp mot?' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Arbeidsgiver: Oslo kommune' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Jeg er usikker' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'private fun SimpleChoiceCard' app/src/main/java/app/ferietur/ui/FerieturApp.kt
if grep -Fq 'Her skiller vi mellom hvem som er arbeidsgiver' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=OLD_LEGALISTIC_FRAMEWORK_INTRO_PRESENT"
    exit 104
fi
if grep -Fq 'Hvordan skal vanlig turnus brukes?' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=OLD_ROSTER_JARGON_PRESENT"
    exit 105
fi

grep -Fq 'enum class PaymentTreatment' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'paymentBasisAmount' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'applicableUnresolvedRuleIds' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'applicableUnresolvedRules' app/src/main/java/app/ferietur/domain/Rules.kt
grep -Fq 'outsideTripRangeBlocks' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'Arbeid ligger utenfor turen' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Grunnturnus – ikke i betalingsgrunnlaget' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'normalRosterSupplementsAreDocumentedButExcludedFromAdditionalPaymentBasis' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'visibleLineAmountsAreRoundedBeforeTheyAreSummed' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'workAfterTripEndIsReturnedAsBlockingRangeIssue' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'snapshotRejectsWorkOutsideExactTripBounds' app/src/test/java/app/ferietur/domain/FinalizedTripSnapshotTest.kt
grep -Fq 'snapshotRejectsStaleSettlementAmount' app/src/test/java/app/ferietur/domain/FinalizedTripSnapshotTest.kt
grep -Fq 'currency(snapshot.calculation.paymentBasisAmount)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'runCatching { rebuildFinalizedSnapshot(draft) }.getOrNull()' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'adjustPlanForComparisonChange' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'changingComparisonModeDoesNotDestroyAnExistingActualWorkPlan' app/src/test/java/app/ferietur/domain/TripPlanOverlayTest.kt

grep -Fq 'object LegacyTripRecovery' app/src/main/java/app/ferietur/domain/LegacyTripRecovery.kt
grep -Fq 'recoverKnownDraft' app/src/main/java/app/ferietur/domain/LegacyTripRecovery.kt
grep -Fq 'recoversKnownSolgardenPlanWhenOnlyTravelSurvived' app/src/test/java/app/ferietur/domain/LegacyTripRecoveryTest.kt
grep -Fq 'trip-draft-backups' app/src/main/java/app/ferietur/data/TripDraftStore.kt
grep -Fq 'MAX_BACKUPS_PER_TRIP = 12' app/src/main/java/app/ferietur/data/TripDraftStore.kt
grep -Fq 'preservePlanForDateRange' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'changingTripDateRangePreservesExistingWorkAndLeavesNewDatesEmpty' app/src/test/java/app/ferietur/domain/TripPlanOverlayTest.kt

grep -Fq 'versionCode = 49' app/build.gradle.kts
grep -Fq 'versionName = "0.5.4-r2"' app/build.gradle.kts
grep -Fq 'FERIETUR_APP_VERSION = "0.5.4-r2-a54r2"' app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt
if grep -Fq 'assertEquals("0.5.1-a51", FERIETUR_APP_VERSION)' app/src/test/java/app/ferietur/domain/FinalRegressionPolicyTest.kt; then
    echo "STOP=STALE_APP_VERSION_LOCK_IN_FINAL_REGRESSION_TEST"
    exit 143
fi
if grep -Fq 'FERIETUR_APP_VERSION == "0.5.1-a51"' tools/A44FinalRegressionSmoke.kt; then
    echo "STOP=STALE_APP_VERSION_LOCK_IN_FINAL_REGRESSION_SMOKE"
    exit 144
fi
grep -Fq 'assertTrue(FERIETUR_APP_VERSION.isNotBlank())' app/src/test/java/app/ferietur/domain/FinalRegressionPolicyTest.kt
grep -Fq 'check(FERIETUR_APP_VERSION.isNotBlank())' tools/A44FinalRegressionSmoke.kt
grep -Fq 'KORT OPPSUMMERING' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'FULLT BEREGNINGSGRUNNLAG' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Foreløpig betalingsgrunnlag i tillegg til grunnturnusen' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Slik er beløpet satt sammen' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Arbeidsgrunnlaget' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Hvorfor blir beløpet slik?' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Her kan du kontrollere hva hver kalenderdag bidrar med' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Den avgjør ikke om arbeidsordningen er lovlig' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'fun moneyRow' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'fun statusRow' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'fun dayTotalHeader' app/src/main/java/app/ferietur/export/PdfExporter.kt
if grep -Fq 'Før beløpet brukes' app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=STALE_LARGE_STATUS_SECTION_IN_PDF"
    exit 106
fi
if grep -Fq 'De sier ikke at turen er ulovlig' app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=STALE_DEFENSIVE_WORKTIME_WORDING_IN_PDF"
    exit 107
fi

grep -Fq 'Mulig tillegg:' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Grunnturnus som sammenligningsgrunnlag' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Beregnede turnustillegg fra grunnturnusen' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Sum fordelt på kalenderdager' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Totalt betalingsgrunnlag' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'fun controlSummaryRow' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'fun keepTogether' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'payableTravelWithResponsibilityMinutes' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'rosterServiceBlocks' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'coveredTurnusSupplementsComeFromOriginalRosterEvenWhenTripWorkDoesNotCoverWholeShift' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'additionalActiveLineSeparatesWorkFromTravelWithResponsibility' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
if [ "$(grep -Fc 'writer.pageBreak()' app/src/main/java/app/ferietur/export/PdfExporter.kt)" -ne 1 ]; then
    echo "STOP=UNEXPECTED_PDF_SECTION_PAGE_BREAK_COUNT"
    exit 108
fi

grep -Fq 'rosterUncoveredMinutes' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'rosterGapConfirmed' app/src/main/java/app/ferietur/domain/SavedTripDraft.kt
grep -Fq 'Jeg har kontrollert at denne turnustiden er riktig registrert' app/src/main/java/app/ferietur/ui/FerieturApp.kt
if grep -Fq 'outside-evening-night-open' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt || grep -Fq 'outside-weekend-open' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt; then
    echo "STOP=STALE_CHAPTER12_STACKING_OPEN_LINES"
    exit 129
fi
grep -Fq 'Med dagens registrerte timer og satser er mulig tillegg' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Kontrollgrunnlag: arbeidsmiljøloven kapittel 10 og Dok. 25 punkt 20.2' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'snapshotRequiresExplicitConfirmationWhenRosterHasUnregisteredTime' app/src/test/java/app/ferietur/domain/FinalizedTripSnapshotTest.kt
grep -Fq 'point20_2EveningMinutesDoNotStackChapter12EveningSupplement' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'rosterGapEvidenceIdentifiesExactUnregisteredHourInsideNightShift' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt

# A3.9C-R3: final PDF v1 hardening is presentation-only.
grep -Fq 'Betalingsscenario' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'fastsetter ikke hvem som rettslig skal bære kostnaden' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Dette er en modellforutsetning, ikke en gjengivelse av ordlyden i Dok. 25 punkt 20.2' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'arbeidstid ut over ordinær arbeidstid etter kapittel 8 kompenseres med timelønn pluss 50 prosent' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq '% av timelønn, minst' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'keepDayAuditTogether' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'keepControlGroupTogether' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'weekendFormulaShowsResolvedRateWithoutApplyingPercentageTwice' app/src/test/java/app/ferietur/export/PdfExporterPolicyTest.kt
grep -Fq 'activeExplanationSeparatesAppComparisonModelFromTariffWording' app/src/test/java/app/ferietur/export/PdfExporterPolicyTest.kt
grep -Fq 'paymentScenarioDisclaimerDoesNotAssignLegalLiability' app/src/test/java/app/ferietur/export/PdfExporterPolicyTest.kt
if grep -Fq 'summaryMetaRow("Arbeidsgiver", employerLabel(s.employerKind), "Betaler"' app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=PDF_STILL_ASSERTS_BETALER_LABEL"
    exit 109
fi
if grep -Fq 'Arbeid som ligger utenfor grunnturnusen beregnes med timelønn pluss 50 prosent etter punkt 20.2.' app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=PDF_STILL_CONFLATES_MODEL_AND_TARIFF_WORDING"
    exit 110
fi


# A3.9C-R4: micro-final PDF v1 acceptance fixes. Presentation-only.
grep -Fq 'keepH2WithFirstDetailBlock' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'sentenceWithFollowUp' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'warningFollowUpDoesNotAddPeriodAfterQuestionMark' app/src/test/java/app/ferietur/export/PdfExporterPolicyTest.kt
if grep -Fq '}}.$extra")' app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=PDF_WARNING_STILL_CAN_RENDER_QUESTION_PERIOD"
    exit 111
fi

# A4.0: point 18.4 travel without supervision responsibility.
grep -Fq 'id = "travel-without-responsibility"' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'Reise uten tilsynsansvar utenfor grunnturnusen' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'Varseltidspunktet registreres separat' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'travelWithoutResponsibilityOutsideRosterIsPaidAtOrdinaryHourlyRate' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'travelWithoutResponsibilityInsideNormalRosterDoesNotCreateAdditionalPayment' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'sleepAllowedNightTravelUsesPassiveOneThirdAndCountsAllNightAsWorktime' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'passiveNightTravelGetsWeekendSupplementAtOneThirdWithoutDoubleCountingHoliday' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'passiveNightTravelSeparatesHolidayFromWeekendSupplement' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'Nattreise med søvntillatelse · arbeid av passiv karakter' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
if grep -R -Fq 'travel-without-responsibility-open' app/src/main/java; then
    echo "STOP=STALE_OPEN_TRAVEL_WITHOUT_RESPONSIBILITY"
    exit 112
fi

# A4.0A: unified work/travel periods. Grunnturnus is comparison only, not an implicit work plan.
grep -Fq 'Legg inn det du faktisk gjør på turen, også utreise og hjemreise' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Triple(TimeKind.TRAVEL_UNCERTAIN, "Reise"' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Ansvar under reisen' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Kopier turnusdelen som ligger i turen' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'plannedBlocksForShiftWithinTrip' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'shortTripInsideLongRosterShiftHasNoArtificialRosterGap' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'comparisonModeNeverSeedsOrDeletesActualTripPlan' app/src/test/java/app/ferietur/domain/TripPlanOverlayTest.kt
grep -Fq 'copyingRosterIntoShortTripClipsToTripBounds' app/src/test/java/app/ferietur/domain/TripPlanOverlayTest.kt
if awk '/private fun flowSequence/{flag=1} flag{print} /private fun screenStepLabel/{flag=0}' app/src/main/java/app/ferietur/ui/FerieturApp.kt | grep -Fq 'add(FlowScreen.TRAVEL)'; then
    echo "STOP=TRAVEL_STILL_MANDATORY_FLOW_STEP"
    exit 113
fi
if grep -Fq 'OverviewSectionRow("Reise til og fra"' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=SEPARATE_TRAVEL_OVERVIEW_STEP_STILL_PRESENT"
    exit 114
fi

# A4.0B: result hierarchy. Ordinary-roster amounts are control information, not a competing payment result.
grep -Fq 'CoveredRosterSummaryCard' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Grunnturnus – ikke i betalingsgrunnlaget' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'i turnustillegg ligger i grunnturnusen og påvirker ikke beløpet over' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Vis turnuskontroll' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq '0,00 kr i tillegg' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'coveredRosterControlSummary' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'dayAuditCollapsedAmountSummary' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'collapsedDaySummaryDoesNotPromoteCoveredRosterAmount' app/src/test/java/app/ferietur/ui/CalculationResultHierarchyPolicyTest.kt
grep -Fq 'coveredRosterSummaryExplicitlyKeepsAmountOutsidePaymentBasis' app/src/test/java/app/ferietur/ui/CalculationResultHierarchyPolicyTest.kt
if grep -Fq 'SectionTitle("Allerede dekket gjennom vanlig turnus")' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=COVERED_ROSTER_STILL_HAS_COMPETING_SECTION_HEADING"
    exit 115
fi
if grep -Fq 'add("allerede dekket: ${currency(audit.alreadyCoveredSubtotal)}")' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=DAY_AUDIT_COLLAPSED_SUMMARY_STILL_PRIORITIZES_COVERED_AMOUNT"
    exit 116
fi

# A4.0C: app-wide content coherence and semantic boundaries.
grep -Fq 'Hvem skal betalingsforslaget settes opp mot?' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Det endrer ikke selve beregningen, og appen fastsetter ikke hvem som rettslig skal bære kostnaden' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Betalingsscenarioet brukes i betalingsforslaget og dokumentasjonen. Det endrer ikke selve beregningen' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Dokumenter et annet avtalt beløp' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'dayAuditPaymentContributions' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'vises bare for kontroll. Den er ikke med i betalingsgrunnlaget for turen' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'kan påvirke det endelige beregnede beløpet' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'Grunnturnus som sammenligningsgrunnlag' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'A4.0C content coherence audit' docs/A40C_CONTENT_COHERENCE_AUDIT.md
if grep -Fq 'kompromiss' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=STALE_SETTLEMENT_COMPROMISE_COPY"
    exit 117
fi
if grep -Fq 'Beboer eller verge skal betale' app/src/main/java/app/ferietur/ui/FerieturApp.kt || grep -Fq 'Oslo kommune skal dekke' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=UI_STILL_ASSERTS_PAYMENT_LIABILITY"
    exit 118
fi
if grep -Fq 'A4.0 beregner' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt; then
    echo "STOP=INTERNAL_SLICE_NAME_LEAKS_TO_USER_COPY"
    exit 119
fi
if grep -Fq 'Hvem som faktisk skal dekke det, avgjøres først når betalingsforslaget lages' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt; then
    echo "STOP=COVERED_ROSTER_COPY_STILL_ASSIGNMENT_AMBIGUOUS"
    exit 120
fi
if grep -Fq 'Text("ALLEREDE DEKKET"' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=OLD_COVERED_ROSTER_LABEL_STILL_VISIBLE"
    exit 121
fi

if grep -Fq 'Valget brukes i beregningen og PDF-en' app/src/main/java/app/ferietur/ui/FerieturApp.kt || grep -Fq 'Betalingsscenarioet er en forutsetning i beregningen' app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=PAYMENT_SCENARIO_STILL_IMPLIES_CALCULATION_EFFECT"
    exit 122
fi
if grep -Eqi 'vanlig turnus|opprinnelig turnus|original turnus' app/src/main/java/app/ferietur/ui/FerieturApp.kt app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=USER_FACING_ROSTER_TERMINOLOGY_DRIFT"
    exit 123
fi

echo "CONTENT_COHERENCE_SMOKE=PASS"

# A4.0D: rule applicability, travel worktime coherence, valid unresolved payer status and responsive controls.
grep -Fq 'fun chapter20Applies' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'Dagstur kan ikke beregnes som ferieopphold' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Dok. 25 punkt 20.1 sier at kapittel 20 ikke gjelder dagsturer' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'require(TripPlanEngine.chapter20Applies(tripStart, tripEnd))' app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt
grep -Fq 'travelInOrdinaryWorkTime' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'controlFindings(workBlocks, unresolvedRules.size, roster)' app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt
grep -Fq 'travelWithoutResponsibilityInsideOrdinaryRosterCountsAsWorktime' app/src/test/java/app/ferietur/domain/RuleApplicabilityWorktimePolicyTest.kt
grep -Fq 'snapshotRejectsDayTripForChapter20HolidayStay' app/src/test/java/app/ferietur/domain/FinalizedTripSnapshotTest.kt
grep -Fq 'unresolvedPaymentScenarioIsValidForConfirmedDocumentBasis' app/src/test/java/app/ferietur/domain/FinalizedTripSnapshotTest.kt
grep -Fq 'PayingParty.UNSPECIFIED -> "Ikke avklart ennå"' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Betalingsscenario: Ikke avklart ennå' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Ikke avklart ennå · gyldig status' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'Når du trykker Beregning' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'statusen på uavklarte regler' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'BasisChip("Tredelt turnus", WeeklyBasis.DOK25_8_2_2, weeklyBasis, onWeeklyBasis, Modifier.weight(1f))' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'OutlinedButton(onClick = { commit(emptyList()) }, modifier = Modifier.fillMaxWidth()) { Text("Sett dagen fri") }' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'A4.0D rule applicability + worktime audit' docs/A40D_RULE_APPLICABILITY_WORKTIME_AUDIT.md
if grep -Fq 'Betalingsscenario er ikke valgt' app/src/main/java/app/ferietur/ui/FerieturApp.kt app/src/main/java/app/ferietur/export/PdfExporter.kt; then
    echo "STOP=UNRESOLVED_PAYMENT_SCENARIO_STILL_TREATED_AS_MISSING"
    exit 122
fi
if grep -Fq 'Når du trykker Neste' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=TRIP_PLAN_COPY_STILL_NAMES_NONEXISTENT_NEXT_CTA"
    exit 123
fi
if grep -Fq 'uten å endre beregningen eller åpne regler' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=SETTLEMENT_COPY_STILL_USES_INTERNAL_OPEN_RULE_JARGON"
    exit 124
fi


# A4.0E: explicit sleep-permission state for 23:00-07:00 travel and range-coherent control output.
grep -Fq 'TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP' app/src/main/java/app/ferietur/domain/TripModels.kt
grep -Fq 'TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED' app/src/main/java/app/ferietur/domain/TripModels.kt
grep -Fq 'D25_20_3_SLEEP_PERMISSION' app/src/main/java/app/ferietur/domain/Rules.kt
grep -Fq 'id = "travel-passive-night"' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'id = "travel-passive-evening-night"' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'id = "travel-night-sleep-open"' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'Nattreise kl. 23:00–07:00' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Hadde du tillatelse til å sove under nattdelen?' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Nattdelen holdes utenfor betalingsgrunnlaget til søvntillatelsen er avklart.' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'sleepAllowedNightTravelUsesPassiveOneThirdAndCountsAllNightAsWorktime' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'passiveNightTravelGetsWeekendSupplementAtOneThirdWithoutDoubleCountingHoliday' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'passiveNightTravelSeparatesHolidayFromWeekendSupplement' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'unresolvedNightTravelIsHeldOpenInsteadOfAssumingOrdinaryTravel' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'coveredRosterControlIsClippedToTripRange' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'crossDateEvidenceShowsBothDatesInsteadOfAmbiguousClockRange' app/src/test/java/app/ferietur/ui/EvidenceFormattingPolicyTest.kt
grep -Fq 'Grunnturnustid som overlapper turen' app/src/main/java/app/ferietur/ui/FerieturApp.kt app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'A4.0E night travel + passive-character audit' docs/A40E_NIGHT_TRAVEL_PASSIVE_AUDIT.md
if grep -Fq 'Appen beregner tiden med ordinær timelønn og forutsetter derfor at du ikke hadde slik tillatelse' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt; then
    echo "STOP=NIGHT_TRAVEL_STILL_ASSUMES_NO_SLEEP"
    exit 125
fi
if grep -Fq '08:00–10:00 · 26 t' app/src/main/java/app/ferietur/ui/FerieturApp.kt; then
    echo "STOP=CROSS_DATE_EVIDENCE_CAN_STILL_RENDER_AMBIGUOUS_RANGE"
    exit 126
fi

echo "SOURCE_SMOKE=PASS"

# A4.0E1 night-travel state-integrity gates
grep -Fq 'travelWithoutResponsibilitySelection(selected)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'fun travelWithoutResponsibilitySelection' app/src/main/java/app/ferietur/ui/NightTravelStatePolicy.kt
grep -Fq 'Søvntillatelse: Ikke avklart' app/src/main/java/app/ferietur/ui/NightTravelStatePolicy.kt
grep -Fq 'NightTravelStateIntegrityPolicyTest' app/src/test/java/app/ferietur/ui/NightTravelStateIntegrityPolicyTest.kt
grep -Fq 'tappingAlreadySelectedWithoutResponsibilityDoesNotEraseSleepAllowed' app/src/test/java/app/ferietur/ui/NightTravelStateIntegrityPolicyTest.kt
grep -Fq 'TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP' app/src/test/java/app/ferietur/domain/SavedTripDraftCodecTest.kt
grep -Fq 'TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED' app/src/test/java/app/ferietur/domain/SavedTripDraftCodecTest.kt
grep -Fq 'periodTypeSelection(block.kind, kind)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'fun periodTypeSelection' app/src/main/java/app/ferietur/ui/NightTravelStatePolicy.kt


# A4.1: close point-20.2 tariff-priority interactions.
grep -Fq 'RuleStatus.IMPLEMENTED' app/src/main/java/app/ferietur/domain/Rules.kt
grep -Fq 'Punkt 12.1.1 og 12.2.2 gjelder ordinær tjeneste' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'punkt 13.1 sier at kapittel 13 gjelder dersom ikke annet er fastsatt i tariffavtalen' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'point20_2SaturdayMinutesDoNotStackChapter12EveningOrWeekendSupplements' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'outsideRosterWorkOnSpecialHolidayUsesPoint20_2InsteadOfCreating133OvertimeOpenAmount' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'TariffPriorityPolicyTest' app/src/test/java/app/ferietur/domain/TariffPriorityPolicyTest.kt
grep -Fq 'A4.1 tariff priority closure audit' docs/A41_TARIFF_PRIORITY_CLOSURE_AUDIT.md
grep -Fq 'D25_20_2_X13_7_3' app/src/main/java/app/ferietur/domain/Rules.kt
if grep -R -Fq --exclude='source-smoke.sh' 'outside-evening-night-open' app/src/main/java; then
    echo "STOP=A41_STALE_EVENING_STACKING_OPEN_ID"
    exit 130
fi
if grep -R -Fq --exclude='source-smoke.sh' 'outside-weekend-open' app/src/main/java; then
    echo "STOP=A41_STALE_WEEKEND_STACKING_OPEN_ID"
    exit 131
fi
if grep -R -Fq --exclude='source-smoke.sh' 'holiday-overtime-open' app/src/main/java; then
    echo "STOP=A41_STALE_HOLIDAY_OVERTIME_OPEN_ID"
    exit 132
fi

echo "A41_TARIFF_PRIORITY_SMOKE=PASS"

# A4.1R2: tariff-priority test gate must assert semantics, not sentence capitalization.
if grep -Fq 'explanation.contains("punkt 12.1.1")' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt; then
    echo "STOP=A41R2_STALE_CASE_SENSITIVE_TARIFF_ASSERTION"
    exit 133
fi
grep -Fq 'activeExplanation.contains("12.1.1")' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'activeExplanation.contains("ikke utbetales for overtid")' app/src/test/java/app/ferietur/domain/TripPlanEngineTest.kt
grep -Fq 'check("12.1.1" in mondayActive.explanation)' tools/A41TariffPrioritySmoke.kt
grep -Fq 'check("ikke utbetales for overtid" in mondayActive.explanation)' tools/A41TariffPrioritySmoke.kt
echo "A41R2_TEST_GATE_SMOKE=PASS"


# A4.2: point-18.4 travel notice and short-notice overtime.
grep -Fq 'enum class TravelNoticeStatus' app/src/main/java/app/ferietur/domain/TripModels.kt
grep -Fq 'SCHEMA_VERSION = 4' app/src/main/java/app/ferietur/domain/SavedTripDraft.kt
grep -Fq 'block.travelNoticeStatus.name' app/src/main/java/app/ferietur/domain/SavedTripDraft.kt
grep -Fq 'Fikk du vite om reisen senest dagen i forveien?' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'travel-short-notice-overtime' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'travel-notice-open' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'travel-short-notice-133-open' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'return shift.code == "F1"' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'val shortNoticeCappedBlocks = takeFirstMinutes(shortNoticeOrdinaryTravelBlocks, 120L)' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'TravelNoticeShortNoticePolicyTest' app/src/test/java/app/ferietur/domain/TravelNoticeShortNoticePolicyTest.kt
grep -Fq 'multipleShortNoticeBlocksShareOneTwoHourCapForTheTrip' app/src/test/java/app/ferietur/domain/TravelNoticeShortNoticePolicyTest.kt
grep -Fq 'schemaThreeTravelMigratesNoticeConservativelyToNotClarified' app/src/test/java/app/ferietur/domain/SavedTripDraftCodecTest.kt
grep -Fq 'ordinaryTravelExplanationNoLongerAssumesNoticeWasKnown' app/src/test/java/app/ferietur/export/PdfExporterPolicyTest.kt
grep -Fq 'A4.2 — point 18.4 travel-notice / short-notice audit' docs/A42_TRAVEL_NOTICE_SHORT_NOTICE_AUDIT.md
grep -Fq 'FERIETUR_RULESET_VERSION = "2026.3"' app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt
if grep -R -Fq --exclude='source-smoke.sh' 'Beregningen forutsetter at reisen var kjent senest dagen før' README.md docs app/src/main/java; then
    echo "STOP=A42_STALE_TRAVEL_NOTICE_ASSUMPTION"
    exit 134
fi
if grep -R -Fq --exclude='source-smoke.sh' 'not yet automatic' README.md docs; then
    echo "STOP=A42_DOCS_STILL_CALL_SHORT_NOTICE_UNIMPLEMENTED"
    exit 135
fi
echo "A42_TRAVEL_NOTICE_SMOKE=PASS"


# A4.3: unresolved travel-notice evidence + separate-trip E2E contract.
grep -Fq 'possibleShortNoticeDelta' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq 'Beløpet på denne åpne posten er derfor et mulig tillegg' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
grep -Fq '"D25_18_4_NOTICE" -> setOf("travel-notice-open")' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'unresolvedNoticePossibleAmountRespectsSharedTwoHourCap' app/src/test/java/app/ferietur/domain/TravelNoticeShortNoticePolicyTest.kt
grep -Fq 'class SeparateTripE2EPolicyTest' app/src/test/java/app/ferietur/domain/SeparateTripE2EPolicyTest.kt
grep -Fq 'separateTripUsesRegisteredPlanAndDoesNotSubtractRoster' app/src/test/java/app/ferietur/domain/SeparateTripE2EPolicyTest.kt
grep -Fq 'separateTripFinalizesWithoutRosterGapConfirmation' app/src/test/java/app/ferietur/domain/SeparateTripE2EPolicyTest.kt
grep -Fq 'A4.3 — separate-trip E2E + open-rule evidence hardening' docs/A43_SEPARATE_TRIP_E2E_EVIDENCE_AUDIT.md
grep -Fq 'assertEquals(calculation.hourlyRate.setScale(2, RoundingMode.HALF_UP), open.amount)' app/src/test/java/app/ferietur/domain/TravelNoticeShortNoticePolicyTest.kt
echo "A43_SEPARATE_TRIP_E2E_SMOKE=PASS"


# A4.4: final regression hardening and mode-sensitive evidence.
grep -Fq 'Hele den registrerte arbeidsplanen brukes som beregningsgrunnlag. Når du trykker Beregning' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Hele den registrerte arbeidsplanen brukes som beregningsgrunnlag.' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'shortFooterDescription(s.rosterComparisonMode)' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'separateTripCalculationNote()' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'separateTripPdfCopyDoesNotClaimGroundRosterIsPartOfTheBasis' app/src/test/java/app/ferietur/export/PdfExporterPolicyTest.kt
grep -Fq 'class FinalRegressionPolicyTest' app/src/test/java/app/ferietur/domain/FinalRegressionPolicyTest.kt
grep -Fq 'A44_FINAL_REGRESSION_DOMAIN_SMOKE=PASS' tools/A44FinalRegressionSmoke.kt
grep -Fq 'solgardenGoldenScenarioKeepsAcceptedPaymentBasisAndNoOpenCalculationRules' app/src/test/java/app/ferietur/domain/FinalRegressionPolicyTest.kt
grep -Fq 'passiveNightTravelThreeStateContractStaysStable' app/src/test/java/app/ferietur/domain/FinalRegressionPolicyTest.kt
grep -Fq 'separateTripGoldenScenarioUsesOnlyRegisteredPlan' app/src/test/java/app/ferietur/domain/FinalRegressionPolicyTest.kt
grep -Fq 'A4.4 — final regression hardening audit' docs/A44_FINAL_REGRESSION_HARDENING_AUDIT.md
grep -Fq 'FERIETUR_RULESET_VERSION = "2026.3"' app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt
echo "A44_FINAL_REGRESSION_SMOKE=PASS"


# A5.0: trip-library lifecycle, safe deletion and copy-as-template.
grep -Fq 'enum class TripLibraryLifecycle' app/src/main/java/app/ferietur/ui/TripLibraryPolicy.kt
grep -Fq 'fun copiedTripDraft' app/src/main/java/app/ferietur/ui/TripLibraryPolicy.kt
grep -Fq 'title = "Pågående"' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'title = "Klar for eksport"' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'title = { Text("Slett turen?") }' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Text("Lag kopi")' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'copyBecomesFreshEditableDraftWithoutOldConfirmations' app/src/test/java/app/ferietur/ui/TripLibraryPolicyTest.kt
grep -Fq 'A5.0 trip library lifecycle audit' docs/A50_TRIP_LIBRARY_LIFECYCLE_AUDIT.md
grep -Fq 'FERIETUR_RULESET_VERSION = "2026.3"' app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt
echo "A50_TRIP_LIBRARY_LIFECYCLE_SMOKE=PASS"


# A5.1: approved Oslo-inspired stable brand accents and deterministic theme.
grep -Fq 'val OsloYellow = Color(0xFFF9C66B)' app/src/main/java/app/ferietur/ui/theme/FerieturTheme.kt
grep -Fq 'val OsloRed = Color(0xFFFF8274)' app/src/main/java/app/ferietur/ui/theme/FerieturTheme.kt
grep -Fq 'val OsloDarkBlue = Color(0xFF2A2859)' app/src/main/java/app/ferietur/ui/theme/FerieturTheme.kt
if grep -Fq 'dynamicDarkColorScheme' app/src/main/java/app/ferietur/ui/theme/FerieturTheme.kt || grep -Fq 'dynamicLightColorScheme' app/src/main/java/app/ferietur/ui/theme/FerieturTheme.kt; then
    echo "STOP=A51_DYNAMIC_WALLPAPER_COLORS_REINTRODUCED"
    exit 136
fi
grep -Fq 'private fun OsloHomeHeader' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'private fun OsloIdentityShapes' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'A5.1 Oslo visual refresh audit' docs/A51_OSLO_VISUAL_REFRESH_AUDIT.md
grep -Fq 'FERIETUR_RULESET_VERSION = "2026.3"' app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt
echo "A51_OSLO_VISUAL_REFRESH_SMOKE=PASS"

# A5.1R1: compact Material list parity with overflow actions and one primary create action.
HOME_BLOCK="$(awk '/private fun HomeScreen\(/{flag=1} /private fun TripOverviewScreen\(/{flag=0} flag' app/src/main/java/app/ferietur/ui/FerieturApp.kt)"
printf '%s' "$HOME_BLOCK" | grep -Fq 'ExtendedFloatingActionButton('
printf '%s' "$HOME_BLOCK" | grep -Fq 'Text("Ny tur"'
printf '%s' "$HOME_BLOCK" | grep -Fq 'private fun TripLibraryList'
printf '%s' "$HOME_BLOCK" | grep -Fq 'private fun SavedTripListItem'
printf '%s' "$HOME_BLOCK" | grep -Fq 'ListItem('
printf '%s' "$HOME_BLOCK" | grep -Fq 'Icons.Rounded.MoreVert'
printf '%s' "$HOME_BLOCK" | grep -Fq 'DropdownMenuItem('
printf '%s' "$HOME_BLOCK" | grep -Fq 'Text("Lag kopi")'
printf '%s' "$HOME_BLOCK" | grep -Fq 'Text("Slett", color = MaterialTheme.colorScheme.error)'
printf '%s' "$HOME_BLOCK" | grep -Fq 'title = "Klar for eksport"'
if printf '%s' "$HOME_BLOCK" | grep -Fq 'TripLibraryStatusStrip('; then
    echo "STOP=A51R1_REDUNDANT_STATUS_STRIP_REINTRODUCED"
    exit 137
fi
if printf '%s' "$HOME_BLOCK" | grep -Fq 'Text("Ny beregning"'; then
    echo "STOP=A51R1_OLD_PRIMARY_ACTION_REINTRODUCED"
    exit 138
fi
grep -Fq 'A5.1R1 library layout parity audit' docs/A51R1_LIBRARY_LAYOUT_PARITY_AUDIT.md
echo "A51R1_LIBRARY_LAYOUT_PARITY_SMOKE=PASS"

# A5.1R2: frozen tariff regression must not be pinned to a transient product/UI version.
grep -Fq 'assertTrue(FERIETUR_APP_VERSION.isNotBlank())' app/src/test/java/app/ferietur/domain/FinalRegressionPolicyTest.kt
grep -Fq 'check(FERIETUR_APP_VERSION.isNotBlank())' tools/A44FinalRegressionSmoke.kt
grep -Fq 'A5.1R2 final regression version-gate hotfix' docs/A51R2_FINAL_REGRESSION_VERSION_GATE_HOTFIX_AUDIT.md
echo "A51R2_FINAL_REGRESSION_VERSION_GATE_SMOKE=PASS"


# A5.1R3: device-driven visual polish without changing the accepted library IA.
HOME_BLOCK_R3="$(awk '/private fun HomeScreen\(/{flag=1} /private fun TripOverviewScreen\(/{flag=0} flag' app/src/main/java/app/ferietur/ui/FerieturApp.kt)"
printf '%s' "$HOME_BLOCK_R3" | grep -Fq '"Beregn ferietur uten Excel."'
printf '%s' "$HOME_BLOCK_R3" | grep -Fq '"Utkast lagres automatisk. Kopier endrer ikke originalen."'
printf '%s' "$HOME_BLOCK_R3" | grep -Fq 'OsloIdentityShapes(modifier = Modifier.size(60.dp))'
printf '%s' "$HOME_BLOCK_R3" | grep -Fq 'modifier = Modifier.size(48.dp)'
printf '%s' "$HOME_BLOCK_R3" | grep -Fq 'tint = MaterialTheme.colorScheme.onSurface'
printf '%s' "$HOME_BLOCK_R3" | grep -Fq 'modifier = modifier.size(40.dp)'
printf '%s' "$HOME_BLOCK_R3" | grep -Fq 'maxLines = 2'
if printf '%s' "$HOME_BLOCK_R3" | grep -Fq 'Box(modifier = Modifier.size(82.dp))'; then
    echo "STOP=A51R3_INFO_DECORATION_COLLISION_REINTRODUCED"
    exit 145
fi
grep -Fq 'A5.1R3 library visual polish audit' docs/A51R3_LIBRARY_VISUAL_POLISH_AUDIT.md
grep -Fq 'FERIETUR_RULESET_VERSION = "2026.3"' app/src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt
echo "A51R3_LIBRARY_VISUAL_POLISH_SMOKE=PASS"


# A5.2: compact guided-flow chrome with explicit progress and low-noise navigation.
grep -Fq 'internal data class FlowChromeProgress' app/src/main/java/app/ferietur/ui/FlowChromePolicy.kt
grep -Fq 'val label: String get() = "Steg $current av $total"' app/src/main/java/app/ferietur/ui/FlowChromePolicy.kt
grep -Fq 'val progress = flowChromeProgress(stepLabel)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'color = MaterialTheme.colorScheme.secondary' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'style = MaterialTheme.typography.headlineSmall' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'class FlowChromePolicyTest' app/src/test/java/app/ferietur/ui/FlowChromePolicyTest.kt
grep -Fq 'A5.2 guided flow chrome audit' docs/A52_GUIDED_FLOW_CHROME_AUDIT.md
echo "A52_GUIDED_FLOW_CHROME_SMOKE=PASS"

# A5.3 surface-hierarchy regression gates.
grep -Fq 'private fun ControlFindingGroupRow' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Åpne en kategori for å se de konkrete periodene.' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'HorizontalDivider(modifier = Modifier.padding(start = 104.dp))' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'private fun ExplainableCalculationCard' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'private fun DayCalculationAuditCard' app/src/main/java/app/ferietur/ui/FerieturApp.kt
if sed -n '/private fun ExplainableCalculationCard/,/private fun DayCalculationAuditCard/p' app/src/main/java/app/ferietur/ui/FerieturApp.kt | grep -Eq '^[[:space:]]+Card\('; then
    echo "STOP=A53_CALCULATION_ROWS_RECARDED"
    exit 151
fi
if sed -n '/private fun SimpleChoiceCard/,/private fun MethodChoiceCard/p' app/src/main/java/app/ferietur/ui/FerieturApp.kt | grep -Eq '^[[:space:]]+Card\('; then
    echo "STOP=A53_CHOICE_ROWS_RECARDED"
    exit 152
fi
grep -Fq 'groupControlFindings(findings)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'internal fun groupControlFindings' app/src/main/java/app/ferietur/ui/SurfaceHierarchyPolicy.kt
grep -Fq 'repeatedControlFindingsCollapseIntoOneCategory' app/src/test/java/app/ferietur/ui/SurfaceHierarchyPolicyTest.kt

# A5.4: Step 1 polish and Norwegian clock notation.
grep -Fq 'Gi turen et navn og velg når den starter og slutter.' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'DateTimeFormatter.ofPattern("HH:mm")' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'DateTimeFormatter.ofPattern("HH:mm")' app/src/main/java/app/ferietur/export/PdfExporter.kt
grep -Fq 'DateTimeFormatter.ofPattern("HH:mm")' app/src/main/java/app/ferietur/domain/TripPlanEngine.kt
if grep -Eq 'kl\. (00|06|07|08|17|20|23|24)\.[0-5][0-9]|(00|06|07|08|17|20|23|24)\.[0-5][0-9]–' app/src/main/java/app/ferietur/{ui/FerieturApp.kt,domain/TripPlanEngine.kt,export/PdfExporter.kt}; then
    echo "STOP=A54_DOTTED_CLOCK_TEXT_REMAINS"
    exit 96
fi
echo "A54_STEP1_AND_NORWEGIAN_TIME_SMOKE=PASS"


# A5.4R1: Step 1 uses one Material 3 date range plus explicit 24-hour TimeInput dialogs.
STEP1_BLOCK="$(awk '/private fun TripBasicsScreen\(/{flag=1} /private fun CalculationMethodScreen\(/{flag=0} flag' app/src/main/java/app/ferietur/ui/FerieturApp.kt)"
printf '%s' "$STEP1_BLOCK" | grep -Fq 'SectionTitle("Datoer")'
printf '%s' "$STEP1_BLOCK" | grep -Fq 'TripBasicsDateRangeButton('
printf '%s' "$STEP1_BLOCK" | grep -Fq 'label = "Avreise"'
printf '%s' "$STEP1_BLOCK" | grep -Fq 'label = "Hjemkomst"'
grep -Fq 'DateRangePickerState(' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'locale = norwegian' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'DateRangePicker(' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'showModeToggle = false' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'TimeInput(state = state)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'is24Hour = true' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'modeToggleButton = null' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'internal fun tripDateRangeLabel' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'class Step1PickerPolicyTest' app/src/test/java/app/ferietur/ui/Step1PickerPolicyTest.kt
grep -Fq '11.–18. august 2026' app/src/test/java/app/ferietur/ui/Step1PickerPolicyTest.kt
if printf '%s' "$STEP1_BLOCK" | grep -Eq 'TripBasicsDateButton|TripBasicsTimeButton'; then
    echo "STOP=A54R1_LEGACY_STEP1_PICKERS_REINTRODUCED"
    exit 97
fi
grep -Fq 'A5.4R1 Step 1 Material pickers audit' docs/A54R1_STEP1_MATERIAL_PICKERS_AUDIT.md
echo "A54R1_STEP1_MATERIAL_PICKERS_SMOKE=PASS"


# A5.4R2: Step 1 picker chrome is fully Norwegian even when the device locale is English.
grep -Fq 'private fun NorwegianMaterialLocale' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'Configuration(currentConfiguration).apply { setLocale(norwegian) }' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'tripDateRangePickerHeadline(state.selectedStartDateMillis, state.selectedEndDateMillis)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'selectedDayContainerColor = MaterialTheme.colorScheme.secondary' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)' app/src/main/java/app/ferietur/ui/FerieturApp.kt
grep -Fq 'pickerHeadlineUsesNorwegianCompactRange' app/src/test/java/app/ferietur/ui/Step1PickerPolicyTest.kt
grep -Fq 'A5.4R2 Step 1 Norwegian picker localization audit' docs/A54R2_STEP1_NORWEGIAN_PICKER_LOCALIZATION_AUDIT.md
echo "A54R2_STEP1_NORWEGIAN_PICKER_LOCALIZATION_SMOKE=PASS"
