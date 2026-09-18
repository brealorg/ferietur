package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDateTime

enum class TariffCalculationLineScope {
    SEGMENT_LOCAL,
    WHOLE_TRIP,
    PER_RESTING_WATCH,
}

/**
 * Stable ownership map for the current calculation lines.
 *
 * This is intentionally independent from presentation order. A future segmented
 * calculation may calculate [SEGMENT_LOCAL] lines once per effective-date slice,
 * but [WHOLE_TRIP] and [PER_RESTING_WATCH] lines must be coordinated before
 * monetary results can be merged.
 */
object TariffCalculationLineScopes {
    private val scopes = mapOf(
        "active" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "holiday-work-plan-scope-open" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "resting-night" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "resting-evening-night" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "resting-weekend" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "resting-holiday" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "evening-night" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "weekend" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "holiday" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "travel-without-responsibility" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "travel-duty-status-open" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "travel-passive-night" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "travel-passive-evening-night" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "travel-passive-weekend" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "travel-passive-holiday" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "travel-night-sleep-open" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "travel-responsibility-open" to TariffCalculationLineScope.SEGMENT_LOCAL,
        "stay-allowance" to TariffCalculationLineScope.WHOLE_TRIP,
        "stay-allowance-exact-threshold-open" to TariffCalculationLineScope.WHOLE_TRIP,
        "travel-notice-open" to TariffCalculationLineScope.WHOLE_TRIP,
        "travel-short-notice-overtime" to TariffCalculationLineScope.WHOLE_TRIP,
        "travel-short-notice-133-open" to TariffCalculationLineScope.WHOLE_TRIP,
        "active-on-resting" to TariffCalculationLineScope.PER_RESTING_WATCH,
    )

    val knownLineIds: Set<String> get() = scopes.keys

    fun forLineId(lineId: String): TariffCalculationLineScope? = scopes[lineId]

    fun requireForLineId(lineId: String): TariffCalculationLineScope =
        requireNotNull(forLineId(lineId)) { "Ukjent beregningslinje uten tariffscope: $lineId" }
}

data class StayAllowanceScopePolicy(
    val amountPerDay: BigDecimal,
    val remainderThresholdMinutes: Long,
)

data class ShortNoticeScopePolicy(
    val maxMinutes: Long,
    val overtimeRoundingStepMinutes: Long,
)

data class RestingWatchTariffScope(
    val watchSourceIndex: Int,
    val activeEventSourceIndices: List<Int>,
    val activeEventSliceIndexes: Set<Int>,
) {
    init {
        require(watchSourceIndex >= 0)
        require(activeEventSourceIndices.isNotEmpty())
        require(activeEventSliceIndexes.isNotEmpty())
    }

    val crossesEffectiveBoundary: Boolean get() = activeEventSliceIndexes.size > 1
}

data class TariffWholeTripScopePlan(
    val rulesetVersion: String,
    val stayAllowancePolicy: StayAllowanceScopePolicy,
    val shortNoticePolicy: ShortNoticeScopePolicy,
    val restingWatchScopes: List<RestingWatchTariffScope>,
) {
    init {
        require(rulesetVersion.isNotBlank())
    }
}

enum class TariffWholeTripScopeFailureReason {
    INCONSISTENT_STAY_ALLOWANCE_POLICY,
    INCONSISTENT_SHORT_NOTICE_POLICY,
    SOURCE_BLOCK_RECONSTRUCTION_FAILED,
    ACTIVE_EVENT_RATE_ALLOCATION_REQUIRED,
}

sealed interface TariffWholeTripScopeResult {
    data class Success(val scopePlan: TariffWholeTripScopePlan) : TariffWholeTripScopeResult

    data class Failure(
        val reason: TariffWholeTripScopeFailureReason,
        val detail: String,
        val sourceIndex: Int? = null,
    ) : TariffWholeTripScopeResult
}


data class ReconstructedTariffSourceBlock(
    val sourceIndex: Int,
    val block: WorkBlock,
    val sliceIndexes: Set<Int>,
) {
    init {
        require(sourceIndex >= 0)
        require(sliceIndexes.isNotEmpty())
        require(block.end.isAfter(block.start))
    }
}

sealed interface TariffSourceBlockReconstructionResult {
    data class Success(val blocks: List<ReconstructedTariffSourceBlock>) : TariffSourceBlockReconstructionResult

    data class Failure(
        val sourceIndex: Int,
        val detail: String,
    ) : TariffSourceBlockReconstructionResult
}

/**
 * Reassembles the original projected [WorkBlock]s from A4A2 tariff-slice
 * fragments. The reconstruction is provenance-only: it never invents time,
 * kind, travel-notice state, holiday-plan relation, or travel-duty state.
 */
object TariffSourceBlockReconstructor {
    fun reconstruct(plan: SegmentedTariffCalculationPlan): TariffSourceBlockReconstructionResult {
        val fragments = plan.slices.flatMapIndexed { sliceIndex, slice ->
            slice.workBlocks.map { entry -> Fragment(sliceIndex, entry.sourceIndex, entry.block) }
        }.groupBy { it.sourceIndex }

        val reconstructed = mutableListOf<ReconstructedTariffSourceBlock>()
        fragments.toSortedMap().forEach { (sourceIndex, sourceFragments) ->
            val ordered = sourceFragments.sortedBy { it.block.start }
            val first = ordered.first()
            if (
                ordered.any {
                    it.block.kind != first.block.kind ||
                        it.block.travelNoticeStatus != first.block.travelNoticeStatus ||
                        it.block.holidayWorkPlanRelation != first.block.holidayWorkPlanRelation ||
                        it.block.travelDutyStatus != first.block.travelDutyStatus
                }
            ) {
                return TariffSourceBlockReconstructionResult.Failure(
                    sourceIndex,
                    "Kildefragmentene for arbeidsintervall #$sourceIndex har ulik tidsart eller klassifiseringsstatus.",
                )
            }
            ordered.zipWithNext().forEach { (previous, next) ->
                if (next.block.start.isAfter(previous.block.end)) {
                    return TariffSourceBlockReconstructionResult.Failure(
                        sourceIndex,
                        "Kildefragmentene for arbeidsintervall #$sourceIndex har et hull mellom " +
                            "${previous.block.end} og ${next.block.start}.",
                    )
                }
            }
            reconstructed += ReconstructedTariffSourceBlock(
                sourceIndex = sourceIndex,
                block = WorkBlock(
                    start = ordered.minOf { it.block.start },
                    end = ordered.maxOf { it.block.end },
                    kind = first.block.kind,
                    travelNoticeStatus = first.block.travelNoticeStatus,
                    holidayWorkPlanRelation = first.block.holidayWorkPlanRelation,
                    travelDutyStatus = first.block.travelDutyStatus,
                ),
                sliceIndexes = ordered.map { it.sliceIndex }.toSortedSet(),
            )
        }
        return TariffSourceBlockReconstructionResult.Success(reconstructed)
    }

    private data class Fragment(
        val sliceIndex: Int,
        val sourceIndex: Int,
        val block: WorkBlock,
    )
}

/**
 * Coordinates rules whose scope is wider than one effective-date slice.
 *
 * A4A4 does not calculate or merge money. It proves whether the segmented plan
 * has enough unambiguous ownership to proceed to monetary coordination later.
 */
object TariffWholeTripScopeCoordinator {
    fun coordinate(plan: SegmentedTariffCalculationPlan): TariffWholeTripScopeResult {
        val firstRateSet = plan.slices.first().segment.rateSet
        val stayPolicy = StayAllowanceScopePolicy(
            amountPerDay = firstRateSet.stayAllowancePerDay,
            remainderThresholdMinutes = firstRateSet.stayAllowanceRemainderThresholdMinutes,
        )
        val inconsistentStay = plan.slices.firstOrNull { slice ->
            val rateSet = slice.segment.rateSet
            rateSet.stayAllowancePerDay.compareTo(stayPolicy.amountPerDay) != 0 ||
                rateSet.stayAllowanceRemainderThresholdMinutes != stayPolicy.remainderThresholdMinutes
        }
        if (inconsistentStay != null) {
            return TariffWholeTripScopeResult.Failure(
                reason = TariffWholeTripScopeFailureReason.INCONSISTENT_STAY_ALLOWANCE_POLICY,
                detail = "Døgngodtgjøringens sats eller resttidsgrense endres ved ${inconsistentStay.windowStart}. " +
                    "En hel-tur-regel kan ikke splittes automatisk uten en eksplisitt overgangsregel.",
            )
        }

        val shortNoticePolicy = ShortNoticeScopePolicy(
            maxMinutes = firstRateSet.shortNoticeMaxMinutes,
            overtimeRoundingStepMinutes = firstRateSet.overtimeRoundingStepMinutes,
        )
        val inconsistentShortNotice = plan.slices.firstOrNull { slice ->
            val rateSet = slice.segment.rateSet
            rateSet.shortNoticeMaxMinutes != shortNoticePolicy.maxMinutes ||
                rateSet.overtimeRoundingStepMinutes != shortNoticePolicy.overtimeRoundingStepMinutes
        }
        if (inconsistentShortNotice != null) {
            return TariffWholeTripScopeResult.Failure(
                reason = TariffWholeTripScopeFailureReason.INCONSISTENT_SHORT_NOTICE_POLICY,
                detail = "Kortvarselsgrensen eller overtidsavrundingen endres ved ${inconsistentShortNotice.windowStart}. " +
                    "Den felles turgrensen kan ikke koordineres automatisk før overgangsregelen er avklart.",
            )
        }

        val reconstructed = TariffSourceBlockReconstructor.reconstruct(plan)
        if (reconstructed is TariffSourceBlockReconstructionResult.Failure) {
            return TariffWholeTripScopeResult.Failure(
                reason = TariffWholeTripScopeFailureReason.SOURCE_BLOCK_RECONSTRUCTION_FAILED,
                detail = reconstructed.detail,
                sourceIndex = reconstructed.sourceIndex,
            )
        }
        reconstructed as TariffSourceBlockReconstructionResult.Success

        val restingWatches = reconstructed.blocks.filter { it.block.kind == TimeKind.RESTING_NIGHT_WATCH }
        val activeEvents = reconstructed.blocks.filter { it.block.kind == TimeKind.ACTIVE_EVENT_ON_RESTING }
        val watchScopes = mutableListOf<RestingWatchTariffScope>()

        restingWatches.forEach { watch ->
            val events = activeEvents.filter { event -> intersects(watch.block, event.block) }
            if (events.isEmpty()) return@forEach

            val eventSliceIndexes = events.flatMap { it.sliceIndexes }.toSortedSet()
            val pricingKeys = eventSliceIndexes.map { sliceIndex ->
                val slice = plan.slices[sliceIndex]
                ActiveRestingPricingKey(
                    tariffPackageId = slice.segment.tariffPackage.id,
                    hourlyRate = slice.hourlyRate.normalizedMoneyKey(),
                    chapter20ActiveMultiplier = slice.segment.rateSet.chapter20ActiveMultiplier.normalizedMoneyKey(),
                    roundingStepMinutes = slice.segment.rateSet.activeNightRoundingStepMinutes,
                    roundUpRemainderAtMinutes = slice.segment.rateSet.activeNightRoundUpRemainderAtMinutes,
                )
            }.distinct()

            if (pricingKeys.size > 1) {
                return TariffWholeTripScopeResult.Failure(
                    reason = TariffWholeTripScopeFailureReason.ACTIVE_EVENT_RATE_ALLOCATION_REQUIRED,
                    detail = "Aktivt arbeid under én hvilende nattevakt berører flere pris-/avrundingskontekster. " +
                        "Punkt 20.4 krever avrunding per vakt, og Ferietur fordeler ikke avrundet betalt tid mellom " +
                        "to satser uten en eksplisitt fordelingsregel.",
                    sourceIndex = watch.sourceIndex,
                )
            }

            watchScopes += RestingWatchTariffScope(
                watchSourceIndex = watch.sourceIndex,
                activeEventSourceIndices = events.map { it.sourceIndex }.sorted(),
                activeEventSliceIndexes = eventSliceIndexes,
            )
        }

        return TariffWholeTripScopeResult.Success(
            TariffWholeTripScopePlan(
                rulesetVersion = plan.rulesetVersion,
                stayAllowancePolicy = stayPolicy,
                shortNoticePolicy = shortNoticePolicy,
                restingWatchScopes = watchScopes,
            ),
        )
    }

    private data class ActiveRestingPricingKey(
        val tariffPackageId: String,
        val hourlyRate: BigDecimal,
        val chapter20ActiveMultiplier: BigDecimal,
        val roundingStepMinutes: Int,
        val roundUpRemainderAtMinutes: Int,
    )

    private fun intersects(first: WorkBlock, second: WorkBlock): Boolean =
        first.start.isBefore(second.end) && second.start.isBefore(first.end)

    private fun BigDecimal.normalizedMoneyKey(): BigDecimal = stripTrailingZeros()
}
