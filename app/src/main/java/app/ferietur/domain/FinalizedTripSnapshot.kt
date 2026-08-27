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

data class FinalizedTripSnapshot(
    val id: String,
    val createdAt: LocalDateTime,
    val appVersionName: String,
    val appVersionCode: Int,
    val rulesetVersion: String,
    val salaryTableId: String,
    val salaryTableEffectiveFrom: LocalDate,
    val salaryTableSourceLabel: String,
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
    val ruleBasis: String get() = employerKind.ruleBasisLabel()
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
        rulesetVersion: String = FERIETUR_RULESET_VERSION,
        salaryTableId: String = "test-table",
        salaryTableEffectiveFrom: LocalDate = tripStart.toLocalDate(),
        salaryTableSourceLabel: String = "Test table",
    ): FinalizedTripSnapshot {
        require(TripPlanEngine.chapter20Applies(tripStart, tripEnd)) {
            "Dok. 25 kapittel 20 gjelder ikke dagsturer"
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
        val findings = TripPlanEngine.controlFindings(workBlocks, unresolvedRules.size, roster)
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
            salaryTableId = salaryTableId,
            salaryTableEffectiveFrom = salaryTableEffectiveFrom,
            salaryTableSourceLabel = salaryTableSourceLabel,
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
