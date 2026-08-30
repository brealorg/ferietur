package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

const val FERIETUR_RULESET_VERSION = "2026.3"

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
    val calculation: PreliminaryCalculation,
    val settlement: SettlementSnapshot,
    val findings: List<ControlFinding>,
    val unresolvedRules: List<DomainRule>,
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
    }

    val hasMultipleTariffContexts: Boolean get() = tariffContexts.size > 1

    val ruleBasis: String get() = when (employerKind) {
        EmployerKind.OSLO_KOMMUNE -> {
            val tariffLabel = FerieturTariffs.packageForId(tariffPackageId)?.label ?: tariffPackageId
            "Oslo kommune – $tariffLabel, kapittel 20"
        }
        else -> employerKind.ruleBasisLabel()
    }
    val isRuleBasisConfirmed: Boolean get() = employerKind.isRuleBasisConfirmed()
    val isFrameworkComplete: Boolean get() = employerKind != EmployerKind.UNSPECIFIED
    val isConfirmedDocumentBasis: Boolean get() = isRuleBasisConfirmed && isFrameworkComplete
}

object FinalizedTripSnapshotBuilder {
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
            calculation = calculation,
            settlement = settlement,
            findings = findings,
            unresolvedRules = unresolvedRules,
        )
    }
}
