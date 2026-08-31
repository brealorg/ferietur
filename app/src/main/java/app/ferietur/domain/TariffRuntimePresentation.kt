package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Live presentation-safe view of an effective-dated runtime calculation.
 *
 * This is intentionally parallel to [FinalizedCalculationPresentation], but is
 * derived from current draft inputs rather than frozen snapshot data. It gives
 * Compose one common surface for both single-context and segmented calculations
 * without inventing a single hourly rate for a multi-context trip.
 */
data class TariffRuntimePresentationLine(
    val key: String,
    val scope: TariffCalculationLineScope,
    val line: CalculationLine,
    val tariffContextIndex: Int?,
    val watchSourceIndex: Int? = null,
)

data class TariffRuntimeCalculationPresentation(
    val mode: TariffRuntimeCalculationMode,
    val lineEntries: List<TariffRuntimePresentationLine>,
    val knownAmount: BigDecimal,
    val paymentBasisAmount: BigDecimal,
    val alreadyCoveredByNormalRosterAmount: BigDecimal,
    val excludedKnownRuleAmount: BigDecimal,
    val applicableUnresolvedRuleIds: Set<String>,
    val dayAudits: List<DayCalculationAudit>,
    val rosterMinutes: Long,
    val rosterUncoveredMinutes: Long,
    val rosterUncoveredEvidence: List<CalculationEvidence>,
    val activeInsideRosterMinutes: Long,
    val restingNightMinutes: Long,
    val restingNightOutsideRosterMinutes: Long,
    val provenance: List<TariffRuntimeProvenanceSlice>,
    val sharedControlRateSet: TariffRateSet?,
) {
    val lines: List<CalculationLine> get() = lineEntries.map { it.line }
    val hasMultipleTariffContexts: Boolean get() = provenance.size > 1
    val singleHourlyRateOrNull: BigDecimal?
        get() = provenance.singleOrNull()?.hourlyRate

    fun contextForLine(entry: TariffRuntimePresentationLine): TariffRuntimeProvenanceSlice? =
        entry.tariffContextIndex?.let(provenance::getOrNull)
}

object TariffRuntimeCalculationPresentations {
    fun fromRuntime(
        calculation: TariffRuntimeCalculation,
        fundingMode: FundingMode,
        dates: List<LocalDate>,
        roster: Map<LocalDate, String>,
        plans: Map<LocalDate, List<PlannedBlock>>,
        weeklyBasis: WeeklyBasis,
        tripStart: java.time.LocalDateTime,
        tripEnd: java.time.LocalDateTime,
    ): TariffRuntimeCalculationPresentation {
        val blocks = TripPlanEngine.projectRange(dates, plans)
        val worktime = when (calculation) {
            is TariffRuntimeCalculation.SingleContext -> CalculationWorktimeAudit(
                rosterMinutes = calculation.preliminary.rosterMinutes,
                rosterUncoveredMinutes = calculation.preliminary.rosterUncoveredMinutes,
                rosterUncoveredEvidence = calculation.preliminary.rosterUncoveredEvidence,
                activeMinutes = calculation.preliminary.activeMinutes,
                activeInsideRosterMinutes = calculation.preliminary.activeInsideRosterMinutes,
                activeOutsideRosterMinutes = calculation.preliminary.activeOutsideRosterMinutes,
                restingNightMinutes = calculation.preliminary.restingNightMinutes,
                restingNightOutsideRosterMinutes = calculation.preliminary.restingNightOutsideRosterMinutes,
            )

            is TariffRuntimeCalculation.SegmentedContexts -> TripPlanEngine.buildWorktimeAudit(
                fundingMode = fundingMode,
                blocks = blocks,
                roster = roster,
                tripStart = tripStart,
                tripEnd = tripEnd,
            )
        }

        val entries = when (calculation) {
            is TariffRuntimeCalculation.SingleContext -> calculation.preliminary.lines.mapIndexed { index, line ->
                TariffRuntimePresentationLine(
                    key = "single:$index:${line.id}",
                    scope = TariffCalculationLineScopes.requireForLineId(line.id),
                    line = line,
                    tariffContextIndex = 0,
                )
            }

            is TariffRuntimeCalculation.SegmentedContexts -> calculation.segmented.lineEntries.mapIndexed { index, entry ->
                TariffRuntimePresentationLine(
                    key = "segmented:$index:${entry.scope}:${entry.sliceIndex ?: -1}:${entry.watchSourceIndex ?: -1}:${entry.line.id}",
                    scope = entry.scope,
                    line = entry.line,
                    tariffContextIndex = when (entry.scope) {
                        TariffCalculationLineScope.SEGMENT_LOCAL -> entry.sliceIndex
                        TariffCalculationLineScope.WHOLE_TRIP -> sharedRateSetContextIndex(calculation.provenance)
                        TariffCalculationLineScope.PER_RESTING_WATCH -> restingWatchPricingContextIndex(
                            calculation.provenance,
                            entry.line,
                        )
                    },
                    watchSourceIndex = entry.watchSourceIndex,
                )
            }
        }

        return TariffRuntimeCalculationPresentation(
            mode = calculation.mode,
            lineEntries = entries,
            knownAmount = calculation.knownAmount,
            paymentBasisAmount = calculation.paymentBasisAmount,
            alreadyCoveredByNormalRosterAmount = calculation.alreadyCoveredByNormalRosterAmount,
            excludedKnownRuleAmount = calculation.excludedKnownRuleAmount,
            applicableUnresolvedRuleIds = calculation.applicableUnresolvedRuleIds,
            dayAudits = when (calculation) {
                is TariffRuntimeCalculation.SingleContext -> calculation.preliminary.dayAudits
                is TariffRuntimeCalculation.SegmentedContexts -> TripPlanEngine.buildDayAudits(
                    dates = dates,
                    weeklyBasis = weeklyBasis,
                    lines = calculation.lines,
                )
            },
            rosterMinutes = worktime.rosterMinutes,
            rosterUncoveredMinutes = worktime.rosterUncoveredMinutes,
            rosterUncoveredEvidence = worktime.rosterUncoveredEvidence,
            activeInsideRosterMinutes = worktime.activeInsideRosterMinutes,
            restingNightMinutes = worktime.restingNightMinutes,
            restingNightOutsideRosterMinutes = worktime.restingNightOutsideRosterMinutes,
            provenance = calculation.provenance,
            sharedControlRateSet = sharedControlRateSet(calculation),
        )
    }

    private fun sharedControlRateSet(calculation: TariffRuntimeCalculation): TariffRateSet? {
        val rateSets = when (calculation) {
            is TariffRuntimeCalculation.SingleContext -> calculation.provenance.map { slice ->
                FerieturTariffRates.forId(slice.tariffRateSetId) ?: return null
            }
            is TariffRuntimeCalculation.SegmentedContexts -> calculation.segmented.plan.slices.map { it.segment.rateSet }
        }
        val first = rateSets.firstOrNull() ?: return null
        return first.takeIf { baseline ->
            rateSets.all { candidate ->
                candidate.travelSleepWindowStart == baseline.travelSleepWindowStart &&
                    candidate.travelSleepWindowEnd == baseline.travelSleepWindowEnd &&
                    candidate.passiveWorkDivisor == baseline.passiveWorkDivisor &&
                    candidate.shortNoticeMaxMinutes == baseline.shortNoticeMaxMinutes
            }
        }
    }

    private fun sharedRateSetContextIndex(provenance: List<TariffRuntimeProvenanceSlice>): Int? =
        provenance.takeIf { values -> values.map { it.tariffRateSetId }.distinct().size == 1 }?.let { 0 }

    private fun restingWatchPricingContextIndex(
        provenance: List<TariffRuntimeProvenanceSlice>,
        line: CalculationLine,
    ): Int? {
        val exactEventEvidence = line.evidence.filterNot { evidence ->
            evidence.note.contains("betalt etter avrunding", ignoreCase = true)
        }
        val indexes = exactEventEvidence.mapNotNull { evidence ->
            contextIndexForDate(provenance, evidence.start.toLocalDate())
        }.distinct()
        if (indexes.size == 1) return indexes.single()
        if (indexes.isNotEmpty()) {
            val rateSetIds = indexes.map { provenance[it].tariffRateSetId }.distinct()
            if (rateSetIds.size == 1) return indexes.first()
        }
        return null
    }

    private fun contextIndexForDate(
        provenance: List<TariffRuntimeProvenanceSlice>,
        date: LocalDate,
    ): Int? = provenance.indexOfFirst { !date.isBefore(it.start) && !date.isAfter(it.end) }
        .takeIf { it >= 0 }
}
