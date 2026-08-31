package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Read-only tariff data surface required by resolution.
 *
 * Implementations may represent today's global runtime catalogs or an isolated
 * A5A5 catalog snapshot. The resolver adapter does not mutate either.
 */
interface TariffRuntimeCatalogView {
    fun packageForDate(
        date: LocalDate,
    ): TariffPackage?

    fun rateSetForDate(
        tariffPackageId: String,
        date: LocalDate,
    ): TariffRateSet?

    fun salaryTableForDate(
        date: LocalDate,
    ): SalaryTableDescriptor?

    fun annualSalaryForDate(
        salaryStep: Int,
        date: LocalDate,
    ): BigDecimal?

    fun annualSalaryForTable(
        salaryStep: Int,
        tableId: String,
    ): BigDecimal?
}

/**
 * Read-only adapter over the existing production singletons.
 *
 * A5A6 does not replace FerieturTariffResolver with this implementation.
 */
object FerieturGlobalRuntimeCatalogView :
    TariffRuntimeCatalogView {

    override fun packageForDate(
        date: LocalDate,
    ): TariffPackage? =
        FerieturTariffs.packageForDate(date)

    override fun rateSetForDate(
        tariffPackageId: String,
        date: LocalDate,
    ): TariffRateSet? =
        FerieturTariffRates.forDate(
            tariffPackageId,
            date,
        )

    override fun salaryTableForDate(
        date: LocalDate,
    ): SalaryTableDescriptor? =
        OsloSalaryTables.descriptorForDate(date)

    override fun annualSalaryForDate(
        salaryStep: Int,
        date: LocalDate,
    ): BigDecimal? =
        OsloSalaryTables.annualSalaryForDate(
            salaryStep,
            date,
        )

    override fun annualSalaryForTable(
        salaryStep: Int,
        tableId: String,
    ): BigDecimal? =
        OsloSalaryTables.annualSalaryForTable(
            salaryStep,
            tableId,
        )
}

/**
 * Read-only adapter over an isolated A5A5 runtime catalog snapshot.
 */
class TariffRuntimeCatalogSnapshotView(
    private val snapshot: TariffRuntimeCatalogSnapshot,
) : TariffRuntimeCatalogView {

    override fun packageForDate(
        date: LocalDate,
    ): TariffPackage? =
        snapshot.packageForDate(date)

    override fun rateSetForDate(
        tariffPackageId: String,
        date: LocalDate,
    ): TariffRateSet? =
        snapshot.rateSetForDate(
            tariffPackageId,
            date,
        )

    override fun salaryTableForDate(
        date: LocalDate,
    ): SalaryTableDescriptor? =
        snapshot.salaryTableForDate(date)

    override fun annualSalaryForDate(
        salaryStep: Int,
        date: LocalDate,
    ): BigDecimal? =
        snapshot.annualSalaryForDate(
            salaryStep,
            date,
        )

    override fun annualSalaryForTable(
        salaryStep: Int,
        tableId: String,
    ): BigDecimal? =
        snapshot.annualSalaryForTable(
            salaryStep,
            tableId,
        )
}

/**
 * Resolver logic parameterized by a read-only catalog view.
 *
 * This mirrors the current strict single-context resolution and segmented
 * planning semantics without changing FerieturTariffResolver itself.
 */
class TariffCatalogResolverAdapter(
    private val catalog: TariffRuntimeCatalogView,
) {
    private val segmentPlanner =
        TariffSegmentPlanner(
            packageForDate =
                catalog::packageForDate,
            rateSetForDate =
                catalog::rateSetForDate,
            salaryTableForDate =
                catalog::salaryTableForDate,
        )

    fun resolveDate(
        date: LocalDate,
    ): ResolvedTariffContext? {
        val tariffPackage =
            catalog.packageForDate(date)
                ?: return null

        val rateSet =
            catalog.rateSetForDate(
                tariffPackage.id,
                date,
            )
                ?: return null

        val salaryTable =
            catalog.salaryTableForDate(date)
                ?: return null

        if (
            salaryTable.tariffPackageId !=
            tariffPackage.id
        ) {
            return null
        }

        return ResolvedTariffContext(
            tariffPackage = tariffPackage,
            rateSet = rateSet,
            salaryTable = salaryTable,
        )
    }

    fun annualSalaryForDate(
        salaryStep: Int,
        date: LocalDate,
    ): BigDecimal? {
        val resolved =
            resolveDate(date)
                ?: return null

        val salary =
            catalog.annualSalaryForDate(
                salaryStep,
                date,
            )
                ?: return null

        if (
            catalog.salaryTableForDate(date)?.id !=
            resolved.salaryTable.id
        ) {
            return null
        }

        return salary
    }

    fun resolveRange(
        start: LocalDate,
        end: LocalDate,
    ): ResolvedTariffContext? {
        if (end.isBefore(start)) return null

        val first =
            resolveDate(start)
                ?: return null

        val last =
            resolveDate(end)
                ?: return null

        if (
            first.tariffPackage.id !=
            last.tariffPackage.id
        ) {
            return null
        }

        if (
            first.rateSet.id !=
            last.rateSet.id
        ) {
            return null
        }

        if (
            first.salaryTable.id !=
            last.salaryTable.id
        ) {
            return null
        }

        if (
            !first.tariffPackage.coversRange(
                start,
                end,
            )
        ) {
            return null
        }

        if (
            !first.rateSet.coversRange(
                start,
                end,
            )
        ) {
            return null
        }

        if (
            !first.salaryTable.coversRange(
                start,
                end,
            )
        ) {
            return null
        }

        return first
    }

    fun supportsRange(
        start: LocalDate,
        end: LocalDate,
    ): Boolean =
        resolveRange(start, end) != null

    fun planSegments(
        start: LocalDate,
        end: LocalDate,
    ): TariffSegmentationResult =
        segmentPlanner.planRange(
            start,
            end,
        )

    fun planSegments(
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): TariffSegmentationResult {
        val occupied =
            TariffEffectiveDateRange
                .forTrip(
                    tripStart,
                    tripEnd,
                )
                ?: return TariffSegmentationResult.Failure(
                        reason =
                            TariffSegmentationFailureReason
                                .INVALID_RANGE,
                        date =
                            tripStart.toLocalDate(),
                        detail =
                            "Turens sluttid må være etter starttid " +
                                "og dekke minst ett faktisk tidsintervall.",
                    )

        return segmentPlanner.planRange(
            occupied.start,
            occupied.end,
        )
    }

    fun supportsSegmentedRange(
        start: LocalDate,
        end: LocalDate,
    ): Boolean =
        planSegments(start, end) is
            TariffSegmentationResult.Success

    fun supportsSegmentedRange(
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): Boolean =
        planSegments(
            tripStart,
            tripEnd,
        ) is TariffSegmentationResult.Success
}
