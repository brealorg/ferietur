package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class ResolvedTariffContext(
    val tariffPackage: TariffPackage,
    val rateSet: TariffRateSet,
    val salaryTable: SalaryTableDescriptor,
)

/**
 * Production tariff resolver.
 *
 * A5A7 delegates the existing public resolver API to the qualified read-only
 * adapter backed exclusively by today's global runtime catalogs.
 *
 * This changes resolver structure, not supported tariff/salary data.
 */
object FerieturTariffResolver {

    private val delegate =
        TariffCatalogResolverAdapter(
            FerieturGlobalRuntimeCatalogView,
        )

    fun resolveRange(
        start: LocalDate,
        end: LocalDate,
    ): ResolvedTariffContext? =
        delegate.resolveRange(
            start,
            end,
        )

    fun supportsRange(
        start: LocalDate,
        end: LocalDate,
    ): Boolean =
        delegate.supportsRange(
            start,
            end,
        )

    fun planSegments(
        start: LocalDate,
        end: LocalDate,
    ): TariffSegmentationResult =
        delegate.planSegments(
            start,
            end,
        )

    fun planSegments(
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): TariffSegmentationResult =
        delegate.planSegments(
            tripStart,
            tripEnd,
        )

    fun supportsSegmentedRange(
        start: LocalDate,
        end: LocalDate,
    ): Boolean =
        delegate.supportsSegmentedRange(
            start,
            end,
        )

    fun supportsSegmentedRange(
        tripStart: LocalDateTime,
        tripEnd: LocalDateTime,
    ): Boolean =
        delegate.supportsSegmentedRange(
            tripStart,
            tripEnd,
        )

    fun requireSupportedRange(
        start: LocalDate,
        end: LocalDate,
    ): ResolvedTariffContext =
        requireNotNull(
            resolveRange(
                start,
                end,
            ),
        ) {
            "Ingen komplett verifisert kombinasjon av tariffpakke, satssett og lønnstabell " +
                "dekker hele turperioden $start–$end. Ferietur stopper beregningen fremfor " +
                "å blande tariff- eller lønnsgrunnlag over en uimplementert grense."
        }
}
