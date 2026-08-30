package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Exact, provenance-preserving input slice for a future segmented calculation.
 *
 * The slice is deliberately expressed in projected [WorkBlock]s rather than
 * converting back to [PlannedBlock]. This preserves cross-midnight ownership,
 * travel notice state, and intentional resting-night/active-event overlap when
 * an effective-date boundary cuts through an existing block.
 */
data class TariffCalculationSlice(
    val segment: TariffCalculationSegment,
    val windowStart: LocalDateTime,
    val windowEnd: LocalDateTime,
    val dates: List<LocalDate>,
    val annualSalary: BigDecimal,
    val hourlyRate: BigDecimal,
    val workBlocks: List<SegmentedWorkBlock>,
) {
    init {
        require(windowEnd.isAfter(windowStart)) { "Tariffslicen må ha positiv varighet." }
        require(dates.isNotEmpty()) { "Tariffslicen må inneholde minst én dato." }
        require(dates.first() == segment.start && dates.last() == segment.end) {
            "Tariffslicens datoer må samsvare med tariffsegmentet."
        }
        workBlocks.forEach { entry ->
            require(!entry.block.start.isBefore(windowStart) && !entry.block.end.isAfter(windowEnd)) {
                "Et klippet arbeidsintervall ligger utenfor tariffslicens tidsvindu."
            }
            require(entry.block.end.isAfter(entry.block.start)) {
                "Et klippet arbeidsintervall må ha positiv varighet."
            }
        }
    }
}

data class SegmentedWorkBlock(
    val sourceIndex: Int,
    val block: WorkBlock,
) {
    init {
        require(sourceIndex >= 0)
        require(block.end.isAfter(block.start))
    }
}

data class SegmentedTariffCalculationPlan(
    val rulesetVersion: String,
    val tripStart: LocalDateTime,
    val tripEnd: LocalDateTime,
    val salaryStep: Int,
    val weeklyBasis: WeeklyBasis,
    val slices: List<TariffCalculationSlice>,
) {
    init {
        require(tripEnd.isAfter(tripStart))
        require(rulesetVersion.isNotBlank())
        require(slices.isNotEmpty())
        require(slices.all { it.segment.tariffPackage.rulesetVersion == rulesetVersion })
        require(slices.first().windowStart == tripStart)
        require(slices.last().windowEnd == tripEnd)
        slices.zipWithNext().forEach { (previous, next) ->
            require(previous.windowEnd == next.windowStart) {
                "Tariffslicene må dekke turen sammenhengende uten hull eller overlapp."
            }
        }
    }

    val isSplit: Boolean get() = slices.size > 1
    val boundaryInstants: List<LocalDateTime> get() = slices.dropLast(1).map { it.windowEnd }
}

enum class TariffCalculationPlanFailureReason {
    INVALID_TRIP_RANGE,
    SEGMENT_RANGE_MISMATCH,
    INVALID_WORK_BLOCK,
    WORK_BLOCK_OUTSIDE_TRIP,
    SALARY_NOT_AVAILABLE,
}

sealed interface TariffCalculationPlanResult {
    data class Success(
        val plan: SegmentedTariffCalculationPlan,
    ) : TariffCalculationPlanResult

    data class Failure(
        val reason: TariffCalculationPlanFailureReason,
        val detail: String,
        val date: LocalDate? = null,
    ) : TariffCalculationPlanResult
}

class TariffCalculationSliceBuilder(
    private val annualSalaryForTable: (salaryStep: Int, salaryTableId: String) -> BigDecimal?,
) {
    fun build(
        segmentation: TariffSegmentationResult.Success,
        salaryStep: Int,
        weeklyBasis: WeeklyBasis,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
        workBlocks: List<WorkBlock>,
    ): TariffCalculationPlanResult {
        if (!tripEnd.isAfter(tripStart)) {
            return failure(
                TariffCalculationPlanFailureReason.INVALID_TRIP_RANGE,
                "Turens sluttid må være etter starttid.",
                tripStart.toLocalDate(),
            )
        }

        val occupiedRange = TariffEffectiveDateRange.forTrip(tripStart, tripEnd)
            ?: return failure(
                TariffCalculationPlanFailureReason.INVALID_TRIP_RANGE,
                "Turperioden kan ikke omsettes til et gyldig tariffdatointervall.",
                tripStart.toLocalDate(),
            )
        if (
            segmentation.segments.first().start != occupiedRange.start ||
            segmentation.segments.last().end != occupiedRange.end
        ) {
            return failure(
                TariffCalculationPlanFailureReason.SEGMENT_RANGE_MISMATCH,
                "Segmentplanen ${segmentation.segments.first().start}–${segmentation.segments.last().end} " +
                    "samsvarer ikke med turens tariffaktive datoer ${occupiedRange.start}–${occupiedRange.end}.",
                occupiedRange.start,
            )
        }

        workBlocks.forEachIndexed { index, block ->
            if (!block.end.isAfter(block.start)) {
                return failure(
                    TariffCalculationPlanFailureReason.INVALID_WORK_BLOCK,
                    "Arbeidsintervall #$index har ikke positiv varighet.",
                    block.start.toLocalDate(),
                )
            }
            if (block.start.isBefore(tripStart) || block.end.isAfter(tripEnd)) {
                return failure(
                    TariffCalculationPlanFailureReason.WORK_BLOCK_OUTSIDE_TRIP,
                    "Arbeidsintervall #$index ligger utenfor turperioden og klippes ikke stilltiende bort.",
                    block.start.toLocalDate(),
                )
            }
        }

        val slices = mutableListOf<TariffCalculationSlice>()
        segmentation.segments.forEach { segment ->
            val rawStart = segment.start.atStartOfDay()
            val rawEnd = segment.end.plusDays(1).atStartOfDay()
            val windowStart = maxOf(tripStart, rawStart)
            val windowEnd = minOf(tripEnd, rawEnd)
            if (!windowEnd.isAfter(windowStart)) {
                return failure(
                    TariffCalculationPlanFailureReason.SEGMENT_RANGE_MISMATCH,
                    "Tariffsegmentet ${segment.start}–${segment.end} har ingen faktisk tid inne i turen.",
                    segment.start,
                )
            }

            val annualSalary = annualSalaryForTable(salaryStep, segment.salaryTable.id)
                ?: return failure(
                    TariffCalculationPlanFailureReason.SALARY_NOT_AVAILABLE,
                    "Lønnstrinn $salaryStep kan ikke slås opp i lønnstabellen ${segment.salaryTable.id}.",
                    segment.start,
                )
            val hourlyRate = TariffMath.hourlyRate(annualSalary, weeklyBasis, segment.rateSet)
            val clippedBlocks = workBlocks.mapIndexedNotNull { index, source ->
                clip(source, windowStart, windowEnd)?.let { clipped ->
                    SegmentedWorkBlock(sourceIndex = index, block = clipped)
                }
            }

            slices += TariffCalculationSlice(
                segment = segment,
                windowStart = windowStart,
                windowEnd = windowEnd,
                dates = TripDateRangePolicy.inclusiveDates(segment.start, segment.end),
                annualSalary = annualSalary,
                hourlyRate = hourlyRate,
                workBlocks = clippedBlocks,
            )
        }

        return TariffCalculationPlanResult.Success(
            SegmentedTariffCalculationPlan(
                rulesetVersion = segmentation.rulesetVersion,
                tripStart = tripStart,
                tripEnd = tripEnd,
                salaryStep = salaryStep,
                weeklyBasis = weeklyBasis,
                slices = slices,
            ),
        )
    }

    private fun clip(
        block: WorkBlock,
        windowStart: LocalDateTime,
        windowEnd: LocalDateTime,
    ): WorkBlock? {
        val start = maxOf(block.start, windowStart)
        val end = minOf(block.end, windowEnd)
        if (!end.isAfter(start)) return null
        return block.copy(start = start, end = end)
    }

    private fun failure(
        reason: TariffCalculationPlanFailureReason,
        detail: String,
        date: LocalDate?,
    ): TariffCalculationPlanResult.Failure = TariffCalculationPlanResult.Failure(
        reason = reason,
        detail = detail,
        date = date,
    )
}

object FerieturTariffCalculationSlices {
    private val builder = TariffCalculationSliceBuilder(
        annualSalaryForTable = OsloSalaryTables::annualSalaryForTable,
    )

    fun build(
        segmentation: TariffSegmentationResult.Success,
        salaryStep: Int,
        weeklyBasis: WeeklyBasis,
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
        workBlocks: List<WorkBlock>,
    ): TariffCalculationPlanResult = builder.build(
        segmentation = segmentation,
        salaryStep = salaryStep,
        weeklyBasis = weeklyBasis,
        tripStart = tripStart,
        tripEnd = tripEnd,
        workBlocks = workBlocks,
    )
}
