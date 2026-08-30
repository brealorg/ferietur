package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class ResolvedTariffContext(
    val tariffPackage: TariffPackage,
    val rateSet: TariffRateSet,
    val salaryTable: SalaryTableDescriptor,
)

object FerieturTariffResolver {

    private val segmentPlanner = TariffSegmentPlanner(
        packageForDate = FerieturTariffs::packageForDate,
        rateSetForDate = FerieturTariffRates::forDate,
        salaryTableForDate = OsloSalaryTables::descriptorForDate,
    )
    fun resolveRange(start: LocalDate, end: LocalDate): ResolvedTariffContext? {
        if (end.isBefore(start)) return null
        val tariffPackage = FerieturTariffs.packageForRange(start, end) ?: return null
        val rateSet = FerieturTariffRates.forRange(tariffPackage.id, start, end) ?: return null
        val salaryTable = OsloSalaryTables.descriptorForRange(start, end) ?: return null
        if (salaryTable.tariffPackageId != tariffPackage.id) return null
        return ResolvedTariffContext(tariffPackage, rateSet, salaryTable)
    }

    fun supportsRange(start: LocalDate, end: LocalDate): Boolean =
        resolveRange(start, end) != null

    /**
     * Plans a future split-rate calculation without changing the current strict
     * single-context calculation path. Numeric rate/salary boundaries are
     * segmentable only while the Ferietur ruleset version remains unchanged.
     */
    fun planSegments(start: LocalDate, end: LocalDate): TariffSegmentationResult =
        segmentPlanner.planRange(start, end)

    fun planSegments(tripStart: LocalDateTime, tripEnd: LocalDateTime): TariffSegmentationResult {
        val occupied = TariffEffectiveDateRange.forTrip(tripStart, tripEnd)
            ?: return TariffSegmentationResult.Failure(
                reason = TariffSegmentationFailureReason.INVALID_RANGE,
                date = tripStart.toLocalDate(),
                detail = "Turens sluttid må være etter starttid og dekke minst ett faktisk tidsintervall.",
            )
        return segmentPlanner.planRange(occupied.start, occupied.end)
    }

    fun supportsSegmentedRange(start: LocalDate, end: LocalDate): Boolean =
        planSegments(start, end) is TariffSegmentationResult.Success

    fun supportsSegmentedRange(tripStart: LocalDateTime, tripEnd: LocalDateTime): Boolean =
        planSegments(tripStart, tripEnd) is TariffSegmentationResult.Success

    fun requireSupportedRange(start: LocalDate, end: LocalDate): ResolvedTariffContext =
        requireNotNull(resolveRange(start, end)) {
            "Ingen komplett verifisert kombinasjon av tariffpakke, satssett og lønnstabell " +
                "dekker hele turperioden $start–$end. Ferietur stopper beregningen fremfor " +
                "å blande tariff- eller lønnsgrunnlag over en uimplementert grense."
        }
}
