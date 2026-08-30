package app.ferietur.domain

import java.time.LocalDate

data class TariffPackage(
    val id: String,
    val label: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate,
    val rulesetVersion: String,
    val sourceLabel: String,
    val sourcePageUrl: String,
) {
    init {
        require(!effectiveTo.isBefore(effectiveFrom)) {
            "Tariffperioden $id har sluttdato før startdato."
        }
        require(id.isNotBlank())
        require(rulesetVersion.isNotBlank())
    }

    fun covers(date: LocalDate): Boolean =
        !date.isBefore(effectiveFrom) && !date.isAfter(effectiveTo)

    fun coversRange(start: LocalDate, end: LocalDate): Boolean =
        !end.isBefore(start) && covers(start) && covers(end)
}

object FerieturTariffs {
    const val DOK25_2026_2028_ID = "oslo-dok25-2026-2028"
    const val DOK25_2026_2028_RULESET_VERSION = "2026.3"

    val dok25_2026_2028 = TariffPackage(
        id = DOK25_2026_2028_ID,
        label = "Dok. 25 2026–28",
        effectiveFrom = LocalDate.of(2026, 5, 1),
        effectiveTo = LocalDate.of(2028, 4, 30),
        rulesetVersion = DOK25_2026_2028_RULESET_VERSION,
        sourceLabel = "Oslo kommune, Dok. 25 2026–28",
        sourcePageUrl = "https://www.oslo.kommune.no/jobb-i-oslo-kommune/",
    )

    private val packages = listOf(
        dok25_2026_2028,
    ).sortedBy { it.effectiveFrom }

    init {
        packages.zipWithNext().forEach { (previous, next) ->
            require(next.effectiveFrom.isAfter(previous.effectiveTo)) {
                "Overlappende tariffpakker: ${previous.id} og ${next.id}"
            }
        }
    }

    val earliestSupportedDate: LocalDate =
        requireNotNull(packages.firstOrNull()).effectiveFrom

    val latestSupportedDate: LocalDate =
        requireNotNull(packages.lastOrNull()).effectiveTo

    fun packageForDate(date: LocalDate): TariffPackage? =
        packages.lastOrNull { it.covers(date) }

    fun packageForId(id: String): TariffPackage? =
        packages.singleOrNull { it.id == id }

    fun requireById(id: String): TariffPackage =
        requireNotNull(packageForId(id)) { "Ukjent tariffpakke: $id" }

    fun packageForRange(start: LocalDate, end: LocalDate): TariffPackage? {
        if (end.isBefore(start)) return null
        val first = packageForDate(start) ?: return null
        val last = packageForDate(end) ?: return null
        return first.takeIf { it.id == last.id && it.coversRange(start, end) }
    }

    fun requireSupportedRange(start: LocalDate, end: LocalDate): TariffPackage =
        requireNotNull(packageForRange(start, end)) {
            "Ingen innebygd tariffpakke dekker hele turperioden $start–$end. " +
                "Støttet tariffperiode er $earliestSupportedDate–$latestSupportedDate."
        }
}
