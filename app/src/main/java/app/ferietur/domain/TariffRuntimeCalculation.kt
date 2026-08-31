package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * One frozen tariff/salary context actually used by a runtime calculation.
 *
 * The runtime result exposes this now so a later snapshot migration can persist
 * every effective-dated context without rediscovering it from a newer app.
 */
data class TariffRuntimeProvenanceSlice(
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
        require(!end.isBefore(start))
        require(tariffPackageId.isNotBlank())
        require(rulesetVersion.isNotBlank())
        require(tariffRateSetId.isNotBlank())
        require(salaryTableId.isNotBlank())
    }
}

enum class TariffRuntimeCalculationMode {
    SINGLE_CONTEXT,
    SEGMENTED_CONTEXTS,
}

sealed interface TariffRuntimeCalculation {
    val mode: TariffRuntimeCalculationMode
    val rulesetVersion: String
    val provenance: List<TariffRuntimeProvenanceSlice>
    val lines: List<CalculationLine>
    val knownAmount: BigDecimal
    val paymentBasisAmount: BigDecimal
    val alreadyCoveredByNormalRosterAmount: BigDecimal
    val excludedKnownRuleAmount: BigDecimal
    val applicableUnresolvedRuleIds: Set<String>

    data class SingleContext(
        val preliminary: PreliminaryCalculation,
        override val rulesetVersion: String,
        override val provenance: List<TariffRuntimeProvenanceSlice>,
    ) : TariffRuntimeCalculation {
        init {
            require(provenance.size == 1)
        }

        override val mode: TariffRuntimeCalculationMode = TariffRuntimeCalculationMode.SINGLE_CONTEXT
        override val lines: List<CalculationLine> get() = preliminary.lines
        override val knownAmount: BigDecimal get() = preliminary.knownAmount
        override val paymentBasisAmount: BigDecimal get() = preliminary.paymentBasisAmount
        override val alreadyCoveredByNormalRosterAmount: BigDecimal
            get() = preliminary.alreadyCoveredByNormalRosterAmount
        override val excludedKnownRuleAmount: BigDecimal get() = preliminary.excludedKnownRuleAmount
        override val applicableUnresolvedRuleIds: Set<String> get() = preliminary.applicableUnresolvedRuleIds
    }

    data class SegmentedContexts(
        val segmented: SegmentedTariffMonetaryCalculation,
        override val rulesetVersion: String,
        override val provenance: List<TariffRuntimeProvenanceSlice>,
    ) : TariffRuntimeCalculation {
        init {
            require(provenance.size > 1)
        }

        override val mode: TariffRuntimeCalculationMode = TariffRuntimeCalculationMode.SEGMENTED_CONTEXTS
        override val lines: List<CalculationLine> get() = segmented.lines
        override val knownAmount: BigDecimal get() = segmented.knownAmount
        override val paymentBasisAmount: BigDecimal get() = segmented.paymentBasisAmount
        override val alreadyCoveredByNormalRosterAmount: BigDecimal
            get() = segmented.alreadyCoveredByNormalRosterAmount
        override val excludedKnownRuleAmount: BigDecimal get() = segmented.excludedKnownRuleAmount
        override val applicableUnresolvedRuleIds: Set<String> get() = segmented.applicableUnresolvedRuleIds
    }
}

enum class TariffRuntimeCalculationFailureReason {
    INVALID_TRIP_RANGE,
    CHAPTER20_NOT_APPLICABLE,
    DATE_COVERAGE_MISMATCH,
    WORK_BLOCK_OUTSIDE_TRIP,
    WORK_BLOCK_OVERLAP,
    SEGMENTATION_FAILED,
    SLICE_PLAN_FAILED,
    MONETARY_COORDINATION_FAILED,
}

sealed interface TariffRuntimeCalculationResult {
    data class Success(
        val calculation: TariffRuntimeCalculation,
    ) : TariffRuntimeCalculationResult

    data class Failure(
        val reason: TariffRuntimeCalculationFailureReason,
        val detail: String,
        val date: LocalDate? = null,
        val segmentationReason: TariffSegmentationFailureReason? = null,
        val slicePlanReason: TariffCalculationPlanFailureReason? = null,
        val monetaryReason: TariffSegmentedMonetaryFailureReason? = null,
        val wholeTripScopeReason: TariffWholeTripScopeFailureReason? = null,
        val sourceIndex: Int? = null,
    ) : TariffRuntimeCalculationResult
}

/**
 * Runtime-ready calculation gateway for effective-dated tariff data.
 *
 * This gateway owns the branch between today's exact single-context calculation
 * and A4A5's segmented monetary coordinator. It deliberately does not flatten a
 * multi-context result into [PreliminaryCalculation], because that type has one
 * hourly rate and one tariff provenance identity. A later snapshot/UI slice can
 * consume the common money/line surface while persisting [provenance] exactly.
 */
object FerieturTariffRuntimeCalculator {
    fun calculate(
        fundingMode: FundingMode,
        dates: List<LocalDate>,
        roster: Map<LocalDate, String>,
        plans: Map<LocalDate, List<PlannedBlock>>,
        salaryStep: Int,
        weeklyBasis: WeeklyBasis,
        weekendProfile: WeekendProfile,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): TariffRuntimeCalculationResult {
        if (!tripEnd.isAfter(tripStart)) {
            return failure(
                TariffRuntimeCalculationFailureReason.INVALID_TRIP_RANGE,
                "Turens sluttid må være etter starttid.",
                tripStart.toLocalDate(),
            )
        }
        if (!TripPlanEngine.chapter20Applies(tripStart, tripEnd)) {
            return failure(
                TariffRuntimeCalculationFailureReason.CHAPTER20_NOT_APPLICABLE,
                "Dok. 25 kapittel 20 gjelder ikke dagsturer i Ferieturs beregningsmodell.",
                tripStart.toLocalDate(),
            )
        }

        try {
            TripDateRangePolicy.requireCompleteCoverage(
                dates = dates,
                start = tripStart.toLocalDate(),
                end = tripEnd.toLocalDate(),
            )
        } catch (error: IllegalArgumentException) {
            return failure(
                TariffRuntimeCalculationFailureReason.DATE_COVERAGE_MISMATCH,
                error.message ?: "Turens datoliste dekker ikke hele registrerte periode.",
                tripStart.toLocalDate(),
            )
        }

        val workBlocks = TripPlanEngine.projectRange(dates, plans)
        val outside = TripPlanEngine.outsideTripRangeBlocks(workBlocks, tripStart, tripEnd)
        if (outside.isNotEmpty()) {
            return failure(
                TariffRuntimeCalculationFailureReason.WORK_BLOCK_OUTSIDE_TRIP,
                "Arbeidsplanen inneholder ${outside.size} intervall utenfor turperioden. Ferietur klipper ikke slike feil bort automatisk.",
                outside.first().start.toLocalDate(),
            )
        }
        if (TripPlanEngine.hasUnintendedOverlap(workBlocks)) {
            return failure(
                TariffRuntimeCalculationFailureReason.WORK_BLOCK_OVERLAP,
                "Arbeidsplanen inneholder overlappende perioder som ikke er en tillatt hvilende-vakt/aktiv-hendelse-kombinasjon.",
            )
        }

        val segmentation = FerieturTariffResolver.planSegments(tripStart, tripEnd)
        if (segmentation is TariffSegmentationResult.Failure) {
            return TariffRuntimeCalculationResult.Failure(
                reason = TariffRuntimeCalculationFailureReason.SEGMENTATION_FAILED,
                detail = segmentation.detail,
                date = segmentation.date,
                segmentationReason = segmentation.reason,
            )
        }

        val planResult = FerieturTariffCalculationSlices.build(
            segmentation = segmentation as TariffSegmentationResult.Success,
            salaryStep = salaryStep,
            weeklyBasis = weeklyBasis,
            tripStart = tripStart,
            tripEnd = tripEnd,
            workBlocks = workBlocks,
        )
        if (planResult is TariffCalculationPlanResult.Failure) {
            return TariffRuntimeCalculationResult.Failure(
                reason = TariffRuntimeCalculationFailureReason.SLICE_PLAN_FAILED,
                detail = planResult.detail,
                date = planResult.date,
                slicePlanReason = planResult.reason,
            )
        }

        return calculatePlan(
            plan = (planResult as TariffCalculationPlanResult.Success).plan,
            fundingMode = fundingMode,
            dates = dates,
            roster = roster,
            weekendProfile = weekendProfile,
        )
    }

    /**
     * Explicit catalog-injected runtime path.
     *
     * Used only for deterministic tariff-update qualification/tooling.
     * The ordinary [calculate] production entrypoint above remains unchanged.
     */
    fun calculateWithCatalog(
        catalog: TariffRuntimeCatalogView,
        fundingMode: FundingMode,
        dates: List<LocalDate>,
        roster: Map<LocalDate, String>,
        plans: Map<LocalDate, List<PlannedBlock>>,
        salaryStep: Int,
        weeklyBasis: WeeklyBasis,
        weekendProfile: WeekendProfile,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): TariffRuntimeCalculationResult {
        if (!tripEnd.isAfter(tripStart)) {
            return failure(
                TariffRuntimeCalculationFailureReason.INVALID_TRIP_RANGE,
                "Turens sluttid må være etter starttid.",
                tripStart.toLocalDate(),
            )
        }
        if (!TripPlanEngine.chapter20Applies(tripStart, tripEnd)) {
            return failure(
                TariffRuntimeCalculationFailureReason.CHAPTER20_NOT_APPLICABLE,
                "Dok. 25 kapittel 20 gjelder ikke dagsturer i Ferieturs beregningsmodell.",
                tripStart.toLocalDate(),
            )
        }

        try {
            TripDateRangePolicy.requireCompleteCoverage(
                dates = dates,
                start = tripStart.toLocalDate(),
                end = tripEnd.toLocalDate(),
            )
        } catch (error: IllegalArgumentException) {
            return failure(
                TariffRuntimeCalculationFailureReason.DATE_COVERAGE_MISMATCH,
                error.message ?: "Turens datoliste dekker ikke hele registrerte periode.",
                tripStart.toLocalDate(),
            )
        }

        val workBlocks = TripPlanEngine.projectRange(dates, plans)
        val outside = TripPlanEngine.outsideTripRangeBlocks(workBlocks, tripStart, tripEnd)
        if (outside.isNotEmpty()) {
            return failure(
                TariffRuntimeCalculationFailureReason.WORK_BLOCK_OUTSIDE_TRIP,
                "Arbeidsplanen inneholder ${outside.size} intervall utenfor turperioden. Ferietur klipper ikke slike feil bort automatisk.",
                outside.first().start.toLocalDate(),
            )
        }
        if (TripPlanEngine.hasUnintendedOverlap(workBlocks)) {
            return failure(
                TariffRuntimeCalculationFailureReason.WORK_BLOCK_OVERLAP,
                "Arbeidsplanen inneholder overlappende perioder som ikke er en tillatt hvilende-vakt/aktiv-hendelse-kombinasjon.",
            )
        }

        val segmentation =
            TariffCatalogResolverAdapter(catalog)
                .planSegments(tripStart, tripEnd)
        if (segmentation is TariffSegmentationResult.Failure) {
            return TariffRuntimeCalculationResult.Failure(
                reason = TariffRuntimeCalculationFailureReason.SEGMENTATION_FAILED,
                detail = segmentation.detail,
                date = segmentation.date,
                segmentationReason = segmentation.reason,
            )
        }

        val planResult =
            TariffCalculationSliceBuilder(
                annualSalaryForTable =
                    catalog::annualSalaryForTable,
            ).build(
            segmentation = segmentation as TariffSegmentationResult.Success,
            salaryStep = salaryStep,
            weeklyBasis = weeklyBasis,
            tripStart = tripStart,
            tripEnd = tripEnd,
            workBlocks = workBlocks,
        )
        if (planResult is TariffCalculationPlanResult.Failure) {
            return TariffRuntimeCalculationResult.Failure(
                reason = TariffRuntimeCalculationFailureReason.SLICE_PLAN_FAILED,
                detail = planResult.detail,
                date = planResult.date,
                slicePlanReason = planResult.reason,
            )
        }

        return calculatePlan(
            plan = (planResult as TariffCalculationPlanResult.Success).plan,
            fundingMode = fundingMode,
            dates = dates,
            roster = roster,
            weekendProfile = weekendProfile,
        )
    }

    /**
     * Executes an already verified slice plan. Public for deterministic domain
     * qualification and for future import/migration tooling; app runtime should
     * normally enter through [calculate].
     */
    fun calculatePlan(
        plan: SegmentedTariffCalculationPlan,
        fundingMode: FundingMode,
        dates: List<LocalDate>,
        roster: Map<LocalDate, String>,
        weekendProfile: WeekendProfile,
    ): TariffRuntimeCalculationResult {
        if (plan.slices.size == 1) {
            val slice = plan.slices.single()
            val preliminary = TripPlanEngine.calculatePreliminaryFromProjectedBlocks(
                fundingMode = fundingMode,
                dates = dates,
                roster = roster,
                blocks = slice.workBlocks.map { it.block },
                annualSalary = slice.annualSalary,
                weeklyBasis = plan.weeklyBasis,
                weekendProfile = weekendProfile,
                tripStart = plan.tripStart,
                tripEnd = plan.tripEnd,
                rateSet = slice.segment.rateSet,
            )
            return TariffRuntimeCalculationResult.Success(
                TariffRuntimeCalculation.SingleContext(
                    preliminary = preliminary,
                    rulesetVersion = plan.rulesetVersion,
                    provenance = listOf(slice.toRuntimeProvenance()),
                ),
            )
        }

        val monetary = TariffSegmentedMonetaryCoordinator.calculate(
            plan = plan,
            fundingMode = fundingMode,
            roster = roster,
            weekendProfile = weekendProfile,
        )
        if (monetary is TariffSegmentedMonetaryResult.Failure) {
            return TariffRuntimeCalculationResult.Failure(
                reason = TariffRuntimeCalculationFailureReason.MONETARY_COORDINATION_FAILED,
                detail = monetary.detail,
                monetaryReason = monetary.reason,
                wholeTripScopeReason = monetary.wholeTripScopeReason,
                sourceIndex = monetary.sourceIndex,
            )
        }

        return TariffRuntimeCalculationResult.Success(
            TariffRuntimeCalculation.SegmentedContexts(
                segmented = (monetary as TariffSegmentedMonetaryResult.Success).calculation,
                rulesetVersion = plan.rulesetVersion,
                provenance = plan.slices.map { it.toRuntimeProvenance() },
            ),
        )
    }

    private fun TariffCalculationSlice.toRuntimeProvenance(): TariffRuntimeProvenanceSlice =
        TariffRuntimeProvenanceSlice(
            start = segment.start,
            end = segment.end,
            tariffPackageId = segment.tariffPackage.id,
            rulesetVersion = segment.tariffPackage.rulesetVersion,
            tariffRateSetId = segment.rateSet.id,
            salaryTableId = segment.salaryTable.id,
            salaryTableEffectiveFrom = segment.salaryTable.effectiveFrom,
            salaryTableSourceLabel = segment.salaryTable.sourceLabel,
            annualSalary = annualSalary,
            hourlyRate = hourlyRate,
        )

    private fun failure(
        reason: TariffRuntimeCalculationFailureReason,
        detail: String,
        date: LocalDate? = null,
    ): TariffRuntimeCalculationResult.Failure = TariffRuntimeCalculationResult.Failure(
        reason = reason,
        detail = detail,
        date = date,
    )
}
