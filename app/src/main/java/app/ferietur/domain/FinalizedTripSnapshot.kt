package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

const val FERIETUR_RULESET_VERSION = "2026.4"

data class RosterSnapshotRow(
    val date: LocalDate,
    val code: String,
    val label: String,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
)

data class SettlementSnapshot(
    val calculatedAmount: BigDecimal,
    val proposedAmount: BigDecimal,
    val usesFullCalculation: Boolean,
    val reason: String,
)

data class FinalizedTariffContextSnapshot(
    val start: LocalDate,
    val end: LocalDate,
    val tariffPackageId: String,
    val rulesetVersion: String,
    val tariffRateSetId: String,
    val salaryTableId: String,
    val salaryTableEffectiveFrom: LocalDate,
    val salaryTableSourceLabel: String,
    val annualSalary: BigDecimal,
    val hourlyRate: BigDecimal,
) {
    init {
        require(!end.isBefore(start)) { "Tariffkontekstens sluttdato er før startdato." }
        require(tariffPackageId.isNotBlank())
        require(rulesetVersion.isNotBlank())
        require(tariffRateSetId.isNotBlank())
        require(salaryTableId.isNotBlank())
        require(annualSalary >= BigDecimal.ZERO)
        require(hourlyRate >= BigDecimal.ZERO)
    }

    companion object {
        fun fromRuntime(slice: TariffRuntimeProvenanceSlice): FinalizedTariffContextSnapshot =
            FinalizedTariffContextSnapshot(
                start = slice.start,
                end = slice.end,
                tariffPackageId = slice.tariffPackageId,
                rulesetVersion = slice.rulesetVersion,
                tariffRateSetId = slice.tariffRateSetId,
                salaryTableId = slice.salaryTableId,
                salaryTableEffectiveFrom = slice.salaryTableEffectiveFrom,
                salaryTableSourceLabel = slice.salaryTableSourceLabel,
                annualSalary = slice.annualSalary,
                hourlyRate = slice.hourlyRate,
            )
    }
}

object FinalizedTariffContextSnapshots {
    fun fromRuntime(calculation: TariffRuntimeCalculation): List<FinalizedTariffContextSnapshot> =
        calculation.provenance.map { FinalizedTariffContextSnapshot.fromRuntime(it) }
}

enum class FinalizedCalculationPayloadMode {
    PRELIMINARY,
    SEGMENTED_CONTEXTS,
}

data class FinalizedScopedCalculationLineSnapshot(
    val scope: TariffCalculationLineScope,
    val line: CalculationLine,
    val sliceIndex: Int? = null,
    val watchSourceIndex: Int? = null,
) {
    init {
        when (scope) {
            TariffCalculationLineScope.SEGMENT_LOCAL -> require(sliceIndex != null && watchSourceIndex == null)
            TariffCalculationLineScope.WHOLE_TRIP -> require(sliceIndex == null && watchSourceIndex == null)
            TariffCalculationLineScope.PER_RESTING_WATCH -> require(sliceIndex == null)
        }
    }

    companion object {
        fun fromRuntime(value: TariffScopedCalculationLine): FinalizedScopedCalculationLineSnapshot =
            FinalizedScopedCalculationLineSnapshot(
                scope = value.scope,
                line = value.line,
                sliceIndex = value.sliceIndex,
                watchSourceIndex = value.watchSourceIndex,
            )
    }
}

sealed interface FinalizedCalculationPayload {
    val mode: FinalizedCalculationPayloadMode
    val lines: List<CalculationLine>
    val knownAmount: BigDecimal
    val paymentBasisAmount: BigDecimal
    val alreadyCoveredByNormalRosterAmount: BigDecimal
    val excludedKnownRuleAmount: BigDecimal
    val applicableUnresolvedRuleIds: Set<String>
    val preliminaryOrNull: PreliminaryCalculation?

    data class Preliminary(
        val calculation: PreliminaryCalculation,
    ) : FinalizedCalculationPayload {
        override val mode: FinalizedCalculationPayloadMode = FinalizedCalculationPayloadMode.PRELIMINARY
        override val lines: List<CalculationLine> get() = calculation.lines
        override val knownAmount: BigDecimal get() = calculation.knownAmount
        override val paymentBasisAmount: BigDecimal get() = calculation.paymentBasisAmount
        override val alreadyCoveredByNormalRosterAmount: BigDecimal
            get() = calculation.alreadyCoveredByNormalRosterAmount
        override val excludedKnownRuleAmount: BigDecimal get() = calculation.excludedKnownRuleAmount
        override val applicableUnresolvedRuleIds: Set<String> get() = calculation.applicableUnresolvedRuleIds
        override val preliminaryOrNull: PreliminaryCalculation get() = calculation
    }

    data class SegmentedContexts(
        val lineEntries: List<FinalizedScopedCalculationLineSnapshot>,
        override val applicableUnresolvedRuleIds: Set<String>,
    ) : FinalizedCalculationPayload {
        override val mode: FinalizedCalculationPayloadMode = FinalizedCalculationPayloadMode.SEGMENTED_CONTEXTS
        override val lines: List<CalculationLine> get() = lineEntries.map { it.line }
        override val knownAmount: BigDecimal get() = moneySum(lines.filter { it.includedInKnownTotal })
        override val paymentBasisAmount: BigDecimal get() = moneySum(
            lines.filter {
                it.includedInKnownTotal && it.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS
            },
        )
        override val alreadyCoveredByNormalRosterAmount: BigDecimal get() = moneySum(
            lines.filter {
                it.includedInKnownTotal &&
                    it.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER
            },
        )
        override val excludedKnownRuleAmount: BigDecimal get() = moneySum(lines.filterNot { it.includedInKnownTotal })
        override val preliminaryOrNull: PreliminaryCalculation? = null

        private fun moneySum(values: List<CalculationLine>): BigDecimal = values
            .fold(BigDecimal.ZERO) { total, line -> total.add(line.amount) }
            .setScale(2, java.math.RoundingMode.HALF_UP)
    }

    companion object {
        fun fromRuntime(calculation: TariffRuntimeCalculation): FinalizedCalculationPayload = when (calculation) {
            is TariffRuntimeCalculation.SingleContext -> Preliminary(calculation.preliminary)
            is TariffRuntimeCalculation.SegmentedContexts -> SegmentedContexts(
                lineEntries = calculation.segmented.lineEntries.map {
                    FinalizedScopedCalculationLineSnapshot.fromRuntime(it)
                },
                applicableUnresolvedRuleIds = calculation.applicableUnresolvedRuleIds,
            )
        }
    }
}

data class FinalizedTripSnapshot(
    val id: String,
    val createdAt: LocalDateTime,
    val appVersionName: String,
    val appVersionCode: Int,
    val rulesetVersion: String,
    val tariffPackageId: String,
    val tariffRateSetId: String,
    val salaryTableId: String,
    val salaryTableEffectiveFrom: LocalDate,
    val salaryTableSourceLabel: String,
    val tariffContexts: List<FinalizedTariffContextSnapshot>,
    val title: String,
    val tripStart: LocalDateTime,
    val tripEnd: LocalDateTime,
    val employerKind: EmployerKind,
    val payingParty: PayingParty,
    val rosterComparisonMode: RosterComparisonMode,
    val salaryStep: Int,
    val annualSalary: BigDecimal,
    val weeklyBasis: WeeklyBasis,
    val weekendProfile: WeekendProfile,
    val payslipChecked: Boolean,
    val rosterGapConfirmed: Boolean,
    val roster: List<RosterSnapshotRow>,
    val workBlocks: List<WorkBlock>,
    val calculationPayload: FinalizedCalculationPayload,
    val settlement: SettlementSnapshot,
    val findings: List<ControlFinding>,
    val unresolvedRules: List<DomainRule>,
    val holidayWorkPlanStatus: HolidayWorkPlanStatus = HolidayWorkPlanStatus.NOT_CLARIFIED,
    val workPlanBasis: TripWorkPlanBasis = TripWorkPlanBasis.NOT_CLARIFIED,
    val employerWorkPlanBlocks: List<WorkBlock> = emptyList(),
) {
    init {
        require(tariffContexts.isNotEmpty()) { "Ferdigstilt beregning må ha minst én tariffkontekst." }
        require(tariffContexts.all { it.rulesetVersion == rulesetVersion }) {
            "Alle tariffkontekster må bruke snapshotets frosne regelsett $rulesetVersion."
        }
        tariffContexts.zipWithNext().forEach { (previous, next) ->
            require(next.start == previous.end.plusDays(1)) {
                "Tariffkontekstene må være sammenhengende uten hull eller overlapp."
            }
        }
        val occupied = requireNotNull(TariffEffectiveDateRange.forTrip(tripStart, tripEnd)) {
            "Snapshotet har ugyldig turperiode."
        }
        require(tariffContexts.first().start == occupied.start && tariffContexts.last().end == occupied.end) {
            "Tariffkontekstene må dekke hele den effektive turperioden."
        }
        val primary = tariffContexts.first()
        require(primary.tariffPackageId == tariffPackageId) {
            "Primær tariffpakke samsvarer ikke med første tariffkontekst."
        }
        require(primary.tariffRateSetId == tariffRateSetId) {
            "Primært satssett samsvarer ikke med første tariffkontekst."
        }
        require(primary.salaryTableId == salaryTableId) {
            "Primær lønnstabell samsvarer ikke med første tariffkontekst."
        }
        require(primary.salaryTableEffectiveFrom == salaryTableEffectiveFrom) {
            "Primær lønnstabells virkningsdato samsvarer ikke med første tariffkontekst."
        }
        require(primary.salaryTableSourceLabel == salaryTableSourceLabel) {
            "Primær lønnstabellkilde samsvarer ikke med første tariffkontekst."
        }
        require(primary.annualSalary.compareTo(annualSalary) == 0) {
            "Primær årslønn samsvarer ikke med første tariffkontekst."
        }
        if (calculationPayload is FinalizedCalculationPayload.SegmentedContexts) {
            require(tariffContexts.size > 1) {
                "Segmentert beregningspayload krever flere tariffkontekster."
            }
            calculationPayload.lineEntries.forEach { entry ->
                if (entry.scope == TariffCalculationLineScope.SEGMENT_LOCAL) {
                    val sliceIndex = requireNotNull(entry.sliceIndex)
                    require(sliceIndex in tariffContexts.indices) {
                        "Segmentert beregningslinje peker på ukjent tariffslice $sliceIndex."
                    }
                }
            }
        }
    }

    /**
     * Compatibility view for callers that explicitly require the historical
     * single-context calculation. Finalized summary/PDF presentation uses
     * [presentation] and therefore never asks a segmented snapshot for a fake
     * single hourly rate.
     */
    val calculation: PreliminaryCalculation
        get() = requireNotNull(calculationPayload.preliminaryOrNull) {
            "Segmentert ferdigstilt beregning har ingen entydig PreliminaryCalculation/timelønn."
        }

    val preliminaryCalculationOrNull: PreliminaryCalculation? get() = calculationPayload.preliminaryOrNull
    val presentation: FinalizedCalculationPresentation
        get() = FinalizedCalculationPresentations.fromSnapshot(this)
    val hasSegmentedCalculation: Boolean
        get() = calculationPayload.mode == FinalizedCalculationPayloadMode.SEGMENTED_CONTEXTS
    val hasMultipleTariffContexts: Boolean get() = tariffContexts.size > 1

    val ruleBasis: String get() = when (employerKind) {
        EmployerKind.OSLO_KOMMUNE -> {
            val packageLabels = tariffContexts
                .map { context -> FerieturTariffs.packageForId(context.tariffPackageId)?.label ?: context.tariffPackageId }
                .distinct()
            if (packageLabels.size == 1) {
                val suffix = if (hasMultipleTariffContexts) " · ${tariffContexts.size} tariff-/lønnskontekster" else ""
                "Oslo kommune – ${packageLabels.single()}, kapittel 20$suffix"
            } else {
                "Oslo kommune – ${packageLabels.joinToString(" / ")}, kapittel 20"
            }
        }
        else -> employerKind.ruleBasisLabel()
    }
    val isRuleBasisConfirmed: Boolean get() = employerKind.isRuleBasisConfirmed()
    val isFrameworkComplete: Boolean get() = employerKind != EmployerKind.UNSPECIFIED
    val isConfirmedDocumentBasis: Boolean get() = isRuleBasisConfirmed && isFrameworkComplete
}

object FinalizedTripSnapshotBuilder {
    /**
     * Builds a finalized snapshot from the effective-dated runtime calculation
     * without collapsing multiple salary/rate contexts into one hourly rate.
     *
     * Compose is not wired to this entry point yet; it is the finalization
     * contract that A4A10 can call after obtaining a successful runtime result.
     */
    fun buildFromRuntime(
        title: String,
        employerKind: EmployerKind,
        payingParty: PayingParty,
        rosterComparisonMode: RosterComparisonMode,
        dates: List<LocalDate>,
        roster: Map<LocalDate, String>,
        plans: Map<LocalDate, List<PlannedBlock>>,
        salaryStep: Int,
        weeklyBasis: WeeklyBasis,
        weekendProfile: WeekendProfile,
        payslipChecked: Boolean,
        rosterGapConfirmed: Boolean = false,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
        runtimeCalculation: TariffRuntimeCalculation,
        settlement: SettlementSnapshot,
        snapshotId: String = UUID.randomUUID().toString(),
        createdAt: LocalDateTime = LocalDateTime.now(),
        appVersionName: String = "test",
        appVersionCode: Int = 0,
        holidayWorkPlanStatus: HolidayWorkPlanStatus = HolidayWorkPlanStatus.NOT_CLARIFIED,
        workPlanBasis: TripWorkPlanBasis = TripWorkPlanBasis.NOT_CLARIFIED,
        holidayPlans: Map<LocalDate, List<PlannedBlock>> = emptyMap(),
    ): FinalizedTripSnapshot {
        require(TripPlanEngine.chapter20Applies(tripStart, tripEnd)) {
            "Dok. 25 kapittel 20 gjelder ikke dagsturer"
        }
        TripDateRangePolicy.requireCompleteCoverage(
            dates = dates,
            start = tripStart.toLocalDate(),
            end = tripEnd.toLocalDate(),
        )
        val workBlocks = TripPlanEngine.projectRange(dates, plans)
        val employerWorkPlanBlocks = when (workPlanBasis) {
            TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN -> {
                require(rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
                    "Arbeidsgivers egen turplan krever at vanlig lønn/grunnturnus er valgt som lønnsscenario."
                }
                require(holidayWorkPlanStatus != HolidayWorkPlanStatus.NOT_CLARIFIED) {
                    "Arbeidsgivers arbeidsplan må ha avklart planstatus før ferdigstilling."
                }
                TripPlanEngine.projectRange(dates, holidayPlans).also { frozen ->
                    require(frozen.isNotEmpty()) {
                        "Arbeidsgivers arbeidsplan kan ikke være tom når den er valgt som planbasis."
                    }
                    require(TripPlanEngine.outsideTripRangeBlocks(frozen, tripStart, tripEnd).isEmpty()) {
                        "Arbeidsgivers arbeidsplan inneholder tid utenfor turperioden."
                    }
                    require(!TripPlanEngine.hasUnintendedOverlap(frozen)) {
                        "Arbeidsgivers arbeidsplan inneholder overlappende perioder."
                    }
                }
            }
            TripWorkPlanBasis.NORMAL_ROSTER_APPLIES -> {
                require(rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
                    "Grunnturnus kan bare være planbasis når vanlig lønn/grunnturnus er valgt."
                }
                emptyList()
            }
            TripWorkPlanBasis.NOT_CLARIFIED -> emptyList()
        }
        val finalizedHolidayWorkPlanStatus =
            if (workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN) {
                holidayWorkPlanStatus
            } else {
                HolidayWorkPlanStatus.NOT_CLARIFIED
            }
        require(TripPlanEngine.outsideTripRangeBlocks(workBlocks, tripStart, tripEnd).isEmpty()) {
            "Arbeidsplanen inneholder arbeid utenfor turperioden"
        }
        require(!TripPlanEngine.hasUnintendedOverlap(workBlocks)) {
            "Arbeidsplanen inneholder overlappende perioder"
        }
        require(settlement.calculatedAmount.setScale(2) == runtimeCalculation.paymentBasisAmount.setScale(2)) {
            "Betalingsgrunnlaget samsvarer ikke med runtime-beregningen"
        }

        val tariffContexts = FinalizedTariffContextSnapshots.fromRuntime(runtimeCalculation)
        require(tariffContexts.isNotEmpty()) { "Runtime-beregningen mangler tariffproveniens." }
        val occupied = requireNotNull(TariffEffectiveDateRange.forTrip(tripStart, tripEnd)) {
            "Ugyldig turperiode."
        }
        require(tariffContexts.first().start == occupied.start && tariffContexts.last().end == occupied.end) {
            "Runtime-beregningens tariffkontekster dekker ikke hele turperioden."
        }
        require(tariffContexts.all { it.rulesetVersion == runtimeCalculation.rulesetVersion }) {
            "Runtime-beregningen inneholder flere semantiske regelsett."
        }

        val rosterGapEvidence = if (rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            TripPlanEngine.rosterUncoveredEvidence(workBlocks, roster, tripStart, tripEnd)
        } else {
            emptyList()
        }
        require(rosterGapEvidence.isEmpty() || rosterGapConfirmed) {
            "Turnustid uten registrert arbeidsperiode må kontrolleres før ferdigstilling"
        }

        val unresolvedRules = FerieturRules.applicableUnresolvedRules(
            runtimeCalculation.applicableUnresolvedRuleIds,
        )
        val controlRateSet = requireNotNull(
            FinalizedCalculationPresentations.sharedControlRateSet(tariffContexts),
        ) {
            "Tariffkontekstene bruker ulike tids-/kontrollparametere. Presentasjons- og kontrollgrunnlaget må " +
                "koordineres eksplisitt før turen kan ferdigstilles."
        }
        val findings = TripPlanEngine.controlFindings(
            blocks = workBlocks,
            unresolvedRuleCount = unresolvedRules.size,
            roster = roster,
            rateSet = controlRateSet,
        )
        val rosterRows = if (rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            dates.flatMap { date ->
                RosterEntryCodec.decode(roster[date]).map { shift ->
                    val interval = TurnusOverlapEngine.intervalFor(date, shift)
                    RosterSnapshotRow(
                        date = date,
                        code = shift.code,
                        label = shift.label,
                        start = interval?.first,
                        end = interval?.second,
                    )
                }
            }
        } else {
            emptyList()
        }
        val primary = tariffContexts.first()
        UUID.fromString(snapshotId)
        return FinalizedTripSnapshot(
            id = snapshotId,
            createdAt = createdAt,
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            rulesetVersion = runtimeCalculation.rulesetVersion,
            tariffPackageId = primary.tariffPackageId,
            tariffRateSetId = primary.tariffRateSetId,
            salaryTableId = primary.salaryTableId,
            salaryTableEffectiveFrom = primary.salaryTableEffectiveFrom,
            salaryTableSourceLabel = primary.salaryTableSourceLabel,
            tariffContexts = tariffContexts,
            title = title,
            tripStart = tripStart,
            tripEnd = tripEnd,
            employerKind = employerKind,
            payingParty = payingParty,
            rosterComparisonMode = rosterComparisonMode,
            salaryStep = salaryStep,
            annualSalary = primary.annualSalary,
            weeklyBasis = weeklyBasis,
            weekendProfile = weekendProfile,
            payslipChecked = payslipChecked,
            rosterGapConfirmed = rosterGapConfirmed,
            roster = rosterRows,
            workBlocks = workBlocks,
            calculationPayload = FinalizedCalculationPayload.fromRuntime(runtimeCalculation),
            settlement = settlement,
            findings = findings,
            unresolvedRules = unresolvedRules,
            holidayWorkPlanStatus = finalizedHolidayWorkPlanStatus,
            workPlanBasis = workPlanBasis,
            employerWorkPlanBlocks = employerWorkPlanBlocks,
        )
    }

    fun build(
        title: String,
        employerKind: EmployerKind,
        payingParty: PayingParty,
        rosterComparisonMode: RosterComparisonMode,
        dates: List<LocalDate>,
        roster: Map<LocalDate, String>,
        plans: Map<LocalDate, List<PlannedBlock>>,
        salaryStep: Int,
        annualSalary: BigDecimal,
        weeklyBasis: WeeklyBasis,
        weekendProfile: WeekendProfile,
        payslipChecked: Boolean,
        rosterGapConfirmed: Boolean = false,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
        settlement: SettlementSnapshot,
        snapshotId: String = UUID.randomUUID().toString(),
        createdAt: LocalDateTime = LocalDateTime.now(),
        appVersionName: String = "test",
        appVersionCode: Int = 0,
        tariffPackageId: String = FerieturTariffs.DOK25_2026_2028_ID,
        tariffRateSetId: String = FerieturTariffRates.DOK25_2026_2028_RATE_SET_ID,
        rulesetVersion: String = FerieturTariffs.requireById(tariffPackageId).rulesetVersion,
        salaryTableId: String = "test-table",
        salaryTableEffectiveFrom: LocalDate = tripStart.toLocalDate(),
        salaryTableSourceLabel: String = "Test table",
        holidayWorkPlanStatus: HolidayWorkPlanStatus = HolidayWorkPlanStatus.NOT_CLARIFIED,
    ): FinalizedTripSnapshot {
        require(TripPlanEngine.chapter20Applies(tripStart, tripEnd)) {
            "Dok. 25 kapittel 20 gjelder ikke dagsturer"
        }
        val tariffPackage = FerieturTariffs.requireById(tariffPackageId)
        require(tariffPackage.coversRange(tripStart.toLocalDate(), tripEnd.toLocalDate())) {
            "Tariffpakken $tariffPackageId dekker ikke hele turperioden."
        }
        require(tariffPackage.rulesetVersion == rulesetVersion) {
            "Regelsett $rulesetVersion samsvarer ikke med tariffpakken $tariffPackageId."
        }
        val tariffRateSet = FerieturTariffRates.requireById(tariffRateSetId)
        require(tariffRateSet.tariffPackageId == tariffPackageId) {
            "Satssett $tariffRateSetId tilhører ikke tariffpakken $tariffPackageId."
        }
        require(tariffRateSet.coversRange(tripStart.toLocalDate(), tripEnd.toLocalDate())) {
            "Satssett $tariffRateSetId dekker ikke hele turperioden."
        }
        TripDateRangePolicy.requireCompleteCoverage(
            dates = dates,
            start = tripStart.toLocalDate(),
            end = tripEnd.toLocalDate(),
        )
        require(!tripStart.toLocalDate().isBefore(salaryTableEffectiveFrom)) {
            "Lønnstabellen $salaryTableId gjelder ikke fra turens startdato."
        }
        val fundingMode = rosterComparisonMode.toFundingMode()
        val calculation = TripPlanEngine.calculatePreliminary(
            fundingMode = fundingMode,
            dates = dates,
            roster = roster,
            plans = plans,
            annualSalary = annualSalary,
            weeklyBasis = weeklyBasis,
            weekendProfile = weekendProfile,
            tripStart = tripStart,
            tripEnd = tripEnd,
            rateSet = tariffRateSet,
        )
        val workBlocks = TripPlanEngine.projectRange(dates, plans)
        require(TripPlanEngine.outsideTripRangeBlocks(workBlocks, tripStart, tripEnd).isEmpty()) {
            "Arbeidsplanen inneholder arbeid utenfor turperioden"
        }
        require(!TripPlanEngine.hasUnintendedOverlap(workBlocks)) {
            "Arbeidsplanen inneholder overlappende perioder"
        }
        require(settlement.calculatedAmount.setScale(2) == calculation.paymentBasisAmount.setScale(2)) {
            "Betalingsgrunnlaget samsvarer ikke med beregningen"
        }
        require(calculation.rosterUncoveredMinutes == 0L || rosterGapConfirmed) {
            "Turnustid uten registrert arbeidsperiode må kontrolleres før ferdigstilling"
        }
        val unresolvedRules = FerieturRules.applicableUnresolvedRules(calculation)
        val findings = TripPlanEngine.controlFindings(workBlocks, unresolvedRules.size, roster, tariffRateSet)
        val rosterRows = if (rosterComparisonMode == RosterComparisonMode.USE_NORMAL_ROSTER) {
            dates.flatMap { date ->
                RosterEntryCodec.decode(roster[date]).map { shift ->
                    val interval = TurnusOverlapEngine.intervalFor(date, shift)
                    RosterSnapshotRow(
                        date = date,
                        code = shift.code,
                        label = shift.label,
                        start = interval?.first,
                        end = interval?.second,
                    )
                }
            }
        } else {
            emptyList()
        }
        UUID.fromString(snapshotId)
        return FinalizedTripSnapshot(
            id = snapshotId,
            createdAt = createdAt,
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            rulesetVersion = rulesetVersion,
            tariffPackageId = tariffPackageId,
            tariffRateSetId = tariffRateSetId,
            salaryTableId = salaryTableId,
            salaryTableEffectiveFrom = salaryTableEffectiveFrom,
            salaryTableSourceLabel = salaryTableSourceLabel,
            tariffContexts = listOf(
                FinalizedTariffContextSnapshot(
                    start = requireNotNull(TariffEffectiveDateRange.forTrip(tripStart, tripEnd)).start,
                    end = requireNotNull(TariffEffectiveDateRange.forTrip(tripStart, tripEnd)).end,
                    tariffPackageId = tariffPackageId,
                    rulesetVersion = rulesetVersion,
                    tariffRateSetId = tariffRateSetId,
                    salaryTableId = salaryTableId,
                    salaryTableEffectiveFrom = salaryTableEffectiveFrom,
                    salaryTableSourceLabel = salaryTableSourceLabel,
                    annualSalary = annualSalary,
                    hourlyRate = calculation.hourlyRate,
                ),
            ),
            title = title,
            tripStart = tripStart,
            tripEnd = tripEnd,
            employerKind = employerKind,
            payingParty = payingParty,
            rosterComparisonMode = rosterComparisonMode,
            salaryStep = salaryStep,
            annualSalary = annualSalary,
            weeklyBasis = weeklyBasis,
            weekendProfile = weekendProfile,
            payslipChecked = payslipChecked,
            rosterGapConfirmed = rosterGapConfirmed,
            roster = rosterRows,
            workBlocks = workBlocks,
            calculationPayload = FinalizedCalculationPayload.Preliminary(calculation),
            settlement = settlement,
            findings = findings,
            unresolvedRules = unresolvedRules,
            holidayWorkPlanStatus = holidayWorkPlanStatus,
        )
    }
}
