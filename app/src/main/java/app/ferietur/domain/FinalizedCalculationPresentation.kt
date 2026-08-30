package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Presentation-safe view of a finalized calculation.
 *
 * Unlike [PreliminaryCalculation], this model does not assume that the entire
 * trip has one hourly rate. It is derived only from frozen snapshot data and
 * can therefore be used by the summary/PDF path for both legacy single-context
 * and v4 segmented snapshots.
 */
data class FinalizedCalculationPresentationLine(
    val scope: TariffCalculationLineScope,
    val line: CalculationLine,
    val tariffContextIndex: Int?,
    val watchSourceIndex: Int? = null,
)

data class FinalizedCalculationPresentation(
    val mode: FinalizedCalculationPayloadMode,
    val lineEntries: List<FinalizedCalculationPresentationLine>,
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
    val tariffContexts: List<FinalizedTariffContextSnapshot>,
) {
    val lines: List<CalculationLine> get() = lineEntries.map { it.line }
    val hasMultipleTariffContexts: Boolean get() = tariffContexts.size > 1

    fun contextForLine(entry: FinalizedCalculationPresentationLine): FinalizedTariffContextSnapshot? =
        entry.tariffContextIndex?.let(tariffContexts::getOrNull)

    fun rateSetForLine(entry: FinalizedCalculationPresentationLine): TariffRateSet? =
        contextForLine(entry)?.tariffRateSetId?.let(FerieturTariffRates::forId)

    fun contextLabelForLine(entry: FinalizedCalculationPresentationLine): String? =
        contextForLine(entry)?.let { context ->
            if (context.start == context.end) context.start.toString() else "${context.start}–${context.end}"
        }
}

object FinalizedCalculationPresentations {
    /**
     * Returns one rate set only when every context agrees on the numeric values
     * used by the shared worktime/control copy. Monetary line presentation can
     * still use per-line contexts when this returns null.
     */
    fun sharedControlRateSet(contexts: List<FinalizedTariffContextSnapshot>): TariffRateSet? {
        val rateSets = contexts.map { context ->
            FerieturTariffRates.forId(context.tariffRateSetId) ?: return null
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

    fun fromSnapshot(snapshot: FinalizedTripSnapshot): FinalizedCalculationPresentation {
        val payload = snapshot.calculationPayload
        val preliminary = payload.preliminaryOrNull
        val lineEntries = when (payload) {
            is FinalizedCalculationPayload.Preliminary -> payload.calculation.lines.map { line ->
                FinalizedCalculationPresentationLine(
                    scope = TariffCalculationLineScopes.requireForLineId(line.id),
                    line = line,
                    tariffContextIndex = 0,
                )
            }

            is FinalizedCalculationPayload.SegmentedContexts -> payload.lineEntries.map { entry ->
                FinalizedCalculationPresentationLine(
                    scope = entry.scope,
                    line = entry.line,
                    tariffContextIndex = when (entry.scope) {
                        TariffCalculationLineScope.SEGMENT_LOCAL -> entry.sliceIndex
                        TariffCalculationLineScope.WHOLE_TRIP -> sharedRateSetContextIndex(snapshot.tariffContexts)
                        TariffCalculationLineScope.PER_RESTING_WATCH -> restingWatchPricingContextIndex(
                            snapshot.tariffContexts,
                            entry.line,
                        )
                    },
                    watchSourceIndex = entry.watchSourceIndex,
                )
            }
        }

        val dates = TripDateRangePolicy.inclusiveDates(
            snapshot.tripStart.toLocalDate(),
            snapshot.tripEnd.toLocalDate(),
        )
        val rosterAudit = if (preliminary != null) {
            FrozenRosterAudit(
                rosterMinutes = preliminary.rosterMinutes,
                uncoveredEvidence = preliminary.rosterUncoveredEvidence,
                activeInsideRosterMinutes = preliminary.activeInsideRosterMinutes,
                restingNightMinutes = preliminary.restingNightMinutes,
                restingNightOutsideRosterMinutes = preliminary.restingNightOutsideRosterMinutes,
            )
        } else {
            deriveFrozenRosterAudit(snapshot)
        }

        return FinalizedCalculationPresentation(
            mode = payload.mode,
            lineEntries = lineEntries,
            knownAmount = payload.knownAmount,
            paymentBasisAmount = payload.paymentBasisAmount,
            alreadyCoveredByNormalRosterAmount = payload.alreadyCoveredByNormalRosterAmount,
            excludedKnownRuleAmount = payload.excludedKnownRuleAmount,
            applicableUnresolvedRuleIds = payload.applicableUnresolvedRuleIds,
            dayAudits = preliminary?.dayAudits ?: TripPlanEngine.buildDayAudits(
                dates = dates,
                weeklyBasis = snapshot.weeklyBasis,
                lines = payload.lines,
            ),
            rosterMinutes = rosterAudit.rosterMinutes,
            rosterUncoveredMinutes = rosterAudit.uncoveredEvidence.sumOf { it.minutes },
            rosterUncoveredEvidence = rosterAudit.uncoveredEvidence,
            activeInsideRosterMinutes = rosterAudit.activeInsideRosterMinutes,
            restingNightMinutes = rosterAudit.restingNightMinutes,
            restingNightOutsideRosterMinutes = rosterAudit.restingNightOutsideRosterMinutes,
            tariffContexts = snapshot.tariffContexts,
        )
    }

    private fun sharedRateSetContextIndex(contexts: List<FinalizedTariffContextSnapshot>): Int? =
        contexts.takeIf { values -> values.map { it.tariffRateSetId }.distinct().size == 1 }?.let { 0 }

    private fun restingWatchPricingContextIndex(
        contexts: List<FinalizedTariffContextSnapshot>,
        line: CalculationLine,
    ): Int? {
        val exactEventEvidence = line.evidence.filterNot { evidence ->
            evidence.note.contains("betalt etter avrunding", ignoreCase = true)
        }
        val indexes = exactEventEvidence.mapNotNull { evidence ->
            contextIndexForDate(contexts, evidence.start.toLocalDate())
        }.distinct()
        if (indexes.size == 1) return indexes.single()
        if (indexes.isNotEmpty()) {
            val rateSetIds = indexes.map { contexts[it].tariffRateSetId }.distinct()
            if (rateSetIds.size == 1) return indexes.first()
        }
        return null
    }

    private fun contextIndexForDate(
        contexts: List<FinalizedTariffContextSnapshot>,
        date: LocalDate,
    ): Int? = contexts.indexOfFirst { !date.isBefore(it.start) && !date.isAfter(it.end) }
        .takeIf { it >= 0 }

    private data class FrozenRosterAudit(
        val rosterMinutes: Long,
        val uncoveredEvidence: List<CalculationEvidence>,
        val activeInsideRosterMinutes: Long,
        val restingNightMinutes: Long,
        val restingNightOutsideRosterMinutes: Long,
    )

    private data class Interval(
        val start: LocalDateTime,
        val end: LocalDateTime,
    ) {
        init {
            require(end.isAfter(start))
        }

        val minutes: Long get() = ChronoUnit.MINUTES.between(start, end)
    }

    private fun deriveFrozenRosterAudit(snapshot: FinalizedTripSnapshot): FrozenRosterAudit {
        val rosterIntervals = mergeIntervals(snapshot.roster.mapNotNull { row ->
            val start = row.start ?: return@mapNotNull null
            val end = row.end ?: return@mapNotNull null
            intersect(start, end, snapshot.tripStart, snapshot.tripEnd)?.let { (clippedStart, clippedEnd) ->
                Interval(clippedStart, clippedEnd)
            }
        })
        val registeredIntervals = mergeIntervals(
            snapshot.workBlocks
                .filterNot { it.kind == TimeKind.ACTIVE_EVENT_ON_RESTING }
                .map { Interval(it.start, it.end) },
        )
        val uncovered = rosterIntervals.flatMap { rosterInterval ->
            subtract(rosterInterval, registeredIntervals).map { interval ->
                CalculationEvidence(
                    start = interval.start,
                    end = interval.end,
                    minutes = interval.minutes,
                    note = "Turnustid uten registrert arbeidsperiode på turen",
                )
            }
        }

        val activeIntervals = mergeIntervals(
            snapshot.workBlocks
                .filter {
                    it.kind == TimeKind.ACTIVE_WORK ||
                        it.kind == TimeKind.ACTIVE_NIGHT_WATCH ||
                        it.kind == TimeKind.TRAVEL_WITH_RESPONSIBILITY
                }
                .map { Interval(it.start, it.end) },
        )
        val activeInside = overlapMinutes(activeIntervals, rosterIntervals)

        val restingIntervals = mergeIntervals(
            snapshot.workBlocks
                .filter { it.kind == TimeKind.RESTING_NIGHT_WATCH }
                .map { Interval(it.start, it.end) },
        )
        val restingMinutes = restingIntervals.sumOf { it.minutes }
        val restingInside = overlapMinutes(restingIntervals, rosterIntervals).coerceAtMost(restingMinutes)

        return FrozenRosterAudit(
            rosterMinutes = rosterIntervals.sumOf { it.minutes },
            uncoveredEvidence = uncovered,
            activeInsideRosterMinutes = activeInside,
            restingNightMinutes = restingMinutes,
            restingNightOutsideRosterMinutes = (restingMinutes - restingInside).coerceAtLeast(0),
        )
    }

    private fun overlapMinutes(first: List<Interval>, second: List<Interval>): Long =
        first.sumOf { left ->
            second.sumOf { right ->
                intersect(left.start, left.end, right.start, right.end)?.let { (start, end) ->
                    ChronoUnit.MINUTES.between(start, end)
                } ?: 0L
            }
        }

    private fun mergeIntervals(values: List<Interval>): List<Interval> {
        if (values.isEmpty()) return emptyList()
        val sorted = values.sortedBy { it.start }
        val result = mutableListOf(sorted.first())
        sorted.drop(1).forEach { next ->
            val previous = result.last()
            if (!next.start.isAfter(previous.end)) {
                result[result.lastIndex] = Interval(previous.start, maxOf(previous.end, next.end))
            } else {
                result += next
            }
        }
        return result
    }

    private fun subtract(base: Interval, covered: List<Interval>): List<Interval> {
        var cursor = base.start
        val result = mutableListOf<Interval>()
        covered.forEach { interval ->
            val overlap = intersect(base.start, base.end, interval.start, interval.end) ?: return@forEach
            if (overlap.first.isAfter(cursor)) result += Interval(cursor, overlap.first)
            if (overlap.second.isAfter(cursor)) cursor = overlap.second
        }
        if (cursor.isBefore(base.end)) result += Interval(cursor, base.end)
        return result
    }

    private fun intersect(
        firstStart: LocalDateTime,
        firstEnd: LocalDateTime,
        secondStart: LocalDateTime,
        secondEnd: LocalDateTime,
    ): Pair<LocalDateTime, LocalDateTime>? {
        val start = maxOf(firstStart, secondStart)
        val end = minOf(firstEnd, secondEnd)
        return if (end.isAfter(start)) start to end else null
    }
}
