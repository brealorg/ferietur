package app.ferietur.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class TariffScopedCalculationLine(
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
}

data class SegmentedTariffMonetaryCalculation(
    val plan: SegmentedTariffCalculationPlan,
    val scopePlan: TariffWholeTripScopePlan,
    val lineEntries: List<TariffScopedCalculationLine>,
) {
    val lines: List<CalculationLine> get() = lineEntries.map { it.line }

    val knownAmount: BigDecimal get() = moneySum(lines.filter { it.includedInKnownTotal })

    val paymentBasisAmount: BigDecimal get() = moneySum(
        lines.filter {
            it.includedInKnownTotal && it.paymentTreatment == PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS
        },
    )

    val alreadyCoveredByNormalRosterAmount: BigDecimal get() = moneySum(
        lines.filter {
            it.includedInKnownTotal && it.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER
        },
    )

    val excludedKnownRuleAmount: BigDecimal get() = moneySum(lines.filterNot { it.includedInKnownTotal })

    val applicableUnresolvedRuleIds: Set<String> get() = lines.mapNotNull { line ->
        when (line.id) {
            "travel-night-sleep-open" -> "D25_20_3_SLEEP_PERMISSION"
            "travel-notice-open" -> "D25_18_4_NOTICE"
            "travel-short-notice-133-open" -> "D25_18_4_X13_7_3"
            "stay-allowance-exact-threshold-open" -> "D25_20_6_EXACT_THRESHOLD"
            else -> null
        }
    }.toSet()

    companion object {
        private fun moneySum(lines: List<CalculationLine>): BigDecimal = lines
            .fold(BigDecimal.ZERO) { total, line -> total.add(line.amount) }
            .setScale(2, RoundingMode.HALF_UP)
    }
}

enum class TariffSegmentedMonetaryFailureReason {
    WHOLE_TRIP_SCOPE_UNSAFE,
    SOURCE_BLOCK_RECONSTRUCTION_FAILED,
    SPLIT_TRAVEL_NOTICE_COORDINATION_REQUIRED,
    RESTING_WATCH_SOURCE_MISSING,
    RESTING_EVENT_SOURCE_MISSING,
    RESTING_WATCH_PRICING_CONTEXT_MISSING,
    RESTING_WATCH_LINE_MISSING,
}

sealed interface TariffSegmentedMonetaryResult {
    data class Success(
        val calculation: SegmentedTariffMonetaryCalculation,
    ) : TariffSegmentedMonetaryResult

    data class Failure(
        val reason: TariffSegmentedMonetaryFailureReason,
        val detail: String,
        val sourceIndex: Int? = null,
        val wholeTripScopeReason: TariffWholeTripScopeFailureReason? = null,
    ) : TariffSegmentedMonetaryResult
}

/**
 * First monetary coordinator for effective-dated tariff slices.
 *
 * A split calculation is built from three disjoint ownership scopes:
 * - segment-local lines are calculated once per slice using that slice's frozen
 *   salary and rate set;
 * - stay allowance is calculated once for the whole trip after A4A4 has proved
 *   that its policy is invariant across the trip;
 * - active work under resting night is calculated once per reconstructed
 *   resting watch using the one pricing context A4A4 proved to be unambiguous.
 *
 * Short-notice/unresolved travel notice remains fail-closed for split trips in
 * this slice. Its shared cap plus overtime-band rounding needs a dedicated
 * cross-rate allocation rule before it can be priced safely across a boundary.
 */
object TariffSegmentedMonetaryCoordinator {
    fun calculate(
        plan: SegmentedTariffCalculationPlan,
        fundingMode: FundingMode,
        roster: Map<java.time.LocalDate, String>,
        weekendProfile: WeekendProfile,
    ): TariffSegmentedMonetaryResult {
        val scopeResult = TariffWholeTripScopeCoordinator.coordinate(plan)
        if (scopeResult is TariffWholeTripScopeResult.Failure) {
            return TariffSegmentedMonetaryResult.Failure(
                reason = TariffSegmentedMonetaryFailureReason.WHOLE_TRIP_SCOPE_UNSAFE,
                detail = scopeResult.detail,
                sourceIndex = scopeResult.sourceIndex,
                wholeTripScopeReason = scopeResult.reason,
            )
        }
        val scopePlan = (scopeResult as TariffWholeTripScopeResult.Success).scopePlan

        val reconstructedResult = TariffSourceBlockReconstructor.reconstruct(plan)
        if (reconstructedResult is TariffSourceBlockReconstructionResult.Failure) {
            return TariffSegmentedMonetaryResult.Failure(
                reason = TariffSegmentedMonetaryFailureReason.SOURCE_BLOCK_RECONSTRUCTION_FAILED,
                detail = reconstructedResult.detail,
                sourceIndex = reconstructedResult.sourceIndex,
            )
        }
        val reconstructed = (reconstructedResult as TariffSourceBlockReconstructionResult.Success).blocks

        if (!plan.isSplit) {
            return singleSliceCalculation(
                plan = plan,
                scopePlan = scopePlan,
                fundingMode = fundingMode,
                roster = roster,
                weekendProfile = weekendProfile,
            )
        }

        val splitNoticeSource = reconstructed.firstOrNull { source ->
            source.block.kind.isTravelWithoutResponsibility() &&
                source.block.travelNoticeStatus != TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY
        }
        if (splitNoticeSource != null) {
            return TariffSegmentedMonetaryResult.Failure(
                reason = TariffSegmentedMonetaryFailureReason.SPLIT_TRAVEL_NOTICE_COORDINATION_REQUIRED,
                detail = "Reise uten tilsynsansvar med kort eller uavklart varsel krysser en tur med flere " +
                    "tariff-/lønnskontekster. Punkt 18.4 bruker én felles tidsgrense og punkt 13.3 avrunder " +
                    "overtidsgrunnlaget. Ferietur priser derfor ikke denne whole-trip-posten på tvers av " +
                    "effektivdatogrensen før en egen fordelingsregel er implementert.",
                sourceIndex = splitNoticeSource.sourceIndex,
            )
        }

        val entries = mutableListOf<TariffScopedCalculationLine>()

        plan.slices.forEachIndexed { sliceIndex, slice ->
            val sliceCalculation = TripPlanEngine.calculatePreliminaryFromProjectedBlocks(
                fundingMode = fundingMode,
                dates = slice.dates,
                roster = roster,
                blocks = slice.workBlocks.map { it.block },
                annualSalary = slice.annualSalary,
                weeklyBasis = plan.weeklyBasis,
                weekendProfile = weekendProfile,
                tripStart = slice.windowStart,
                tripEnd = slice.windowEnd,
                rateSet = slice.segment.rateSet,
            )
            sliceCalculation.lines
                .filter { TariffCalculationLineScopes.requireForLineId(it.id) == TariffCalculationLineScope.SEGMENT_LOCAL }
                .forEach { line ->
                    entries += TariffScopedCalculationLine(
                        scope = TariffCalculationLineScope.SEGMENT_LOCAL,
                        line = line,
                        sliceIndex = sliceIndex,
                    )
                }
        }

        entries += wholeTripStayLines(plan)

        val sourceByIndex = reconstructed.associateBy { it.sourceIndex }
        scopePlan.restingWatchScopes.forEach { watchScope ->
            val watch = sourceByIndex[watchScope.watchSourceIndex]
                ?: return TariffSegmentedMonetaryResult.Failure(
                    reason = TariffSegmentedMonetaryFailureReason.RESTING_WATCH_SOURCE_MISSING,
                    detail = "Kildeintervallet for hvilende nattevakt #${watchScope.watchSourceIndex} kan ikke rekonstrueres.",
                    sourceIndex = watchScope.watchSourceIndex,
                )
            val events = watchScope.activeEventSourceIndices.map { eventSourceIndex ->
                sourceByIndex[eventSourceIndex]
                    ?: return TariffSegmentedMonetaryResult.Failure(
                        reason = TariffSegmentedMonetaryFailureReason.RESTING_EVENT_SOURCE_MISSING,
                        detail = "Kildeintervallet for aktiv hendelse #$eventSourceIndex kan ikke rekonstrueres.",
                        sourceIndex = eventSourceIndex,
                    )
            }
            val pricingSliceIndex = watchScope.activeEventSliceIndexes.minOrNull()
                ?: return TariffSegmentedMonetaryResult.Failure(
                    reason = TariffSegmentedMonetaryFailureReason.RESTING_WATCH_PRICING_CONTEXT_MISSING,
                    detail = "Hvilende nattevakt #${watchScope.watchSourceIndex} mangler entydig priskontekst.",
                    sourceIndex = watchScope.watchSourceIndex,
                )
            val pricingSlice = plan.slices.getOrNull(pricingSliceIndex)
                ?: return TariffSegmentedMonetaryResult.Failure(
                    reason = TariffSegmentedMonetaryFailureReason.RESTING_WATCH_PRICING_CONTEXT_MISSING,
                    detail = "Priskontekst $pricingSliceIndex for hvilende nattevakt finnes ikke i segmentplanen.",
                    sourceIndex = watchScope.watchSourceIndex,
                )

            val occupied = TariffEffectiveDateRange.forTrip(watch.block.start, watch.block.end)
                ?: return TariffSegmentedMonetaryResult.Failure(
                    reason = TariffSegmentedMonetaryFailureReason.RESTING_WATCH_PRICING_CONTEXT_MISSING,
                    detail = "Hvilende nattevakt #${watchScope.watchSourceIndex} har ugyldig tidsintervall.",
                    sourceIndex = watchScope.watchSourceIndex,
                )
            val calculation = TripPlanEngine.calculatePreliminaryFromProjectedBlocks(
                fundingMode = FundingMode.VACATION_SEPARATE,
                dates = TripDateRangePolicy.inclusiveDates(occupied.start, occupied.end),
                roster = emptyMap(),
                blocks = listOf(watch.block) + events.map { it.block },
                annualSalary = pricingSlice.annualSalary,
                weeklyBasis = plan.weeklyBasis,
                weekendProfile = weekendProfile,
                tripStart = watch.block.start,
                tripEnd = watch.block.end,
                rateSet = pricingSlice.segment.rateSet,
            )
            val line = calculation.lines.singleOrNull { it.id == "active-on-resting" }
                ?: return TariffSegmentedMonetaryResult.Failure(
                    reason = TariffSegmentedMonetaryFailureReason.RESTING_WATCH_LINE_MISSING,
                    detail = "Aktivt arbeid for hvilende nattevakt #${watchScope.watchSourceIndex} ga ingen entydig beregningslinje.",
                    sourceIndex = watchScope.watchSourceIndex,
                )
            entries += TariffScopedCalculationLine(
                scope = TariffCalculationLineScope.PER_RESTING_WATCH,
                line = line,
                watchSourceIndex = watchScope.watchSourceIndex,
            )
        }

        return TariffSegmentedMonetaryResult.Success(
            SegmentedTariffMonetaryCalculation(
                plan = plan,
                scopePlan = scopePlan,
                lineEntries = entries,
            ),
        )
    }

    private fun singleSliceCalculation(
        plan: SegmentedTariffCalculationPlan,
        scopePlan: TariffWholeTripScopePlan,
        fundingMode: FundingMode,
        roster: Map<java.time.LocalDate, String>,
        weekendProfile: WeekendProfile,
    ): TariffSegmentedMonetaryResult.Success {
        val slice = plan.slices.single()
        val calculation = TripPlanEngine.calculatePreliminaryFromProjectedBlocks(
            fundingMode = fundingMode,
            dates = slice.dates,
            roster = roster,
            blocks = slice.workBlocks.map { it.block },
            annualSalary = slice.annualSalary,
            weeklyBasis = plan.weeklyBasis,
            weekendProfile = weekendProfile,
            tripStart = plan.tripStart,
            tripEnd = plan.tripEnd,
            rateSet = slice.segment.rateSet,
        )
        return TariffSegmentedMonetaryResult.Success(
            SegmentedTariffMonetaryCalculation(
                plan = plan,
                scopePlan = scopePlan,
                lineEntries = calculation.lines.map { line ->
                    TariffScopedCalculationLine(
                        scope = TariffCalculationLineScopes.requireForLineId(line.id),
                        line = line,
                        sliceIndex = if (
                            TariffCalculationLineScopes.requireForLineId(line.id) == TariffCalculationLineScope.SEGMENT_LOCAL
                        ) 0 else null,
                    )
                },
            ),
        )
    }

    private fun wholeTripStayLines(plan: SegmentedTariffCalculationPlan): List<TariffScopedCalculationLine> {
        val first = plan.slices.first()
        val calculation = TripPlanEngine.calculatePreliminaryFromProjectedBlocks(
            fundingMode = FundingMode.VACATION_SEPARATE,
            dates = plan.slices.flatMap { it.dates }.distinct().sorted(),
            roster = emptyMap(),
            blocks = emptyList(),
            annualSalary = first.annualSalary,
            weeklyBasis = plan.weeklyBasis,
            weekendProfile = WeekendProfile.STANDARD,
            tripStart = plan.tripStart,
            tripEnd = plan.tripEnd,
            rateSet = first.segment.rateSet,
        )
        return calculation.lines
            .filter { line ->
                TariffCalculationLineScopes.requireForLineId(line.id) == TariffCalculationLineScope.WHOLE_TRIP
            }
            .map { line ->
                TariffScopedCalculationLine(
                    scope = TariffCalculationLineScope.WHOLE_TRIP,
                    line = line,
                )
            }
    }
}
