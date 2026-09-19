package app.ferietur.ui

import android.app.DatePickerDialog as PlatformDatePickerDialog
import android.app.TimePickerDialog as PlatformTimePickerDialog
import android.content.res.Configuration
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.isInputValid
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ferietur.BuildConfig
import app.ferietur.data.TripRepository
import app.ferietur.data.TripStorageIssue
import app.ferietur.data.TripStorageIssueKind
import app.ferietur.domain.CalculationCertainty
import app.ferietur.domain.CalculationEvidence
import app.ferietur.domain.CalculationLine
import app.ferietur.domain.ClockChangePolicy
import app.ferietur.domain.ControlFinding
import app.ferietur.domain.DayCalculationAudit
import app.ferietur.domain.DayCalculationContribution
import app.ferietur.domain.DayProjectedBlock
import app.ferietur.domain.DomainRule
import app.ferietur.domain.EmployerKind
import app.ferietur.domain.FerieturRules
import app.ferietur.domain.FerieturTariffResolver
import app.ferietur.domain.FerieturTariffRuntimeCalculator
import app.ferietur.domain.FinalizedTripSnapshot
import app.ferietur.domain.FinalizedTripSnapshotBuilder
import app.ferietur.domain.SettlementSnapshot
import app.ferietur.domain.FindingSeverity
import app.ferietur.domain.FundingMode
import app.ferietur.domain.HolidayWorkPlanPolicy
import app.ferietur.domain.HolidayWorkPlanRelation
import app.ferietur.domain.HolidayWorkPlanStatus
import app.ferietur.domain.TravelDutyStatus
import app.ferietur.domain.TripWorkPlanBasis
import app.ferietur.domain.NewTripDefaultsFactory
import app.ferietur.domain.OsloSalaryTable2026
import app.ferietur.domain.OsloSalaryTables
import app.ferietur.domain.PayingParty
import app.ferietur.domain.PaymentTreatment
import app.ferietur.domain.PlannedBlock
import app.ferietur.domain.RosterComparisonMode
import app.ferietur.domain.RosterEntryCodec
import app.ferietur.domain.SavedTripDraft
import app.ferietur.domain.ShiftCategory
import app.ferietur.domain.ShiftDefinition
import app.ferietur.domain.SolhaugenShiftCatalog
import app.ferietur.domain.TariffRuntimeCalculation
import app.ferietur.domain.TariffRuntimeCalculationPresentation
import app.ferietur.domain.TariffRuntimeCalculationPresentations
import app.ferietur.domain.TariffRuntimeCalculationResult
import app.ferietur.domain.TariffRuntimeProvenanceSlice
import app.ferietur.domain.TariffSegmentationResult
import app.ferietur.domain.TimeKind
import app.ferietur.domain.TravelNoticeStatus
import app.ferietur.domain.TripDateRangePolicy
import app.ferietur.domain.TripPlanEngine
import app.ferietur.domain.WeeklyBasis
import app.ferietur.domain.WeekendProfile
import app.ferietur.domain.WorkBlock
import app.ferietur.domain.isTravelKind
import app.ferietur.domain.isTravelWithoutResponsibility
import app.ferietur.domain.ruleBasisLabel
import app.ferietur.domain.toFundingMode
import app.ferietur.export.PdfExportRepository
import app.ferietur.export.PdfExporter
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class FlowScreen(val title: String) {
    HOME("Ferietur"),
    TRIP("Turen"),
    METHOD("Lønn og betaling"),
    PAY("Lønnsopplysninger"),
    ROSTER("Grunnturnus"),
    HOLIDAY_PLAN("Arbeidsgivers arbeidsplan"),
    TRAVEL("Reise til og fra"),
    TRIP_PLAN("Arbeid på turen"),
    CALCULATION("Beregning"),
    SETTLEMENT("Betalingsforslag"),
    CONTROL("Kontroll"),
    SUMMARY("Oppsummering og dokumentasjon"),
}

private enum class SettlementMode {
    FULL_CALCULATION,
    CUSTOM_AGREEMENT,
}

private enum class PlanFabAction {
    ACTIVE_WORK,
    TRAVEL,
    RESTING_NIGHT,
    OTHER,
}

private enum class PlanEntryMode {
    HOLIDAY_PLAN,
    ACTUAL_WORK,
}

private data class PlanPeriodEditTarget(
    val date: LocalDate,
    val index: Int,
)

private enum class DraftSaveState {
    SAVED,
    SAVING,
    ERROR,
}

private enum class PdfExportPhase {
    IDLE,
    CREATING_SHORT,
    CREATING_FULL,
    READY,
    ERROR,
}

private data class PdfExportUiState(
    val phase: PdfExportPhase = PdfExportPhase.IDLE,
    val filePath: String? = null,
    val token: Long = 0L,
    val message: String? = null,
)


private data class SaveUiState(
    val state: DraftSaveState,
    val onSave: () -> Unit,
)

private val LocalSaveUi = staticCompositionLocalOf<SaveUiState?> { null }

private val travelKinds = TimeKind.entries.filterTo(linkedSetOf()) { it.isTravelKind() }


internal data class RosterWorkDraft(
    val code: String,
    val start: LocalTime?,
    val end: LocalTime?,
)

internal fun applyRosterTemplateToPrimaryDraft(
    drafts: List<RosterWorkDraft>,
    template: ShiftDefinition,
): List<RosterWorkDraft> {
    val replacement = RosterWorkDraft(template.code, template.start, template.end)
    if (drafts.isEmpty()) return listOf(replacement)
    return drafts.toMutableList().also { it[0] = replacement }
}

internal fun updateRosterWorkDraftAt(
    drafts: List<RosterWorkDraft>,
    index: Int,
    transform: (RosterWorkDraft) -> RosterWorkDraft,
): List<RosterWorkDraft> {
    if (index !in drafts.indices) return drafts
    return drafts.toMutableList().also { current ->
        current[index] = transform(current[index])
    }
}


private data class SettlementSummary(
    val calculatedAmount: BigDecimal,
    val proposedAmount: BigDecimal,
    val usesFullCalculation: Boolean,
    val reason: String,
    val alreadyCoveredByNormalRosterAmount: BigDecimal,
    val fullKnownCalculationAmount: BigDecimal,
    val unresolvedRules: List<DomainRule>,
) {
    val unresolvedRuleCount: Int get() = unresolvedRules.size
}


private class FerieturSessionViewModel(
    private val tripRepository: TripRepository,
    private val pdfExportRepository: PdfExportRepository,
) : ViewModel() {
    private val defaults = NewTripDefaultsFactory.current()
    private var exportTokenCounter = 0L

    val currentTripId = mutableStateOf<String?>(null)
    val screen = mutableStateOf(FlowScreen.HOME)
    val employerKind = mutableStateOf(EmployerKind.UNSPECIFIED)
    val payingParty = mutableStateOf(PayingParty.UNSPECIFIED)
    val rosterComparisonMode = mutableStateOf(RosterComparisonMode.USE_NORMAL_ROSTER)
    val holidayWorkPlanStatus = mutableStateOf(HolidayWorkPlanStatus.NOT_CLARIFIED)
    val workPlanBasis = mutableStateOf(TripWorkPlanBasis.NOT_CLARIFIED)
    val tripTitle = mutableStateOf(defaults.title)
    val startDate = mutableStateOf(defaults.startDate)
    val endDate = mutableStateOf(defaults.endDate)
    val startTime = mutableStateOf(defaults.startTime)
    val endTime = mutableStateOf(defaults.endTime)
    val salaryStep = mutableStateOf(defaults.provisionalSalaryStep)
    val weeklyBasis = mutableStateOf(defaults.provisionalWeeklyBasis)
    val weekendProfile = mutableStateOf(defaults.provisionalWeekendProfile)
    val payslipChecked = mutableStateOf(false)
    val rosterGapConfirmed = mutableStateOf(false)
    val roster = mutableStateOf<Map<LocalDate, String>>(emptyMap())
    val plans = mutableStateOf(
        tripDates(defaults.startDate, defaults.endDate).associateWith { emptyList<PlannedBlock>() },
    )
    val holidayPlans = mutableStateOf(
        tripDates(defaults.startDate, defaults.endDate).associateWith { emptyList<PlannedBlock>() },
    )
    val selectedRosterDate = mutableStateOf<LocalDate?>(null)
    val selectedHolidayPlanPeriod = mutableStateOf<PlanPeriodEditTarget?>(null)
    val pendingHolidayPlanAction = mutableStateOf<PlanFabAction?>(null)
    val pendingHolidayPlanDate = mutableStateOf<LocalDate?>(null)
    val selectedPlanPeriod = mutableStateOf<PlanPeriodEditTarget?>(null)
    val pendingPlanAction = mutableStateOf<PlanFabAction?>(null)
    val pendingPlanDate = mutableStateOf<LocalDate?>(null)
    val outboundArrival = mutableStateOf(LocalDateTime.of(defaults.startDate, defaults.startTime).plusHours(4))
    val returnDeparture = mutableStateOf(LocalDateTime.of(defaults.endDate, defaults.endTime).minusHours(4))
    val outboundTravelKind = mutableStateOf<TimeKind?>(null)
    val returnTravelKind = mutableStateOf<TimeKind?>(null)
    val showHolidayPlanValidation = mutableStateOf(false)
    val showPlanValidation = mutableStateOf(false)
    val settlementMode = mutableStateOf(SettlementMode.FULL_CALCULATION)
    val settlementAmountText = mutableStateOf("")
    val settlementReason = mutableStateOf("")
    val finalizedSnapshot = mutableStateOf<FinalizedTripSnapshot?>(null)
    val finalizationHistory = mutableStateOf<List<FinalizedTripSnapshot>>(emptyList())
    val migrationHistory = mutableStateOf<Set<String>>(emptySet())
    val tripOverviewOpen = mutableStateOf(false)
    val aboutOpen = mutableStateOf(false)
    val directFromOverview = mutableStateOf(false)
    val saveState = mutableStateOf(DraftSaveState.SAVED)
    val lastSavedAt = mutableStateOf<Long?>(null)

    val savedTrips = mutableStateOf<List<SavedTripDraft>>(emptyList())
    val storageIssues = mutableStateOf<List<TripStorageIssue>>(emptyList())
    val libraryLoaded = mutableStateOf(false)
    val exportState = mutableStateOf(PdfExportUiState())

    init {
        refreshLibrary()
    }

    private fun applyLibrary(snapshot: app.ferietur.data.TripLibrarySnapshot) {
        savedTrips.value = snapshot.drafts
        storageIssues.value = snapshot.issues
        libraryLoaded.value = true
    }

    private fun recordRepositoryFailure(error: Throwable) {
        libraryLoaded.value = true
        storageIssues.value = listOf(
            TripStorageIssue(
                kind = TripStorageIssueKind.IO_ERROR,
                draftId = null,
                detail = error.message ?: "Kunne ikke lese eller lagre lokale turdata.",
            ),
        )
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            runCatchingCancellable { tripRepository.loadLibrary() }
                .onSuccess(::applyLibrary)
                .onFailure(::recordRepositoryFailure)
        }
    }

    fun persistDraft(
        draft: SavedTripDraft,
        updateSaveIndicator: Boolean = true,
    ) {
        if (updateSaveIndicator) {
            saveState.value = DraftSaveState.SAVING
        }
        viewModelScope.launch {
            runCatchingCancellable { tripRepository.save(draft) }
                .onSuccess { snapshot ->
                    applyLibrary(snapshot)
                    if (updateSaveIndicator) {
                        lastSavedAt.value = draft.updatedAtEpochMillis
                        saveState.value = DraftSaveState.SAVED
                    }
                }
                .onFailure { error ->
                    recordRepositoryFailure(error)
                    if (updateSaveIndicator) {
                        saveState.value = DraftSaveState.ERROR
                    }
                }
        }
    }

    fun deleteDraft(id: String) {
        viewModelScope.launch {
            runCatchingCancellable { tripRepository.delete(id) }
                .onSuccess(::applyLibrary)
                .onFailure(::recordRepositoryFailure)
        }
    }

    fun duplicateDraft(draft: SavedTripDraft) {
        viewModelScope.launch {
            runCatchingCancellable { tripRepository.save(draft) }
                .onSuccess(::applyLibrary)
                .onFailure(::recordRepositoryFailure)
        }
    }

    fun createPdfExport(
        snapshot: FinalizedTripSnapshot,
        variant: PdfExporter.Variant,
    ) {
        exportState.value = PdfExportUiState(
            phase = if (variant == PdfExporter.Variant.SHORT) {
                PdfExportPhase.CREATING_SHORT
            } else {
                PdfExportPhase.CREATING_FULL
            },
        )
        viewModelScope.launch {
            runCatchingCancellable { pdfExportRepository.create(snapshot, variant) }
                .onSuccess { file ->
                    exportTokenCounter += 1
                    exportState.value = PdfExportUiState(
                        phase = PdfExportPhase.READY,
                        filePath = file.absolutePath,
                        token = exportTokenCounter,
                    )
                }
                .onFailure { error ->
                    exportState.value = PdfExportUiState(
                        phase = PdfExportPhase.ERROR,
                        message = error.message ?: "Kunne ikke lage PDF.",
                    )
                }
        }
    }

    fun consumePdfExport(token: Long) {
        val current = exportState.value
        if (current.phase == PdfExportPhase.READY && current.token == token) {
            exportState.value = PdfExportUiState()
        }
    }

    fun failPdfExportLaunch(token: Long, error: Throwable) {
        val current = exportState.value
        if (current.phase == PdfExportPhase.READY && current.token == token) {
            exportState.value = PdfExportUiState(
                phase = PdfExportPhase.ERROR,
                message = error.message ?: "Kunne ikke åpne deling.",
            )
        }
    }
}

@Composable
fun FerieturApp() {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val tripRepository = remember(appContext) { TripRepository(appContext) }
    val pdfExportRepository = remember(appContext) { PdfExportRepository(appContext) }
    val appInfoScope = rememberCoroutineScope()
    var disclaimerAcknowledgedVersion by remember { mutableStateOf<Int?>(null) }
    var disclaimerWriteInProgress by remember { mutableStateOf(false) }
    var disclaimerConfirmed by rememberSaveable { mutableStateOf(false) }
    var lastAcknowledgedVersionCode by remember { mutableStateOf<Int?>(null) }
    var updatePromptArmed by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var updateAcknowledgementWriteInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(appContext) {
        AppInfoPreferences.disclaimerAcknowledgementVersion(appContext)
            .collectLatest { version -> disclaimerAcknowledgedVersion = version }
    }

    LaunchedEffect(appContext) {
        AppInfoPreferences.lastAcknowledgedVersionCode(appContext)
            .collectLatest { version -> lastAcknowledgedVersionCode = version }
    }

    LaunchedEffect(disclaimerAcknowledgedVersion, lastAcknowledgedVersionCode) {
        val disclaimerVersion = disclaimerAcknowledgedVersion ?: return@LaunchedEffect
        val acknowledgedVersionCode = lastAcknowledgedVersionCode ?: return@LaunchedEffect
        if (updatePromptArmed != null) return@LaunchedEffect

        when (
            AppChangelog.updatePromptAction(
                disclaimerAcknowledgementVersion = disclaimerVersion,
                lastAcknowledgedVersionCode = acknowledgedVersionCode,
                currentVersionCode = BuildConfig.VERSION_CODE,
            )
        ) {
            UpdatePromptAction.NONE -> updatePromptArmed = false
            UpdatePromptAction.SHOW_CHANGELOG -> updatePromptArmed = true
            UpdatePromptAction.ACKNOWLEDGE_SILENTLY -> {
                updatePromptArmed = false
                runCatchingCancellable {
                    AppInfoPreferences.acknowledgeVersionCode(
                        context = appContext,
                        versionCode = BuildConfig.VERSION_CODE,
                    )
                }.onSuccess {
                    lastAcknowledgedVersionCode = BuildConfig.VERSION_CODE
                }
            }
        }
    }

    // Screen-level UI state belongs in an Activity-scoped ViewModel so
    // configuration changes (rotation, fold/unfold, window resize) do not
    // recreate the active trip at HOME. The ViewModel-backed TripRepository remains
    // the durable source used for resume across process/app restarts.
    val session: FerieturSessionViewModel = viewModel {
        FerieturSessionViewModel(
            tripRepository = tripRepository,
            pdfExportRepository = pdfExportRepository,
        )
    }
    var currentTripId by session.currentTripId
    var screen by session.screen
    var employerKind by session.employerKind
    var payingParty by session.payingParty
    var rosterComparisonMode by session.rosterComparisonMode
    var holidayWorkPlanStatus by session.holidayWorkPlanStatus
    var workPlanBasis by session.workPlanBasis
    var tripTitle by session.tripTitle
    var startDate by session.startDate
    var endDate by session.endDate
    var startTime by session.startTime
    var endTime by session.endTime
    var salaryStep by session.salaryStep
    var weeklyBasis by session.weeklyBasis
    var weekendProfile by session.weekendProfile
    var payslipChecked by session.payslipChecked
    var rosterGapConfirmed by session.rosterGapConfirmed
    var roster by session.roster
    var plans by session.plans
    var holidayPlans by session.holidayPlans
    var selectedRosterDate by session.selectedRosterDate
    var selectedHolidayPlanPeriod by session.selectedHolidayPlanPeriod
    var pendingHolidayPlanAction by session.pendingHolidayPlanAction
    var pendingHolidayPlanDate by session.pendingHolidayPlanDate
    var selectedPlanPeriod by session.selectedPlanPeriod
    var pendingPlanAction by session.pendingPlanAction
    var pendingPlanDate by session.pendingPlanDate
    var outboundArrival by session.outboundArrival
    var returnDeparture by session.returnDeparture
    var outboundTravelKind by session.outboundTravelKind
    var returnTravelKind by session.returnTravelKind
    var showHolidayPlanValidation by session.showHolidayPlanValidation
    var showPlanValidation by session.showPlanValidation
    var settlementMode by session.settlementMode
    var settlementAmountText by session.settlementAmountText
    var settlementReason by session.settlementReason
    var finalizedSnapshot by session.finalizedSnapshot
    var finalizationHistory by session.finalizationHistory
    var migrationHistory by session.migrationHistory
    var tripOverviewOpen by session.tripOverviewOpen
    var aboutOpen by session.aboutOpen
    var directFromOverview by session.directFromOverview
    var saveState by session.saveState
    var lastSavedAt by session.lastSavedAt
    val savedTrips by session.savedTrips
    val storageIssues by session.storageIssues
    val libraryLoaded by session.libraryLoaded
    val exportState by session.exportState
    var backupBusy by rememberSaveable { mutableStateOf(false) }
    var backupMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var backupMessageIsError by rememberSaveable { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            backupBusy = true
            backupMessage = null
            appInfoScope.launch {
                runCatchingCancellable {
                    appContext.contentResolver.openOutputStream(uri)?.use { output ->
                        tripRepository.exportBackup(
                            output = output,
                            appVersionName = BuildConfig.VERSION_NAME,
                            appVersionCode = BuildConfig.VERSION_CODE,
                        )
                    } ?: error("Kunne ikke åpne valgt fil for skriving.")
                }.onSuccess { count ->
                    backupMessageIsError = false
                    backupMessage = if (count == 1) {
                        "1 tur er eksportert til lokal sikkerhetskopi."
                    } else {
                        "$count turer er eksportert til lokal sikkerhetskopi."
                    }
                }.onFailure { error ->
                    backupMessageIsError = true
                    backupMessage = error.message ?: "Kunne ikke eksportere sikkerhetskopien."
                }
                backupBusy = false
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            backupBusy = true
            backupMessage = null
            appInfoScope.launch {
                runCatchingCancellable {
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        tripRepository.importBackup(input)
                    } ?: error("Kunne ikke åpne valgt sikkerhetskopi.")
                }.onSuccess { result ->
                    backupMessageIsError = false
                    backupMessage = if (result.importedCount == 1) {
                        "1 tur er gjenopprettet. Eksisterende versjon med samme tur-ID ble sikkerhetskopiert først."
                    } else {
                        "${result.importedCount} turer er gjenopprettet. Eksisterende versjoner med samme tur-ID ble sikkerhetskopiert først."
                    }
                    session.refreshLibrary()
                }.onFailure { error ->
                    backupMessageIsError = true
                    backupMessage = error.message ?: "Kunne ikke importere sikkerhetskopien."
                }
                backupBusy = false
            }
        }
    }

    val fundingMode = rosterComparisonMode.toFundingMode()
    val dates = tripDates(startDate, endDate)
    val rosterHasOverlap = TripPlanEngine.hasRosterOverlap(roster)
    val rosterComplete = dates.all { date -> RosterEntryCodec.decode(roster[date]).isNotEmpty() } && !rosterHasOverlap
    val tripStart = LocalDateTime.of(startDate, startTime)
    val tripEnd = LocalDateTime.of(endDate, endTime)
    val validRange = tripEnd.isAfter(tripStart)
    val chapter20Applicable = TripPlanEngine.chapter20Applies(tripStart, tripEnd)
    val tariffSegmentation = remember(tripStart, tripEnd, validRange) {
        if (validRange) FerieturTariffResolver.planSegments(tripStart, tripEnd) else null
    }
    val salaryRangeSupported = tariffSegmentation is TariffSegmentationResult.Success
    val multipleSalaryContexts = (tariffSegmentation as? TariffSegmentationResult.Success)
        ?.segments
        ?.map { it.salaryTable.id }
        ?.distinct()
        ?.size
        ?.let { it > 1 }
        ?: false
    // The pay screen shows the table effective at trip start. Runtime money is
    // never calculated from this scalar; the tariff runtime gateway resolves
    // and freezes annual salary independently for every effective-date slice.
    val annualSalary = OsloSalaryTables.annualSalaryForDate(salaryStep, startDate) ?: BigDecimal.ZERO
    val effectiveRoster = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) roster else emptyMap<LocalDate, String>()
    val runtimePlans = remember(fundingMode, workPlanBasis, dates, plans, holidayPlans) {
        TripPlanEngine.runtimePlansForWorkPlanBasis(
            fundingMode = fundingMode,
            workPlanBasis = workPlanBasis,
            dates = dates,
            actualPlans = plans,
            holidayPlans = holidayPlans,
        )
    }
    val effectiveHolidayWorkPlanStatus = remember(
        fundingMode,
        workPlanBasis,
        holidayWorkPlanStatus,
    ) {
        TripPlanEngine.calculationHolidayWorkPlanStatusForBasis(
            fundingMode = fundingMode,
            workPlanBasis = workPlanBasis,
            holidayWorkPlanStatus = holidayWorkPlanStatus,
        )
    }
    val rosterGapEvidence = if (rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
        TripPlanEngine.rosterUncoveredEvidence(TripPlanEngine.projectRange(dates, runtimePlans), effectiveRoster, tripStart, tripEnd)
    } else {
        emptyList()
    }
    val rosterGapMinutes = rosterGapEvidence.sumOf { it.minutes }
    val travelPrefill = travelPlans(
        tripStart = tripStart,
        outboundArrival = outboundArrival,
        outboundKind = outboundTravelKind,
        returnDeparture = returnDeparture,
        tripEnd = tripEnd,
        returnKind = returnTravelKind,
    )
    val planIssues = planValidationIssues(
        dates = dates,
        plans = plans,
        tripStart = tripStart,
        tripEnd = tripEnd,
        fundingMode = fundingMode,
        holidayWorkPlanStatus = holidayWorkPlanStatus,
    )
    val holidayPlanIssues = planValidationIssues(
        dates = dates,
        plans = holidayPlans,
        tripStart = tripStart,
        tripEnd = tripEnd,
        fundingMode = fundingMode,
        holidayWorkPlanStatus = holidayWorkPlanStatus,
    )
    val holidayPlanRequired =
        fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL &&
            workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN
    val holidayPlanHasEntries = holidayPlans.values.flatten().isNotEmpty()
    val holidayPlanReady =
        !holidayPlanRequired ||
            (holidayPlanHasEntries && holidayPlanIssues.isEmpty())
    val canSeedHolidayPlanFromRoster =
        holidayPlanRequired &&
            holidayPlans.values.flatten().none { !it.kind.isTravelKind() } &&
            effectiveRoster.values.any { encoded ->
                RosterEntryCodec.decode(encoded).any {
                    it.category != ShiftCategory.OFF && it.start != null && it.end != null
                }
            }
    val travelValid = validRange &&
        outboundTravelKind != null && returnTravelKind != null &&
        outboundArrival.isAfter(tripStart) && !outboundArrival.isAfter(returnDeparture) &&
        returnDeparture.isBefore(tripEnd)

    val tariffRuntimeResult: TariffRuntimeCalculationResult? = remember(
        fundingMode,
        effectiveHolidayWorkPlanStatus,
        dates,
        effectiveRoster,
        runtimePlans,
        salaryStep,
        weeklyBasis,
        weekendProfile,
        tripStart,
        tripEnd,
        chapter20Applicable,
        salaryRangeSupported,
    ) {
        if (validRange && chapter20Applicable && salaryRangeSupported) {
            FerieturTariffRuntimeCalculator.calculate(
                fundingMode = fundingMode,
                holidayWorkPlanStatus = effectiveHolidayWorkPlanStatus,
                dates = dates,
                roster = effectiveRoster,
                plans = runtimePlans,
                salaryStep = salaryStep,
                weeklyBasis = weeklyBasis,
                weekendProfile = weekendProfile,
                tripStart = tripStart,
                tripEnd = tripEnd,
            )
        } else {
            null
        }
    }
    val tariffRuntimeCalculation = (tariffRuntimeResult as? TariffRuntimeCalculationResult.Success)?.calculation
    val tariffRuntimeFailure = tariffRuntimeResult as? TariffRuntimeCalculationResult.Failure
    val tariffRuntimePresentation = remember(
        tariffRuntimeCalculation,
        fundingMode,
        dates,
        effectiveRoster,
        runtimePlans,
        weeklyBasis,
        tripStart,
        tripEnd,
    ) {
        tariffRuntimeCalculation?.let { calculation ->
            TariffRuntimeCalculationPresentations.fromRuntime(
                calculation = calculation,
                fundingMode = fundingMode,
                dates = dates,
                roster = effectiveRoster,
                plans = runtimePlans,
                weeklyBasis = weeklyBasis,
                tripStart = tripStart,
                tripEnd = tripEnd,
            )
        }
    }
    val currentSettlementSummary = tariffRuntimeCalculation?.let { calculation ->
        settlementSummary(
            calculation = calculation,
            mode = settlementMode,
            customAmountText = settlementAmountText,
            reason = settlementReason,
        )
    }

    fun currentDraft(now: Long = System.currentTimeMillis()): SavedTripDraft? {
        val id = currentTripId ?: return null
        return SavedTripDraft(
            id = id,
            updatedAtEpochMillis = now,
            screen = screen.name,
            title = tripTitle,
            employerKind = employerKind,
            payingParty = payingParty,
            rosterComparisonMode = rosterComparisonMode,
            holidayWorkPlanStatus = holidayWorkPlanStatus,
            workPlanBasis = workPlanBasis,
            startDate = startDate,
            endDate = endDate,
            startTime = startTime,
            endTime = endTime,
            salaryStep = salaryStep,
            weeklyBasis = weeklyBasis,
            weekendProfile = weekendProfile,
            payslipChecked = payslipChecked,
            rosterGapConfirmed = rosterGapConfirmed,
            roster = roster,
            plans = plans,
            holidayPlans = holidayPlans,
            outboundArrival = outboundArrival,
            returnDeparture = returnDeparture,
            outboundTravelKind = outboundTravelKind,
            returnTravelKind = returnTravelKind,
            settlementMode = settlementMode.name,
            settlementAmountText = settlementAmountText,
            settlementReason = settlementReason,
            finalizedSnapshot = finalizedSnapshot,
            finalizationHistory = finalizationHistory,
            migrationHistory = migrationHistory,
        )
    }

    fun startNewTrip() {
        val defaults = NewTripDefaultsFactory.current()
        val newStart = defaults.startDate
        val newEnd = defaults.endDate
        val newStartTime = defaults.startTime
        val newEndTime = defaults.endTime
        val newRoster = emptyMap<LocalDate, String>()
        currentTripId = UUID.randomUUID().toString()
        employerKind = EmployerKind.UNSPECIFIED
        payingParty = PayingParty.UNSPECIFIED
        rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER
        holidayWorkPlanStatus = HolidayWorkPlanStatus.NOT_CLARIFIED
        workPlanBasis = TripWorkPlanBasis.NOT_CLARIFIED
        tripTitle = defaults.title
        startDate = newStart
        endDate = newEnd
        startTime = newStartTime
        endTime = newEndTime
        salaryStep = defaults.provisionalSalaryStep
        weeklyBasis = defaults.provisionalWeeklyBasis
        weekendProfile = defaults.provisionalWeekendProfile
        payslipChecked = false
        rosterGapConfirmed = false
        roster = newRoster
        plans = tripDates(newStart, newEnd).associateWith { emptyList<PlannedBlock>() }
        holidayPlans = tripDates(newStart, newEnd).associateWith { emptyList<PlannedBlock>() }
        selectedRosterDate = null
        selectedHolidayPlanPeriod = null
        pendingHolidayPlanAction = null
        pendingHolidayPlanDate = null
        selectedPlanPeriod = null
        pendingPlanAction = null
        pendingPlanDate = null
        outboundArrival = LocalDateTime.of(newStart, newStartTime).plusHours(4)
        returnDeparture = LocalDateTime.of(newEnd, newEndTime).minusHours(4)
        outboundTravelKind = null
        returnTravelKind = null
        showHolidayPlanValidation = false
        showPlanValidation = false
        settlementMode = SettlementMode.FULL_CALCULATION
        settlementAmountText = ""
        settlementReason = ""
        finalizedSnapshot = null
        finalizationHistory = emptyList()
        migrationHistory = emptySet()
        tripOverviewOpen = false
        aboutOpen = false
        directFromOverview = false
        saveState = DraftSaveState.SAVING
        lastSavedAt = null
        screen = FlowScreen.TRIP
    }

    fun resumeTrip(saved: SavedTripDraft) {
        val effectiveSaved = saved
        currentTripId = effectiveSaved.id
        employerKind = effectiveSaved.employerKind
        payingParty = effectiveSaved.payingParty
        rosterComparisonMode = effectiveSaved.rosterComparisonMode
        holidayWorkPlanStatus = effectiveSaved.holidayWorkPlanStatus
        workPlanBasis = effectiveSaved.workPlanBasis
        tripTitle = effectiveSaved.title
        startDate = effectiveSaved.startDate
        endDate = effectiveSaved.endDate
        startTime = effectiveSaved.startTime
        endTime = effectiveSaved.endTime
        salaryStep = effectiveSaved.salaryStep
        weeklyBasis = effectiveSaved.weeklyBasis
        weekendProfile = effectiveSaved.weekendProfile
        val savedTripStart = LocalDateTime.of(effectiveSaved.startDate, effectiveSaved.startTime)
        val savedTripEnd = LocalDateTime.of(effectiveSaved.endDate, effectiveSaved.endTime)
        val unsupportedSalaryRange = !FerieturTariffResolver.supportsSegmentedRange(
            savedTripStart,
            savedTripEnd,
        )
        payslipChecked = effectiveSaved.payslipChecked && !unsupportedSalaryRange
        rosterGapConfirmed = effectiveSaved.rosterGapConfirmed
        roster = effectiveSaved.roster
        plans = effectiveSaved.plans
        holidayPlans = effectiveSaved.holidayPlans
        selectedRosterDate = null
        selectedHolidayPlanPeriod = null
        pendingHolidayPlanAction = null
        pendingHolidayPlanDate = null
        selectedPlanPeriod = null
        pendingPlanAction = null
        pendingPlanDate = null
        outboundArrival = effectiveSaved.outboundArrival
        returnDeparture = effectiveSaved.returnDeparture
        outboundTravelKind = effectiveSaved.outboundTravelKind
        returnTravelKind = effectiveSaved.returnTravelKind
        showHolidayPlanValidation = false
        showPlanValidation = false
        settlementMode = runCatching { SettlementMode.valueOf(effectiveSaved.settlementMode) }.getOrDefault(SettlementMode.FULL_CALCULATION)
        settlementAmountText = effectiveSaved.settlementAmountText
        settlementReason = effectiveSaved.settlementReason
        finalizedSnapshot = effectiveSaved.finalizedSnapshot
        finalizationHistory = effectiveSaved.finalizationHistory
        migrationHistory = effectiveSaved.migrationHistory
        val restoredRawScreen = FlowScreen.entries.firstOrNull { it.name == effectiveSaved.screen } ?: FlowScreen.TRIP
        val unsupportedDayTrip = TripPlanEngine.isDayTrip(savedTripStart, savedTripEnd)
        val restoredScreen = when {
            unsupportedDayTrip || unsupportedSalaryRange -> FlowScreen.TRIP
            restoredRawScreen == FlowScreen.TRAVEL -> FlowScreen.TRIP_PLAN
            else -> restoredRawScreen
        }
        lastSavedAt = effectiveSaved.updatedAtEpochMillis
        saveState = DraftSaveState.SAVED
        directFromOverview = false
        val restoredFundingMode = effectiveSaved.rosterComparisonMode.toFundingMode()
        val needsWorkPlanBasisReview =
            restoredFundingMode == FundingMode.TURNUS_PLUS_EXTERNAL &&
                effectiveSaved.workPlanBasis == TripWorkPlanBasis.NOT_CLARIFIED &&
                restoredScreen in setOf(
                    FlowScreen.PAY,
                    FlowScreen.ROSTER,
                    FlowScreen.HOLIDAY_PLAN,
                    FlowScreen.TRAVEL,
                    FlowScreen.TRIP_PLAN,
                    FlowScreen.CALCULATION,
                    FlowScreen.SETTLEMENT,
                    FlowScreen.CONTROL,
                    FlowScreen.SUMMARY,
                )
        val needsEmployerPlanReview =
            restoredFundingMode == FundingMode.TURNUS_PLUS_EXTERNAL &&
                effectiveSaved.workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN &&
                holidayPlans.values.flatten().isEmpty() &&
                restoredScreen in setOf(
                    FlowScreen.TRAVEL,
                    FlowScreen.TRIP_PLAN,
                    FlowScreen.CALCULATION,
                    FlowScreen.SETTLEMENT,
                    FlowScreen.CONTROL,
                    FlowScreen.SUMMARY,
                )
        screen = when {
            needsWorkPlanBasisReview -> FlowScreen.METHOD
            needsEmployerPlanReview -> FlowScreen.HOLIDAY_PLAN
            restoredScreen == FlowScreen.SUMMARY && finalizedSnapshot == null -> FlowScreen.CONTROL
            else -> restoredScreen
        }
        tripOverviewOpen = !unsupportedDayTrip && !unsupportedSalaryRange
    }

    fun saveNow() {
        currentDraft()?.let(session::persistDraft)
    }

    fun archiveCurrentFinalizationForEdit() {
        val current = finalizedSnapshot ?: return
        if (finalizationHistory.none { it.id == current.id }) {
            finalizationHistory = finalizationHistory + current
        }
        finalizedSnapshot = null
    }

    fun openFromOverview(target: FlowScreen) {
        if (target == FlowScreen.SUMMARY && finalizedSnapshot == null) {
            directFromOverview = true
            screen = FlowScreen.CONTROL
            tripOverviewOpen = false
            return
        }
        if (
            target in setOf(
                FlowScreen.TRIP,
                FlowScreen.METHOD,
                FlowScreen.PAY,
                FlowScreen.ROSTER,
                FlowScreen.TRAVEL,
                FlowScreen.TRIP_PLAN,
                FlowScreen.SETTLEMENT,
                FlowScreen.CONTROL,
            )
        ) {
            archiveCurrentFinalizationForEdit()
        }
        directFromOverview = true
        screen = target
        tripOverviewOpen = false
    }

    fun continueFromOverview() {
        directFromOverview = false
        tripOverviewOpen = false
    }

    fun closeTripOverview() {
        tripOverviewOpen = false
        directFromOverview = false
        screen = FlowScreen.HOME
    }

    fun deleteTrip(saved: SavedTripDraft) {
        session.deleteDraft(saved.id)
        if (currentTripId == saved.id) currentTripId = null
    }

    fun duplicateTrip(saved: SavedTripDraft) {
        val duplicate = copiedTripDraft(
            source = saved,
            newId = UUID.randomUUID().toString(),
            now = System.currentTimeMillis(),
        )
        session.duplicateDraft(duplicate)
    }

    fun resetRange(newStart: LocalDate, newEnd: LocalDate) {
        val safeEnd = if (newEnd.isBefore(newStart)) newStart else newEnd
        val oldRoster = roster
        val oldPlans = plans
        val oldHolidayPlans = holidayPlans
        startDate = newStart
        endDate = safeEnd
        val newDates = tripDates(newStart, safeEnd)
        val newRoster = newDates.mapNotNull { date -> oldRoster[date]?.let { date to it } }.toMap()
        roster = newRoster
        plans = TripPlanEngine.preservePlanForDateRange(
            dates = newDates,
            roster = newRoster,
            currentPlans = oldPlans,
            comparisonMode = rosterComparisonMode,
        )
        holidayPlans = TripPlanEngine.preservePlanForDateRange(
            dates = newDates,
            roster = newRoster,
            currentPlans = oldHolidayPlans,
            comparisonMode = rosterComparisonMode,
        )
        val newTripStart = LocalDateTime.of(newStart, startTime)
        val newTripEnd = LocalDateTime.of(safeEnd, endTime)
        outboundArrival = newTripStart.plusHours(4).coerceAtMost(newTripEnd.minusMinutes(1))
        returnDeparture = newTripEnd.minusHours(4).coerceAtLeast(outboundArrival)
        outboundTravelKind = null
        returnTravelKind = null
        showHolidayPlanValidation = false
        showPlanValidation = false
        rosterGapConfirmed = false
    }

    LaunchedEffect(
        currentTripId, screen, employerKind, payingParty, rosterComparisonMode, holidayWorkPlanStatus, workPlanBasis, tripTitle, startDate, endDate, startTime, endTime,
        salaryStep, weeklyBasis, weekendProfile, payslipChecked, rosterGapConfirmed, roster, plans, holidayPlans, outboundArrival,
        returnDeparture, outboundTravelKind, returnTravelKind, settlementMode, settlementAmountText,
        settlementReason, finalizedSnapshot, finalizationHistory, migrationHistory,
    ) {
        if (currentTripId != null && screen != FlowScreen.HOME) {
            saveState = DraftSaveState.SAVING
            delay(250)
            val draft = currentDraft()
            if (draft != null) {
                session.persistDraft(draft)
            }
        }
    }

    val runtimeCalculationReady = tariffRuntimeCalculation != null
    val runtimeControlReady = tariffRuntimePresentation?.sharedControlRateSet != null
    val nextEnabled = when (screen) {
        FlowScreen.HOME -> true
        FlowScreen.TRIP -> validRange && chapter20Applicable && salaryRangeSupported && tripTitle.isNotBlank()
        FlowScreen.METHOD -> when {
            rosterComparisonMode == RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER -> true
            employerKind != EmployerKind.OSLO_KOMMUNE -> false
            else -> workPlanBasis != TripWorkPlanBasis.NOT_CLARIFIED
        }
        FlowScreen.PAY -> payslipChecked
        FlowScreen.ROSTER -> rosterComplete
        FlowScreen.HOLIDAY_PLAN -> true
        FlowScreen.TRAVEL -> travelValid
        FlowScreen.TRIP_PLAN -> true
        FlowScreen.CALCULATION -> runtimeCalculationReady
        FlowScreen.SETTLEMENT -> runtimeCalculationReady && (
            settlementMode == SettlementMode.FULL_CALCULATION ||
                (settlementAmountText.toNorwegianMoneyOrNull() != null && settlementReason.isNotBlank())
            )
        FlowScreen.CONTROL -> runtimeCalculationReady && runtimeControlReady &&
            (rosterGapMinutes == 0L || rosterGapConfirmed) &&
            when {
                fundingMode != FundingMode.TURNUS_PLUS_EXTERNAL -> true
                workPlanBasis == TripWorkPlanBasis.NORMAL_ROSTER_APPLIES -> true
                workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN ->
                    holidayPlanReady &&
                        HolidayWorkPlanPolicy.canFinalize(employerKind, holidayWorkPlanStatus)
                else -> false
            }
        FlowScreen.SUMMARY -> true
    }

    fun goBack() {
        if (directFromOverview) {
            directFromOverview = false
            tripOverviewOpen = true
        } else {
            if (screen == FlowScreen.SUMMARY) {
                archiveCurrentFinalizationForEdit()
            }
            screen = previousScreen(screen, fundingMode, workPlanBasis)
        }
    }

    fun goNext() {
        if (directFromOverview) directFromOverview = false
        if (screen == FlowScreen.TRAVEL) {
            plans = TripPlanEngine.overlayTravelOnPlan(dates, plans, travelPrefill)
            rosterGapConfirmed = false
            showPlanValidation = false
        }
        if (screen == FlowScreen.HOLIDAY_PLAN) {
            if (!holidayPlanReady) {
                showHolidayPlanValidation = true
                return
            }
            showHolidayPlanValidation = false
            if (plans.values.flatten().none { !it.kind.isTravelKind() }) {
                plans = TripPlanEngine.seedActualWorkFromHolidayPlan(
                    dates = dates,
                    holidayPlans = holidayPlans,
                    existingActualPlans = plans,
                )
            }
        }
        if (screen == FlowScreen.TRIP_PLAN) {
            if (planIssues.isNotEmpty()) {
                showPlanValidation = true
                return
            }
            showPlanValidation = false
        }
        if (screen == FlowScreen.CONTROL) {
            if (
                fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL &&
                workPlanBasis == TripWorkPlanBasis.NOT_CLARIFIED
            ) {
                screen = FlowScreen.METHOD
                return
            }
            if (
                workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN &&
                !holidayPlanReady
            ) {
                showHolidayPlanValidation = true
                screen = FlowScreen.HOLIDAY_PLAN
                return
            }
            if (
                workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN &&
                !HolidayWorkPlanPolicy.canFinalize(employerKind, holidayWorkPlanStatus)
            ) {
                screen = FlowScreen.METHOD
                return
            }
            if (rosterGapMinutes > 0L && !rosterGapConfirmed) return
            if (planIssues.isNotEmpty()) {
                showPlanValidation = true
                screen = FlowScreen.TRIP_PLAN
                return
            }
            val runtimeCalculation = tariffRuntimeCalculation ?: return
            val summary = currentSettlementSummary ?: return
            if (tariffRuntimePresentation?.sharedControlRateSet == null) return
            finalizedSnapshot = FinalizedTripSnapshotBuilder.buildFromRuntime(
                title = tripTitle,
                employerKind = employerKind,
                payingParty = payingParty,
                rosterComparisonMode = rosterComparisonMode,
                holidayWorkPlanStatus = holidayWorkPlanStatus,
                workPlanBasis = workPlanBasis,
                holidayPlans = holidayPlans,
                dates = dates,
                roster = effectiveRoster,
                plans = runtimePlans,
                salaryStep = salaryStep,
                weeklyBasis = weeklyBasis,
                weekendProfile = weekendProfile,
                payslipChecked = payslipChecked,
                rosterGapConfirmed = rosterGapConfirmed,
                tripStart = tripStart,
                tripEnd = tripEnd,
                runtimeCalculation = runtimeCalculation,
                settlement = SettlementSnapshot(
                    calculatedAmount = summary.calculatedAmount,
                    proposedAmount = summary.proposedAmount,
                    usesFullCalculation = summary.usesFullCalculation,
                    reason = summary.reason,
                ),
                snapshotId = UUID.randomUUID().toString(),
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
            )
            screen = FlowScreen.SUMMARY
            currentDraft()?.let(session::persistDraft)
            return
        }
        screen = if (screen == FlowScreen.SUMMARY) FlowScreen.HOME else nextScreen(screen, fundingMode, workPlanBasis)
    }

    // Android system back / edge-swipe must follow the same in-app hierarchy as
    // the visible Back controls. Nested Material/Compose BackHandlers still
    // take precedence when a sheet/menu owns the current back action.
    BackHandler(enabled = aboutOpen || tripOverviewOpen || screen != FlowScreen.HOME) {
        when {
            aboutOpen -> aboutOpen = false
            tripOverviewOpen -> closeTripOverview()
            else -> goBack()
        }
    }

    val shouldShowDisclaimer = disclaimerAcknowledgedVersion
        ?.let { it < AppInfoPreferences.CURRENT_DISCLAIMER_VERSION }
        ?: false
    val pendingReleaseNotes = lastAcknowledgedVersionCode
        ?.let { acknowledged ->
            AppChangelog.releasesAfter(
                lastAcknowledgedVersionCode = acknowledged,
                currentVersionCode = BuildConfig.VERSION_CODE,
            )
        }
        .orEmpty()
    val shouldShowWhatsNew =
        updatePromptArmed == true &&
            pendingReleaseNotes.isNotEmpty() &&
            !shouldShowDisclaimer

    if (shouldShowDisclaimer) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(APP_DISCLAIMER_TITLE) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(APP_DISCLAIMER_TEXT)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = disclaimerConfirmed,
                                enabled = !disclaimerWriteInProgress,
                                role = Role.Checkbox,
                                onValueChange = { disclaimerConfirmed = it },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Checkbox(
                            checked = disclaimerConfirmed,
                            onCheckedChange = null,
                            enabled = !disclaimerWriteInProgress,
                        )
                        Text(
                            "Jeg har lest og forstått",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = disclaimerConfirmed && !disclaimerWriteInProgress,
                    onClick = {
                        disclaimerWriteInProgress = true
                        appInfoScope.launch {
                            runCatchingCancellable {
                                AppInfoPreferences.acknowledgeCurrentDisclaimer(appContext)
                            }.onSuccess {
                                disclaimerAcknowledgedVersion =
                                    AppInfoPreferences.CURRENT_DISCLAIMER_VERSION
                            }
                            disclaimerWriteInProgress = false
                        }
                    },
                ) {
                    Text(if (disclaimerWriteInProgress) "Lagrer…" else "Fortsett")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        )
    }

    fun acknowledgeWhatsNew() {
        if (updateAcknowledgementWriteInProgress) return
        updateAcknowledgementWriteInProgress = true
        appInfoScope.launch {
            runCatchingCancellable {
                AppInfoPreferences.acknowledgeVersionCode(
                    context = appContext,
                    versionCode = BuildConfig.VERSION_CODE,
                )
            }.onSuccess {
                lastAcknowledgedVersionCode = BuildConfig.VERSION_CODE
                updatePromptArmed = false
            }
            updateAcknowledgementWriteInProgress = false
        }
    }

    val saveUi = if (currentTripId != null && screen != FlowScreen.HOME) SaveUiState(saveState, ::saveNow) else null
    CompositionLocalProvider(LocalSaveUi provides saveUi) {
        Scaffold(
            bottomBar = {
                if (
                    screen != FlowScreen.HOME &&
                    !tripOverviewOpen &&
                    !aboutOpen &&
                    !shouldShowWhatsNew
                ) {
                    FlowBottomBar(
                        nextLabel = nextButtonLabel(screen, fundingMode, workPlanBasis),
                        nextEnabled = nextEnabled,
                        onBack = ::goBack,
                        onNext = ::goNext,
                    )
                }
            },
            floatingActionButton = {
                if (
                    (screen == FlowScreen.HOLIDAY_PLAN || screen == FlowScreen.TRIP_PLAN) &&
                    !tripOverviewOpen &&
                    !aboutOpen &&
                    !shouldShowWhatsNew
                ) {
                    PlanFabMenu(
                        onAction = { action ->
                            if (screen == FlowScreen.HOLIDAY_PLAN) {
                                pendingHolidayPlanDate = null
                                pendingHolidayPlanAction = action
                            } else {
                                pendingPlanDate = null
                                pendingPlanAction = action
                            }
                        },
                    )
                }
            },
        ) { padding ->
            if (shouldShowWhatsNew) {
                WhatsNewScreen(
                    padding = padding,
                    releases = pendingReleaseNotes,
                    mode = WhatsNewMode.POST_UPDATE,
                    writeInProgress = updateAcknowledgementWriteInProgress,
                    onDone = ::acknowledgeWhatsNew,
                )
            } else if (aboutOpen) {
                AboutFerieturScreen(
                    padding = padding,
                    onBack = { aboutOpen = false },
                )
            } else if (tripOverviewOpen) {
                TripOverviewScreen(
                    padding = padding,
                    title = tripTitle,
                    startDate = startDate,
                    endDate = endDate,
                    lastStep = screen,
                    lastSavedAt = lastSavedAt,
                    employerKind = employerKind,
                    payingParty = payingParty,
                    fundingMode = fundingMode,
                    workPlanBasis = workPlanBasis,
                    dates = dates,
                    plans = plans,
                    roster = effectiveRoster,
                    settlementSummary = currentSettlementSummary,
                    controlRateSet = tariffRuntimePresentation?.sharedControlRateSet,
                    runtimeFailureDetail = tariffRuntimeFailure?.detail,
                    canExport = finalizedSnapshot != null,
                    onBack = ::closeTripOverview,
                    onContinue = ::continueFromOverview,
                    onOpen = ::openFromOverview,
                )
            } else when (screen) {
                FlowScreen.HOME -> HomeScreen(
                    padding = padding,
                    savedTrips = savedTrips,
                    storageIssues = storageIssues,
                    libraryLoaded = libraryLoaded,
                    onStart = ::startNewTrip,
                    onResume = ::resumeTrip,
                    onDuplicate = ::duplicateTrip,
                    onDelete = ::deleteTrip,
                    backupBusy = backupBusy,
                    backupMessage = backupMessage,
                    backupMessageIsError = backupMessageIsError,
                    onExportBackup = {
                        backupMessage = null
                        exportBackupLauncher.launch("ferietur-backup-${LocalDate.now()}.ferietur")
                    },
                    onImportBackup = {
                        backupMessage = null
                        importBackupLauncher.launch(arrayOf("*/*"))
                    },
                    onAbout = { aboutOpen = true },
                )
            FlowScreen.TRIP -> TripBasicsScreen(
                padding = padding,
                stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                onBack = ::goBack,
                tripTitle = tripTitle,
                onTripTitle = { tripTitle = it },
                startDate = startDate,
                endDate = endDate,
                startTime = startTime,
                endTime = endTime,
                onStartDate = { newDate -> resetRange(newDate, if (endDate.isBefore(newDate)) newDate.plusDays(6) else endDate) },
                onEndDate = { newDate -> resetRange(startDate, newDate) },
                onStartTime = { value ->
                    startTime = value
                    rosterGapConfirmed = false
                    val newStart = LocalDateTime.of(startDate, value)
                    if (!outboundArrival.isAfter(newStart)) outboundArrival = newStart.plusHours(4)
                },
                onEndTime = { value ->
                    endTime = value
                    rosterGapConfirmed = false
                    val newEnd = LocalDateTime.of(endDate, value)
                    if (!returnDeparture.isBefore(newEnd)) returnDeparture = newEnd.minusHours(4)
                },
                validRange = validRange,
                chapter20Applicable = chapter20Applicable,
                salaryRangeSupported = salaryRangeSupported,
                earliestSalaryDate = OsloSalaryTables.earliestSupportedDate,
                latestSalaryDate = OsloSalaryTables.latestSupportedDate,
            )
            FlowScreen.METHOD -> CalculationMethodScreen(
                padding = padding,
                stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                onBack = ::goBack,
                employerKind = employerKind,
                onEmployerKind = { employerKind = it },
                payingParty = payingParty,
                onPayingParty = { payingParty = it },
                rosterComparisonMode = rosterComparisonMode,
                workPlanBasis = workPlanBasis,
                onWorkPlanBasis = { value ->
                    if (workPlanBasis != value) {
                        workPlanBasis = value
                        holidayWorkPlanStatus = HolidayWorkPlanStatus.NOT_CLARIFIED
                        showHolidayPlanValidation = false
                    }
                },
                holidayWorkPlanStatus = holidayWorkPlanStatus,
                onHolidayWorkPlanStatus = { holidayWorkPlanStatus = it },
                onRosterComparisonMode = { mode ->
                    if (mode != rosterComparisonMode) {
                        rosterComparisonMode = mode
                        rosterGapConfirmed = false
                        showPlanValidation = false
                    }
                },
            )
            FlowScreen.PAY -> PayBasisScreen(
                padding = padding,
                stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                onBack = ::goBack,
                salaryStep = salaryStep,
                onSalaryStep = {
                    salaryStep = it.coerceIn(OsloSalaryTable2026.minStep, OsloSalaryTable2026.maxStep)
                    payslipChecked = false
                },
                annualSalary = annualSalary,
                multipleSalaryContexts = multipleSalaryContexts,
                weeklyBasis = weeklyBasis,
                onWeeklyBasis = {
                    weeklyBasis = it
                    payslipChecked = false
                },
                weekendProfile = weekendProfile,
                onWeekendProfile = {
                    weekendProfile = it
                    payslipChecked = false
                },
                payslipChecked = payslipChecked,
                onPayslipChecked = { payslipChecked = it },
            )
            FlowScreen.ROSTER -> RosterScreen(
                padding = padding,
                stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                onBack = ::goBack,
                dates = dates,
                roster = roster,
                hasOverlap = rosterHasOverlap,
                onPick = { selectedRosterDate = it },
            )
            FlowScreen.TRAVEL -> TravelScreen(
                padding = padding,
                stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                onBack = ::goBack,
                tripStart = tripStart,
                outboundArrival = outboundArrival,
                onOutboundArrival = { outboundArrival = it },
                outboundKind = outboundTravelKind,
                onOutboundKind = { outboundTravelKind = it },
                returnDeparture = returnDeparture,
                onReturnDeparture = { returnDeparture = it },
                tripEnd = tripEnd,
                returnKind = returnTravelKind,
                onReturnKind = { returnTravelKind = it },
                valid = travelValid,
            )
            FlowScreen.HOLIDAY_PLAN -> HolidayPlanScreen(
                padding = padding,
                stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                onBack = ::goBack,
                dates = dates,
                roster = effectiveRoster,
                plans = holidayPlans,
                required = holidayPlanRequired,
                validationIssues = if (showHolidayPlanValidation) {
                    holidayPlanIssues
                } else {
                    emptyMap<LocalDate, List<PlanValidationIssue>>()
                },
                canSeedPlanFromRoster = canSeedHolidayPlanFromRoster,
                onSeedPlanFromRoster = {
                    holidayPlans = TripPlanEngine.seedPlanFromRosterForEditing(
                        dates = dates,
                        roster = effectiveRoster,
                        existingPlans = holidayPlans,
                        tripStart = tripStart,
                        tripEnd = tripEnd,
                    )
                    showHolidayPlanValidation = false
                },
                onEditPeriod = { date, index ->
                    selectedHolidayPlanPeriod = PlanPeriodEditTarget(date, index)
                },
                onAddPeriod = { date ->
                    pendingHolidayPlanDate = date
                    pendingHolidayPlanAction = PlanFabAction.OTHER
                },
            )
            FlowScreen.TRIP_PLAN -> TripPlanScreen(
                padding = padding,
                stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                onBack = ::goBack,
                fundingMode = fundingMode,
                workPlanBasis = workPlanBasis,
                dates = dates,
                roster = effectiveRoster,
                plans = plans,
                baselinePlans = holidayPlans,
                derivedPlans = if (
                    workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN &&
                    holidayPlanHasEntries
                ) {
                    runtimePlans
                } else {
                    null
                },
                validationIssues = if (showPlanValidation) {
                    planIssues
                } else {
                    emptyMap<LocalDate, List<PlanValidationIssue>>()
                },
                onEditPeriod = { date, index -> selectedPlanPeriod = PlanPeriodEditTarget(date, index) },
                onAddPeriod = { date ->
                    pendingPlanDate = date
                    pendingPlanAction = PlanFabAction.OTHER
                },
            )
            FlowScreen.CALCULATION -> CalculationScreen(
                padding = padding,
                stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                onBack = ::goBack,
                employerKind = employerKind,
                fundingMode = fundingMode,
                result = tariffRuntimePresentation,
                runtimeFailureDetail = tariffRuntimeFailure?.detail,
                weeklyBasis = weeklyBasis,
            )
            FlowScreen.SETTLEMENT -> SettlementScreen(
                padding = padding,
                stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                onBack = ::goBack,
                employerKind = employerKind,
                fundingMode = fundingMode,
                result = tariffRuntimePresentation,
                runtimeFailureDetail = tariffRuntimeFailure?.detail,
                settlementMode = settlementMode,
                onSettlementMode = { settlementMode = it },
                customAmountText = settlementAmountText,
                onCustomAmountText = { settlementAmountText = it },
                reason = settlementReason,
                onReason = { settlementReason = it },
            )
            FlowScreen.CONTROL -> ControlScreen(
                padding = padding,
                stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                onBack = ::goBack,
                employerKind = employerKind,
                dates = dates,
                plans = runtimePlans,
                roster = effectiveRoster,
                rosterGapEvidence = rosterGapEvidence,
                rosterGapConfirmed = rosterGapConfirmed,
                onRosterGapConfirmed = { rosterGapConfirmed = it },
                settlementSummary = currentSettlementSummary,
                controlRateSet = tariffRuntimePresentation?.sharedControlRateSet,
                runtimeFailureDetail = tariffRuntimeFailure?.detail,
            )
                FlowScreen.SUMMARY -> FinalSummaryScreen(
                    padding = padding,
                    stepLabel = screenStepLabel(screen, fundingMode, workPlanBasis),
                    onBack = ::goBack,
                    snapshot = finalizedSnapshot,
                    exportState = exportState,
                    onExport = session::createPdfExport,
                    onExportConsumed = session::consumePdfExport,
                    onExportLaunchFailed = session::failPdfExportLaunch,
                )
            }
        }
    }

    selectedRosterDate?.let { date ->
        val templates = roster.values
            .flatMap(RosterEntryCodec::decode)
            .filter {
                it.category != ShiftCategory.OFF &&
                    it.start != null &&
                    it.end != null &&
                    it.code.length <= RosterEntryCodec.MAX_CODE_LENGTH
            }
            .distinctBy { Triple(it.code, it.start, it.end) }
        RosterDayEditorSheet(
            date = date,
            currentValue = roster[date],
            templates = templates,
            onDismiss = { selectedRosterDate = null },
            onSave = { encoded ->
                roster = roster + (date to encoded)
                rosterGapConfirmed = false
                selectedRosterDate = null
                showPlanValidation = false
            },
            onClear = {
                roster = roster - date
                rosterGapConfirmed = false
                selectedRosterDate = null
                showPlanValidation = false
            },
        )
    }

    pendingHolidayPlanAction?.let { action ->
        val suggestedDate = pendingHolidayPlanDate
            ?: dates.firstOrNull { holidayPlans[it].orEmpty().isEmpty() }
            ?: dates.firstOrNull()
        if (suggestedDate != null) {
            val initialKind = when (action) {
                PlanFabAction.ACTIVE_WORK -> TimeKind.ACTIVE_WORK
                PlanFabAction.TRAVEL -> TimeKind.TRAVEL_UNCERTAIN
                PlanFabAction.RESTING_NIGHT -> TimeKind.RESTING_NIGHT_WATCH
                PlanFabAction.OTHER -> null
            }
            PlanPeriodEditorSheet(
                title = "Legg til periode",
                entryMode = PlanEntryMode.HOLIDAY_PLAN,
                fundingMode = fundingMode,
                initialDate = suggestedDate,
                initialBlock = null,
                initialKind = initialKind,
                dates = dates,
                plans = holidayPlans,
                tripStart = tripStart,
                tripEnd = tripEnd,
                onDismiss = {
                    pendingHolidayPlanAction = null
                    pendingHolidayPlanDate = null
                },
                onSave = { date, block ->
                    val updated = TripPlanEngine.normalizeTravelClassification(
                        date,
                        holidayPlans[date].orEmpty() + normalizeTravelSleepKind(date, block),
                    )
                    holidayPlans = holidayPlans + (date to updated)
                    showHolidayPlanValidation = false
                    pendingHolidayPlanAction = null
                    pendingHolidayPlanDate = null
                },
            )
        }
    }

    selectedHolidayPlanPeriod?.let { target ->
        val existing = holidayPlans[target.date].orEmpty().getOrNull(target.index)
        if (existing != null) {
            PlanPeriodEditorSheet(
                title = "Rediger periode",
                entryMode = PlanEntryMode.HOLIDAY_PLAN,
                fundingMode = fundingMode,
                initialDate = target.date,
                initialBlock = existing,
                initialKind = existing.kind,
                dates = dates,
                plans = holidayPlans,
                tripStart = tripStart,
                tripEnd = tripEnd,
                onDismiss = { selectedHolidayPlanPeriod = null },
                onSave = { targetDate, updatedBlock ->
                    if (targetDate == target.date) {
                        val source = holidayPlans[target.date].orEmpty().toMutableList()
                        if (target.index in source.indices) {
                            source[target.index] = normalizeTravelSleepKind(targetDate, updatedBlock)
                        }
                        holidayPlans = holidayPlans + (
                            targetDate to TripPlanEngine.normalizeTravelClassification(targetDate, source)
                        )
                    } else {
                        val source = holidayPlans[target.date].orEmpty().toMutableList()
                        if (target.index in source.indices) source.removeAt(target.index)
                        val normalizedSource =
                            TripPlanEngine.normalizeTravelClassification(target.date, source)
                        val destination =
                            holidayPlans[targetDate].orEmpty() +
                                normalizeTravelSleepKind(targetDate, updatedBlock)
                        val normalizedDestination =
                            TripPlanEngine.normalizeTravelClassification(targetDate, destination)
                        holidayPlans =
                            holidayPlans +
                                (target.date to normalizedSource) +
                                (targetDate to normalizedDestination)
                    }
                    showHolidayPlanValidation = false
                    selectedHolidayPlanPeriod = null
                },
                onDelete = {
                    val source = holidayPlans[target.date].orEmpty().toMutableList()
                    if (target.index in source.indices) source.removeAt(target.index)
                    holidayPlans = holidayPlans + (
                        target.date to TripPlanEngine.normalizeTravelClassification(target.date, source)
                    )
                    showHolidayPlanValidation = false
                    selectedHolidayPlanPeriod = null
                },
            )
        }
    }

    pendingPlanAction?.let { action ->
        val suggestedDate = pendingPlanDate
            ?: dates.firstOrNull { plans[it].orEmpty().isEmpty() }
            ?: dates.firstOrNull()
        if (suggestedDate != null) {
            val initialKind = when (action) {
                PlanFabAction.ACTIVE_WORK -> TimeKind.ACTIVE_WORK
                PlanFabAction.TRAVEL -> TimeKind.TRAVEL_UNCERTAIN
                PlanFabAction.RESTING_NIGHT -> TimeKind.RESTING_NIGHT_WATCH
                PlanFabAction.OTHER -> null
            }
            PlanPeriodEditorSheet(
                title = "Legg til periode",
                entryMode = PlanEntryMode.ACTUAL_WORK,
                fundingMode = fundingMode,
                initialDate = suggestedDate,
                initialBlock = null,
                initialKind = initialKind,
                dates = dates,
                plans = plans,
                tripStart = tripStart,
                tripEnd = tripEnd,
                onDismiss = {
                    pendingPlanAction = null
                    pendingPlanDate = null
                },
                onSave = { date, block ->
                    val updated = TripPlanEngine.normalizeTravelClassification(
                        date,
                        plans[date].orEmpty() + normalizeTravelSleepKind(date, block),
                    )
                    plans = plans + (date to updated)
                    rosterGapConfirmed = false
                    showPlanValidation = false
                    pendingPlanAction = null
                    pendingPlanDate = null
                },
            )
        }
    }

    selectedPlanPeriod?.let { target ->
        val existing = plans[target.date].orEmpty().getOrNull(target.index)
        if (existing != null) {
            PlanPeriodEditorSheet(
                title = "Rediger periode",
                entryMode = PlanEntryMode.ACTUAL_WORK,
                fundingMode = fundingMode,
                initialDate = target.date,
                initialBlock = existing,
                initialKind = existing.kind,
                dates = dates,
                plans = plans,
                tripStart = tripStart,
                tripEnd = tripEnd,
                onDismiss = { selectedPlanPeriod = null },
                onSave = { targetDate, updatedBlock ->
                    if (targetDate == target.date) {
                        val source = plans[target.date].orEmpty().toMutableList()
                        if (target.index in source.indices) {
                            source[target.index] = normalizeTravelSleepKind(targetDate, updatedBlock)
                        }
                        plans = plans + (
                            targetDate to TripPlanEngine.normalizeTravelClassification(targetDate, source)
                        )
                    } else {
                        val source = plans[target.date].orEmpty().toMutableList()
                        if (target.index in source.indices) source.removeAt(target.index)
                        val normalizedSource = TripPlanEngine.normalizeTravelClassification(target.date, source)
                        val destination = plans[targetDate].orEmpty() + normalizeTravelSleepKind(targetDate, updatedBlock)
                        val normalizedDestination = TripPlanEngine.normalizeTravelClassification(targetDate, destination)
                        plans = plans + (target.date to normalizedSource) + (targetDate to normalizedDestination)
                    }
                    rosterGapConfirmed = false
                    showPlanValidation = false
                    selectedPlanPeriod = null
                },
                onDelete = {
                    val source = plans[target.date].orEmpty().toMutableList()
                    if (target.index in source.indices) source.removeAt(target.index)
                    plans = plans + (
                        target.date to TripPlanEngine.normalizeTravelClassification(target.date, source)
                    )
                    rosterGapConfirmed = false
                    showPlanValidation = false
                    selectedPlanPeriod = null
                },
            )
        }
    }

}


@Composable
private fun WhatsNewChange(change: AppChange) {
    val badgeLabel = when (change.severity) {
        ChangeSeverity.NORMAL -> "NYTT"
        ChangeSeverity.IMPORTANT -> "VIKTIG"
        ChangeSeverity.CALCULATION_CHANGE -> "BEREGNING"
    }
    val badgeContainer = when (change.severity) {
        ChangeSeverity.NORMAL -> MaterialTheme.colorScheme.surfaceContainerHighest
        ChangeSeverity.IMPORTANT -> MaterialTheme.colorScheme.primaryContainer
        ChangeSeverity.CALCULATION_CHANGE -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeContent = when (change.severity) {
        ChangeSeverity.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
        ChangeSeverity.IMPORTANT -> MaterialTheme.colorScheme.onPrimaryContainer
        ChangeSeverity.CALCULATION_CHANGE -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = badgeContainer,
            contentColor = badgeContent,
        ) {
            Text(
                badgeLabel,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            change.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            change.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BoxScope.WhatsNewScrollEdgeFade(
    visible: Boolean,
    atTop: Boolean,
    bottomInset: Dp = 0.dp,
) {
    if (!visible) {
        return
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val brush = remember(surfaceColor, atTop) {
        if (atTop) {
            Brush.verticalGradient(
                0f to surfaceColor,
                1f to surfaceColor.copy(alpha = 0f),
            )
        } else {
            Brush.verticalGradient(
                0f to surfaceColor.copy(alpha = 0f),
                1f to surfaceColor,
            )
        }
    }

    Box(
        modifier = Modifier
            .align(if (atTop) Alignment.TopCenter else Alignment.BottomCenter)
            .padding(bottom = if (atTop) 0.dp else bottomInset)
            .fillMaxWidth()
            .height(28.dp)
            .background(brush),
    )
}


private const val APP_DISCLAIMER_TITLE = "Kontroller alltid beregningen"

private const val APP_DISCLAIMER_TEXT =
    "Ferietur er et hjelpemiddel for planlegging og beregning. Appen kan inneholde feil og " +
        "erstatter ikke kontroll mot gjeldende tariff, turnus, avtaler eller lønnsopplysninger.\n\n" +
        "Du er selv ansvarlig for å kontrollere opplysningene og beregningen før de brukes som " +
        "grunnlag for betaling eller beslutninger."

private const val APP_INDEPENDENCE_TEXT =
    "Ferietur er et uavhengig hjelpemiddel og er ikke en offisiell app fra Oslo kommune."

private enum class WhatsNewMode {
    POST_UPDATE,
    ABOUT,
}

@Composable
private fun WhatsNewScreen(
    padding: PaddingValues,
    releases: List<AppReleaseNotes>,
    mode: WhatsNewMode,
    writeInProgress: Boolean = false,
    onDone: () -> Unit,
) {
    val latest = releases.maxByOrNull { it.versionCode } ?: return
    val listState = rememberLazyListState()
    val canScrollBackward by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val canScrollForward by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            val viewportEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
            lastVisible.index < layoutInfo.totalItemsCount - 1 ||
                lastVisible.offset + lastVisible.size > viewportEnd
        }
    }
    val bottomFadeInset = if (mode == WhatsNewMode.POST_UPDATE) 88.dp else 0.dp

    BackHandler(enabled = true) {
        if (mode == WhatsNewMode.ABOUT && !writeInProgress) {
            onDone()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 14.dp,
                    end = 20.dp,
                    bottom = if (mode == WhatsNewMode.POST_UPDATE) 112.dp else 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    if (mode == WhatsNewMode.ABOUT) {
                        ScreenHeader(
                            "Hva er nytt",
                            "Ferietur ${latest.versionName}",
                            onDone,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Hva er nytt",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Ferietur ${latest.versionName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Text(
                            latest.intro,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                releases.forEach { release ->
                    if (releases.size > 1) {
                        item {
                            Text(
                                "Ferietur ${release.versionName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    release.changes.forEach { change ->
                        item {
                            WhatsNewChange(change)
                        }
                    }
                }
            }

            WhatsNewScrollEdgeFade(
                visible = canScrollBackward,
                atTop = true,
            )
            WhatsNewScrollEdgeFade(
                visible = canScrollForward,
                atTop = false,
                bottomInset = bottomFadeInset,
            )
        }

        if (mode == WhatsNewMode.POST_UPDATE) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
            ) {
                androidx.compose.material3.Button(
                    onClick = onDone,
                    enabled = !writeInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text(if (writeInProgress) "Lagrer…" else "Forstått")
                }
            }
        }
    }
}


@Composable
private fun AboutFerieturScreen(
    padding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showRuleInfo by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var emailLaunchFailed by rememberSaveable { mutableStateOf(false) }
    val currentReleaseNotes = remember(BuildConfig.VERSION_CODE) {
        AppChangelog.releaseForVersion(BuildConfig.VERSION_CODE)
    }
    var privacyLaunchFailed by rememberSaveable { mutableStateOf(false) }
    val emailIntent = remember {
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.fromParts("mailto", APP_CONTACT_EMAIL, null)
            putExtra(Intent.EXTRA_SUBJECT, "Ferietur – tilbakemelding")
        }
    }

    val privacyIntent = remember {
        Intent(Intent.ACTION_VIEW, Uri.parse(APP_PRIVACY_POLICY_URL))
    }

    fun openContactEmail() {
        emailLaunchFailed = false
        try {
            context.startActivity(emailIntent)
        } catch (_: ActivityNotFoundException) {
            emailLaunchFailed = true
        }
    }

    fun openPrivacyPolicy() {
        privacyLaunchFailed = false
        try {
            context.startActivity(privacyIntent)
        } catch (_: ActivityNotFoundException) {
            privacyLaunchFailed = true
        }
    }

    if (showWhatsNew) {
        currentReleaseNotes?.let { release ->
            WhatsNewScreen(
                padding = padding,
                releases = listOf(release),
                mode = WhatsNewMode.ABOUT,
                onDone = { showWhatsNew = false },
            )
            return
        }
    }

    if (showRuleInfo) {
        AlertDialog(
            onDismissRequest = { showRuleInfo = false },
            icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
            title = { Text("Regler og beregningsgrunnlag") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Oslo kommune · Dok. 25 2026–28", fontWeight = FontWeight.Bold)
                    Text(
                        "Kapittel 20 brukes for ferieopphold. Kveld/natt følger punkt 12.1.1 og " +
                            "lørdag/søndag følger punkt 12.2.2. Lønnstabellen fra 01.05.2026 er innebygd.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRuleInfo = false }) { Text("Lukk") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { ScreenHeader("Om Ferietur", "", onBack) }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Ferietur",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Hjelpemiddel for planlegging og beregning av arbeid og betaling ved ferieopphold.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    APP_INDEPENDENCE_TEXT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            MethodSectionHeading("Viktig å vite")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        APP_DISCLAIMER_TITLE,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        APP_DISCLAIMER_TEXT,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            MethodSectionHeading("Personvern")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Opplysninger du registrerer lagres lokalt på enheten og sendes ikke til utvikleren. " +
                            "Ferietur krever ingen konto, har ingen annonser eller analyseverktøy og har ikke " +
                            "internettilgang. PDF-er deles bare når du selv velger det.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { openPrivacyPolicy() }) {
                        Text("Les personvernerklæringen")
                    }
                    if (privacyLaunchFailed) {
                        Text(
                            "Kunne ikke åpne personvernerklæringen i en nettleser.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item {
            MethodSectionHeading("Kontakt")
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable { openContactEmail() },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = null,
                    )
                },
                trailingContent = {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                },
                supportingContent = {
                    Text("Feil, spørsmål eller forslag?")
                },
                content = {
                    Text(
                        "Send tilbakemelding",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
            if (emailLaunchFailed) {
                Text(
                    "Kunne ikke åpne en e-postapp.",
                    modifier = Modifier.padding(start = 56.dp, top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        currentReleaseNotes?.let { release ->
            item {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWhatsNew = true },
                    leadingContent = {
                        Icon(Icons.Rounded.Info, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    },
                    supportingContent = {
                        Text("Endringer i Ferietur ${release.versionName}")
                    },
                    content = {
                        Text(
                            "Hva er nytt",
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }
        }

        item {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRuleInfo = true },
                leadingContent = {
                    Icon(Icons.Rounded.Info, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                },
                content = {
                    Text(
                        "Regler og beregningsgrunnlag",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }

        item {
            MethodSectionHeading("Versjon")
            Text(
                "Ferietur ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    savedTrips: List<SavedTripDraft>,
    storageIssues: List<TripStorageIssue>,
    libraryLoaded: Boolean,
    onStart: () -> Unit,
    onResume: (SavedTripDraft) -> Unit,
    onDuplicate: (SavedTripDraft) -> Unit,
    onDelete: (SavedTripDraft) -> Unit,
    backupBusy: Boolean,
    backupMessage: String?,
    backupMessageIsError: Boolean,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onAbout: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<SavedTripDraft?>(null) }
    val inProgress = savedTrips.filter { savedTripLifecycle(it) == TripLibraryLifecycle.IN_PROGRESS }
    val summaryReady = savedTrips.filter { savedTripLifecycle(it) == TripLibraryLifecycle.SUMMARY_READY }

    pendingDelete?.let { saved ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Slett turen?") },
            text = {
                Text(
                    "${saved.title.ifBlank { "Ferietur" }} og lokale sikkerhetskopier slettes fra denne telefonen. Dette kan ikke angres.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(saved)
                    pendingDelete = null
                }) { Text("Slett", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Avbryt") }
            },
        )
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { OsloHomeHeader(onAbout = onAbout) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Mine turer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Utkast lagres automatisk. Kopier endrer ikke originalen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Save,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Column(Modifier.weight(1f)) {
                                Text("Lokal sikkerhetskopi", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Eksporter alle turer til én fil, eller gjenopprett en tidligere Ferietur-backup.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onExportBackup,
                                enabled = savedTrips.isNotEmpty() && !backupBusy,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Eksporter")
                            }
                            FilledTonalButton(
                                onClick = onImportBackup,
                                enabled = !backupBusy,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Importer")
                            }
                        }
                        Text(
                            "Import fletter inn backupen. En eksisterende tur med samme ID sikkerhetskopieres internt før den erstattes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            backupMessage?.let { message ->
                item {
                    InlineMessage(
                        if (backupMessageIsError) FindingSeverity.CRITICAL else FindingSeverity.OK,
                        if (backupMessageIsError) "Sikkerhetskopi mislyktes" else "Sikkerhetskopi fullført",
                        message,
                    )
                }
            }

            if (!libraryLoaded) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            storageIssues.forEach { issue ->
                item {
                    val severity = when (issue.kind) {
                        TripStorageIssueKind.RECOVERED -> FindingSeverity.REVIEW
                        TripStorageIssueKind.CORRUPT,
                        TripStorageIssueKind.UNSUPPORTED_SCHEMA,
                        TripStorageIssueKind.IO_ERROR,
                        -> FindingSeverity.CRITICAL
                    }
                    val title = when (issue.kind) {
                        TripStorageIssueKind.RECOVERED -> "Lagret tur gjenopprettet"
                        TripStorageIssueKind.CORRUPT -> "En lagret tur kunne ikke leses"
                        TripStorageIssueKind.UNSUPPORTED_SCHEMA -> "En lagret tur bruker et nyere lagringsformat"
                        TripStorageIssueKind.IO_ERROR -> "Problem med lokal lagring"
                    }
                    InlineMessage(
                        severity = severity,
                        title = title,
                        detail = issue.detail,
                    )
                }
            }

            if (libraryLoaded && savedTrips.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            LibraryStatusIcon(ready = false)
                            Column(Modifier.weight(1f)) {
                                Text("Ingen lagrede turer", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Start en ny tur. Den lagres automatisk mens du arbeider.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                if (inProgress.isNotEmpty()) {
                    item {
                        TripLibrarySectionHeader(
                            title = "Pågående",
                            count = inProgress.size,
                            ready = false,
                        )
                    }
                    item {
                        TripLibraryList(
                            trips = inProgress,
                            onResume = onResume,
                            onDuplicate = onDuplicate,
                            onDelete = { pendingDelete = it },
                        )
                    }
                }

                if (summaryReady.isNotEmpty()) {
                    item {
                        TripLibrarySectionHeader(
                            title = "Klar for eksport",
                            count = summaryReady.size,
                            ready = true,
                        )
                    }
                    item {
                        TripLibraryList(
                            trips = summaryReady,
                            onResume = onResume,
                            onDuplicate = onDuplicate,
                            onDelete = { pendingDelete = it },
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onStart,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 18.dp),
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Ny tur", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun OsloHomeHeader(onAbout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Ferietur",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Beregn ferietur uten Excel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OsloIdentityShapes(modifier = Modifier.size(60.dp))
            IconButton(
                onClick = onAbout,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = "Om Ferietur",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun OsloIdentityShapes(modifier: Modifier = Modifier) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondary = MaterialTheme.colorScheme.secondary
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 3.0f
        drawRect(
            color = primaryContainer,
            topLeft = Offset(0f, cell * 0.15f),
            size = Size(cell, cell),
        )
        drawRect(
            color = secondary,
            topLeft = Offset(cell, cell * 1.15f),
            size = Size(cell, cell),
        )
        drawRect(
            color = primaryContainer,
            topLeft = Offset(cell * 2f, cell * 1.15f),
            size = Size(cell, cell),
        )
        drawCircle(
            color = primary,
            radius = cell * 0.48f,
            center = Offset(cell * 2.45f, cell * 0.62f),
        )
    }
}

@Composable
private fun TripLibrarySectionHeader(
    title: String,
    count: Int,
    ready: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (ready) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
            contentDescription = null,
            tint = if (ready) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun LibraryStatusIcon(
    ready: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (ready) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val foreground = if (ready) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Surface(
        shape = CircleShape,
        color = background,
        modifier = modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (ready) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun TripLibraryList(
    trips: List<SavedTripDraft>,
    onResume: (SavedTripDraft) -> Unit,
    onDuplicate: (SavedTripDraft) -> Unit,
    onDelete: (SavedTripDraft) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column {
            trips.forEachIndexed { index, saved ->
                SavedTripListItem(
                    saved = saved,
                    onResume = { onResume(saved) },
                    onDuplicate = { onDuplicate(saved) },
                    onDelete = { onDelete(saved) },
                )
                if (index != trips.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 66.dp, end = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedTripListItem(
    saved: SavedTripDraft,
    onResume: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember(saved.id) { mutableStateOf(false) }
    val progress = savedTripProgress(saved)
    val ready = savedTripLifecycle(saved) == TripLibraryLifecycle.SUMMARY_READY
    val statusText = if (ready) {
        "Oppsummering klar"
    } else {
        "Steg ${progress.currentStep} av ${progress.totalSteps} · ${savedStepLabel(saved.screen)}"
    }

    ListItem(
        onClick = onResume,
        modifier = Modifier.fillMaxWidth(),
        leadingContent = { LibraryStatusIcon(ready = ready) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "${shortDate(saved.startDate)}–${shortDate(saved.endDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (ready) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Sist lagret ${savedTimestamp(saved.updatedAtEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Flere valg")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Lag kopi") },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Slett", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        },
    ) {
        Text(
            saved.title.ifBlank { "Ferietur" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TripOverviewScreen(
    padding: PaddingValues,
    title: String,
    startDate: LocalDate,
    endDate: LocalDate,
    lastStep: FlowScreen,
    lastSavedAt: Long?,
    employerKind: EmployerKind,
    payingParty: PayingParty,
    fundingMode: FundingMode,
    workPlanBasis: TripWorkPlanBasis,
    dates: List<LocalDate>,
    plans: Map<LocalDate, List<PlannedBlock>>,
    roster: Map<LocalDate, String>,
    settlementSummary: SettlementSummary?,
    controlRateSet: app.ferietur.domain.TariffRateSet?,
    runtimeFailureDetail: String?,
    canExport: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onOpen: (FlowScreen) -> Unit,
) {
    val unresolvedCount = settlementSummary?.unresolvedRuleCount ?: 0
    val blocks = TripPlanEngine.projectRange(dates, plans)
    val findings = controlRateSet?.let { rateSet ->
        TripPlanEngine.controlFindings(blocks, unresolvedCount, roster, rateSet)
    }.orEmpty()
    val reviewCount = findings.count { it.severity == FindingSeverity.REVIEW || it.severity == FindingSeverity.CRITICAL }
    val flow = flowSequence(fundingMode, workPlanBasis)
    val calculationIndex = flow.indexOf(FlowScreen.CALCULATION)
    val currentIndex = flow.indexOf(lastStep)
    val reachedCalculation = currentIndex >= calculationIndex && calculationIndex >= 0
    val continueLabel = if (lastStep == FlowScreen.SUMMARY) "Se oppsummering" else "Fortsett der du slapp"

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader("Turoversikt", "", onBack) }
        item {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title.ifBlank { "Ferietur" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(
                        "${fullDate(startDate)}–${fullDate(endDate)}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Arbeidsgiver: ${employerLabel(employerKind)} · betalingsscenario: ${payingPartyLabel(payingParty)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Regler og beregningsgrunnlag: ${employerKind.ruleBasisLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (reachedCalculation && settlementSummary != null) {
                        Spacer(Modifier.height(4.dp))
                        Text("Beregnet med opplysningene som er lagret nå", style = MaterialTheme.typography.labelLarge)
                        Text(currency(settlementSummary.calculatedAmount), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                        if (!settlementSummary.usesFullCalculation) {
                            Text(
                                "Betalingsforslag: ${currency(settlementSummary.proposedAmount)}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    } else if (reachedCalculation) {
                        Text(
                            runtimeFailureDetail ?: "Beregningen kan ikke fullføres med det registrerte grunnlaget.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        Text(
                            "Beregningen er ikke kommet til beregningssteget ennå.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                    Text(continueLabel, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Sist arbeidssteg: ${savedStepLabel(lastStep.name)}" +
                        (lastSavedAt?.let { " · lagret ${savedTimestamp(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onOpen(FlowScreen.TRIP) }, modifier = Modifier.weight(1f)) { Text("Rediger tur") }
                OutlinedButton(onClick = { onOpen(FlowScreen.CALCULATION) }, modifier = Modifier.weight(1f)) { Text("Se beregning") }
            }
        }
        item {
            FilledTonalButton(
                onClick = { onOpen(FlowScreen.SUMMARY) },
                enabled = canExport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (canExport) "Oppsummering og dokumentasjon" else "Dokumentasjon blir tilgjengelig etter Kontroll")
            }
        }
        item { SectionTitle("Status") }
        item {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusRow(employerKind != EmployerKind.UNSPECIFIED, "Arbeidsgiver: ${employerLabel(employerKind)}")
                    StatusRow(true, "Betalingsscenario: ${payingPartyLabel(payingParty)}")
                    StatusRow(
                        controlRateSet != null && reviewCount == 0,
                        when {
                            controlRateSet == null -> "Arbeidstidskontrollen mangler et entydig felles tariffgrunnlag"
                            reviewCount == 0 -> "Ingen arbeidstidsforhold er flagget"
                            else -> "$reviewCount arbeidstidsforhold bør vurderes"
                        },
                    )
                    StatusRow(
                        settlementSummary != null && unresolvedCount == 0,
                        when {
                            settlementSummary == null -> "Beregningsgrunnlaget er ikke tilgjengelig"
                            unresolvedCount == 0 -> "Ingen beregningsregler står åpne"
                            else -> "$unresolvedCount beregningsregler må avklares"
                        },
                    )
                    StatusRow(canExport, if (canExport) "Turen har et ferdigstilt dokumentasjonsgrunnlag" else "Fullfør Kontroll før du lager PDF")
                }
            }
        }
        item { SectionTitle("Gå til") }
        item { OverviewSectionRow("Turen", "Navn, datoer og tidspunkt") { onOpen(FlowScreen.TRIP) } }
        item { OverviewSectionRow("Lønn og betaling", "Vanlig lønn, betalingsscenario og arbeidsgiver") { onOpen(FlowScreen.METHOD) } }
        item { OverviewSectionRow("Lønnsopplysninger", "Lønnstrinn, full arbeidsuke og helgesats") { onOpen(FlowScreen.PAY) } }
        if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) {
            item { OverviewSectionRow("Grunnturnus", "Den vanlige turnusen i perioden") { onOpen(FlowScreen.ROSTER) } }
            if (workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN) {
                item {
                    OverviewSectionRow(
                        title = "Arbeidsgivers arbeidsplan",
                        supporting = "Planen arbeidsgiver faktisk fastsatte for turen",
                    ) { onOpen(FlowScreen.HOLIDAY_PLAN) }
                }
            }
        }
        item {
            OverviewSectionRow(
                title = "Arbeid på turen",
                supporting = "Registrert arbeid og reise under ferieoppholdet",
                debugTestTag = "overview-nav-trip-plan",
            ) { onOpen(FlowScreen.TRIP_PLAN) }
        }
        item { OverviewSectionRow("Beregning", "Se hvordan beløpet er satt sammen") { onOpen(FlowScreen.CALCULATION) } }
        item { OverviewSectionRow("Betalingsforslag", "Beregnet grunnlag eller dokumentert avtalt beløp") { onOpen(FlowScreen.SETTLEMENT) } }
        item { OverviewSectionRow("Kontroll", "Arbeidstid og regler som må avklares") { onOpen(FlowScreen.CONTROL) } }
        item {
            OverviewSectionRow(
                title = "Oppsummering og dokumentasjon",
                supporting = if (canExport) "Lag kort oppsummering eller full dokumentasjon som PDF" else "Tilgjengelig når Kontroll er fullført",
                enabled = canExport,
            ) { onOpen(FlowScreen.SUMMARY) }
        }
    }
}

@Composable
private fun OverviewSectionRow(
    title: String,
    supporting: String,
    enabled: Boolean = true,
    debugTestTag: String? = null,
    onClick: () -> Unit,
) {
    val debugModifier = if (BuildConfig.DEBUG && debugTestTag != null) {
        Modifier.testTag(debugTestTag)
    } else {
        Modifier
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(debugModifier)
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

private fun savedStepLabel(screenName: String): String = when (screenName) {
    "TRIP" -> "Turen"
    "METHOD" -> "Lønn og betaling"
    "PAY" -> "Lønnsopplysninger"
    "ROSTER" -> "Grunnturnus"
    "HOLIDAY_PLAN" -> "Arbeidsgivers plan"
    "TRAVEL" -> "Reise"
    "TRIP_PLAN" -> "Arbeid på turen"
    "CALCULATION" -> "Beregning"
    "SETTLEMENT" -> "Betalingsforslag"
    "CONTROL" -> "Kontroll"
    "SUMMARY" -> "Oppsummering"
    else -> "Utkast"
}

private fun savedTimestamp(epochMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    return "${shortDate(dateTime.toLocalDate())} kl. ${dateTime.toLocalTime().format(timeFormat)}"
}

private fun employerLabel(value: EmployerKind): String = when (value) {
    EmployerKind.OSLO_KOMMUNE -> "Oslo kommune"
    EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED -> "Privat/annet – må avklares"
    EmployerKind.UNSPECIFIED -> "Må velges"
}

private fun payingPartyLabel(value: PayingParty): String = when (value) {
    PayingParty.OSLO_KOMMUNE -> "Oslo kommune"
    PayingParty.RESIDENT_OR_GUARDIAN -> "Beboer/verge"
    PayingParty.OTHER -> "Annet"
    PayingParty.UNSPECIFIED -> "Ikke avklart ennå"
}

@Composable
private fun TripBasicsScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    tripTitle: String,
    onTripTitle: (String) -> Unit,
    startDate: LocalDate,
    endDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    onStartDate: (LocalDate) -> Unit,
    onEndDate: (LocalDate) -> Unit,
    onStartTime: (LocalTime) -> Unit,
    onEndTime: (LocalTime) -> Unit,
    validRange: Boolean,
    chapter20Applicable: Boolean,
    salaryRangeSupported: Boolean,
    earliestSalaryDate: LocalDate,
    latestSalaryDate: LocalDate,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeader("Turen", stepLabel, onBack) }
        item { Text("Gi turen et navn og velg når den starter og slutter.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Navn på turen", style = MaterialTheme.typography.labelLarge)
                TripTitleTextField(
                    value = tripTitle,
                    onValueChange = onTripTitle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item { SectionTitle("Datoer") }
        item {
            TripBasicsDateRangeButton(
                startDate = startDate,
                endDate = endDate,
                minimumDate = earliestSalaryDate,
                onRangeSelected = { selectedStart, selectedEnd ->
                    onStartDate(selectedStart)
                    onEndDate(selectedEnd)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TripBasicsTimeInputButton(
                    label = "Avreise",
                    value = startTime,
                    onValue = onStartTime,
                    modifier = Modifier.weight(1f),
                )
                TripBasicsTimeInputButton(
                    label = "Hjemkomst",
                    value = endTime,
                    onValue = onEndTime,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            // TIME01: the domain has no time-zone model; all registered times are Norwegian time.
            Text(
                ClockChangePolicy.REGISTER_IN_NORWEGIAN_TIME_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!validRange) {
            item { InlineMessage(FindingSeverity.CRITICAL, "Slutt må være etter start", "Juster dato eller klokkeslett før du går videre.") }
        } else if (!salaryRangeSupported) {
            item {
                InlineMessage(
                    FindingSeverity.CRITICAL,
                    "Datoene støttes ikke av verifisert tariff- og lønnsgrunnlag",
                    "Ferietur har verifisert beregningsgrunnlag innenfor perioden ${fullDate(earliestSalaryDate)}–${fullDate(latestSalaryDate)}. " +
                        "Hele turen må være dekket av sammenhengende verifiserte tariff-, sats- og lønnstabellperioder. Rene sats- og lønnstabellskifter kan kombineres automatisk; semantiske regelendringer stoppes.",
                )
            }
        } else if (!chapter20Applicable) {
            item {
                InlineMessage(
                    FindingSeverity.CRITICAL,
                    "Dagstur kan ikke beregnes som ferieopphold",
                    "Dok. 25 punkt 20.1 sier at kapittel 20 ikke gjelder dagsturer. Appen stopper derfor ferieoppholdsberegningen her, slik at feil regelsett ikke brukes. Velg en sluttdato etter startdatoen for et ferieopphold.",
                )
            }
        }
    }
}


@Composable
private fun TripTitleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberTextFieldState(initialText = value)
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .collectLatest { current ->
                onValueChange(current)
            }
    }
    LaunchedEffect(value) {
        if (state.text.toString() != value) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }

    OutlinedTextField(
        state = state,
        modifier = modifier.heightIn(min = 56.dp),
        placeholder = { Text("For eksempel Høsttur 2026") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboardController?.hide() },
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun CalculationMethodScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    employerKind: EmployerKind,
    onEmployerKind: (EmployerKind) -> Unit,
    payingParty: PayingParty,
    onPayingParty: (PayingParty) -> Unit,
    rosterComparisonMode: RosterComparisonMode,
    workPlanBasis: TripWorkPlanBasis,
    onWorkPlanBasis: (TripWorkPlanBasis) -> Unit,
    holidayWorkPlanStatus: HolidayWorkPlanStatus,
    onHolidayWorkPlanStatus: (HolidayWorkPlanStatus) -> Unit,
    onRosterComparisonMode: (RosterComparisonMode) -> Unit,
) {
    val normalSalarySelected =
        rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER &&
            employerKind == EmployerKind.OSLO_KOMMUNE
    val separateTripSelected =
        rosterComparisonMode == RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER

    var paymentMenuOpen by remember { mutableStateOf(false) }
    var employerMenuOpen by remember { mutableStateOf(false) }
    var showRuleInfo by remember { mutableStateOf(false) }

    val paymentLabel = when (payingParty) {
        PayingParty.RESIDENT_OR_GUARDIAN -> "Beboer eller verge"
        PayingParty.OSLO_KOMMUNE -> "Oslo kommune"
        PayingParty.OTHER -> "En annen"
        PayingParty.UNSPECIFIED -> "Ikke avklart ennå"
    }

    val employerLabel = when {
        normalSalarySelected -> "Oslo kommune"
        employerKind == EmployerKind.OSLO_KOMMUNE -> "Oslo kommune"
        employerKind == EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED -> "Privat eller annet oppdrag"
        else -> "Ikke avklart ennå"
    }

    if (showRuleInfo) {
        AlertDialog(
            onDismissRequest = { showRuleInfo = false },
            icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
            title = { Text("Regler og beregningsgrunnlag") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        employerKind.ruleBasisLabel(),
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (employerKind == EmployerKind.OSLO_KOMMUNE) {
                        Text(
                            "For Oslo kommune brukes Dok. 25 2026–28. Kapittel 20 gjelder ferieopphold, med relevante tillegg fra tariffbestemmelsene.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Regelgrunnlaget kan ikke fastsettes endelig før arbeidsgiveren på turen er avklart.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRuleInfo = false }) { Text("Lukk") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader("Lønn og betaling", stepLabel, onBack) }

        item {
            MethodFormSection(
                title = "Hvordan skal turen lønnes?",
            ) {
                MethodRadioRow(
                    selected = normalSalarySelected,
                    title = "Vanlig turnus beholdes",
                    supporting = "Vanlig lønn følger grunnturnusen. Appen beregner bare tilleggene.",
                    onClick = {
                        onEmployerKind(EmployerKind.OSLO_KOMMUNE)
                        onRosterComparisonMode(RosterComparisonMode.USE_NORMAL_ROSTER)
                    },
                )
                HorizontalDivider()
                MethodRadioRow(
                    selected = separateTripSelected,
                    title = "Hele turen beregnes separat",
                    supporting = "Grunnturnusen holdes utenfor. Turen beregnes som eget oppdrag.",
                    onClick = {
                        onRosterComparisonMode(RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER)
                        onWorkPlanBasis(TripWorkPlanBasis.NOT_CLARIFIED)
                    },
                )
            }
        }

        if (normalSalarySelected) {
            item {
                MethodFormSection(title = "Hvilken arbeidsplan har arbeidsgiver fastsatt?") {
                    Text(
                        "Velg hva arbeidsgiver faktisk har bestemt. En arbeidsfordeling de ansatte lager seg imellom på turen regnes ikke automatisk som en egen arbeidsplan fra arbeidsgiver.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    MethodRadioRow(
                        selected = workPlanBasis == TripWorkPlanBasis.NORMAL_ROSTER_APPLIES,
                        title = "Vanlig grunnturnus gjelder",
                        supporting = "Arbeidsgiver har ikke fastsatt en egen arbeidsplan for turen. Ferietur bruker grunnturnusen som sammenligningsgrunnlag.",
                        onClick = {
                            onWorkPlanBasis(TripWorkPlanBasis.NORMAL_ROSTER_APPLIES)
                        },
                    )
                    HorizontalDivider()
                    MethodRadioRow(
                        selected = workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN,
                        title = "Arbeidsgiver har fastsatt en egen plan for turen",
                        supporting = "Velg bare dette når arbeidsgiver faktisk har satt eller godkjent arbeidsplanen som skal gjelde under ferieoppholdet.",
                        onClick = {
                            onWorkPlanBasis(TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN)
                        },
                    )
                    HorizontalDivider()
                    MethodRadioRow(
                        selected = workPlanBasis == TripWorkPlanBasis.NOT_CLARIFIED,
                        title = "Ikke avklart",
                        supporting = "Avklar med leder hvilken arbeidsplan arbeidsgiver mener gjelder før beregningen brukes som betalingsgrunnlag.",
                        onClick = {
                            onWorkPlanBasis(TripWorkPlanBasis.NOT_CLARIFIED)
                        },
                    )
                }
            }
        }

        if (
            normalSalarySelected &&
            workPlanBasis == TripWorkPlanBasis.NORMAL_ROSTER_APPLIES
        ) {
            item {
                CompactInfoCard(
                    title = "Grunnturnusen er baseline",
                    body = "Ferietur sammenligner registrert arbeid på turen med grunnturnusen. En intern fordeling mellom ansatte erstatter ikke grunnturnusen som arbeidsgivers plan.",
                )
            }
        }

        if (
            normalSalarySelected &&
            workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN
        ) {
            item {
                MethodFormSection(title = "Arbeidsgivers arbeidsplan for turen") {
                    Text(
                        "Når arbeidsgiver har fastsatt en egen plan, registreres den senere i flyten. Statusen under avgjør hvordan Ferietur kan behandle punkt 20.2.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    MethodRadioRow(
                        selected = holidayWorkPlanStatus == HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
                        title = "Godkjent og varslet minst 14 dager før",
                        supporting = "Arbeidsgivers plan var godkjent på forhånd, og endringen ble varslet minst 14 dager før.",
                        onClick = {
                            onHolidayWorkPlanStatus(HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED)
                        },
                    )
                    HorizontalDivider()
                    MethodRadioRow(
                        selected = holidayWorkPlanStatus == HolidayWorkPlanStatus.NOT_APPROVED_OR_LATE,
                        title = "Ikke godkjent eller varslet senere",
                        supporting = "Minst ett av vilkårene over er ikke oppfylt. Ferietur holder tariffbehandlingen åpen der utfallet ikke kan fastsettes sikkert.",
                        onClick = {
                            onHolidayWorkPlanStatus(HolidayWorkPlanStatus.NOT_APPROVED_OR_LATE)
                        },
                    )
                    HorizontalDivider()
                    MethodRadioRow(
                        selected = holidayWorkPlanStatus == HolidayWorkPlanStatus.NOT_CLARIFIED,
                        title = "Ikke avklart ennå",
                        supporting = "Du kan registrere turen videre, men beregningen kan ikke ferdigstilles før planstatusen er avklart.",
                        onClick = {
                            onHolidayWorkPlanStatus(HolidayWorkPlanStatus.NOT_CLARIFIED)
                        },
                    )
                }
            }
        }

        if (!normalSalarySelected && !separateTripSelected) {
            item {
                InlineMessage(
                    FindingSeverity.REVIEW,
                    "Velg det som skjer i praksis",
                    "Eldre lagrede turer kan mangle dette valget. Resten av turdataene er beholdt.",
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MethodSectionHeading("Betalingsforslaget gjelder")
                Text(
                    "Velg hvem ekstrakostnadene skal settes opp mot. Påvirker ikke lønnsberegningen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MethodDropdownField(
                    label = "Betalingsforslag",
                    value = paymentLabel,
                    expanded = paymentMenuOpen,
                    onExpandedChange = { paymentMenuOpen = it },
                ) {
                    MethodDropdownOption(
                        text = "Beboer eller verge",
                        selected = payingParty == PayingParty.RESIDENT_OR_GUARDIAN,
                    ) {
                        onPayingParty(PayingParty.RESIDENT_OR_GUARDIAN)
                        paymentMenuOpen = false
                    }
                    MethodDropdownOption(
                        text = "Oslo kommune",
                        selected = payingParty == PayingParty.OSLO_KOMMUNE,
                    ) {
                        onPayingParty(PayingParty.OSLO_KOMMUNE)
                        paymentMenuOpen = false
                    }
                    MethodDropdownOption(
                        text = "En annen",
                        selected = payingParty == PayingParty.OTHER,
                    ) {
                        onPayingParty(PayingParty.OTHER)
                        paymentMenuOpen = false
                    }
                    MethodDropdownOption(
                        text = "Ikke avklart ennå",
                        selected = payingParty == PayingParty.UNSPECIFIED,
                    ) {
                        onPayingParty(PayingParty.UNSPECIFIED)
                        paymentMenuOpen = false
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MethodSectionHeading("Arbeidsgiver på turen")
                MethodDropdownField(
                    label = "Arbeidsgiver",
                    value = employerLabel,
                    expanded = employerMenuOpen,
                    enabled = separateTripSelected,
                    onExpandedChange = { employerMenuOpen = it },
                ) {
                    MethodDropdownOption(
                        text = "Oslo kommune",
                        selected = employerKind == EmployerKind.OSLO_KOMMUNE,
                    ) {
                        onEmployerKind(EmployerKind.OSLO_KOMMUNE)
                        employerMenuOpen = false
                    }
                    MethodDropdownOption(
                        text = "Privat eller annet oppdrag",
                        selected = employerKind == EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED,
                    ) {
                        onEmployerKind(EmployerKind.PRIVATE_OR_OTHER_UNRESOLVED)
                        employerMenuOpen = false
                    }
                    MethodDropdownOption(
                        text = "Ikke avklart ennå",
                        selected = employerKind == EmployerKind.UNSPECIFIED,
                    ) {
                        onEmployerKind(EmployerKind.UNSPECIFIED)
                        employerMenuOpen = false
                    }
                }
                Text(
                    if (normalSalarySelected) {
                        "Følger grunnturnusen."
                    } else {
                        "Avgjør hvilket regelverk appen bruker."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            MethodRulesRow(
                ruleLabel = employerKind.ruleBasisLabel(),
                onClick = { showRuleInfo = true },
            )
        }
    }
}

@Composable
private fun MethodFormSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        MethodSectionHeading(title)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
            ),
        ) {
            Column(
                modifier = Modifier.selectableGroup(),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun MethodSectionHeading(text: String) {
    Text(
        text,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun MethodRadioRow(
    selected: Boolean,
    title: String,
    supporting: String,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.09f)
        } else {
            Color.Transparent
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton,
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.secondary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MethodDropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    enabled: Boolean = true,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) {
                    onExpandedChange(!expanded)
                },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (expanded && enabled) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f)
                },
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                        },
                    )
                }
                if (enabled) {
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = "Velg $label",
                        tint = if (expanded) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                } else {
                    Text(
                        "Fast",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            content()
        }
    }
}

@Composable
private fun MethodDropdownOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        trailingIcon = {
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Valgt",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun MethodRulesRow(
    ruleLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    "Regler og beregningsgrunnlag",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    ruleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PayBasisScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    salaryStep: Int,
    onSalaryStep: (Int) -> Unit,
    annualSalary: BigDecimal,
    multipleSalaryContexts: Boolean,
    weeklyBasis: WeeklyBasis,
    onWeeklyBasis: (WeeklyBasis) -> Unit,
    weekendProfile: WeekendProfile,
    onWeekendProfile: (WeekendProfile) -> Unit,
    payslipChecked: Boolean,
    onPayslipChecked: (Boolean) -> Unit,
) {
    var weeklyMenuOpen by remember { mutableStateOf(false) }
    var showPayRuleInfo by remember { mutableStateOf(false) }

    val weeklyBasisLabel = when (weeklyBasis) {
        WeeklyBasis.HOURS_37_5 -> "37,5 timer"
        WeeklyBasis.HOURS_35_5 -> "35,5 timer"
        WeeklyBasis.DOK25_8_2_2 -> "Tredelt turnus"
        WeeklyBasis.HOURS_33_6 -> "33,6 timer"
    }

    val annualSalaryFormatter = NumberFormat.getNumberInstance(norwegian).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    val annualSalaryLabel = "${annualSalaryFormatter.format(annualSalary)} kr/år"

    if (showPayRuleInfo) {
        AlertDialog(
            onDismissRequest = { showPayRuleInfo = false },
            icon = {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            },
            title = { Text("Regler og beregningsgrunnlag") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Lønnstrinn", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (multipleSalaryContexts) {
                                "Årslønnen hentes fra hver verifiserte lønnstabell som gjelder i turperioden, med samme valgte lønnstrinn."
                            } else {
                                "${OsloSalaryTable2026.sourceLabel}. Årslønnen hentes fra tabellen for valgt lønnstrinn."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Full arbeidsuke", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Valget bestemmer hvor mange årstimer appen bruker når timelønnen regnes ut.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Lørdags- og søndagstillegg", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Satsene følger Dok. 25 2026–28, punkt 12.2.2. Kontroller hvilken sats du faktisk har på en nyere lønnsslipp.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPayRuleInfo = false }) { Text("Lukk") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader("Lønnsopplysninger", stepLabel, onBack) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                MethodSectionHeading("Lønnstrinn")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconButton(
                        onClick = { onSalaryStep(salaryStep - 1) },
                        enabled = salaryStep > OsloSalaryTable2026.minStep,
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Lavere lønnstrinn")
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.secondary,
                        ),
                    ) {
                        Text(
                            salaryStep.toString(),
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    IconButton(
                        onClick = { onSalaryStep(salaryStep + 1) },
                        enabled = salaryStep < OsloSalaryTable2026.maxStep,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Høyere lønnstrinn")
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            annualSalaryLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (multipleSalaryContexts) {
                                "Turen bruker flere verifiserte lønnsperioder; beløpet over er perioden ved turstart."
                            } else {
                                "Lønnstabell fra 1. mai 2026"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                MethodSectionHeading("Full arbeidsuke")
                MethodDropdownField(
                    label = "Arbeidsuke",
                    value = weeklyBasisLabel,
                    expanded = weeklyMenuOpen,
                    onExpandedChange = { weeklyMenuOpen = it },
                ) {
                    MethodDropdownOption(
                        text = "37,5 timer",
                        selected = weeklyBasis == WeeklyBasis.HOURS_37_5,
                    ) {
                        onWeeklyBasis(WeeklyBasis.HOURS_37_5)
                        weeklyMenuOpen = false
                    }
                    MethodDropdownOption(
                        text = "35,5 timer",
                        selected = weeklyBasis == WeeklyBasis.HOURS_35_5,
                    ) {
                        onWeeklyBasis(WeeklyBasis.HOURS_35_5)
                        weeklyMenuOpen = false
                    }
                    MethodDropdownOption(
                        text = "Tredelt turnus",
                        selected = weeklyBasis == WeeklyBasis.DOK25_8_2_2,
                    ) {
                        onWeeklyBasis(WeeklyBasis.DOK25_8_2_2)
                        weeklyMenuOpen = false
                    }
                    MethodDropdownOption(
                        text = "33,6 timer",
                        selected = weeklyBasis == WeeklyBasis.HOURS_33_6,
                    ) {
                        onWeeklyBasis(WeeklyBasis.HOURS_33_6)
                        weeklyMenuOpen = false
                    }
                }
                Text(
                    "Brukes når appen beregner timelønn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                MethodSectionHeading("Lørdags- og søndagstillegg")
                WeekendProfileSelector(
                    selected = weekendProfile,
                    onSelect = onWeekendProfile,
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = payslipChecked,
                        role = Role.Checkbox,
                        onValueChange = onPayslipChecked,
                    )
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = payslipChecked,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.secondary,
                        checkmarkColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        "Jeg har kontrollert opplysningene mot en nyere lønnsslipp",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (payslipChecked) {
                            "Bekreft på nytt hvis du endrer lønnsopplysningene."
                        } else {
                            "Lønnstrinn, full arbeidsuke og helgesats er ubekreftede startverdier til denne kontrollen er huket av."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            MethodRulesRow(
                ruleLabel = "Lønnstabell 1. mai 2026 · Dok. 25 2026–28",
                onClick = { showPayRuleInfo = true },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RosterScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    dates: List<LocalDate>,
    roster: Map<LocalDate, String>,
    hasOverlap: Boolean,
    onPick: (LocalDate) -> Unit,
) {
    var addDateSheetOpen by remember { mutableStateOf(false) }
    val registeredCount = dates.count { date -> RosterEntryCodec.decode(roster[date]).isNotEmpty() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenHeader("Grunnturnus", stepLabel, onBack) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Legg inn vaktene som står i grunnturnusen din.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "$registeredCount av ${dates.size} dager registrert",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (registeredCount == dates.size) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (hasOverlap) {
                item {
                    InlineMessage(
                        FindingSeverity.CRITICAL,
                        "Vakter overlapper",
                        "To eller flere vakter i grunnturnusen dekker samme tid. Endre vaktene før du går videre.",
                    )
                }
            }
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
                    ),
                ) {
                    Column {
                        dates.forEachIndexed { index, date ->
                            RosterDayCard(
                                date = date,
                                shifts = RosterEntryCodec.decode(roster[date]),
                                onClick = { onPick(date) },
                            )
                            if (index != dates.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(start = 104.dp))
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { addDateSheetOpen = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Legg til vakter") },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }

    if (addDateSheetOpen) {
        ModalBottomSheet(onDismissRequest = { addDateSheetOpen = false }) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Text(
                        "Legg til vakter",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
                items(dates.sortedBy { if (roster.containsKey(it)) 1 else 0 }, key = { "add-$it" }) { date ->
                    val shifts = RosterEntryCodec.decode(roster[date])
                    ListItem(
                        content = { Text("${dayName(date)} ${shortDate(date)}", fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text(
                                if (shifts.isEmpty()) "Ikke registrert" else rosterDaySummary(shifts),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable {
                            addDateSheetOpen = false
                            onPick(date)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TravelScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    tripStart: LocalDateTime,
    outboundArrival: LocalDateTime,
    onOutboundArrival: (LocalDateTime) -> Unit,
    outboundKind: TimeKind?,
    onOutboundKind: (TimeKind) -> Unit,
    returnDeparture: LocalDateTime,
    onReturnDeparture: (LocalDateTime) -> Unit,
    tripEnd: LocalDateTime,
    returnKind: TimeKind?,
    onReturnKind: (TimeKind) -> Unit,
    valid: Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("Reise til og fra", stepLabel, onBack) }
        item { Text("Registrer reisen før du lager arbeidsplanen. Da ligger reisetiden allerede på de riktige dagene, og du unngår å føre den samme tiden som vanlig arbeid en gang til.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            TravelLegCard(
                title = "Utreise",
                fixedLabel = "Avreise",
                fixedValue = tripStart,
                editableLabel = "Ankomst",
                editableValue = outboundArrival,
                onEditableValue = onOutboundArrival,
                selectedKind = outboundKind,
                onSelectedKind = onOutboundKind,
            )
        }
        item {
            TravelLegCard(
                title = "Hjemreise",
                fixedLabel = "Hjemme",
                fixedValue = tripEnd,
                editableLabel = "Avreise",
                editableValue = returnDeparture,
                onEditableValue = onReturnDeparture,
                selectedKind = returnKind,
                onSelectedKind = onReturnKind,
                editableFirst = true,
            )
        }
        if (!valid) {
            item { InlineMessage(FindingSeverity.CRITICAL, "Reisen må fylles ut", "Velg ansvar for både utreise og hjemreise, og kontroller at klokkeslettene ligger i riktig rekkefølge.") }
        }
    }
}

@Composable
private fun HolidayPlanScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    dates: List<LocalDate>,
    roster: Map<LocalDate, String>,
    plans: Map<LocalDate, List<PlannedBlock>>,
    required: Boolean,
    validationIssues: Map<LocalDate, List<PlanValidationIssue>>,
    canSeedPlanFromRoster: Boolean,
    onSeedPlanFromRoster: () -> Unit,
    onEditPeriod: (LocalDate, Int) -> Unit,
    onAddPeriod: (LocalDate) -> Unit,
) {
    WorkPlanEditorScreen(
        padding = padding,
        stepLabel = stepLabel,
        title = "Arbeidsgivers arbeidsplan",
        intro = "Registrer bare arbeidsplanen arbeidsgiver faktisk fastsatte for turen. Ikke registrer en intern arbeidsfordeling de ansatte laget seg imellom.",
        dates = dates,
        roster = roster,
        plans = plans,
        validationIssues = validationIssues,
        emptyRequiredMessage = if (required && plans.values.flatten().isEmpty()) {
            "Feriearbeidsplanen er oppgitt som godkjent og varslet minst 14 dager før. Registrer planen før du går videre."
        } else {
            null
        },
        canSeedPlanFromRoster = canSeedPlanFromRoster,
        onSeedPlanFromRoster = onSeedPlanFromRoster,
        derivedPlans = null,
        onBack = onBack,
        onEditPeriod = onEditPeriod,
        onAddPeriod = onAddPeriod,
    )
}

@Composable
private fun TripPlanScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    fundingMode: FundingMode,
    workPlanBasis: TripWorkPlanBasis,
    dates: List<LocalDate>,
    roster: Map<LocalDate, String>,
    plans: Map<LocalDate, List<PlannedBlock>>,
    baselinePlans: Map<LocalDate, List<PlannedBlock>>,
    derivedPlans: Map<LocalDate, List<PlannedBlock>>?,
    validationIssues: Map<LocalDate, List<PlanValidationIssue>>,
    onEditPeriod: (LocalDate, Int) -> Unit,
    onAddPeriod: (LocalDate) -> Unit,
) {
    WorkPlanEditorScreen(
        padding = padding,
        stepLabel = stepLabel,
        title = "Arbeid på turen",
        intro = when {
            fundingMode != FundingMode.TURNUS_PLUS_EXTERNAL ->
                "Registrer arbeid og reise på turen. Hele arbeidsplanen brukes i beregningen."
            workPlanBasis == TripWorkPlanBasis.NORMAL_ROSTER_APPLIES ->
                "Vi sammenligner arbeid på turen med grunnturnusen automatisk og markerer arbeid utenfor avtalt tid."
            workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN ->
                "Vi sammenligner arbeid på turen med arbeidsgivers plan automatisk og markerer arbeid utenfor avtalt tid."
            else ->
                "Registrer arbeid og reise på turen. Planbasis må avklares før beregningen kan ferdigstilles."
        },
        dates = dates,
        roster = roster,
        plans = plans,
        validationIssues = validationIssues,
        emptyRequiredMessage = null,
        canSeedPlanFromRoster = false,
        onSeedPlanFromRoster = {},
        derivedPlans = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) derivedPlans else null,
        comparisonBasis = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) workPlanBasis else null,
        baselinePlans = baselinePlans,
        onBack = onBack,
        onEditPeriod = onEditPeriod,
        onAddPeriod = onAddPeriod,
    )
}

@Composable
private fun WorkPlanEditorScreen(
    padding: PaddingValues,
    stepLabel: String,
    title: String,
    intro: String,
    dates: List<LocalDate>,
    roster: Map<LocalDate, String>,
    plans: Map<LocalDate, List<PlannedBlock>>,
    validationIssues: Map<LocalDate, List<PlanValidationIssue>>,
    emptyRequiredMessage: String?,
    canSeedPlanFromRoster: Boolean,
    onSeedPlanFromRoster: () -> Unit,
    derivedPlans: Map<LocalDate, List<PlannedBlock>>?,
    comparisonBasis: TripWorkPlanBasis? = null,
    baselinePlans: Map<LocalDate, List<PlannedBlock>> = emptyMap(),
    onBack: () -> Unit,
    onEditPeriod: (LocalDate, Int) -> Unit,
    onAddPeriod: (LocalDate) -> Unit,
) {
    val listState = rememberLazyListState()
    val firstErrorDate = dates.firstOrNull { validationIssues[it].orEmpty().isNotEmpty() }
    val derivedBeyondMinutes = derivedPlans
        ?.let { TripPlanEngine.projectRange(dates, it) }
        ?.filter {
            it.holidayWorkPlanRelation == HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN
        }
        ?.sumOf { ChronoUnit.MINUTES.between(it.start, it.end) }

    LaunchedEffect(firstErrorDate, canSeedPlanFromRoster) {
        if (firstErrorDate != null) {
            val headerCount =
                2 +
                    (if (emptyRequiredMessage != null) 1 else 0) +
                    (if (canSeedPlanFromRoster) 1 else 0) +
                    (if (derivedPlans != null) 1 else 0) +
                    (if (comparisonBasis != null) 1 else 0) +
                    (if (validationIssues.isNotEmpty()) 1 else 0)
            val index = headerCount + dates.indexOf(firstErrorDate)
            listState.animateScrollToItem(index.coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 112.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(title, stepLabel, onBack) }
        item {
            Text(
                intro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (comparisonBasis != null) {
            item {
                WorkComparisonLegend()
            }
        }

        emptyRequiredMessage?.let { message ->
            item {
                InlineMessage(
                    FindingSeverity.REVIEW,
                    "Arbeidsgivers arbeidsplan mangler",
                    message,
                )
            }
        }

        if (canSeedPlanFromRoster) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Bruk grunnturnusen som utgangspunkt", fontWeight = FontWeight.Bold)
                        Text(
                            "Kopier grunnturnusen som et redigerbart utgangspunkt for arbeidsplanen arbeidsgiver fastsatte. Endre deretter bare det arbeidsgiver faktisk la om.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = onSeedPlanFromRoster) {
                            Text("Legg inn grunnturnusen")
                        }
                        Text(
                            "Kopieringen er bare registreringshjelp. Bekreft at endringene faktisk kommer fra arbeidsgivers plan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (derivedPlans != null) {
            item {
                val beyond = derivedBeyondMinutes ?: 0L
                InlineMessage(
                    if (beyond > 0L) FindingSeverity.REVIEW else FindingSeverity.OK,
                    if (beyond > 0L) {
                        "Ferietur fant arbeid utover arbeidsgivers plan"
                    } else {
                        "Registrert arbeid ligger innenfor arbeidsgivers plan"
                    },
                    if (beyond > 0L) {
                        "${minutesLabel(beyond)} registrert arbeid ligger utenfor arbeidsplanen arbeidsgiver fastsatte. Sammenligningen er beregnet automatisk."
                    } else {
                        "Ingen registrert aktiv arbeidstid ligger utover arbeidsplanen arbeidsgiver fastsatte."
                    },
                )
            }
        }

        if (validationIssues.isNotEmpty()) {
            item {
                InlineMessage(
                    FindingSeverity.CRITICAL,
                    "${validationIssues.values.flatten().distinctBy { it.id }.size} opplysninger må rettes før du kan gå videre",
                    "De er markert på dagen det gjelder. Trykk på perioden for å redigere.",
                )
            }
        }

        items(dates, key = { it.toString() }) { date ->
            val shifts = RosterEntryCodec.decode(roster[date])
            val blocks = TripPlanEngine.projectVisibleDay(date, plans)
            val derivedDayBlocks = derivedPlans?.let { derived ->
                TripPlanEngine.projectVisibleDay(date, derived)
            }
            TripDayCard(
                date = date,
                shifts = shifts,
                blocks = blocks,
                roster = roster,
                plans = plans,
                comparisonBasis = comparisonBasis,
                baselinePlans = baselinePlans,
                derivedBlocks = derivedDayBlocks,
                issues = validationIssues[date].orEmpty(),
                onEditPeriod = onEditPeriod,
                onAddPeriod = onAddPeriod,
            )
        }
    }
}

@Composable
private fun CalculationScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    employerKind: EmployerKind,
    fundingMode: FundingMode,
    result: TariffRuntimeCalculationPresentation?,
    runtimeFailureDetail: String?,
    weeklyBasis: WeeklyBasis,
) {
    if (result == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenHeader("Beregning", stepLabel, onBack) }
            item {
                InlineMessage(
                    FindingSeverity.CRITICAL,
                    "Beregningen kan ikke fullføres",
                    runtimeFailureDetail ?: "Kontroller tariffperiode, arbeidsplan og registrerte reiseopplysninger.",
                )
            }
        }
        return
    }

    val unresolved = FerieturRules.applicableUnresolvedRules(result.applicableUnresolvedRuleIds)
    val includedLines = result.lineEntries.filter {
        it.line.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS
    }
    val coveredRosterLines = result.lineEntries.filter {
        it.line.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER
    }
    val openLines = result.lineEntries.filter {
        it.line.paymentTreatment == PaymentTreatment.OPEN
    }

    var selectedLineKey by remember { mutableStateOf<String?>(null) }
    var showCoveredRoster by remember { mutableStateOf(false) }
    var showDayAudit by remember { mutableStateOf(false) }
    var selectedAuditDate by remember { mutableStateOf<LocalDate?>(null) }
    var showRules by remember { mutableStateOf(false) }

    selectedLineKey?.let { lineKey ->
        result.lineEntries.firstOrNull { it.key == lineKey }?.let { entry ->
            CalculationLineDetailSheet(
                line = entry.line,
                onDismiss = { selectedLineKey = null },
            )
        }
    }

    if (showCoveredRoster) {
        CoveredRosterDetailSheet(
            lines = coveredRosterLines.map { it.line },
            activeInsideRosterMinutes = result.activeInsideRosterMinutes,
            totalAmount = result.alreadyCoveredByNormalRosterAmount,
            onDismiss = { showCoveredRoster = false },
        )
    }

    if (showDayAudit) {
        CalculationDayAuditSheet(
            audits = result.dayAudits,
            selectedDate = selectedAuditDate,
            onSelectDate = { selectedAuditDate = it },
            onBackToDays = { selectedAuditDate = null },
            onDismiss = {
                showDayAudit = false
                selectedAuditDate = null
            },
        )
    }

    if (showRules) {
        CalculationRulesSheet(
            contexts = result.provenance,
            weeklyBasis = weeklyBasis,
            unresolved = unresolved,
            onDismiss = { showRules = false },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Beregning", stepLabel, onBack) }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        when {
                            fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL ->
                                "Beløp som kommer i tillegg"
                            employerKind == EmployerKind.OSLO_KOMMUNE ->
                                "Beregnet betalingsgrunnlag"
                            else ->
                                "Foreløpig beregning"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        currency(result.paymentBasisAmount),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        when {
                            fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL ->
                                "Vanlig lønn etter grunnturnusen er ikke med."
                            employerKind == EmployerKind.OSLO_KOMMUNE ->
                                "Dette er beregningen før et eventuelt annet betalingsbeløp avtales."
                            else ->
                                "Regelgrunnlaget må avklares før beløpet kan regnes som endelig."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        if (employerKind != EmployerKind.OSLO_KOMMUNE) {
            item {
                InlineMessage(
                    FindingSeverity.OPEN,
                    "Arbeidsgiverforholdet må avklares",
                    "Beløpet er et foreløpig regnegrunnlag til arbeidsgiver og regelverk er avklart.",
                )
            }
        }

        item { MethodSectionHeading("Det som kommer i tillegg") }

        items(includedLines, key = { it.key }) { entry ->
            CalculationSummaryRow(
                line = entry.line,
                onClick = { selectedLineKey = entry.key },
            )
        }

        if (openLines.isNotEmpty()) {
            item { MethodSectionHeading("Må avklares") }
            items(openLines, key = { it.key }) { entry ->
                CalculationSummaryRow(
                    line = entry.line,
                    onClick = { selectedLineKey = entry.key },
                )
            }
        }

        if (
            fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL &&
            (coveredRosterLines.isNotEmpty() || result.activeInsideRosterMinutes > 0)
        ) {
            item {
                CalculationNavigationRow(
                    title = "Grunnturnus – kun sammenligning",
                    supporting = buildString {
                        if (result.activeInsideRosterMinutes > 0) {
                            append("${minutesLabel(result.activeInsideRosterMinutes)} aktiv tid")
                        }
                        if (result.alreadyCoveredByNormalRosterAmount > BigDecimal.ZERO) {
                            if (isNotEmpty()) append(" · ")
                            append("${currency(result.alreadyCoveredByNormalRosterAmount)} turnustillegg")
                        }
                        if (isEmpty()) append("Vis turnuskontroll")
                    },
                    icon = Icons.Rounded.CheckCircle,
                    onClick = { showCoveredRoster = true },
                )
            }
        }

        item {
            CalculationNavigationRow(
                title = "Dag-for-dag kontroll",
                supporting = "${result.dayAudits.size} dager · kontroller hvilke poster hver dag bidrar med",
                icon = Icons.Rounded.CalendarMonth,
                onClick = {
                    selectedAuditDate = null
                    showDayAudit = true
                },
            )
        }

        item {
            CalculationNavigationRow(
                title = "Regler og beregningsgrunnlag",
                supporting = result.singleHourlyRateOrNull?.let { hourlyRate ->
                    "${currency(hourlyRate)}/t · detaljer, kilder og eventuelle åpne regler"
                } ?: "${result.provenance.size} tariff-/lønnsperioder · detaljer, kilder og eventuelle åpne regler",
                icon = Icons.Rounded.Info,
                onClick = { showRules = true },
            )
        }
    }
}

@Composable
private fun CalculationSummaryRow(
    line: CalculationLine,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    line.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    line.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (line.amount != BigDecimal.ZERO || line.includedInKnownTotal) {
                    Text(currency(line.amount), fontWeight = FontWeight.Bold)
                }
                when (line.certainty) {
                    CalculationCertainty.OPEN ->
                        Text(
                            "MÅ AVKLARES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    CalculationCertainty.ASSUMPTION ->
                        Text(
                            "VALGT FORUTSETNING",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    CalculationCertainty.CONFIRMED -> Unit
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Vis detaljer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalculationNavigationRow(
    title: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationLineDetailSheet(
    line: CalculationLine,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val quickReasons = calculationQuickReasons(line)
    var expandedReason by remember(line.id) { mutableStateOf<Int?>(null) }
    var showAllEvidence by remember(line.id) { mutableStateOf(false) }
    var showRuleBasis by remember(line.id) { mutableStateOf(false) }
    val evidenceToShow = if (showAllEvidence) line.evidence else line.evidence.take(4)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        // The content is bounded, while disclosures change height dynamically.
        // A regular scroll container gives stable eager layout and avoids lazy
        // item reuse / long-screenshot seams around expanded explanations.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    line.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                if (line.amount != BigDecimal.ZERO || line.includedInKnownTotal) {
                    Text(
                        currency(line.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    line.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CertaintyLabel(line)
            }

            if (quickReasons.isNotEmpty()) {
                Text(
                    "Kort forklart",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                quickReasons.forEachIndexed { index, reason ->
                    CalculationQuickReasonRow(
                        reason = reason,
                        expanded = expandedReason == index,
                        onClick = {
                            expandedReason = if (expandedReason == index) null else index
                        },
                    )
                }
            }

            if (line.evidence.isNotEmpty()) {
                Text(
                    "Timer som er med",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                evidenceToShow.forEach { evidence ->
                    CalculationEvidenceCompactRow(evidence)
                }

                if (line.evidence.size > 4) {
                    TextButton(
                        onClick = { showAllEvidence = !showAllEvidence },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            if (showAllEvidence) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (showAllEvidence) {
                                "Vis færre"
                            } else {
                                "Vis alle ${line.evidence.size}"
                            },
                        )
                    }
                }
            }

            CalculationRuleBasisDisclosure(
                source = line.source,
                explanation = line.explanation,
                expanded = showRuleBasis,
                onClick = { showRuleBasis = !showRuleBasis },
            )
        }
    }
}

private data class CalculationQuickReason(
    val title: String,
    val explanation: String,
    val icon: ImageVector,
)

private fun calculationQuickReasons(line: CalculationLine): List<CalculationQuickReason> = when (line.id) {
    "active" -> buildList {
        add(
            CalculationQuickReason(
                title = "Kontroller hvilken arbeidsplan som ligger til grunn",
                explanation = "Punkt 20.2-posten skal bygge på feriearbeidsplanen. Grunnturnusen er bare sammenligningsinformasjon i den nye ferieplanmodellen.",
                icon = Icons.Rounded.Schedule,
            ),
        )
        add(
            CalculationQuickReason(
                title = "Arbeid klassifisert etter punkt 20.2 beregnes med +50 %",
                explanation = "Timene på denne posten beregnes med timelønn pluss 50 prosent etter Dok. 25 punkt 20.2.",
                icon = Icons.Rounded.Work,
            ),
        )
        if (line.title.contains("reise", ignoreCase = true)) {
            add(
                CalculationQuickReason(
                    title = "Reise med ansvar regnes som arbeidstid",
                    explanation = "Reisetid med ansvar for beboer inngår som arbeidstid etter punkt 20.3 og er derfor med på denne posten.",
                    icon = Icons.Rounded.DirectionsCar,
                ),
            )
        }
    }

    "resting-night" -> listOf(
        CalculationQuickReason(
            "Hele vakten teller som arbeidstid",
            "Den registrerte hvilende nattevakten teller som arbeidstid time for time.",
            Icons.Rounded.Bedtime,
        ),
        CalculationQuickReason(
            "Grunnbetalingen beregnes i forholdet 1:3",
            "Tre timer hvilende nattevakt gir én time lønnsekvivalent etter punkt 20.4.",
            Icons.Rounded.Schedule,
        ),
        CalculationQuickReason(
            "Aktivt arbeid beregnes separat",
            "Aktive hendelser under den hvilende nattevakten legges på en egen beregningspost.",
            Icons.Rounded.Work,
        ),
    )

    "active-on-resting" -> listOf(
        CalculationQuickReason(
            "Bare aktiv tid under den hvilende vakten er med",
            "Denne posten gjelder registrerte aktive hendelser inne i en hvilende nattevakt.",
            Icons.Rounded.Work,
        ),
        CalculationQuickReason(
            "Tiden avrundes per hvilende nattevakt",
            "14 minutter eller mindre strykes. 15 minutter eller mer rundes opp til neste halve time.",
            Icons.Rounded.Schedule,
        ),
        CalculationQuickReason(
            "Den avrundede tiden betales med +50 %",
            "Betalingen beregnes med timelønn pluss 50 prosent etter punkt 20.4.",
            Icons.Rounded.Info,
        ),
    )

    "resting-evening-night" -> listOf(
        CalculationQuickReason(
            "Dette er kveld-/nattillegget på den hvilende tiden",
            "Tillegget beregnes bare for den delen av den hvilende vakten som ligger i relevante kveld-/nattperioder.",
            Icons.Rounded.Bedtime,
        ),
        CalculationQuickReason(
            "Tillegget betales i forholdet 1:3",
            "Arbeid av passiv karakter får også dette tillegget beregnet til en tredel av registrert hvilende tid.",
            Icons.Rounded.Schedule,
        ),
    )

    "resting-weekend" -> listOf(
        CalculationQuickReason(
            "Dette er helgetillegget på den hvilende tiden",
            "Posten gjelder hvilende tid som ligger på lørdag eller søndag.",
            Icons.Rounded.CalendarMonth,
        ),
        CalculationQuickReason(
            "Tillegget betales i forholdet 1:3",
            "Lørdags- og søndagstillegget under arbeid av passiv karakter beregnes til en tredel.",
            Icons.Rounded.Schedule,
        ),
        CalculationQuickReason(
            "Høyere høytidstillegg prioriteres",
            "Timer som samtidig ligger i en periode med høyere helge-/høytidstillegg tas ikke med her.",
            Icons.Rounded.Info,
        ),
    )

    "resting-holiday" -> listOf(
        CalculationQuickReason(
            "Dette er høytidstillegget på den hvilende tiden",
            "Posten gjelder hvilende tid i helge- og høytidsperiodene i punkt 12.2.3.",
            Icons.Rounded.CalendarMonth,
        ),
        CalculationQuickReason(
            "Tillegget betales i forholdet 1:3",
            "Også helge- og høytidstillegget beregnes i forholdet 1:3 under hvilende nattevakt.",
            Icons.Rounded.Schedule,
        ),
    )

    "evening-night" -> listOf(
        CalculationQuickReason(
            "Tillegget gjelder ordinært arbeid på kveld og natt",
            "Relevant ordinært arbeid mellom kl. 17:00 og 06:00 kan gi 40 prosent tillegg.",
            Icons.Rounded.Bedtime,
        ),
        CalculationQuickReason(
            "Nattevakt kan få tillegg til vakten slutter",
            "For nattevakt kan tillegget fortsette etter kl. 06:00, men ikke lenger enn til kl. 08:00.",
            Icons.Rounded.Schedule,
        ),
        CalculationQuickReason(
            "Tillegget gis ikke på overtid",
            "Kveld-/nattillegget er et ordinært tjenestetillegg og legges ikke på de samme minuttene som overtidsberegnes.",
            Icons.Rounded.Info,
        ),
    )

    "weekend" -> listOf(
        CalculationQuickReason(
            "Tillegget gjelder ordinært arbeid lørdag og søndag",
            "Posten gjelder ordinære timer i helgeperioden som kvalifiserer for lørdags- og søndagstillegg.",
            Icons.Rounded.CalendarMonth,
        ),
        CalculationQuickReason(
            "Appen bruker satsen du kontrollerte mot lønnsslippen",
            "Helgesatsen følger valget som ble bekreftet i lønnsopplysningene.",
            Icons.Rounded.CheckCircle,
        ),
        CalculationQuickReason(
            "Høyere høytidstillegg prioriteres",
            "Timer med høyere helge-/høytidstillegg tas ikke samtidig med på denne posten.",
            Icons.Rounded.Info,
        ),
    )

    "holiday" -> listOf(
        CalculationQuickReason(
            "Posten gjelder særskilte helge- og høytidsperioder",
            "Bare timer som ligger i periodene omfattet av punkt 12.2.3 er med.",
            Icons.Rounded.CalendarMonth,
        ),
        CalculationQuickReason(
            "Dette tillegget prioriteres foran vanlig helgetillegg",
            "Samme minutter legges ikke også til lørdags- og søndagstillegget når høytidstillegget er høyere.",
            Icons.Rounded.Info,
        ),
    )

    "stay-allowance" -> listOf(
        CalculationQuickReason(
            "Godtgjøringen gjelder hele ferieoppholdet",
            "Dette er en døgngodtgjøring etter punkt 20.6 og fordeles derfor ikke på enkeltdager i dag-for-dag-kontrollen.",
            Icons.Rounded.CalendarMonth,
        ),
        CalculationQuickReason(
            "Satsen er 110 kr per døgn",
            "Antall døgn i beregningen multipliseres med den faste satsen.",
            Icons.Rounded.Info,
        ),
        CalculationQuickReason(
            "En rest over seks timer teller som et døgn",
            "Påbegynt resttid på mer enn seks timer gir et helt ekstra døgn; kortere resttid gjør ikke det.",
            Icons.Rounded.Schedule,
        ),
    )

    "travel-without-responsibility" -> listOf(
        CalculationQuickReason(
            "Dette er reise uten tilsynsansvar",
            "Reisetiden behandles etter reisetidsreglene fordi du ikke har registrert aktivt tilsynsansvar i perioden.",
            Icons.Rounded.DirectionsCar,
        ),
        CalculationQuickReason(
            "Ordinær reisetid betales med timelønn",
            "Den ordinære reisetidsbetalingen på denne posten bruker vanlig timelønn.",
            Icons.Rounded.Schedule,
        ),
        CalculationQuickReason(
            "Kort varsel håndteres separat",
            "Et eventuelt overtids­tillegg ved kort varsel ligger på en egen beregningspost.",
            Icons.Rounded.Info,
        ),
    )

    "travel-short-notice-overtime" -> listOf(
        CalculationQuickReason(
            "Reisen var ikke kjent senest dagen i forveien",
            "Kort varsel kan utløse overtidsbetaling for en begrenset del av reisetiden utenfor ordinær arbeidstid.",
            Icons.Rounded.Warning,
        ),
        CalculationQuickReason(
            "Den ordinære reisetidsbetalingen står separat",
            "Denne posten er overtidsdelen i tillegg til den ordinære timelønnen for reisen.",
            Icons.Rounded.DirectionsCar,
        ),
        CalculationQuickReason(
            "Overtidstillegget avrundes til halvtime",
            "Beregningsgrunnlaget avrundes etter overtidsregelen som brukes på denne posten.",
            Icons.Rounded.Schedule,
        ),
    )

    "travel-passive-night" -> listOf(
        CalculationQuickReason(
            "Du har registrert søvntillatelse under nattreisen",
            "Reisetid mellom kl. 23:00 og 07:00 behandles da som arbeid av passiv karakter.",
            Icons.Rounded.Bedtime,
        ),
        CalculationQuickReason(
            "Tiden teller som arbeidstid",
            "Nattreisen teller som arbeidstid time for time.",
            Icons.Rounded.Schedule,
        ),
        CalculationQuickReason(
            "Grunnbetalingen beregnes i forholdet 1:3",
            "For arbeid av passiv karakter er grunnbetalingen en tredel av timelønnen per registrert time.",
            Icons.Rounded.Info,
        ),
    )

    "travel-passive-evening-night",
    "travel-passive-weekend",
    "travel-passive-holiday" -> listOf(
        CalculationQuickReason(
            "Dette er et tillegg under passiv nattreise",
            "Posten gjelder et ordinært tillegg på nattreisen som er klassifisert som arbeid av passiv karakter.",
            Icons.Rounded.Bedtime,
        ),
        CalculationQuickReason(
            "Tillegget beregnes i forholdet 1:3",
            "Tillegget følger samme 1:3-forhold som den passive nattreisen.",
            Icons.Rounded.Schedule,
        ),
    )

    "travel-notice-open",
    "travel-night-sleep-open",
    "travel-responsibility-open",
    "travel-short-notice-133-open" -> listOf(
        CalculationQuickReason(
            "En opplysning mangler",
            "Appen kan ikke gjøre denne posten endelig før den registrerte forutsetningen er avklart.",
            Icons.Rounded.Warning,
        ),
        CalculationQuickReason(
            "Beløpet holdes utenfor kjent total når regelen er åpen",
            "Den åpne posten vises for kontroll, men behandles ikke som en bekreftet del av betalingsgrunnlaget.",
            Icons.Rounded.Info,
        ),
    )

    else -> listOf(
        CalculationQuickReason(
            "Posten beregnes separat",
            "Denne lønnsposten vises separat slik at beløp og beregningsgrunnlag kan kontrolleres.",
            Icons.Rounded.Info,
        ),
    )
}

@Composable
private fun CalculationQuickReasonRow(
    reason: CalculationQuickReason,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            reason.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    reason.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Skjul forklaring" else "Vis forklaring",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (expanded) {
                HorizontalDivider()
                Text(
                    reason.explanation,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalculationEvidenceCompactRow(
    evidence: CalculationEvidence,
) {
    var expanded by remember(evidence.start, evidence.end, evidence.note) {
        mutableStateOf(false)
    }
    val dateFormatter = DateTimeFormatter.ofPattern("EEE d. MMM", norwegian)
    val startDate = evidence.start.toLocalDate().format(dateFormatter)
    val endDate = evidence.end.toLocalDate().format(dateFormatter)
    val interval = if (evidence.start.toLocalDate() == evidence.end.toLocalDate()) {
        "${timeFormat.format(evidence.start.toLocalTime())}–${timeFormat.format(evidence.end.toLocalTime())}"
    } else {
        "${timeFormat.format(evidence.start.toLocalTime())} → $endDate ${timeFormat.format(evidence.end.toLocalTime())}"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    startDate,
                    modifier = Modifier.widthIn(min = 72.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    interval,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    minutesLabel(evidence.minutes),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            if (expanded) {
                HorizontalDivider()
                Text(
                    evidence.note,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalculationRuleBasisDisclosure(
    source: String,
    explanation: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Kilde og regelgrunnlag",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        source,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ChevronRight,
                    contentDescription = null,
                )
            }

            if (expanded) {
                HorizontalDivider()
                Text(
                    explanation,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoveredRosterDetailSheet(
    lines: List<CalculationLine>,
    activeInsideRosterMinutes: Long,
    totalAmount: BigDecimal,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "Allerede dekket av grunnturnusen",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (activeInsideRosterMinutes > 0) {
                        Text(
                            "${minutesLabel(activeInsideRosterMinutes)} aktiv tid",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (totalAmount > BigDecimal.ZERO) {
                        Text(
                            "${currency(totalAmount)} i turnustillegg",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        "Vanlig lønn og disse turnuspostene er forutsatt dekket gjennom grunnturnusen og er ikke med i beløpet på hovedskjermen.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            itemsIndexed(lines, key = { index, line -> "covered-detail-$index-${line.id}" }) { _, line ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(line.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                line.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (line.amount != BigDecimal.ZERO || line.includedInKnownTotal) {
                            Text(currency(line.amount), fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        line.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    line.evidence.forEach { EvidenceRow(it) }
                    Text(
                        "Kilde: ${line.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationDayAuditSheet(
    audits: List<DayCalculationAudit>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    onBackToDays: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        val selected = selectedDate?.let { date ->
            audits.firstOrNull { it.date == date }
        }

        if (selected == null) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Dag-for-dag kontroll",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            "Se hvilke lønnsposter hver kalenderdag bidrar med. Døgngodtgjøringen gjelder hele reisen og fordeles ikke på enkeltdager.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(audits, key = { "audit-nav-${it.date}" }) { audit ->
                    val paymentContributions = dayAuditPaymentContributions(audit)
                    val amountLabel = when {
                        audit.paymentSubtotal > BigDecimal.ZERO && audit.openSubtotal > BigDecimal.ZERO ->
                            "${currency(audit.paymentSubtotal)} · ${currency(audit.openSubtotal)} må avklares"
                        audit.paymentSubtotal > BigDecimal.ZERO ->
                            currency(audit.paymentSubtotal)
                        audit.openSubtotal > BigDecimal.ZERO ->
                            "${currency(audit.openSubtotal)} må avklares"
                        paymentContributions.isEmpty() ->
                            "Ingen tillegg"
                        else ->
                            "0,00 kr"
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDate(audit.date) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    "${dayName(audit.date)} ${shortDate(audit.date)}",
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (audit.holidayLabels.isNotEmpty()) {
                                    Text(
                                        audit.holidayLabels.joinToString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                            Text(
                                amountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = "Vis dagen",
                            )
                        }
                    }
                }
            }
        } else {
            val paymentContributions = dayAuditPaymentContributions(selected)
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(onClick = onBackToDays) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Tilbake til dager",
                            )
                        }
                        Column {
                            Text(
                                "${dayName(selected.date)} ${shortDate(selected.date)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                dayAuditCollapsedAmountSummary(selected),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (paymentContributions.isEmpty()) {
                    item {
                        Text(
                            "Ingen poster fra turen er med i betalingsgrunnlaget denne dagen.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    itemsIndexed(paymentContributions) { index, contribution ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DayCalculationContributionRow(contribution)
                            if (index != paymentContributions.lastIndex) HorizontalDivider()
                        }
                    }
                }

                if (selected.alreadyCoveredSubtotal > BigDecimal.ZERO) {
                    item {
                        Text(
                            "Grunnturnus: ${currency(selected.alreadyCoveredSubtotal)} i turnustillegg er allerede dekket og ikke med i betalingsgrunnlaget.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculationRulesSheet(
    contexts: List<TariffRuntimeProvenanceSlice>,
    weeklyBasis: WeeklyBasis,
    unresolved: List<DomainRule>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "Regler og beregningsgrunnlag",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            if (contexts.size == 1) {
                item {
                    val context = contexts.single()
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Timelønn", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${currency(context.hourlyRate)} per time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Årslønn ${currency(context.annualSalary)} deles på ${weeklyBasis.divisor} timer (${weeklyBasisUserLabel(weeklyBasis)}).",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Kilde: ${context.salaryTableSourceLabel} · Dok. 25 punkt 9.6",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "${contexts.size} tariff- og lønnsperioder brukes i turen. Hver periode beholder sin egen årslønn og timelønn.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                itemsIndexed(contexts, key = { index, context -> "$index-${context.tariffRateSetId}-${context.salaryTableId}" }) { index, context ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Periode ${index + 1}: ${shortDate(context.start)}–${shortDate(context.end)}",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${currency(context.hourlyRate)}/t · årslønn ${currency(context.annualSalary)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            context.salaryTableSourceLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Tariff ${context.tariffPackageId} · regelsett ${context.rulesetVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Satssett ${context.tariffRateSetId} · lønnstabell ${context.salaryTableId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (index < contexts.lastIndex) HorizontalDivider()
                }
            }

            if (unresolved.isNotEmpty()) {
                item { HorizontalDivider() }
                item {
                    Text(
                        "Regler som må avklares for denne turen",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(unresolved, key = { "rule-detail-${it.id}" }) { rule ->
                    RuleCompactCard(rule.title, rule.source)
                }
            } else {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text("Ingen åpne regelspørsmål er registrert for denne turen.")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    employerKind: EmployerKind,
    fundingMode: FundingMode,
    result: TariffRuntimeCalculationPresentation?,
    runtimeFailureDetail: String?,
    settlementMode: SettlementMode,
    onSettlementMode: (SettlementMode) -> Unit,
    customAmountText: String,
    onCustomAmountText: (String) -> Unit,
    reason: String,
    onReason: (String) -> Unit,
) {
    if (result == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScreenHeader("Betalingsforslag", stepLabel, onBack) }
            item {
                InlineMessage(
                    FindingSeverity.CRITICAL,
                    "Beregningen kan ikke brukes til oppgjør",
                    runtimeFailureDetail ?: "Gå tilbake til arbeidsplanen og kontroller registreringene.",
                )
            }
        }
        return
    }
    // State-based TextFields own text, cursor, selection and IME composition.
    // The outer draft state is mirrored from snapshotFlow and no longer drives
    // the live editor on every keystroke.
    val amountFieldState = rememberTextFieldState(initialText = customAmountText)
    val reasonFieldState = rememberTextFieldState(initialText = reason)
    val amountInputTransformation = remember {
        InputTransformation.byValue { _, proposed ->
            proposed.filter {
                it.isDigit() || it == ',' || it == '.' || it == ' '
            }
        }
    }

    LaunchedEffect(amountFieldState) {
        snapshotFlow { amountFieldState.text.toString() }
            .collectLatest { current ->
                onCustomAmountText(current)
            }
    }
    LaunchedEffect(reasonFieldState) {
        snapshotFlow { reasonFieldState.text.toString() }
            .collectLatest { current ->
                onReason(current)
            }
    }

    val currentAmountText = amountFieldState.text.toString()
    val currentReason = reasonFieldState.text.toString()
    val customAmount = currentAmountText.toNorwegianMoneyOrNull()

    var amountFocusedOnce by remember(settlementMode) { mutableStateOf(false) }
    var amountTouched by remember(settlementMode) { mutableStateOf(false) }
    var reasonFocusedOnce by remember(settlementMode) { mutableStateOf(false) }
    var reasonTouched by remember(settlementMode) { mutableStateOf(false) }

    val amountError = when {
        settlementMode != SettlementMode.CUSTOM_AGREEMENT -> null
        !amountTouched -> null
        currentAmountText.isBlank() -> "Beløp må fylles ut"
        customAmount == null -> "Skriv inn et gyldig beløp"
        else -> null
    }
    val reasonError = when {
        settlementMode != SettlementMode.CUSTOM_AGREEMENT -> null
        !reasonTouched -> null
        currentReason.isBlank() -> "Forklar hvorfor beløpet avviker"
        else -> null
    }

    // This screen contains a small, bounded form. Using an eager scroll
    // container avoids LazyColumn item remeasurement fighting the IME while
    // the multiline reason field is edited.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader("Betalingsforslag", stepLabel, onBack)

        if (employerKind != EmployerKind.OSLO_KOMMUNE) {
            InlineMessage(
                FindingSeverity.OPEN,
                "Foreløpig betalingsgrunnlag",
                "Arbeidsgiverforholdet er ikke bekreftet som Oslo kommune. Beløpet er derfor et foreløpig regnegrunnlag.",
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Beregnet grunnlag",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    currency(result.paymentBasisAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    when {
                        fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL ->
                            "Tillegg og arbeid som beregnes særskilt for ferieoppholdet."
                        employerKind == EmployerKind.OSLO_KOMMUNE ->
                            "Beregnet betalingsgrunnlag."
                        else ->
                            "Foreløpig regnegrunnlag."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        MethodFormSection(title = "Hva skal betalingsforslaget vise?") {
            MethodRadioRow(
                selected = settlementMode == SettlementMode.FULL_CALCULATION,
                title = "Bruk beregnet beløp",
                supporting = currency(result.paymentBasisAmount),
                onClick = {
                    onSettlementMode(SettlementMode.FULL_CALCULATION)
                },
            )
            HorizontalDivider()
            MethodRadioRow(
                selected = settlementMode == SettlementMode.CUSTOM_AGREEMENT,
                title = "Dokumenter avtalt beløp",
                supporting = "Brukes hvis partene allerede har avtalt et annet beløp.",
                onClick = {
                    onSettlementMode(SettlementMode.CUSTOM_AGREEMENT)
                },
            )
        }

        if (settlementMode == SettlementMode.CUSTOM_AGREEMENT) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    state = amountFieldState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                amountFocusedOnce = true
                            } else if (amountFocusedOnce) {
                                amountTouched = true
                            }
                        },
                    label = { Text("Avtalt beløp") },
                    isError = amountError != null,
                    inputTransformation = amountInputTransformation,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                Text(
                    amountError
                        ?: if (currentAmountText.isBlank()) {
                            "Obligatorisk · eksempel: 27 500,00 kr"
                        } else {
                            "Eksempel: 27 500,00 kr"
                        },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (amountError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    state = reasonFieldState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                reasonFocusedOnce = true
                            } else if (reasonFocusedOnce) {
                                reasonTouched = true
                            }
                        },
                    label = { Text("Begrunnelse") },
                    placeholder = {
                        Text("Hvorfor avviker beløpet fra beregningen?")
                    },
                    isError = reasonError != null,
                    lineLimits = TextFieldLineLimits.MultiLine(
                        minHeightInLines = 5,
                        maxHeightInLines = 5,
                    ),
                )
                Text(
                    reasonError
                        ?: if (currentReason.isBlank()) {
                            "Obligatorisk"
                        } else {
                            "Kort begrunnelse for avviket"
                        },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (reasonError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            SettlementInfoRow(
                calculatedAmount = result.paymentBasisAmount,
                customAmount = customAmount,
            )
        }
    }
}

@Composable
private fun SettlementInfoRow(
    calculatedAmount: BigDecimal,
    customAmount: BigDecimal?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 2.dp).size(20.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                "Et avtalt beløp endrer ikke beregningen eller arbeidstidsvarslene.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (customAmount == null) {
                    "Beregnet grunnlag: ${currency(calculatedAmount)}"
                } else {
                    val difference = calculatedAmount.subtract(customAmount)
                    "Beregnet ${currency(calculatedAmount)} · avtalt ${currency(customAmount)} · forskjell ${currency(difference)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ControlScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    employerKind: EmployerKind,
    dates: List<LocalDate>,
    plans: Map<LocalDate, List<PlannedBlock>>,
    roster: Map<LocalDate, String>,
    rosterGapEvidence: List<CalculationEvidence>,
    rosterGapConfirmed: Boolean,
    onRosterGapConfirmed: (Boolean) -> Unit,
    settlementSummary: SettlementSummary?,
    controlRateSet: app.ferietur.domain.TariffRateSet?,
    runtimeFailureDetail: String?,
) {
    val blocks = TripPlanEngine.projectRange(dates, plans)
    val unresolved = settlementSummary?.unresolvedRuleCount ?: 0
    val findings = controlRateSet?.let { rateSet ->
        TripPlanEngine.controlFindings(blocks, unresolved, roster, rateSet)
    }.orEmpty()
    val reviewCount = findings.count {
        it.severity == FindingSeverity.REVIEW || it.severity == FindingSeverity.CRITICAL
    }
    val groupedFindings = groupControlFindings(findings)
        .filter { it.severity != FindingSeverity.OK }

    var selectedGroupTitle by remember { mutableStateOf<String?>(null) }
    var selectedFindingIndex by remember { mutableStateOf<Int?>(null) }

    selectedGroupTitle?.let { title ->
        groupedFindings.firstOrNull { it.title == title }?.let { group ->
            ControlFindingGroupSheet(
                group = group,
                selectedFindingIndex = selectedFindingIndex,
                onSelectFinding = { selectedFindingIndex = it },
                onBackToGroup = { selectedFindingIndex = null },
                onDismiss = {
                    selectedFindingIndex = null
                    selectedGroupTitle = null
                },
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Kontroll", stepLabel, onBack) }

        if (settlementSummary == null || controlRateSet == null) {
            item {
                InlineMessage(
                    FindingSeverity.CRITICAL,
                    "Kontrollgrunnlaget kan ikke ferdigstilles",
                    runtimeFailureDetail
                        ?: "Tariffperiodene har ikke ett entydig felles arbeidstidsgrunnlag. Ferietur stopper ferdigstillingen fremfor å blande kontrollregler.",
                )
            }
        }

        if (employerKind != EmployerKind.OSLO_KOMMUNE) {
            item {
                InlineMessage(
                    FindingSeverity.OPEN,
                    "Foreløpig grunnlag",
                    "Arbeidsgiverforholdet er ikke bekreftet som Oslo kommune. Kontroll og beløp kan brukes til dokumentasjon, men endelig regelgrunnlag og samlet kostnad må avklares.",
                )
            }
        }

        settlementSummary?.let { summary ->
            item {
                ControlSettlementStatusRow(summary)
            }
        }

        if (rosterGapEvidence.isNotEmpty()) {
            item {
                val gapMinutes = rosterGapEvidence.sumOf { it.minutes }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    "Turnustid uten registrert arbeidsperiode",
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${minutesUi(gapMinutes)} i grunnturnusen mangler arbeidsperiode på turen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = rosterGapConfirmed,
                                    role = Role.Checkbox,
                                    onValueChange = onRosterGapConfirmed,
                                ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = rosterGapConfirmed,
                                onCheckedChange = null,
                            )
                            Text(
                                "Jeg har kontrollert at dette er riktig registrert",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }

        if (controlRateSet != null) {
            item {
                Text(
                    if (reviewCount == 0) {
                        "Ingen forhold krever særskilt vurdering"
                    } else {
                        "$reviewCount forhold bør vurderes"
                    },
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            if (groupedFindings.isEmpty()) {
                item {
                    ListItem(
                        leadingContent = {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        supportingContent = {
                            Text(
                                "Den registrerte planen utløser ingen av kontrollkategoriene.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        content = {
                            Text(
                                "Ingen åpenbare arbeidstidsvarsler",
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                    )
                }
            } else {
                itemsIndexed(groupedFindings, key = { _, group -> group.title }) { index, group ->
                    ControlFindingSummaryRow(
                        group = group,
                        onClick = {
                            selectedGroupTitle = group.title
                            selectedFindingIndex = if (group.findings.size == 1) 0 else null
                        },
                    )
                    if (index != groupedFindings.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 46.dp))
                    }
                }
            }
        }

        val unresolvedRules = settlementSummary?.unresolvedRules.orEmpty()
        if (unresolvedRules.isNotEmpty()) {
            item {
                MethodSectionHeading("Regler som må avklares")
            }
            items(
                unresolvedRules,
                key = { "control-rule-${it.id}" },
            ) { rule ->
                RuleCompactCard(rule.title, rule.source)
            }
        }

        item {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    "Kontrollen peker på forhold som bør vurderes. Den avgjør ikke om arbeidstidsordningen er juridisk godkjent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ControlSettlementStatusRow(
    settlementSummary: SettlementSummary,
) {
    val followsCalculation = settlementSummary.usesFullCalculation
    ListItem(
        leadingContent = {
            Icon(
                if (followsCalculation) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.Info
                },
                contentDescription = null,
                tint = if (followsCalculation) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
            )
        },
        supportingContent = {
            Text(
                if (followsCalculation) {
                    "Følger beregningen"
                } else {
                    "Avviker fra beregnet grunnlag ${currency(settlementSummary.calculatedAmount)}"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        content = {
            Text(
                "Betalingsforslag · ${currency(settlementSummary.proposedAmount)}",
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

@Composable
private fun ControlFindingSummaryRow(
    group: ControlFindingGroup,
    onClick: () -> Unit,
) {
    val icon = controlFindingIcon(group.severity)
    val tint = controlFindingTint(group.severity)
    val supporting = controlGroupSupportingText(group)

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        },
        supportingContent = {
            Text(
                supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Vis detaljer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        content = {
            Text(
                group.title,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlFindingGroupSheet(
    group: ControlFindingGroup,
    selectedFindingIndex: Int?,
    onSelectFinding: (Int) -> Unit,
    onBackToGroup: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val selectedFinding = selectedFindingIndex?.let(group.findings::getOrNull)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        if (selectedFinding == null) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            group.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            controlGroupCountLabel(group),
                            style = MaterialTheme.typography.titleMedium,
                            color = controlFindingTint(group.severity),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            controlGroupDescription(group.title),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                itemsIndexed(
                    group.findings,
                    key = { index, finding ->
                        "control-${group.title}-$index-${finding.detail.hashCode()}"
                    },
                ) { index, finding ->
                    ControlFindingCompactRow(
                        finding = finding,
                        onClick = { onSelectFinding(index) },
                    )
                    if (index != group.findings.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 44.dp))
                    }
                }
            }
        } else {
            val compact = compactControlFinding(selectedFinding)
            var showOriginal by remember(selectedFinding.detail) {
                mutableStateOf(false)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (group.findings.size > 1) {
                    IconButton(onClick = onBackToGroup) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Tilbake til ${group.title}",
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        compact.primary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    compact.secondary?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Hva bør kontrolleres?",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            compact.checkText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                TextButton(
                    onClick = { showOriginal = !showOriginal },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        if (showOriginal) {
                            Icons.Rounded.ExpandLess
                        } else {
                            Icons.Rounded.ExpandMore
                        },
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (showOriginal) {
                            "Skjul original kontrolltekst"
                        } else {
                            "Vis original kontrolltekst"
                        },
                    )
                }

                if (showOriginal) {
                    Text(
                        selectedFinding.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlFindingCompactRow(
    finding: ControlFinding,
    onClick: () -> Unit,
) {
    val compact = compactControlFinding(finding)
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                controlFindingIcon(finding.severity),
                contentDescription = null,
                tint = controlFindingTint(finding.severity),
                modifier = Modifier.size(20.dp),
            )
        },
        supportingContent = compact.secondary?.let { secondary ->
            {
                Text(
                    secondary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Vis kontrollpunkt",
            )
        },
        content = {
            Text(
                compact.primary,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

private data class CompactControlFinding(
    val primary: String,
    val secondary: String?,
    val checkText: String,
)

private fun compactControlFinding(
    finding: ControlFinding,
): CompactControlFinding = when (finding.title) {
    "Kort hvile mellom arbeidsperioder" -> {
        val detail = finding.detail
        if (
            detail.startsWith("Du har bare ") &&
            " sammenhengende fri." in detail &&
            "Den første arbeidsperioden slutter " in detail &&
            ", og den neste begynner " in detail
        ) {
            val duration = detail
                .substringAfter("Du har bare ")
                .substringBefore(" sammenhengende fri.")
            val firstEnd = detail
                .substringAfter("Den første arbeidsperioden slutter ")
                .substringBefore(", og den neste begynner ")
            val nextStart = detail
                .substringAfter(", og den neste begynner ")
                .substringBefore(". Kontroller")

            CompactControlFinding(
                primary = "$duration hvile",
                secondary = "${compactControlDateTime(firstEnd)} → ${compactControlDateTime(nextStart)}",
                checkText = "Kontroller hvilken arbeidstidsordning som gjelder, og om det kreves kompenserende hvile.",
            )
        } else {
            genericCompactControlFinding(finding)
        }
    }

    "Lang sammenhengende arbeidsperiode" -> {
        val detail = finding.detail
        if (
            detail.startsWith("Arbeidsperioden varer ") &&
            ", fra " in detail &&
            " til " in detail
        ) {
            val duration = detail
                .substringAfter("Arbeidsperioden varer ")
                .substringBefore(", fra ")
            val start = detail
                .substringAfter(", fra ")
                .substringBefore(" til ")
            val end = detail
                .substringAfter(" til ")
                .substringBefore(". Kontroller")

            CompactControlFinding(
                primary = "$duration arbeid",
                secondary = "${compactControlDateTime(start)} → ${compactControlDateTime(end)}",
                checkText = "Kontroller at arbeidsperiodens lengde er tillatt etter arbeidstidsordningen som gjelder for turen.",
            )
        } else {
            genericCompactControlFinding(finding)
        }
    }

    "Mer enn 48 timer i den viste perioden" -> {
        val registered = finding.detail
            .substringAfter("Du har registrert ")
            .substringBefore(" som arbeid i perioden.")
            .takeIf { it != finding.detail }

        CompactControlFinding(
            primary = registered?.let { "$it registrert" } ?: finding.title,
            secondary = "Samlet arbeidstid i den viste perioden",
            checkText = "Kontroller hvilken arbeidstidsordning som gjelder for turen.",
        )
    }

    else -> genericCompactControlFinding(finding)
}

private fun genericCompactControlFinding(
    finding: ControlFinding,
): CompactControlFinding {
    val firstSentence = finding.detail
        .substringBefore(". ")
        .trim()
        .let { if (it.endsWith(".")) it else "$it." }
    val remainder = finding.detail
        .removePrefix(firstSentence)
        .trim()

    return CompactControlFinding(
        primary = finding.title,
        secondary = firstSentence,
        checkText = remainder.ifBlank {
            "Kontroller dette punktet mot arbeidstidsordningen og regelgrunnlaget som gjelder for turen."
        },
    )
}

private fun controlGroupSupportingText(
    group: ControlFindingGroup,
): String = when {
    group.findings.size == 1 ->
        compactControlFinding(group.findings.first()).primary
    group.title == "Kort hvile mellom arbeidsperioder" ->
        "${group.findings.size} perioder"
    group.title == "Lang sammenhengende arbeidsperiode" ->
        "${group.findings.size} perioder"
    else ->
        "${group.findings.size} forhold"
}

private fun controlGroupCountLabel(
    group: ControlFindingGroup,
): String = when {
    group.findings.size == 1 -> "1 forhold"
    group.title == "Kort hvile mellom arbeidsperioder" ->
        "${group.findings.size} perioder"
    group.title == "Lang sammenhengende arbeidsperiode" ->
        "${group.findings.size} perioder"
    else ->
        "${group.findings.size} forhold"
}

private fun controlGroupDescription(
    title: String,
): String = when (title) {
    "Kort hvile mellom arbeidsperioder" ->
        "Perioder med mindre enn 11 timer sammenhengende fri."
    "Lang sammenhengende arbeidsperiode" ->
        "Sammenhengende arbeidsperioder over 13 timer."
    "Mer enn 48 timer i den viste perioden" ->
        "Samlet registrert arbeidstid i perioden."
    else ->
        "Konkrete kontrollpunkter fra den registrerte arbeidsplanen."
}

private fun compactControlDateTime(
    value: String,
): String {
    val replacements = listOf(
        "mandag" to "man.",
        "tirsdag" to "tir.",
        "onsdag" to "ons.",
        "torsdag" to "tor.",
        "fredag" to "fre.",
        "lørdag" to "lør.",
        "søndag" to "søn.",
        "januar" to "jan.",
        "februar" to "feb.",
        "mars" to "mar.",
        "april" to "apr.",
        "mai" to "mai",
        "juni" to "jun.",
        "juli" to "jul.",
        "august" to "aug.",
        "september" to "sep.",
        "oktober" to "okt.",
        "november" to "nov.",
        "desember" to "des.",
        " kl. " to " ",
    )
    return replacements.fold(value) { current, (from, to) ->
        current.replace(from, to)
    }
}

@Composable
private fun controlFindingTint(
    severity: FindingSeverity,
): Color = when (severity) {
    FindingSeverity.OK -> MaterialTheme.colorScheme.primary
    FindingSeverity.CRITICAL -> MaterialTheme.colorScheme.error
    FindingSeverity.REVIEW -> MaterialTheme.colorScheme.secondary
    FindingSeverity.OPEN -> MaterialTheme.colorScheme.tertiary
}

private fun controlFindingIcon(
    severity: FindingSeverity,
): ImageVector = when (severity) {
    FindingSeverity.OK -> Icons.Rounded.CheckCircle
    FindingSeverity.REVIEW, FindingSeverity.CRITICAL -> Icons.Rounded.Warning
    FindingSeverity.OPEN -> Icons.AutoMirrored.Rounded.HelpOutline
}

@Composable
private fun FinalSummaryScreen(
    padding: PaddingValues,
    stepLabel: String,
    onBack: () -> Unit,
    snapshot: FinalizedTripSnapshot?,
    exportState: PdfExportUiState,
    onExport: (FinalizedTripSnapshot, PdfExporter.Variant) -> Unit,
    onExportConsumed: (Long) -> Unit,
    onExportLaunchFailed: (Long, Throwable) -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(exportState.phase, exportState.token) {
        if (exportState.phase == PdfExportPhase.READY) {
            val filePath = exportState.filePath ?: return@LaunchedEffect
            runCatching {
                PdfExporter.sharePrepared(context, filePath)
            }.onSuccess {
                onExportConsumed(exportState.token)
            }.onFailure { error ->
                onExportLaunchFailed(exportState.token, error)
            }
        }
    }
    if (snapshot == null) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ScreenHeader("Oppsummering og dokumentasjon", stepLabel, onBack) }
            item {
                InlineMessage(
                    FindingSeverity.REVIEW,
                    "Beregningen er ikke ferdigstilt",
                    "Gå tilbake til Kontroll og videre hit igjen for å opprette et låst beregningsgrunnlag.",
                )
            }
        }
        return
    }

    val presentation = snapshot.presentation
    val reviewCount = snapshot.findings.count {
        it.severity == FindingSeverity.REVIEW ||
            it.severity == FindingSeverity.CRITICAL
    }
    val registrationOk =
        presentation.rosterUncoveredMinutes == 0L ||
            snapshot.rosterGapConfirmed

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader("Oppsummering og dokumentasjon", stepLabel, onBack) }

        if (snapshot.employerKind != EmployerKind.OSLO_KOMMUNE) {
            item {
                InlineMessage(
                    FindingSeverity.OPEN,
                    "Foreløpig beregningsgrunnlag",
                    "Arbeidsgiverforholdet er ikke bekreftet som Oslo kommune. PDF-en merkes som foreløpig, og samlet arbeidsgiverkostnad er ikke beregnet.",
                )
            }
        } else if (snapshot.payingParty == PayingParty.UNSPECIFIED) {
            item {
                CompactInfoCard(
                    title = "Betalingsscenario: Ikke avklart ennå",
                    body = "Dette er en gyldig status. Beregningen og PDF-en kan ferdigstilles uten å fastsette hvem som rettslig skal bære kostnaden.",
                )
            }
        }

        item {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        when {
                            snapshot.employerKind != EmployerKind.OSLO_KOMMUNE ->
                                "Foreløpig regnegrunnlag"
                            snapshot.rosterComparisonMode ==
                                RosterComparisonMode.USE_NORMAL_ROSTER ->
                                "Beregnet tillegg og særskilt godtgjøring"
                            snapshot.isRuleBasisConfirmed ->
                                "Beregnet lønn og godtgjøring"
                            else ->
                                "Foreløpig regnegrunnlag"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        currency(presentation.paymentBasisAmount),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (snapshot.settlement.usesFullCalculation) {
                            "Betalingsforslaget følger beregningen."
                        } else {
                            "Betalingsforslag ${currency(snapshot.settlement.proposedAmount)} · " +
                                "forskjell ${
                                    currency(
                                        presentation.paymentBasisAmount
                                            .subtract(snapshot.settlement.proposedAmount),
                                    )
                                }."
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // One high-emphasis short document and one medium-emphasis full document.
        item { MethodSectionHeading("Dokumentasjon") }
        item {
            Text(
                "Velg en kort oppsummering eller full dokumentasjon med beregning, regler og kilder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val exportBusy = exportState.phase == PdfExportPhase.CREATING_SHORT ||
            exportState.phase == PdfExportPhase.CREATING_FULL ||
            exportState.phase == PdfExportPhase.READY

        item {
            Button(
                enabled = !exportBusy,
                onClick = { onExport(snapshot, PdfExporter.Variant.SHORT) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (exportState.phase == PdfExportPhase.CREATING_SHORT) "Lager kort oppsummering…" else "Kort oppsummering",
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Beløp og status · PDF", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        item {
            OutlinedButton(
                enabled = !exportBusy,
                onClick = { onExport(snapshot, PdfExporter.Variant.FULL) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (exportState.phase == PdfExportPhase.CREATING_FULL) "Lager full dokumentasjon…" else "Full dokumentasjon",
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Beregning, regler og kilder · PDF", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (exportState.phase == PdfExportPhase.ERROR) {
            item {
                InlineMessage(
                    FindingSeverity.CRITICAL,
                    "Kunne ikke lage PDF",
                    exportState.message ?: "Prøv igjen.",
                )
            }
        }

        item {
            Text(
                "Full dokumentasjon inkluderer planbasis, grunnturnus når relevant, " +
                    "arbeidsgivers arbeidsplan når registrert, arbeid på turen, " +
                    "beregningsspesifikasjon, dag-for-dag-kontroll, " +
                    "arbeidstidsvarsler, regler, kilder, satser og versjoner.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { MethodSectionHeading("Status") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                FinalSummaryStatusRow(
                    label = "Arbeidsgiver",
                    value = employerLabel(snapshot.employerKind),
                    resolved = snapshot.employerKind != EmployerKind.UNSPECIFIED,
                )
                HorizontalDivider(modifier = Modifier.padding(start = 38.dp))

                if (
                    snapshot.employerKind == EmployerKind.OSLO_KOMMUNE &&
                    snapshot.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER
                ) {
                    FinalSummaryStatusRow(
                        label = "Planbasis",
                        value = when (snapshot.workPlanBasis) {
                            TripWorkPlanBasis.NORMAL_ROSTER_APPLIES ->
                                "Vanlig grunnturnus gjelder"
                            TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN ->
                                "Arbeidsgiver har fastsatt egen plan"
                            TripWorkPlanBasis.NOT_CLARIFIED ->
                                "Ikke lagret i eldre ferdigstilling"
                        },
                        resolved = snapshot.workPlanBasis != TripWorkPlanBasis.NOT_CLARIFIED,
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 38.dp))

                    if (snapshot.workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN) {
                        FinalSummaryStatusRow(
                            label = "Arbeidsgivers plan",
                            value = when (snapshot.holidayWorkPlanStatus) {
                                HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED ->
                                    "Godkjent · minst 14 dagers varsel"
                                HolidayWorkPlanStatus.NOT_APPROVED_OR_LATE ->
                                    "Ikke godkjent / kortere varsel"
                                HolidayWorkPlanStatus.NOT_CLARIFIED ->
                                    "Ikke avklart"
                            },
                            resolved = snapshot.holidayWorkPlanStatus != HolidayWorkPlanStatus.NOT_CLARIFIED,
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 38.dp))
                    }
                }

                FinalSummaryStatusRow(
                    label = "Betalingsscenario",
                    value = payingPartyLabel(snapshot.payingParty),
                    resolved = snapshot.payingParty != PayingParty.UNSPECIFIED,
                    informationalWhenOpen = true,
                )
                HorizontalDivider(modifier = Modifier.padding(start = 38.dp))

                FinalSummaryStatusRow(
                    label = "Lønnsopplysninger",
                    value = if (snapshot.payslipChecked) {
                        "Kontrollert"
                    } else {
                        "Må kontrolleres"
                    },
                    resolved = snapshot.payslipChecked,
                )
                HorizontalDivider(modifier = Modifier.padding(start = 38.dp))

                FinalSummaryStatusRow(
                    label = "Beregningsregler",
                    value = if (snapshot.unresolvedRules.isEmpty()) {
                        "Ingen åpne"
                    } else {
                        "${snapshot.unresolvedRules.size} må avklares"
                    },
                    resolved = snapshot.unresolvedRules.isEmpty(),
                )
                HorizontalDivider(modifier = Modifier.padding(start = 38.dp))

                if (snapshot.hasMultipleTariffContexts) {
                    FinalSummaryStatusRow(
                        label = "Tariffgrunnlag",
                        value = "${snapshot.tariffContexts.size} perioder",
                        resolved = true,
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 38.dp))
                }

                FinalSummaryStatusRow(
                    label = "Registrering",
                    value = when {
                        presentation.rosterUncoveredMinutes == 0L ->
                            "Ingen feil"
                        snapshot.rosterGapConfirmed ->
                            "Kontrollert"
                        else ->
                            "Må kontrolleres"
                    },
                    resolved = registrationOk,
                )
            }
        }

        if (reviewCount > 0) {
            item {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBack),
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    supportingContent = {
                        Text(
                            "Se kontrollpunktene før dokumentet brukes.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = "Tilbake til Kontroll",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    content = {
                        Text(
                            "$reviewCount arbeidstidsforhold bør vurderes",
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }
        }

        item { MethodSectionHeading("Dokumentasjon") }

        if (!snapshot.settlement.usesFullCalculation) {
            item {
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    supportingContent = {
                        Text(
                            "Avtalt ${currency(snapshot.settlement.proposedAmount)} · " +
                                "beregnet ${currency(presentation.paymentBasisAmount)}. " +
                                snapshot.settlement.reason,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    content = {
                        Text(
                            "Avtalt betalingsforslag",
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }
        }

        if (snapshot.rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            item {
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    },
                    supportingContent = {
                        Text(
                            "${minutesUi(presentation.rosterMinutes)} overlapper turen.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    content = {
                        Text(
                            "Grunnturnusen følger med i fullt grunnlag",
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }
        }

        item {
            Text(
                "Beregning-ID: ${snapshot.id}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FinalSummaryStatusRow(
    label: String,
    value: String,
    resolved: Boolean,
    informationalWhenOpen: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            when {
                resolved -> Icons.Rounded.CheckCircle
                informationalWhenOpen -> Icons.Rounded.Info
                else -> Icons.Rounded.Warning
            },
            contentDescription = null,
            tint = when {
                resolved -> MaterialTheme.colorScheme.primary
                informationalWhenOpen -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(20.dp),
        )
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            value,
            modifier = Modifier.widthIn(max = 170.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusRow(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
        )
        Text(text, modifier = Modifier.weight(1f))
    }
}

private fun minutesUi(value: Long): String = buildString {
    append(value / 60)
    append(" t")
    if (value % 60L != 0L) {
        append(" ")
        append(value % 60)
        append(" min")
    }
}

@Composable
private fun FlowBottomBar(nextLabel: String, nextEnabled: Boolean, onBack: () -> Unit, onNext: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tilbake")
            }
            Button(
                onClick = onNext,
                enabled = nextEnabled,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(nextLabel, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun RosterDayCard(
    date: LocalDate,
    shifts: List<ShiftDefinition>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.width(74.dp)) {
            Text(dayName(date), fontWeight = FontWeight.Bold)
            Text(
                shortDate(date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (shifts.isEmpty()) {
            Column(Modifier.weight(1f)) {
                Text("Ikke registrert", fontWeight = FontWeight.SemiBold)
                Text(
                    "Trykk for å registrere",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val first = shifts.first()
            val badge = if (shifts.size == 1) first.code else "${shifts.size}×"
            Surface(
                modifier = Modifier
                    .width(54.dp)
                    .heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.medium,
                color = shiftColor(first.category),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        badge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    if (shifts.size == 1) first.label else "${shifts.size} vakter",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    rosterDaySummary(shifts),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TripDayCard(
    date: LocalDate,
    shifts: List<ShiftDefinition>,
    blocks: List<DayProjectedBlock>,
    roster: Map<LocalDate, String>,
    plans: Map<LocalDate, List<PlannedBlock>>,
    comparisonBasis: TripWorkPlanBasis?,
    baselinePlans: Map<LocalDate, List<PlannedBlock>>,
    derivedBlocks: List<DayProjectedBlock>?,
    issues: List<PlanValidationIssue>,
    onEditPeriod: (LocalDate, Int) -> Unit,
    onAddPeriod: (LocalDate) -> Unit,
) {
    val comparison = workComparisonVisual(
        date = date,
        basis = comparisonBasis,
        roster = roster,
        actualBlocks = blocks,
        employerPlan = baselinePlans,
        derivedBlocks = derivedBlocks,
    )
    val editableSources = blocks.mapNotNull { projected ->
        sourcePlanIndex(projected, plans)?.let { sourceIndex ->
            projected.sourceDate to sourceIndex
        }
    }.distinct()
    val singleEditableSource = editableSources.singleOrNull()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (BuildConfig.DEBUG) Modifier.testTag("workplan-day-$date") else Modifier
            )
            .then(
                when {
                    blocks.isEmpty() ->
                        Modifier.clickable(role = Role.Button) { onAddPeriod(date) }
                    singleEditableSource != null ->
                        Modifier.clickable(role = Role.Button) {
                            onEditPeriod(
                                singleEditableSource.first,
                                singleEditableSource.second,
                            )
                        }
                    else ->
                        Modifier
                },
            ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "${dayName(date)} ${shortDate(date)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    when {
                        comparison != null -> {
                            Text(
                                "${comparison.baselineLabel} ${comparisonBaselineSummary(date, comparison.baselineSegments)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        roster.isNotEmpty() -> {
                            Text(
                                "Grunnturnus ${if (shifts.isEmpty()) "fri / ikke satt" else rosterDaySummary(shifts)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(
                    if (blocks.isEmpty()) "Ingen perioder"
                    else "${blocks.size} ${if (blocks.size == 1) "periode" else "perioder"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (comparison != null) {
                WorkComparisonTimeline(date, comparison)
            } else {
                DayTimeline(date, blocks, plans, onEditPeriod)
            }

            if (blocks.isEmpty()) {
                Text(
                    "Trykk på dagen eller «Legg til periode» for å registrere.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                blocks.forEach { projected ->
                    PlanActivityRow(
                        date = date,
                        projected = projected,
                        plans = plans,
                        onEditPeriod = onEditPeriod,
                    )
                }
                comparison?.let { value ->
                    if (value.extraMinutes > 0L) {
                        ExtraWorkSummaryChip(value.extraMinutes)
                    }
                }
            }

            issues.forEach { issue ->
                InlineMessage(FindingSeverity.CRITICAL, issue.title, issue.detail)
            }
        }
    }
}

@Composable
private fun PlanActivityRow(
    date: LocalDate,
    projected: DayProjectedBlock,
    plans: Map<LocalDate, List<PlannedBlock>>,
    onEditPeriod: (LocalDate, Int) -> Unit,
) {
    val block = projected.block
    val sourceIndex = sourcePlanIndex(projected, plans)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (sourceIndex != null) {
                    Modifier.clickable { onEditPeriod(projected.sourceDate, sourceIndex) }
                } else {
                    Modifier
                },
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.size(9.dp),
            shape = CircleShape,
            color = timeKindAccentColor(block.kind),
            content = {},
        )
        Text(
            compactTimeKindLabel(projected),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
        )
        Text(
            projectedTimeLabel(date, projected, plans),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        if (sourceIndex != null) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Rediger periode",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun sourcePlanIndex(
    projected: DayProjectedBlock,
    plans: Map<LocalDate, List<PlannedBlock>>,
): Int? {
    val projectedBlock = projected.block
    val index = plans[projected.sourceDate].orEmpty().indexOfFirst { planned ->
        val source = planned.toWorkBlock(projected.sourceDate)
        source.kind == projectedBlock.kind &&
            source.travelNoticeStatus == projectedBlock.travelNoticeStatus &&
            source.travelDutyStatus == projectedBlock.travelDutyStatus &&
            !source.start.isAfter(projectedBlock.start) &&
            !source.end.isBefore(projectedBlock.end)
    }
    return index.takeIf { it >= 0 }
}

private fun compactTimeKindLabel(projected: DayProjectedBlock): String {
    val block = projected.block
    val base = when (block.kind) {
        TimeKind.ACTIVE_WORK -> "Aktivt arbeid"
        TimeKind.ACTIVE_NIGHT_WATCH -> "Nattevakt"
        TimeKind.RESTING_NIGHT_WATCH -> "Hvilende nattevakt"
        TimeKind.TRAVEL_WITH_RESPONSIBILITY -> "Reise med ansvar"
        TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
        TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
        TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED -> "Reise uten ansvar"
        TimeKind.TRAVEL_UNCERTAIN -> "Reise · ansvar må avklares"
        TimeKind.ACTIVE_EVENT_ON_RESTING -> "Aktivt arbeid under hvilende natt"
    }
    val duty = if (block.kind.isTravelWithoutResponsibility()) {
        when (block.travelDutyStatus) {
            TravelDutyStatus.ON_DUTY -> " · på vakt"
            TravelDutyStatus.OFF_DUTY -> " · ikke på vakt"
            TravelDutyStatus.NOT_CLARIFIED -> " · vaktstatus ikke avklart"
        }
    } else {
        ""
    }
    val label = "$base$duty"
    return if (projected.continuesFromPreviousDay) {
        "$label (fra ${dayName(projected.sourceDate).lowercase(norwegian)})"
    } else {
        label
    }
}

private fun projectedTimeLabel(
    date: LocalDate,
    projected: DayProjectedBlock,
    plans: Map<LocalDate, List<PlannedBlock>>,
): String {
    val block = projected.block
    if (projected.continuesIntoNextDay) {
        val source = plans[projected.sourceDate].orEmpty().firstOrNull { planned ->
            samePeriodCategory(planned.kind, block.kind) &&
                planned.start == block.start.toLocalTime()
        }
        if (source != null) {
            return "${timeFormat.format(source.start)}–${timeFormat.format(source.end)} → ${dayName(date.plusDays(1)).lowercase(norwegian)}"
        }
    }

    val dayStart = date.atStartOfDay()
    val dayEnd = date.plusDays(1).atStartOfDay()
    val start = if (block.start == dayStart) "00:00" else timeFormat.format(block.start.toLocalTime())
    val end = if (block.end == dayEnd) "24:00" else timeFormat.format(block.end.toLocalTime())
    return "$start–$end"
}

private data class WorkComparisonVisual(
    val baselineLabel: String,
    val extraReference: String,
    val baselineSegments: List<Pair<LocalDateTime, LocalDateTime>>,
    val insideSegments: List<Pair<LocalDateTime, LocalDateTime>>,
    val extraSegments: List<Pair<LocalDateTime, LocalDateTime>>,
) {
    val actualMinutes: Long
        get() = (insideSegments + extraSegments).sumOf { (start, end) ->
            ChronoUnit.MINUTES.between(start, end)
        }

    val extraMinutes: Long
        get() = extraSegments.sumOf { (start, end) ->
            ChronoUnit.MINUTES.between(start, end)
        }
}

private fun workComparisonVisual(
    date: LocalDate,
    basis: TripWorkPlanBasis?,
    roster: Map<LocalDate, String>,
    actualBlocks: List<DayProjectedBlock>,
    employerPlan: Map<LocalDate, List<PlannedBlock>>,
    derivedBlocks: List<DayProjectedBlock>?,
): WorkComparisonVisual? {
    val actual = mergeVisualIntervals(
        actualBlocks
            .map { it.block }
            .filter(::countsAsComparisonWork)
            .map { it.start to it.end },
    )

    return when (basis) {
        TripWorkPlanBasis.NORMAL_ROSTER_APPLIES -> {
            val baseline = rosterVisualIntervals(date, roster)
            val extra = actual.flatMap { interval ->
                subtractVisualInterval(interval, baseline)
            }.let(::mergeVisualIntervals)
            val inside = intersectionVisualIntervals(actual, baseline)
            WorkComparisonVisual(
                baselineLabel = "Grunnturnus",
                extraReference = "grunnturnusen",
                baselineSegments = baseline,
                insideSegments = inside,
                extraSegments = extra,
            )
        }

        TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN -> {
            val baseline = mergeVisualIntervals(
                TripPlanEngine.projectVisibleDay(date, employerPlan)
                    .map { it.block }
                    .filter(::countsAsComparisonDuty)
                    .map { it.start to it.end },
            )
            val derived = derivedBlocks.orEmpty()
                .map { it.block }
                .filter(::countsAsComparisonWork)
            val inside = mergeVisualIntervals(
                derived
                    .filter {
                        it.holidayWorkPlanRelation == HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN
                    }
                    .map { it.start to it.end },
            )
            val extra = mergeVisualIntervals(
                derived
                    .filter {
                        it.holidayWorkPlanRelation == HolidayWorkPlanRelation.BEYOND_HOLIDAY_WORK_PLAN
                    }
                    .map { it.start to it.end },
            )
            WorkComparisonVisual(
                baselineLabel = "Arbeidsgivers plan",
                extraReference = "arbeidsgivers plan",
                baselineSegments = baseline,
                insideSegments = inside,
                extraSegments = extra,
            )
        }

        TripWorkPlanBasis.NOT_CLARIFIED,
        null,
        -> null
    }
}

private fun countsAsComparisonWork(block: WorkBlock): Boolean =
    block.kind == TimeKind.ACTIVE_WORK ||
        block.kind == TimeKind.ACTIVE_NIGHT_WATCH ||
        block.kind == TimeKind.TRAVEL_WITH_RESPONSIBILITY ||
        (
            block.kind.isTravelWithoutResponsibility() &&
                block.travelDutyStatus == TravelDutyStatus.ON_DUTY
            )

private fun countsAsComparisonDuty(block: WorkBlock): Boolean =
    countsAsComparisonWork(block) ||
        block.kind == TimeKind.RESTING_NIGHT_WATCH

private fun rosterVisualIntervals(
    date: LocalDate,
    roster: Map<LocalDate, String>,
): List<Pair<LocalDateTime, LocalDateTime>> {
    val dayStart = date.atStartOfDay()
    val dayEnd = date.plusDays(1).atStartOfDay()
    val sourceDates = listOf(date.minusDays(1), date)

    return mergeVisualIntervals(
        sourceDates.flatMap { sourceDate ->
            RosterEntryCodec.decode(roster[sourceDate]).mapNotNull { shift ->
                val start = shift.start ?: return@mapNotNull null
                val end = shift.end ?: return@mapNotNull null
                if (shift.category == ShiftCategory.OFF) return@mapNotNull null

                val startDateTime = LocalDateTime.of(sourceDate, start)
                val endDate = if (end.isAfter(start)) sourceDate else sourceDate.plusDays(1)
                val endDateTime = LocalDateTime.of(endDate, end)
                clipVisualInterval(startDateTime to endDateTime, dayStart, dayEnd)
            }
        },
    )
}

private fun clipVisualInterval(
    interval: Pair<LocalDateTime, LocalDateTime>,
    lower: LocalDateTime,
    upper: LocalDateTime,
): Pair<LocalDateTime, LocalDateTime>? {
    val start = if (interval.first.isAfter(lower)) interval.first else lower
    val end = if (interval.second.isBefore(upper)) interval.second else upper
    return (start to end).takeIf { start.isBefore(end) }
}

private fun mergeVisualIntervals(
    intervals: List<Pair<LocalDateTime, LocalDateTime>>,
): List<Pair<LocalDateTime, LocalDateTime>> {
    if (intervals.isEmpty()) return emptyList()
    val sorted = intervals
        .filter { (start, end) -> start.isBefore(end) }
        .sortedBy { it.first }
    if (sorted.isEmpty()) return emptyList()

    val merged = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
    var currentStart = sorted.first().first
    var currentEnd = sorted.first().second

    sorted.drop(1).forEach { (start, end) ->
        if (!start.isAfter(currentEnd)) {
            if (end.isAfter(currentEnd)) currentEnd = end
        } else {
            merged += currentStart to currentEnd
            currentStart = start
            currentEnd = end
        }
    }
    merged += currentStart to currentEnd
    return merged
}

private fun intersectionVisualIntervals(
    actual: List<Pair<LocalDateTime, LocalDateTime>>,
    baseline: List<Pair<LocalDateTime, LocalDateTime>>,
): List<Pair<LocalDateTime, LocalDateTime>> =
    mergeVisualIntervals(
        actual.flatMap { (actualStart, actualEnd) ->
            baseline.mapNotNull { (baselineStart, baselineEnd) ->
                val start = if (actualStart.isAfter(baselineStart)) actualStart else baselineStart
                val end = if (actualEnd.isBefore(baselineEnd)) actualEnd else baselineEnd
                (start to end).takeIf { start.isBefore(end) }
            }
        },
    )

private fun subtractVisualInterval(
    actual: Pair<LocalDateTime, LocalDateTime>,
    baseline: List<Pair<LocalDateTime, LocalDateTime>>,
): List<Pair<LocalDateTime, LocalDateTime>> {
    var remaining = listOf(actual)
    baseline.forEach { (baselineStart, baselineEnd) ->
        remaining = remaining.flatMap { (start, end) ->
            if (!baselineStart.isBefore(end) || !baselineEnd.isAfter(start)) {
                listOf(start to end)
            } else {
                buildList {
                    if (start.isBefore(baselineStart)) {
                        add(start to minOf(end, baselineStart))
                    }
                    if (baselineEnd.isBefore(end)) {
                        add(maxOf(start, baselineEnd) to end)
                    }
                }.filter { (partStart, partEnd) -> partStart.isBefore(partEnd) }
            }
        }
    }
    return remaining
}

private fun comparisonBaselineSummary(
    date: LocalDate,
    intervals: List<Pair<LocalDateTime, LocalDateTime>>,
): String {
    if (intervals.isEmpty()) return "fri / ikke satt"
    val dayStart = date.atStartOfDay()
    val dayEnd = date.plusDays(1).atStartOfDay()
    return intervals.joinToString(" + ") { (start, end) ->
        val startLabel = if (start == dayStart) "00:00" else timeFormat.format(start.toLocalTime())
        val endLabel = if (end == dayEnd) "24:00" else timeFormat.format(end.toLocalTime())
        "$startLabel–$endLabel"
    }
}

@Composable
private fun WorkComparisonLegend() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            WorkComparisonLegendItem(
                color = MaterialTheme.colorScheme.primary,
                label = "Avtalt tid",
            )
            WorkComparisonLegendItem(
                color = MaterialTheme.colorScheme.error,
                label = "Ekstra arbeid",
            )
        }
    }
}

@Composable
private fun WorkComparisonLegendItem(
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = color,
            content = {},
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExtraWorkSummaryChip(minutes: Long) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.58f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.34f),
        ),
    ) {
        Text(
            "+ ${minutesLabel(minutes)} utenfor avtalt tid",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun WorkComparisonTimeline(
    date: LocalDate,
    comparison: WorkComparisonVisual,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val baselineColor = MaterialTheme.colorScheme.primary
    val ordinaryColor = MaterialTheme.colorScheme.primary
    val extraColor = MaterialTheme.colorScheme.error
    val dayStart = date.atStartOfDay()

    fun fraction(value: LocalDateTime): Float =
        (ChronoUnit.MINUTES.between(dayStart, value).toFloat() / 1440f).coerceIn(0f, 1f)

    @Composable
    fun TimelineLane(
        label: String,
        baseline: Boolean,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                label,
                modifier = Modifier.weight(0.30f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Canvas(
                Modifier
                    .weight(0.70f)
                    .height(8.dp),
            ) {
                val radius = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(trackColor, size = size, cornerRadius = radius)

                if (baseline) {
                    comparison.baselineSegments.forEach { (start, end) ->
                        val left = size.width * fraction(start)
                        val right = size.width * fraction(end)
                        if (right > left) {
                            drawRoundRect(
                                color = baselineColor,
                                topLeft = Offset(left, 0f),
                                size = Size(right - left, size.height),
                                cornerRadius = radius,
                            )
                        }
                    }
                } else {
                    comparison.insideSegments.forEach { (start, end) ->
                        val left = size.width * fraction(start)
                        val right = size.width * fraction(end)
                        if (right > left) {
                            drawRoundRect(
                                color = ordinaryColor,
                                topLeft = Offset(left, 0f),
                                size = Size(right - left, size.height),
                                cornerRadius = radius,
                            )
                        }
                    }
                    comparison.extraSegments.forEach { (start, end) ->
                        val left = size.width * fraction(start)
                        val right = size.width * fraction(end)
                        if (right > left) {
                            drawRoundRect(
                                color = extraColor,
                                topLeft = Offset(left, 0f),
                                size = Size(right - left, size.height),
                                cornerRadius = radius,
                            )
                        }
                    }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TimelineLane("Avtalt tid", baseline = true)
        TimelineLane("Arbeid på turen", baseline = false)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(0.30f))
            Row(
                modifier = Modifier.weight(0.70f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("00", "06", "12", "18", "24").forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayTimeline(
    date: LocalDate,
    blocks: List<DayProjectedBlock>,
    plans: Map<LocalDate, List<PlannedBlock>>,
    onEditPeriod: (LocalDate, Int) -> Unit,
) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val dayStart = date.atStartOfDay()
    val coloredBlocks = blocks.map { projected ->
        projected.block to timeKindAccentColor(projected.block.kind)
    }
    val lanes = coloredBlocks.size.coerceAtLeast(1)
    val timelineHeight = (lanes * 10 + (lanes - 1) * 3).dp

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(timelineHeight)
                .then(
                    if (blocks.isNotEmpty()) {
                        Modifier.pointerInput(date, blocks, plans) {
                            detectTapGestures { tap ->
                                if (size.width <= 0 || size.height <= 0) {
                                    return@detectTapGestures
                                }

                                // Each projected period occupies one visual lane. Use the
                                // full lane height as the vertical hit target, but only
                                // activate when the tap is on (or just beside) the
                                // colored time segment itself.
                                val laneBand = size.height.toFloat() / lanes.toFloat()
                                val laneIndex = (tap.y / laneBand)
                                    .toInt()
                                    .coerceIn(0, blocks.lastIndex)
                                val projected = blocks[laneIndex]
                                val block = projected.block

                                val startMinutes = ChronoUnit.MINUTES
                                    .between(dayStart, block.start)
                                    .toFloat()
                                val endMinutes = ChronoUnit.MINUTES
                                    .between(dayStart, block.end)
                                    .toFloat()
                                val startX = size.width * (startMinutes / 1440f)
                                val endX = size.width * (endMinutes / 1440f)

                                // Give the thin timeline segment a little horizontal
                                // forgiveness without making the empty timeline itself
                                // an edit target.
                                val hitPadding = 8.dp.toPx()
                                if (tap.x >= startX - hitPadding && tap.x <= endX + hitPadding) {
                                    val sourceIndex = sourcePlanIndex(projected, plans)
                                    if (sourceIndex != null) {
                                        onEditPeriod(projected.sourceDate, sourceIndex)
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            val laneHeight = 10.dp.toPx()
            val laneGap = 3.dp.toPx()
            repeat(lanes) { lane ->
                val y = lane * (laneHeight + laneGap)
                drawRoundRect(
                    color = baseColor,
                    topLeft = Offset(0f, y),
                    size = Size(size.width, laneHeight),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                )
            }
            coloredBlocks.forEachIndexed { index, (block, blockColor) ->
                val startMinutes = ChronoUnit.MINUTES.between(dayStart, block.start).toFloat()
                val endMinutes = ChronoUnit.MINUTES.between(dayStart, block.end).toFloat()
                val x = size.width * (startMinutes / 1440f)
                val width = size.width * ((endMinutes - startMinutes) / 1440f)
                val y = index * (laneHeight + laneGap)
                drawRoundRect(
                    color = blockColor,
                    topLeft = Offset(x, y),
                    size = Size(width.coerceAtLeast(2f), laneHeight),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


@Composable
private fun RosterCodeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberTextFieldState(initialText = value)
    val codeInputTransformation = remember {
        InputTransformation.maxLength(RosterEntryCodec.MAX_CODE_LENGTH)
    }

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .collectLatest { current ->
                onValueChange(current)
            }
    }
    LaunchedEffect(value) {
        if (state.text.toString() != value) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }

    OutlinedTextField(
        state = state,
        label = { Text("Vaktkode") },
        supportingText = {
            Text("Obligatorisk · maks ${RosterEntryCodec.MAX_CODE_LENGTH} tegn")
        },
        inputTransformation = codeInputTransformation,
        lineLimits = TextFieldLineLimits.SingleLine,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RosterDayEditorSheet(
    date: LocalDate,
    currentValue: String?,
    templates: List<ShiftDefinition>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    val currentShifts = remember(currentValue) { RosterEntryCodec.decode(currentValue) }
    val currentFree = currentShifts.singleOrNull()?.takeIf { it.category == ShiftCategory.OFF }
    var freeSelected by remember(date, currentValue) { mutableStateOf(currentFree != null) }
    var freeCode by remember(date, currentValue) { mutableStateOf(currentFree?.code.orEmpty()) }
    var weeklyOff by remember(date, currentValue) { mutableStateOf(currentFree?.weeklyOff) }
    var workDrafts by remember(date, currentValue) {
        mutableStateOf(
            currentShifts
                .filter { it.category != ShiftCategory.OFF }
                .map { RosterWorkDraft(it.code, it.start, it.end) }
                .ifEmpty { listOf(RosterWorkDraft("", null, null)) },
        )
    }

    val workValid = workDrafts.isNotEmpty() && workDrafts.all {
        it.code.isNotBlank() &&
            it.code.length <= RosterEntryCodec.MAX_CODE_LENGTH &&
            it.start != null &&
            it.end != null
    }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val canSave = if (freeSelected) {
        freeCode.isNotBlank() &&
            freeCode.length <= RosterEntryCodec.MAX_CODE_LENGTH &&
            weeklyOff != null
    } else {
        workValid
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "${dayName(date)} ${shortDate(date)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "Hva står i grunnturnusen?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.selectableGroup(),
                    ) {
                        MethodRadioRow(
                            selected = !freeSelected,
                            title = "Arbeidsvakt",
                            supporting = "Registrer vaktkode og faktisk fra-/til-tid.",
                            onClick = { freeSelected = false },
                        )
                        HorizontalDivider()
                        MethodRadioRow(
                            selected = freeSelected,
                            title = "Fri",
                            supporting = "Registrer vaktkoden som står i turnusen.",
                            onClick = { freeSelected = true },
                        )
                    }
                }
            }

            if (!freeSelected) {
                if (templates.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Tidligere brukt",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Trykk for å fylle inn vaktkode og klokkeslett.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                templates.take(8).forEach { template ->
                                    AssistChip(
                                        onClick = {
                                            workDrafts = applyRosterTemplateToPrimaryDraft(workDrafts, template)
                                        },
                                        label = { Text("${template.code} · ${shiftTimeLabel(template)}") },
                                    )
                                }
                            }
                        }
                    }
                }

                items(workDrafts.indices.toList(), key = { "roster-work-$it" }) { index ->
                    val draft = workDrafts[index]
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (workDrafts.size == 1) "Vakt" else "Vakt ${index + 1}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                if (workDrafts.size > 1) {
                                    IconButton(
                                        onClick = {
                                            workDrafts = workDrafts.toMutableList().also { it.removeAt(index) }
                                        },
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Fjern vakt")
                                    }
                                }
                            }
                            RosterCodeTextField(
                                value = draft.code,
                                onValueChange = { value ->
                                    workDrafts = updateRosterWorkDraftAt(workDrafts, index) {
                                        it.copy(code = value)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RosterTimeInputButton(
                                    label = "Fra",
                                    value = draft.start,
                                    onValue = { value ->
                                        workDrafts = updateRosterWorkDraftAt(workDrafts, index) {
                                            it.copy(start = value)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                RosterTimeInputButton(
                                    label = "Til",
                                    value = draft.end,
                                    onValue = { value ->
                                        workDrafts = updateRosterWorkDraftAt(workDrafts, index) {
                                            it.copy(end = value)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (draft.start != null && draft.end != null && !draft.end.isAfter(draft.start)) {
                                Text(
                                    "Slutttiden tolkes som neste døgn.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

            } else {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RosterCodeTextField(
                                value = freeCode,
                                onValueChange = { freeCode = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Fridagstype",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "Velg hva fridagen betyr i turnusen, uavhengig av vaktkoden.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.selectableGroup(),
                                    ) {
                                        MethodRadioRow(
                                            selected = weeklyOff == true,
                                            title = "Ukentlig fridag",
                                            supporting = "Brukes blant annet ved overtidsvurdering.",
                                            onClick = { weeklyOff = true },
                                        )
                                        HorizontalDivider()
                                        MethodRadioRow(
                                            selected = weeklyOff == false,
                                            title = "Annen eller ekstra fridag",
                                            supporting = "Fridag som ikke er den ukentlige fridagen.",
                                            onClick = { weeklyOff = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val shifts = if (freeSelected) {
                                listOf(
                                    RosterEntryCodec.manualFree(
                                        freeCode,
                                        requireNotNull(weeklyOff),
                                    ),
                                )
                            } else {
                                workDrafts.map { draft ->
                                    RosterEntryCodec.manualWork(
                                        draft.code,
                                        requireNotNull(draft.start),
                                        requireNotNull(draft.end),
                                    )
                                }
                            }
                            onSave(RosterEntryCodec.encode(shifts))
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (!freeSelected && workDrafts.size > 1) "Lagre vakter" else "Lagre")
                    }
                    if (currentValue != null) {
                        TextButton(
                            onClick = onClear,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Fjern registrering", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RosterTimeInputButton(
    label: String,
    value: LocalTime?,
    onValue: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { open = true },
        modifier = modifier.heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value?.let(timeFormat::format) ?: "Velg tid",
                style = MaterialTheme.typography.titleMedium,
                color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    if (open) {
        val initial = value ?: LocalTime.of(7, 0)
        val state = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    enabled = state.isInputValid,
                    onClick = {
                        onValue(LocalTime.of(state.hour, state.minute))
                        open = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Avbryt") } },
            title = { Text("Velg klokkeslett") },
            modeToggleButton = null,
        ) {
            NorwegianMaterialLocale {
                TimeInput(state = state)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlanFabMenu(
    onAction: (PlanFabAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }

    val osloFabContainerColor = MaterialTheme.colorScheme.secondary
    val osloFabContentColor = MaterialTheme.colorScheme.onSecondary

    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ExtendedFloatingActionButton(
                onClick = { expanded = !expanded },
                icon = {
                    Icon(
                        if (expanded) Icons.Rounded.Close else Icons.Rounded.Add,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(
                        if (expanded) "Lukk" else "Legg til periode",
                        fontWeight = FontWeight.Bold,
                    )
                },
                containerColor = osloFabContainerColor,
                contentColor = osloFabContentColor,
            )
        },
    ) {
        fun choose(action: PlanFabAction) {
            expanded = false
            onAction(action)
        }

        FloatingActionButtonMenuItem(
            onClick = { choose(PlanFabAction.ACTIVE_WORK) },
            icon = { Icon(Icons.Rounded.Work, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondary) },
            text = { Text("Arbeid", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondary) },
            containerColor = MaterialTheme.colorScheme.secondary,
        )
        FloatingActionButtonMenuItem(
            onClick = { choose(PlanFabAction.TRAVEL) },
            icon = { Icon(Icons.Rounded.DirectionsCar, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondary) },
            text = { Text("Reise", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondary) },
            containerColor = MaterialTheme.colorScheme.secondary,
        )
        FloatingActionButtonMenuItem(
            onClick = { choose(PlanFabAction.RESTING_NIGHT) },
            icon = { Icon(Icons.Rounded.Bedtime, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondary) },
            text = { Text("Hvilende natt", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondary) },
            containerColor = MaterialTheme.colorScheme.secondary,
        )
        FloatingActionButtonMenuItem(
            onClick = { choose(PlanFabAction.OTHER) },
            icon = { Icon(Icons.Rounded.MoreHoriz, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondary) },
            text = { Text("Annet", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondary) },
            containerColor = MaterialTheme.colorScheme.secondary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanPeriodEditorSheet(
    title: String,
    entryMode: PlanEntryMode,
    fundingMode: FundingMode,
    initialDate: LocalDate,
    initialBlock: PlannedBlock?,
    initialKind: TimeKind?,
    dates: List<LocalDate>,
    plans: Map<LocalDate, List<PlannedBlock>>,
    tripStart: LocalDateTime,
    tripEnd: LocalDateTime,
    onDismiss: () -> Unit,
    onSave: (LocalDate, PlannedBlock) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val fallbackBlock = remember(initialDate, initialKind) {
        initialKind?.let { defaultPlannedBlockWithinTrip(it, initialDate, tripStart, tripEnd) }
            ?: PlannedBlock(TimeKind.ACTIVE_NIGHT_WATCH, LocalTime.of(21, 30), LocalTime.of(7, 30))
    }
    val seed = initialBlock ?: fallbackBlock

    var selectedDate by remember(initialDate, initialBlock) { mutableStateOf(initialDate) }
    var selectedKind by remember(initialBlock, initialKind) { mutableStateOf(initialBlock?.kind ?: initialKind) }
    var start by remember(initialBlock, initialKind) { mutableStateOf(seed.start) }
    var end by remember(initialBlock, initialKind) { mutableStateOf(seed.end) }
    var travelNoticeStatus by remember(initialBlock, initialKind) {
        mutableStateOf(initialBlock?.travelNoticeStatus ?: TravelNoticeStatus.NOT_CLARIFIED)
    }
    var travelDutyStatus by remember(initialBlock, initialKind) {
        mutableStateOf(initialBlock?.travelDutyStatus ?: TravelDutyStatus.NOT_CLARIFIED)
    }
    var dateMenuOpen by remember { mutableStateOf(false) }
    var typeMenuOpen by remember { mutableStateOf(false) }

    val kind = selectedKind
    val hasRestingNight = plans[selectedDate].orEmpty().any { it.kind == TimeKind.RESTING_NIGHT_WATCH } ||
        kind == TimeKind.RESTING_NIGHT_WATCH
    val travelClassified = kind != TimeKind.TRAVEL_UNCERTAIN
    val restingEventValid = kind != TimeKind.ACTIVE_EVENT_ON_RESTING || hasRestingNight
    val canSave = kind != null && travelClassified && restingEventValid
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        LazyColumn(
            modifier = if (BuildConfig.DEBUG) {
                Modifier.testTag("plan-period-editor-$initialDate")
            } else {
                Modifier
            },
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ) {
                            Icon(
                                if (onDelete == null) Icons.Rounded.Add else Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.padding(9.dp).size(20.dp),
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (entryMode == PlanEntryMode.HOLIDAY_PLAN) {
                                    "Feriearbeidsplan"
                                } else {
                                    "Faktisk arbeid"
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        }
                        if (onDelete != null) {
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Rounded.Delete, "Slett periode", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                MethodDropdownField(
                    label = "Dato",
                    value = "${dayName(selectedDate)} ${shortDate(selectedDate)}",
                    expanded = dateMenuOpen,
                    onExpandedChange = { dateMenuOpen = it },
                ) {
                    dates.forEach { date ->
                        MethodDropdownOption(
                            text = "${dayName(date)} ${shortDate(date)}",
                            selected = date == selectedDate,
                        ) {
                            selectedDate = date
                            dateMenuOpen = false
                        }
                    }
                }
            }

            item {
                MethodDropdownField(
                    label = "Type",
                    value = kind?.let { if (it in travelKinds) "Reise" else timeKindLabel(it) } ?: "Velg type",
                    expanded = typeMenuOpen,
                    onExpandedChange = { typeMenuOpen = it },
                ) {
                    periodTypeChoices()
                        .filterNot {
                            entryMode == PlanEntryMode.HOLIDAY_PLAN &&
                                it.first == TimeKind.ACTIVE_EVENT_ON_RESTING
                        }
                        .forEach { (candidate, typeTitle, _) ->
                        val enabled = candidate != TimeKind.ACTIVE_EVENT_ON_RESTING || hasRestingNight
                        DropdownMenuItem(
                            text = {
                                Text(
                                    typeTitle,
                                    fontWeight = if (kind != null && samePeriodCategory(kind, candidate)) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                            enabled = enabled,
                            trailingIcon = {
                                if (kind != null && samePeriodCategory(kind, candidate)) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Valgt",
                                        tint = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            },
                            onClick = {
                                selectedKind = candidate
                                if (initialBlock == null) {
                                    val defaults = defaultPlannedBlockWithinTrip(candidate, selectedDate, tripStart, tripEnd)
                                    start = defaults.start
                                    end = defaults.end
                                }
                                typeMenuOpen = false
                            },
                        )
                    }
                }
            }

            if (kind != null && kind in travelKinds) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Ansvar under reisen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        InlineTravelKindSelector(
                            selected = kind,
                            onSelect = { selectedKind = it },
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RosterTimeInputButton(
                        label = "Fra",
                        value = start,
                        onValue = { start = it },
                        modifier = Modifier.weight(1f),
                    )
                    RosterTimeInputButton(
                        label = "Til",
                        value = end,
                        onValue = { end = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (kind?.isTravelWithoutResponsibility() == true) {
                item {
                    TravelDutyStatusSelector(
                        selected = travelDutyStatus,
                        onSelect = { travelDutyStatus = it },
                    )
                }
            }


            if (kind == TimeKind.RESTING_NIGHT_WATCH) {
                item {
                    InlineMessage(
                        FindingSeverity.OK,
                        "Hvilende nattevakt følger punkt 20.4",
                        "Nattevakt mellom kl. 23:00 og 07:00 under ferieoppholdet behandles normalt som arbeid av passiv karakter: hele tiden teller som arbeidstid, mens betalingen beregnes 1:3. Grunnturnusen brukes bare som sammenligning.",
                    )
                }
            }

            if (
                entryMode == PlanEntryMode.ACTUAL_WORK &&
                kind?.isTravelWithoutResponsibility() == true
            ) {
                if (travelDutyStatus == TravelDutyStatus.OFF_DUTY) {
                    item {
                        TravelNoticeSelector(
                            selected = travelNoticeStatus,
                            onSelect = { travelNoticeStatus = it },
                        )
                    }
                }
                if (travelOverlapsNight(selectedDate, PlannedBlock(requireNotNull(kind), start, end, travelNoticeStatus))) {
                    item {
                        NightTravelSleepSelector(
                            selected = kind,
                            onSelect = { selectedKind = it },
                        )
                    }
                }
            }

            if (!end.isAfter(start)) {
                item {
                    Text(
                        "Perioden slutter neste døgn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (kind == TimeKind.ACTIVE_EVENT_ON_RESTING && !hasRestingNight) {
                item {
                    InlineMessage(
                        FindingSeverity.CRITICAL,
                        "Mangler hvilende nattevakt",
                        "Aktivt arbeid under hvilende nattevakt må ligge sammen med en hvilende nattevakt.",
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Avbryt") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val savedKind = requireNotNull(kind)
                            onSave(
                                selectedDate,
                                normalizeTravelSleepKind(
                                    selectedDate,
                                    PlannedBlock(
                                        kind = savedKind,
                                        start = start,
                                        end = end,
                                        travelNoticeStatus =
                                            if (entryMode == PlanEntryMode.ACTUAL_WORK) {
                                                travelNoticeStatus
                                            } else {
                                                TravelNoticeStatus.NOT_CLARIFIED
                                            },
                                        holidayWorkPlanRelation =
                                            HolidayWorkPlanRelation.NOT_CLARIFIED,
                                        travelDutyStatus = travelDutyStatus,
                                    ),
                                ),
                            )
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) {
                        Text("Lagre")
                    }
                }
            }
        }
    }
}

private fun samePeriodCategory(first: TimeKind, second: TimeKind): Boolean =
    (first in travelKinds && second in travelKinds) || first == second


private fun periodTypeChoices(): List<Triple<TimeKind, String, String>> = listOf(
    Triple(TimeKind.ACTIVE_WORK, "Aktivt arbeid", "Vanlig aktivt arbeid på turen"),
    Triple(TimeKind.ACTIVE_NIGHT_WATCH, "Nattevakt", "Aktiv nattevakt"),
    Triple(TimeKind.RESTING_NIGHT_WATCH, "Hvilende nattevakt", "Vakt der du normalt kan sove, men må være tilgjengelig"),
    Triple(TimeKind.ACTIVE_EVENT_ON_RESTING, "Aktivt arbeid under hvilende nattevakt", "Aktiv hendelse inne i en hvilende nattevakt"),
    Triple(TimeKind.TRAVEL_UNCERTAIN, "Reise", "Utreise, hjemreise eller annen reisetid – velg ansvar i neste felt"),
)

private fun defaultPlannedBlock(kind: TimeKind): PlannedBlock = when (kind) {
    TimeKind.ACTIVE_WORK -> PlannedBlock(kind, LocalTime.of(7, 0), LocalTime.of(15, 0))
    TimeKind.ACTIVE_NIGHT_WATCH -> PlannedBlock(kind, LocalTime.of(21, 30), LocalTime.of(7, 30))
    TimeKind.RESTING_NIGHT_WATCH -> PlannedBlock(kind, LocalTime.of(23, 0), LocalTime.of(7, 0))
    TimeKind.ACTIVE_EVENT_ON_RESTING -> PlannedBlock(kind, LocalTime.of(2, 0), LocalTime.of(2, 30))
    TimeKind.TRAVEL_WITH_RESPONSIBILITY,
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED,
    TimeKind.TRAVEL_UNCERTAIN -> PlannedBlock(kind, LocalTime.of(8, 0), LocalTime.of(9, 0))
}

private fun defaultPlannedBlockWithinTrip(
    kind: TimeKind,
    date: LocalDate,
    tripStart: LocalDateTime,
    tripEnd: LocalDateTime,
): PlannedBlock {
    val preferred = defaultPlannedBlock(kind).toWorkBlock(date)
    val preferredStart = if (preferred.start.isBefore(tripStart)) tripStart else preferred.start
    val preferredEnd = if (preferred.end.isAfter(tripEnd)) tripEnd else preferred.end
    if (preferredEnd.isAfter(preferredStart) && preferredStart.toLocalDate() == date) {
        return PlannedBlock(kind, preferredStart.toLocalTime(), preferredEnd.toLocalTime())
    }

    val dayStart = date.atStartOfDay()
    val availableStart = if (tripStart.isAfter(dayStart)) tripStart else dayStart
    val availableEnd = listOf(tripEnd, date.plusDays(1).atStartOfDay()).minOrNull() ?: tripEnd
    val fallbackEnd = listOf(availableStart.plusHours(1), availableEnd).minOrNull() ?: availableEnd
    return if (fallbackEnd.isAfter(availableStart)) {
        PlannedBlock(kind, availableStart.toLocalTime(), fallbackEnd.toLocalTime())
    } else {
        defaultPlannedBlock(kind)
    }
}

@Composable
private fun AddPeriodButton(title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TravelChoiceCard(
    onWithResponsibility: () -> Unit,
    onWithoutResponsibility: () -> Unit,
    onUncertain: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Hvordan var reisen organisert?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Legg bare inn reisen som en egen periode når den skal behandles annerledes enn vanlig arbeid. At du reiser sammen med beboeren er ikke i seg selv nok til å velge tilsynsansvar.",
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                "Er dere to ansatte på reisen, betyr det ikke automatisk at bare én har ansvaret. Velg det som faktisk var avtalt og nødvendig.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            FilledTonalButton(onClick = onWithResponsibility, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text("Jeg har ansvar for beboeren", fontWeight = FontWeight.Bold)
                    Text("Jeg skal følge med, hjelpe eller kunne gripe inn under reisen", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = onWithoutResponsibility, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text("Jeg reiser uten tilsynsansvar", fontWeight = FontWeight.SemiBold)
                    Text("Reisetiden beregnes etter reisetidsreglene; enkelte unntak må fortsatt kontrolleres", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = onUncertain, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text("Jeg er usikker", fontWeight = FontWeight.SemiBold)
                    Text("Registrer tiden nå og avklar ansvaret senere", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Kilde: Dok. 25 2026–28, punkt 20.3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun ExplainableRateCard(
    hourlyRate: BigDecimal,
    annualSalary: BigDecimal,
    weeklyBasis: WeeklyBasis,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Timelønn", fontWeight = FontWeight.Bold)
                Text("${currency(hourlyRate)} per time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = "Vis grunnlag")
        }
        if (expanded) {
            Text("Slik finner vi timelønnen", fontWeight = FontWeight.SemiBold)
            Text("Du har valgt ${weeklyBasisUserLabel(weeklyBasis)}. Da deler appen årslønnen ${currency(annualSalary)} på ${weeklyBasis.divisor} timer. Det gir ${currency(hourlyRate)} per time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Kilde: Dok. 25 2026–28, punkt 9.6", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider()
    }
}

@Composable
private fun CoveredRosterSummaryCard(
    lines: List<CalculationLine>,
    totalAmount: BigDecimal,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Grunnturnus – ikke i betalingsgrunnlaget", fontWeight = FontWeight.SemiBold)
                Text(coveredRosterControlSummary(totalAmount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = if (expanded) "Skjul turnuskontroll" else "Vis turnuskontroll")
        }
        if (expanded) {
            Text("Kontroll av grunnturnus", fontWeight = FontWeight.SemiBold)
            Text("Dette er lønnsposter appen forutsetter at Oslo kommune betaler som del av grunnturnusen. De vises bare for kontroll og er ikke med i betalingsgrunnlaget for turen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            lines.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider()
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(line.title, fontWeight = FontWeight.SemiBold)
                        Text(line.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (line.amount != BigDecimal.ZERO || line.includedInKnownTotal) Text(currency(line.amount), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
                Text(line.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                line.evidence.forEach { evidence -> EvidenceRow(evidence) }
                Text("Kilde: ${line.source}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun ExplainableCalculationCard(line: CalculationLine, expanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(line.title, fontWeight = FontWeight.Bold)
                Text(line.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (line.amount != BigDecimal.ZERO || line.includedInKnownTotal) Text(currency(line.amount), fontWeight = FontWeight.Bold)
                CertaintyLabel(line)
            }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = "Vis grunnlag")
        }
        if (expanded) {
            Text("Hvorfor er dette med?", fontWeight = FontWeight.SemiBold)
            Text(line.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (line.evidence.isNotEmpty()) {
                Text("Timer som er med i beregningen", fontWeight = FontWeight.SemiBold)
                line.evidence.forEach { evidence -> EvidenceRow(evidence) }
            }
            Text("Kilde: ${line.source}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider()
    }
}

@Composable
private fun DayCalculationAuditCard(audit: DayCalculationAudit, expanded: Boolean, onToggle: () -> Unit) {
    val paymentContributions = dayAuditPaymentContributions(audit)
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${dayName(audit.date)} · ${shortDate(audit.date)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (audit.holidayLabels.isNotEmpty()) {
                    Text("Helge-/høytidsperiode: ${audit.holidayLabels.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    if (audit.contributions.isEmpty()) "Ingen beregnede lønnsposter denne dagen" else dayAuditCollapsedAmountSummary(audit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = if (expanded) "Skjul dagen" else "Vis dagen")
        }
        if (expanded) {
            if (paymentContributions.isEmpty()) {
                Text("Ingen poster fra turen er med i betalingsgrunnlaget denne dagen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                paymentContributions.forEachIndexed { index, contribution ->
                    DayCalculationContributionRow(contribution)
                    if (index != paymentContributions.lastIndex) HorizontalDivider()
                }
            }
            if (audit.alreadyCoveredSubtotal > BigDecimal.ZERO) {
                if (paymentContributions.isNotEmpty()) HorizontalDivider()
                Text("Grunnturnus: ${currency(audit.alreadyCoveredSubtotal)} i turnustillegg vises i turnuskontrollen over og er ikke med i betalingsgrunnlaget.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun DayCalculationContributionRow(contribution: DayCalculationContribution) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(contribution.title, fontWeight = FontWeight.SemiBold)
                Text(
                    minutesLabel(contribution.minutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(currency(contribution.amount), fontWeight = FontWeight.Bold)
                when {
                    !contribution.includedInKnownTotal || contribution.certainty == CalculationCertainty.OPEN ->
                        Text("MÅ AVKLARES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    contribution.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER ->
                        Text("GRUNNTURNUS · IKKE MED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        contribution.evidence.forEach { evidence ->
            Text(
                dayAuditEvidenceLabel(evidence),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("Kilde: ${contribution.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

private fun dayAuditEvidenceLabel(evidence: CalculationEvidence): String {
    val start = timeFormat.format(evidence.start.toLocalTime())
    val midnightNextDay = evidence.end.toLocalTime() == LocalTime.MIDNIGHT && evidence.end.toLocalDate().isAfter(evidence.start.toLocalDate())
    val end = if (midnightNextDay) "24:00" else timeFormat.format(evidence.end.toLocalTime())
    return "kl. $start–$end · ${minutesLabel(evidence.minutes)} · ${evidence.note}"
}

@Composable
private fun CertaintyLabel(line: CalculationLine) {
    val text = when {
        line.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER -> "GRUNNTURNUS · IKKE MED"
        line.certainty == CalculationCertainty.OPEN -> "MÅ AVKLARES"
        line.certainty == CalculationCertainty.ASSUMPTION -> "VALGT FORUTSETNING"
        else -> "AVKLART"
    }
    Text(text, style = MaterialTheme.typography.labelSmall, color = if (line.certainty == CalculationCertainty.OPEN) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EvidenceRow(evidence: CalculationEvidence) {
    Text(evidenceRowText(evidence), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

internal fun evidenceRowText(evidence: CalculationEvidence): String {
    val formatter = DateTimeFormatter.ofPattern("EEE d. MMM", norwegian)
    val startDate = evidence.start.toLocalDate().format(formatter)
    val interval = if (evidence.start.toLocalDate() == evidence.end.toLocalDate()) {
        "$startDate · ${timeFormat.format(evidence.start.toLocalTime())}–${timeFormat.format(evidence.end.toLocalTime())}"
    } else {
        val endDate = evidence.end.toLocalDate().format(formatter)
        "$startDate kl. ${timeFormat.format(evidence.start.toLocalTime())} → $endDate kl. ${timeFormat.format(evidence.end.toLocalTime())}"
    }
    return "$interval · ${minutesLabel(evidence.minutes)} · ${evidence.note}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripBasicsDateRangeButton(
    startDate: LocalDate,
    endDate: LocalDate,
    minimumDate: LocalDate,
    onRangeSelected: (LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { open = true },
        modifier = modifier.heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text("Datoer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(tripDateRangeLabel(startDate, endDate), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    if (open) {
        val selectableDates = remember(minimumDate) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !utcDateFromMillis(utcTimeMillis).isBefore(minimumDate)
            }
        }
        val state = remember(startDate, endDate, minimumDate) {
            val initialStart = startDate.takeUnless { it.isBefore(minimumDate) }
            val initialEnd = if (initialStart == null) {
                null
            } else {
                endDate.takeUnless { it.isBefore(minimumDate) }
            }
            DateRangePickerState(
                locale = norwegian,
                initialSelectedStartDate = initialStart,
                initialSelectedEndDate = initialEnd,
                selectableDates = selectableDates,
            )
        }
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                    onClick = {
                        val selectedStart = state.selectedStartDateMillis?.let(::utcDateFromMillis)
                        val selectedEnd = state.selectedEndDateMillis?.let(::utcDateFromMillis)
                        if (selectedStart != null && selectedEnd != null) {
                            onRangeSelected(selectedStart, selectedEnd)
                            open = false
                        }
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Avbryt") } },
        ) {
            NorwegianMaterialLocale {
                DateRangePicker(
                    state = state,
                    modifier = Modifier.weight(1f),
                    showModeToggle = false,
                    title = { Text("Velg datoer", modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)) },
                    headline = {
                        Text(
                            text = tripDateRangePickerHeadline(state.selectedStartDateMillis, state.selectedEndDateMillis),
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    colors = DatePickerDefaults.colors(
                        selectedDayContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedDayContentColor = MaterialTheme.colorScheme.onSecondary,
                        dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
                        dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onSurface,
                        todayContentColor = MaterialTheme.colorScheme.primary,
                        todayDateBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripBasicsTimeInputButton(
    label: String,
    value: LocalTime,
    onValue: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { open = true },
        modifier = modifier.heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(timeFormat.format(value), style = MaterialTheme.typography.titleMedium)
        }
    }

    if (open) {
        val state = rememberTimePickerState(
            initialHour = value.hour,
            initialMinute = value.minute,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    enabled = state.isInputValid,
                    onClick = {
                        onValue(LocalTime.of(state.hour, state.minute))
                        open = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Avbryt") } },
            title = { Text("Velg klokkeslett") },
            modeToggleButton = null,
        ) {
            NorwegianMaterialLocale {
                TimeInput(state = state)
            }
        }
    }
}

@Composable
private fun NorwegianMaterialLocale(content: @Composable () -> Unit) {
    val currentConfiguration = LocalConfiguration.current
    val norwegianConfiguration = remember(currentConfiguration) {
        Configuration(currentConfiguration).apply { setLocale(norwegian) }
    }
    CompositionLocalProvider(LocalConfiguration provides norwegianConfiguration) {
        content()
    }
}

internal fun tripDateRangePickerHeadline(startDateMillis: Long?, endDateMillis: Long?): String {
    val startDate = startDateMillis?.let(::utcDateFromMillis)
    val endDate = endDateMillis?.let(::utcDateFromMillis)
    return when {
        startDate != null && endDate != null -> tripDateRangeLabel(startDate, endDate)
        startDate != null -> "${startDate.format(dateFormat)} – velg sluttdato"
        else -> "Velg start- og sluttdato"
    }
}

internal fun tripDateRangeLabel(startDate: LocalDate, endDate: LocalDate): String {
    if (startDate == endDate) return startDate.format(dateFormat)
    return when {
        startDate.year == endDate.year && startDate.month == endDate.month ->
            "${startDate.dayOfMonth}.–${endDate.dayOfMonth}. ${endDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", norwegian))}"
        startDate.year == endDate.year ->
            "${startDate.format(DateTimeFormatter.ofPattern("d. MMMM", norwegian))}–${endDate.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", norwegian))}"
        else -> "${startDate.format(dateFormat)}–${endDate.format(dateFormat)}"
    }
}

private fun utcDateFromMillis(epochMillis: Long): LocalDate = LocalDate.ofEpochDay(epochMillis / 86_400_000L)

@Composable
private fun DateButton(label: String, value: LocalDate, onValue: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            PlatformDatePickerDialog(context, { _, year, month, day -> onValue(LocalDate.of(year, month + 1, day)) }, value.year, value.monthValue - 1, value.dayOfMonth).show()
        },
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(dateFormat.format(value), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TimeButton(label: String, value: LocalTime, onValue: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { PlatformTimePickerDialog(context, { _, hour, minute -> onValue(LocalTime.of(hour, minute)) }, value.hour, value.minute, true).show() },
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(timeFormat.format(value))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekendProfileSelector(
    selected: WeekendProfile,
    onSelect: (WeekendProfile) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    "Helgetillegg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    weekendProfileTitle(selected),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    selected.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = "Velg sats",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Column(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Hvilket lørdags- og søndagstillegg har du?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            "Sjekk en nyere lønnsslipp der du har jobbet helg. Velg satsen som faktisk gjelder for deg.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(WeekendProfile.entries, key = { it.name }) { profile ->
                    ListItem(
                        content = {
                            Text(
                                weekendProfileTitle(profile),
                                fontWeight = if (profile == selected) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                },
                                color = if (profile == selected) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        },
                        supportingContent = {
                            Text(weekendProfileDescription(profile))
                        },
                        trailingContent = {
                            if (profile == selected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Valgt",
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            onSelect(profile)
                            open = false
                        },
                    )
                }
                item {
                    Text(
                        "Kilde: Dok. 25 2026–28, punkt 12.2.2. Hvis lønnsslippen ikke gjør satsen tydelig, spør lønn eller leder.",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BasisChip(label: String, value: WeeklyBasis, selected: WeeklyBasis, onSelect: (WeeklyBasis) -> Unit, modifier: Modifier = Modifier) {
    FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label, maxLines = 1) }, modifier = modifier)
}

@Composable
private fun CompactInfoCard(title: String, body: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RuleCompactCard(title: String, source: String) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("MÅ AVKLARES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
        }
        HorizontalDivider()
    }
}

@Composable
private fun InlineMessage(severity: FindingSeverity, title: String, detail: String) {
    val colors = when (severity) {
        FindingSeverity.OK -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurface
        FindingSeverity.REVIEW -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        FindingSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        FindingSeverity.OPEN -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    val icon = when (severity) {
        FindingSeverity.OK -> Icons.Rounded.CheckCircle
        FindingSeverity.REVIEW, FindingSeverity.CRITICAL -> Icons.Rounded.Warning
        FindingSeverity.OPEN -> Icons.AutoMirrored.Rounded.HelpOutline
    }
    Surface(shape = MaterialTheme.shapes.large, color = colors.first, contentColor = colors.second) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
private fun shiftColor(category: ShiftCategory): Color = when (category) {
    ShiftCategory.LONG_DAY -> MaterialTheme.colorScheme.tertiaryContainer
    ShiftCategory.NIGHT -> MaterialTheme.colorScheme.secondaryContainer
    ShiftCategory.OFF -> MaterialTheme.colorScheme.surfaceVariant
    else -> MaterialTheme.colorScheme.primaryContainer
}

private fun flowSequence(mode: FundingMode, workPlanBasis: TripWorkPlanBasis): List<FlowScreen> = buildList {
    add(FlowScreen.TRIP)
    add(FlowScreen.METHOD)
    add(FlowScreen.PAY)
    if (mode == FundingMode.TURNUS_PLUS_EXTERNAL) {
        add(FlowScreen.ROSTER)
        if (workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN) {
            add(FlowScreen.HOLIDAY_PLAN)
        }
    }
    add(FlowScreen.TRIP_PLAN)
    add(FlowScreen.CALCULATION)
    add(FlowScreen.SETTLEMENT)
    add(FlowScreen.CONTROL)
    add(FlowScreen.SUMMARY)
}

private fun screenStepLabel(
    screen: FlowScreen,
    mode: FundingMode,
    workPlanBasis: TripWorkPlanBasis,
): String {
    val flow = flowSequence(mode, workPlanBasis)
    val index = flow.indexOf(screen)
    return if (index >= 0) "${index + 1} av ${flow.size}" else ""
}

private fun previousScreen(
    screen: FlowScreen,
    mode: FundingMode,
    workPlanBasis: TripWorkPlanBasis,
): FlowScreen {
    val flow = flowSequence(mode, workPlanBasis)
    val index = flow.indexOf(screen)
    return if (index <= 0) FlowScreen.HOME else flow[index - 1]
}

private fun nextScreen(
    screen: FlowScreen,
    mode: FundingMode,
    workPlanBasis: TripWorkPlanBasis,
): FlowScreen {
    val flow = flowSequence(mode, workPlanBasis)
    val index = flow.indexOf(screen)
    return if (index < 0 || index == flow.lastIndex) FlowScreen.HOME else flow[index + 1]
}

private fun nextButtonLabel(
    screen: FlowScreen,
    mode: FundingMode,
    workPlanBasis: TripWorkPlanBasis,
): String = when (nextScreen(screen, mode, workPlanBasis)) {
    FlowScreen.METHOD -> "Lønn og betaling"
    FlowScreen.PAY -> "Lønnsopplysninger"
    FlowScreen.ROSTER -> "Grunnturnus"
    FlowScreen.HOLIDAY_PLAN -> "Arbeidsgivers plan"
    FlowScreen.TRAVEL -> "Reise"
    FlowScreen.TRIP_PLAN -> "Arbeid på turen"
    FlowScreen.CALCULATION -> "Beregning"
    FlowScreen.SETTLEMENT -> "Betalingsforslag"
    FlowScreen.CONTROL -> "Kontroll"
    FlowScreen.SUMMARY -> "Oppsummering"
    FlowScreen.HOME -> "Ferdig"
    else -> "Neste"
}

@Composable
private fun ScreenHeader(title: String, stepLabel: String, onBack: () -> Unit) {
    val saveUi = LocalSaveUi.current
    val progress = flowChromeProgress(stepLabel)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Tilbake")
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                progress?.let {
                    Text(
                        it.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            saveUi?.let { SaveStatusAction(it) }
        }
        progress?.let {
            LinearProgressIndicator(
                progress = { it.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun SaveStatusAction(saveUi: SaveUiState) {
    val label = when (saveUi.state) {
        DraftSaveState.SAVED -> "Lagret"
        DraftSaveState.SAVING -> "Lagrer…"
        DraftSaveState.ERROR -> "Ikke lagret"
    }
    val tint = when (saveUi.state) {
        DraftSaveState.SAVED -> MaterialTheme.colorScheme.primary
        DraftSaveState.SAVING -> MaterialTheme.colorScheme.onSurfaceVariant
        DraftSaveState.ERROR -> MaterialTheme.colorScheme.error
    }
    TextButton(
        onClick = saveUi.onSave,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            imageVector = if (saveUi.state == DraftSaveState.ERROR) Icons.Rounded.Warning else Icons.Rounded.Save,
            contentDescription = if (saveUi.state == DraftSaveState.ERROR) "Prøv å lagre på nytt" else "Lagre nå",
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}

@Composable
private fun SimpleChoiceCard(selected: Boolean, title: String, body: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.ChevronRight,
                contentDescription = if (selected) "Valgt" else null,
                tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun MethodChoiceCard(selected: Boolean, title: String, body: String, footer: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 13.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(footer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.ChevronRight, contentDescription = if (selected) "Valgt" else null, tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
    }
}

@Composable
private fun TravelLegCard(
    title: String,
    fixedLabel: String,
    fixedValue: LocalDateTime,
    editableLabel: String,
    editableValue: LocalDateTime,
    onEditableValue: (LocalDateTime) -> Unit,
    selectedKind: TimeKind?,
    onSelectedKind: (TimeKind) -> Unit,
    editableFirst: Boolean = false,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            if (editableFirst) {
                DateTimeButtons(editableLabel, editableValue, onEditableValue)
                ReadOnlyDateTime(fixedLabel, fixedValue)
            } else {
                ReadOnlyDateTime(fixedLabel, fixedValue)
                DateTimeButtons(editableLabel, editableValue, onEditableValue)
            }
            HorizontalDivider()
            Text("Har du ansvar for beboeren under reisen?", fontWeight = FontWeight.Bold)
            TravelResponsibilitySelector(selectedKind, onSelectedKind)
            when (selectedKind) {
                TimeKind.TRAVEL_WITH_RESPONSIBILITY -> CompactInfoCard(
                    "Reisen regnes som arbeidstid",
                    "Du har oppgitt at du faktisk har ansvar for å følge med, hjelpe eller kunne gripe inn under reisen. Dette hindrer ikke at reisetiden samtidig ligger innenfor grunnturnusen.",
                )
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED -> CompactInfoCard(
                    "Reisen beregnes etter reisetidsregelen",
                    "Når du ikke har tilsynsansvar, brukes Dok. 25 punkt 18.4. For nattreise kl. 23:00–07:00 brukes punkt 20.3 når du hadde tillatelse til å sove; dette registreres på selve reiseperioden i arbeidsplanen.",
                )
                TimeKind.TRAVEL_UNCERTAIN -> InlineMessage(
                    FindingSeverity.REVIEW,
                    "Ansvar må avklares",
                    "Du kan fortsette planleggingen, men beregningen kan ikke behandle denne reisen som avklart før ansvaret er kjent.",
                )
                else -> Unit
            }
            Text("Kilde: Dok. 25 2026–28, punkt 18.4 og 20.3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TravelResponsibilitySelector(selected: TimeKind?, onSelect: (TimeKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(TimeKind.TRAVEL_WITH_RESPONSIBILITY, "Ja, jeg har ansvar", "Jeg skal følge med, hjelpe eller kunne gripe inn"),
            Triple(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, "Nei, ikke i denne perioden", "Jeg reiser, men har ikke tilsynsansvaret"),
            Triple(TimeKind.TRAVEL_UNCERTAIN, "Usikker", "Registrer reisen nå og avklar ansvaret før betalingsgrunnlaget brukes"),
        ).forEach { (kind, title, subtitle) ->
            FilledTonalButton(onClick = { onSelect(kind) }, modifier = Modifier.fillMaxWidth()) {
                val isSelected = if (kind == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY) selected?.isTravelWithoutResponsibility() == true else selected == kind
                if (isSelected) Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                if (isSelected) Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun InlineTravelKindSelector(selected: TimeKind, onSelect: (TimeKind) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == TimeKind.TRAVEL_WITH_RESPONSIBILITY,
            onClick = { onSelect(TimeKind.TRAVEL_WITH_RESPONSIBILITY) },
            label = { Text("Med ansvar") },
        )
        FilterChip(
            selected = selected.isTravelWithoutResponsibility(),
            onClick = { onSelect(travelWithoutResponsibilitySelection(selected)) },
            label = { Text("Uten ansvar") },
        )
        FilterChip(
            selected = selected == TimeKind.TRAVEL_UNCERTAIN,
            onClick = { onSelect(TimeKind.TRAVEL_UNCERTAIN) },
            label = { Text("Usikker") },
        )
    }
}

@Composable
private fun TravelDutyStatusSelector(selected: TravelDutyStatus, onSelect: (TravelDutyStatus) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("Vaktstatus under reisen", fontWeight = FontWeight.Bold)
        Text("Var du på vakt i denne reiseperioden?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selected == TravelDutyStatus.ON_DUTY, onClick = { onSelect(TravelDutyStatus.ON_DUTY) }, label = { Text("På vakt") })
            FilterChip(selected = selected == TravelDutyStatus.OFF_DUTY, onClick = { onSelect(TravelDutyStatus.OFF_DUTY) }, label = { Text("Ikke på vakt") })
            FilterChip(selected = selected == TravelDutyStatus.NOT_CLARIFIED, onClick = { onSelect(TravelDutyStatus.NOT_CLARIFIED) }, label = { Text("Ikke avklart") })
        }
        Text(
            when (selected) {
                TravelDutyStatus.ON_DUTY -> "Reisen behandles som arbeidstid. Ferietur sammenligner perioden automatisk med feriearbeidsplanen."
                TravelDutyStatus.OFF_DUTY -> "Reisen behandles etter reisetidsreglene. Ordinær reisetid kan godtgjøres uten å telle som arbeidstid. Passiv nattreise med søvntillatelse er et eget unntak."
                TravelDutyStatus.NOT_CLARIFIED -> "Ferietur holder den ordinære reisedelen åpen til vaktstatusen er avklart."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Kilde: Dok. 25 punkt 18.4 og 20.3 · Oslo kommune EQS ID 53398", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TravelNoticeSelector(selected: TravelNoticeStatus, onSelect: (TravelNoticeStatus) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("Varsel om reisen", fontWeight = FontWeight.Bold)
        Text("Fikk du vite om reisen senest dagen i forveien?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selected == TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY,
                onClick = { onSelect(TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY) },
                label = { Text("Ja") },
            )
            FilterChip(
                selected = selected == TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY,
                onClick = { onSelect(TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY) },
                label = { Text("Nei") },
            )
            FilterChip(
                selected = selected == TravelNoticeStatus.NOT_CLARIFIED,
                onClick = { onSelect(TravelNoticeStatus.NOT_CLARIFIED) },
                label = { Text("Ikke avklart") },
            )
        }
        Text(
            when (selected) {
                TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY -> "Reisetid utenfor ordinær arbeidstid godtgjøres med ordinær timelønn etter punkt 18.4."
                TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY -> "Kort varsel: ordinær reisetidsbetaling beholdes, og inntil to timer reisetid utenfor ordinær arbeidstid får overtidstillegg. Overtidstillegget følger kapittel 13."
                TravelNoticeStatus.NOT_CLARIFIED -> "Ordinær reisetidsbetaling kan beregnes, men et mulig overtidstillegg for inntil to timer står åpent til varseltidspunktet er avklart."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Kilde: Dok. 25 2026–28, punkt 18.4, 13.2 og 13.3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun NightTravelSleepSelector(selected: TimeKind, onSelect: (TimeKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("Nattreise kl. 23:00–07:00", fontWeight = FontWeight.Bold)
        Text("Hadde du tillatelse til å sove under nattdelen?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selected == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED,
                onClick = { onSelect(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED) },
                label = { Text("Ja") },
            )
            FilterChip(
                selected = selected == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
                onClick = { onSelect(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP) },
                label = { Text("Nei") },
            )
            FilterChip(
                selected = selected == TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
                onClick = { onSelect(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY) },
                label = { Text("Ikke avklart") },
            )
        }
        Text(
            when (selected) {
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED -> "Nattdelen beregnes som arbeid av passiv karakter: arbeidstid time for time og grunnbetaling i forholdet 1:3. Tillegg for passiv karakter beregnes også i forholdet 1:3."
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP -> "Nattdelen behandles som ordinær reisetid etter punkt 18.4. Den blir ikke gjort om til passiv arbeidstid etter punkt 20.3."
                else -> "Nattdelen holdes utenfor betalingsgrunnlaget til søvntillatelsen er avklart."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Kilde: Dok. 25 2026–28, punkt 20.3 og 8.9", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private fun travelOverlapsNight(date: LocalDate, block: PlannedBlock): Boolean {
    if (!block.kind.isTravelWithoutResponsibility()) return false
    val work = block.toWorkBlock(date)
    var windowDate = work.start.toLocalDate().minusDays(1)
    val last = work.end.toLocalDate()
    while (!windowDate.isAfter(last)) {
        val nightStart = LocalDateTime.of(windowDate, LocalTime.of(23, 0))
        val nightEnd = LocalDateTime.of(windowDate.plusDays(1), LocalTime.of(7, 0))
        if (work.start.isBefore(nightEnd) && work.end.isAfter(nightStart)) return true
        windowDate = windowDate.plusDays(1)
    }
    return false
}

private fun normalizeTravelSleepKind(date: LocalDate, block: PlannedBlock): PlannedBlock =
    if (block.kind.isTravelWithoutResponsibility() && !travelOverlapsNight(date, block)) {
        block.copy(kind = TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY)
    } else {
        block
    }

@Composable
private fun ReadOnlyDateTime(label: String, value: LocalDateTime) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${fullDate(value.toLocalDate())} · kl. ${timeFormat.format(value.toLocalTime())}", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DateTimeButtons(label: String, value: LocalDateTime, onValue: (LocalDateTime) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DateButton("Dato", value.toLocalDate(), { date -> onValue(LocalDateTime.of(date, value.toLocalTime())) }, Modifier.weight(1f))
            TimeButton("Kl.", value.toLocalTime(), { time -> onValue(LocalDateTime.of(value.toLocalDate(), time)) }, Modifier.weight(1f))
        }
    }
}

private data class PlanValidationIssue(
    val id: String,
    val title: String,
    val detail: String,
)

private data class PlanOverlapRaw(
    val first: WorkBlock,
    val second: WorkBlock,
    val overlapStart: LocalDateTime,
    val overlapEnd: LocalDateTime,
)

private fun planValidationIssues(
    dates: List<LocalDate>,
    plans: Map<LocalDate, List<PlannedBlock>>,
    tripStart: LocalDateTime,
    tripEnd: LocalDateTime,
    fundingMode: FundingMode,
    @Suppress("UNUSED_PARAMETER")
    holidayWorkPlanStatus: HolidayWorkPlanStatus,
): Map<LocalDate, List<PlanValidationIssue>> {
    if (dates.isEmpty()) return emptyMap()
    val blocks = TripPlanEngine.projectRange(dates, plans).sortedBy { it.start }
    val byDate = linkedMapOf<LocalDate, MutableList<PlanValidationIssue>>()

    if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) {
        plans.forEach { (date, dayPlans) ->
            dayPlans.forEachIndexed { index, block ->
                if (block.kind.isTravelWithoutResponsibility() && block.travelDutyStatus == TravelDutyStatus.NOT_CLARIFIED) {
                    byDate.getOrPut(date) { mutableListOf() } += PlanValidationIssue(
                        id = "travel-duty-status-$date-$index",
                        title = "Vaktstatus under reisen må avklares",
                        detail = "Velg om arbeidstakeren var på vakt eller ikke på vakt i reiseperioden. Grunnturnusen brukes bare som sammenligning og avgjør ikke vaktstatusen.",
                    )
                }
            }
        }
    }

    for (i in blocks.indices) {
        val first = blocks[i]
        for (j in i + 1 until blocks.size) {
            val second = blocks[j]
            if (!second.start.isBefore(first.end)) break
            if (TripPlanEngine.overlapIsIntentional(first.kind, second.kind)) continue
            val overlapStart = if (first.start.isAfter(second.start)) first.start else second.start
            val overlapEnd = if (first.end.isBefore(second.end)) first.end else second.end
            if (!overlapEnd.isAfter(overlapStart)) continue
            val raw = PlanOverlapRaw(first, second, overlapStart, overlapEnd)
            listOf(first.start.toLocalDate(), second.start.toLocalDate()).distinct().forEach { date ->
                if (date !in dates) return@forEach
                byDate.getOrPut(date) { mutableListOf() }.add(
                    PlanValidationIssue(
                        id = "overlap-${first.start}-${second.start}-$date",
                        title = "Samme tid er registrert to ganger",
                        detail = planOverlapIssueText(raw, date),
                    ),
                )
            }
        }
    }

    TripPlanEngine.outsideTripRangeBlocks(blocks, tripStart, tripEnd).forEach { block ->
        val date = block.start.toLocalDate().let { candidate ->
            when {
                candidate in dates -> candidate
                candidate.isBefore(dates.first()) -> dates.first()
                else -> dates.last()
            }
        }
        val before = block.start.isBefore(tripStart)
        val boundary = if (before) tripStart else tripEnd
        val direction = if (before) "før turen starter" else "etter at turen er slutt"
        val action = if (before) "Endre turens starttid eller arbeidsperioden." else "Endre turens sluttid eller arbeidsperioden."
        byDate.getOrPut(date) { mutableListOf() }.add(
            PlanValidationIssue(
                id = "range-${block.start}-${block.end}",
                title = "Arbeid ligger utenfor turen",
                detail = "${timeKindLabel(block.kind)} ${userInterval(block)} ligger $direction ${fullDate(boundary.toLocalDate())} kl. ${timeFormat.format(boundary.toLocalTime())}. $action",
            ),
        )
    }
    return byDate
}

private fun planOverlapIssueText(issue: PlanOverlapRaw, date: LocalDate): String {
    val overlapMinutes = ChronoUnit.MINUTES.between(issue.overlapStart, issue.overlapEnd)
    val other = if (issue.first.start.toLocalDate() == date) issue.second else issue.first
    val current = if (issue.first.start.toLocalDate() == date) issue.first else issue.second
    val currentLabel = "${timeKindLabel(current.kind)} ${userInterval(current)}"
    val otherDay = dayName(other.start.toLocalDate()).lowercase(norwegian)
    val otherLabel = "${timeKindLabel(other.kind).lowercase(norwegian)} $otherDay ${userInterval(other)}"
    return "$currentLabel overlapper med $otherLabel i ${minutesLabel(overlapMinutes)}. Endre en av periodene før du går videre."
}

private fun userInterval(block: WorkBlock): String =
    if (block.start.toLocalDate() == block.end.toLocalDate()) {
        "kl. ${timeFormat.format(block.start.toLocalTime())}–${timeFormat.format(block.end.toLocalTime())}"
    } else {
        "fra kl. ${timeFormat.format(block.start.toLocalTime())} til kl. ${timeFormat.format(block.end.toLocalTime())} neste dag"
    }

private fun travelPlans(
    tripStart: LocalDateTime,
    outboundArrival: LocalDateTime,
    outboundKind: TimeKind?,
    returnDeparture: LocalDateTime,
    tripEnd: LocalDateTime,
    returnKind: TimeKind?,
): Map<LocalDate, List<PlannedBlock>> {
    val result = linkedMapOf<LocalDate, MutableList<PlannedBlock>>()
    if (outboundKind != null && outboundArrival.isAfter(tripStart)) {
        splitTravelLeg(tripStart, outboundArrival, outboundKind).forEach { (date, block) -> result.getOrPut(date) { mutableListOf() }.add(block) }
    }
    if (returnKind != null && tripEnd.isAfter(returnDeparture)) {
        splitTravelLeg(returnDeparture, tripEnd, returnKind).forEach { (date, block) -> result.getOrPut(date) { mutableListOf() }.add(block) }
    }
    return result
}

private fun splitTravelLeg(start: LocalDateTime, end: LocalDateTime, kind: TimeKind): List<Pair<LocalDate, PlannedBlock>> {
    if (!end.isAfter(start)) return emptyList()
    val result = mutableListOf<Pair<LocalDate, PlannedBlock>>()
    var cursor = start
    while (cursor.isBefore(end)) {
        val nextMidnight = cursor.toLocalDate().plusDays(1).atStartOfDay()
        val segmentEnd = if (end.isBefore(nextMidnight)) end else nextMidnight
        result += cursor.toLocalDate() to PlannedBlock(kind, cursor.toLocalTime(), segmentEnd.toLocalTime())
        cursor = segmentEnd
    }
    return result
}

private fun mergePlanMaps(base: Map<LocalDate, List<PlannedBlock>>, extra: Map<LocalDate, List<PlannedBlock>>): Map<LocalDate, List<PlannedBlock>> =
    (base.keys + extra.keys).associateWith { date -> base[date].orEmpty() + extra[date].orEmpty() }

private fun settlementSummary(
    calculation: TariffRuntimeCalculation,
    mode: SettlementMode,
    customAmountText: String,
    reason: String,
): SettlementSummary {
    val custom = customAmountText.toNorwegianMoneyOrNull()
    val proposed = if (mode == SettlementMode.FULL_CALCULATION) {
        calculation.paymentBasisAmount
    } else {
        custom ?: calculation.paymentBasisAmount
    }
    return SettlementSummary(
        calculatedAmount = calculation.paymentBasisAmount,
        proposedAmount = proposed,
        usesFullCalculation = mode == SettlementMode.FULL_CALCULATION,
        reason = reason.trim(),
        alreadyCoveredByNormalRosterAmount = calculation.alreadyCoveredByNormalRosterAmount,
        fullKnownCalculationAmount = calculation.knownAmount,
        unresolvedRules = FerieturRules.applicableUnresolvedRules(calculation.applicableUnresolvedRuleIds),
    )
}

private fun String.toNorwegianMoneyOrNull(): BigDecimal? {
    val normalized = trim().replace(" ", "").replace(',', '.')
    if (normalized.isBlank()) return null
    return normalized.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
}

private fun fullDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", norwegian))

private fun weeklyBasisUserLabel(basis: WeeklyBasis): String = when (basis) {
    WeeklyBasis.HOURS_37_5 -> "37,5 timers full arbeidsuke"
    WeeklyBasis.HOURS_35_5 -> "35,5 timers full arbeidsuke"
    WeeklyBasis.DOK25_8_2_2 -> "tredelt skift- eller turnusarbeid"
    WeeklyBasis.HOURS_33_6 -> "33,6 timers full arbeidsuke"
}

private fun weekendProfileTitle(profile: WeekendProfile): String = when (profile) {
    WeekendProfile.STANDARD -> "Vanlig sats"
    WeekendProfile.EXTENDED_30 -> "Forhøyet sats"
    WeekendProfile.EXTENDED_35 -> "Høyeste sats"
}

private fun weekendProfileDescription(profile: WeekendProfile): String = when (profile) {
    WeekendProfile.STANDARD -> "23 %, minst 73 kr per time. Dette er grunnsatsen."
    WeekendProfile.EXTENDED_30 -> "30 %, minst 110 kr per time. Gjelder ved minst 19 helger eller 285 timer lørdag/søndag i tjenesteplanen."
    WeekendProfile.EXTENDED_35 -> "35 %, minst 135 kr per time. Gjelder aktuell turnusgruppe ved minst 345 timer lørdag/søndag i tjenesteplanen."
}

private fun tripDates(start: LocalDate, end: LocalDate): List<LocalDate> =
    TripDateRangePolicy.inclusiveDates(start, end)

private fun rosterDaySummary(shifts: List<ShiftDefinition>): String {
    if (shifts.isEmpty()) return "Ikke registrert"
    val first = shifts.first()
    return if (shifts.size == 1) {
        shiftTimeLabel(first)
    } else {
        "${shiftTimeLabel(first)} +${shifts.size - 1}"
    }
}

private fun shiftTimeLabel(shift: ShiftDefinition): String = when {
    shift.start == null || shift.end == null -> "Fri"
    else -> "${timeFormat.format(shift.start)}–${timeFormat.format(shift.end)}"
}

private fun categoryLabel(category: ShiftCategory): String = when (category) {
    ShiftCategory.DAY -> "Dagvakt"
    ShiftCategory.LONG_DAY -> "Langvakt"
    ShiftCategory.EVENING -> "Aftenvakt"
    ShiftCategory.NIGHT -> "Nattevakt"
    ShiftCategory.OFF -> "Fri"
    ShiftCategory.EXCLUDED -> "Ekskludert"
}

@Composable
private fun timeKindAccentColor(kind: TimeKind): Color = when (kind) {
    TimeKind.ACTIVE_WORK -> MaterialTheme.colorScheme.primary
    TimeKind.ACTIVE_NIGHT_WATCH -> MaterialTheme.colorScheme.secondary
    TimeKind.RESTING_NIGHT_WATCH -> MaterialTheme.colorScheme.tertiary
    TimeKind.TRAVEL_WITH_RESPONSIBILITY -> MaterialTheme.colorScheme.inversePrimary
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED -> MaterialTheme.colorScheme.outline
    TimeKind.TRAVEL_UNCERTAIN -> MaterialTheme.colorScheme.error
    TimeKind.ACTIVE_EVENT_ON_RESTING -> MaterialTheme.colorScheme.error
}

@Composable
private fun timeKindContainerColor(kind: TimeKind): Color = when (kind) {
    TimeKind.ACTIVE_WORK -> MaterialTheme.colorScheme.primaryContainer
    TimeKind.ACTIVE_NIGHT_WATCH -> MaterialTheme.colorScheme.secondaryContainer
    TimeKind.RESTING_NIGHT_WATCH -> MaterialTheme.colorScheme.tertiaryContainer
    TimeKind.TRAVEL_WITH_RESPONSIBILITY -> MaterialTheme.colorScheme.surfaceContainerHighest
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED -> MaterialTheme.colorScheme.surfaceVariant
    TimeKind.TRAVEL_UNCERTAIN -> MaterialTheme.colorScheme.errorContainer
    TimeKind.ACTIVE_EVENT_ON_RESTING -> MaterialTheme.colorScheme.errorContainer
}

private fun oneThirdDisplay(minutes: Long): String =
    if (minutes % 3L == 0L) minutesLabel(minutes / 3L) else "${minutesLabel(minutes)} × ⅓"

private fun timeKindLabel(kind: TimeKind): String = when (kind) {
    TimeKind.ACTIVE_WORK -> "Aktivt arbeid"
    TimeKind.ACTIVE_NIGHT_WATCH -> "Nattevakt"
    TimeKind.RESTING_NIGHT_WATCH -> "Hvilende nattevakt"
    TimeKind.TRAVEL_WITH_RESPONSIBILITY -> "Reise med ansvar for beboeren"
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
    TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED -> "Reise uten tilsynsansvar"
    TimeKind.TRAVEL_UNCERTAIN -> "Reise · ansvar må avklares"
    TimeKind.ACTIVE_EVENT_ON_RESTING -> "Aktivt arbeid under hvilende nattevakt"
}

private fun workBlockOverlapsNight(block: WorkBlock): Boolean {
    var windowDate = block.start.toLocalDate().minusDays(1)
    val last = block.end.toLocalDate()
    while (!windowDate.isAfter(last)) {
        val nightStart = LocalDateTime.of(windowDate, LocalTime.of(23, 0))
        val nightEnd = LocalDateTime.of(windowDate.plusDays(1), LocalTime.of(7, 0))
        if (block.start.isBefore(nightEnd) && block.end.isAfter(nightStart)) return true
        windowDate = windowDate.plusDays(1)
    }
    return false
}

private fun dateTimeIntervalLabel(block: WorkBlock): String =
    if (block.start.toLocalDate() == block.end.toLocalDate()) {
        "${timeFormat.format(block.start.toLocalTime())}–${timeFormat.format(block.end.toLocalTime())}"
    } else {
        "${timeFormat.format(block.start.toLocalTime())}–${timeFormat.format(block.end.toLocalTime())} neste døgn"
    }

private fun travelNoticeStatusLabel(status: TravelNoticeStatus): String = when (status) {
    TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY -> "Varsel: kjent senest dagen før"
    TravelNoticeStatus.NOT_KNOWN_BY_PREVIOUS_DAY -> "Varsel: kort varsel"
    TravelNoticeStatus.NOT_CLARIFIED -> "Varsel: ikke avklart"
}

private fun travelDutyStatusLabel(status: TravelDutyStatus): String = when (status) {
    TravelDutyStatus.ON_DUTY -> "Vaktstatus: på vakt"
    TravelDutyStatus.OFF_DUTY -> "Vaktstatus: ikke på vakt"
    TravelDutyStatus.NOT_CLARIFIED -> "Vaktstatus: ikke avklart"
}

private fun dayProjectedBlockLabel(date: LocalDate, projected: DayProjectedBlock): String {
    val block = projected.block
    val dayStart = date.atStartOfDay()
    val dayEnd = date.plusDays(1).atStartOfDay()
    val start = if (block.start == dayStart) "00:00" else timeFormat.format(block.start.toLocalTime())
    val end = if (block.end == dayEnd) "24:00" else timeFormat.format(block.end.toLocalTime())
    val title = buildString {
        append(timeKindLabel(block.kind))
        if (block.kind.isTravelWithoutResponsibility()) {
            append(" · ").append(travelDutyStatusLabel(block.travelDutyStatus).lowercase(norwegian))
            if (block.travelDutyStatus == TravelDutyStatus.OFF_DUTY) {
                append(" · ").append(travelNoticeStatusLabel(block.travelNoticeStatus).lowercase(norwegian))
            }
        }
        nightTravelSleepStatus(block.kind, workBlockOverlapsNight(block))?.let { append(" · ").append(it.lowercase(norwegian)) }
    }
    return when {
        projected.continuesFromPreviousDay -> "$title fra ${dayName(projected.sourceDate).lowercase(norwegian)} · $start–$end"
        projected.continuesIntoNextDay -> "$title · $start–$end · fortsetter ${dayName(date.plusDays(1)).lowercase(norwegian)}"
        else -> "$title · $start–$end"
    }
}

private fun <T> List<T>.updated(index: Int, value: T): List<T> = toMutableList().also { it[index] = value }

private fun toggle(values: Set<String>, key: String): Set<String> = if (key in values) values - key else values + key

internal fun coveredRosterControlSummary(totalAmount: BigDecimal): String =
    if (totalAmount > BigDecimal.ZERO) {
        "${currency(totalAmount)} i turnustillegg ligger i grunnturnusen og påvirker ikke beløpet over."
    } else {
        "Lønnspostene ligger i grunnturnusen og påvirker ikke beløpet over."
    }

internal fun dayAuditPaymentContributions(audit: DayCalculationAudit): List<DayCalculationContribution> =
    audit.contributions.filter { it.paymentTreatment != PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER }

internal fun dayAuditCollapsedAmountSummary(audit: DayCalculationAudit): String {
    val parts = buildList {
        if (audit.paymentSubtotal > BigDecimal.ZERO) add("kommer i tillegg: ${currency(audit.paymentSubtotal)}")
        if (audit.openSubtotal > BigDecimal.ZERO) add("må avklares: ${currency(audit.openSubtotal)}")
    }
    return parts.joinToString(" · ").ifBlank {
        if (audit.alreadyCoveredSubtotal > BigDecimal.ZERO) "0,00 kr i tillegg" else "Ingen beløp denne dagen"
    }
}

private fun dayName(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE", norwegian)).replaceFirstChar { it.uppercase(norwegian) }
private fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d. MMM", norwegian))

private fun minutesLabel(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "$h t" else "$h t $m min"
}

private fun currency(value: BigDecimal): String {
    val formatter = NumberFormat.getCurrencyInstance(norwegian)
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return formatter.format(value.setScale(2, RoundingMode.HALF_UP))
}

private val norwegian = Locale.forLanguageTag("nb-NO")
private val dateFormat = DateTimeFormatter.ofPattern("d. MMMM yyyy", norwegian)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
