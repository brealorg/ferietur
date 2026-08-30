package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate

data class SalaryTableDescriptor(
    val id: String,
    val effectiveFrom: LocalDate,
    val verifiedThrough: LocalDate,
    val tariffPackageId: String,
    val sourceLabel: String,
    val sourcePageUrl: String,
) {
    init {
        require(!verifiedThrough.isBefore(effectiveFrom)) {
            "Lønnstabellen $id har verifisert sluttdato før startdato."
        }
    }

    fun covers(date: LocalDate): Boolean =
        !date.isBefore(effectiveFrom) && !date.isAfter(verifiedThrough)

    fun coversRange(start: LocalDate, end: LocalDate): Boolean =
        !end.isBefore(start) && covers(start) && covers(end)
}

class SalaryTablePeriod(
    val descriptor: SalaryTableDescriptor,
    val annualSalary: (Int) -> BigDecimal,
)

class SalaryTableCatalog(periods: List<SalaryTablePeriod>) {
    private val ordered = periods.sortedBy { it.descriptor.effectiveFrom }
    private val byId = ordered.associateBy { it.descriptor.id }

    init {
        require(ordered.isNotEmpty()) { "Lønnstabellkatalogen kan ikke være tom." }
        require(byId.size == ordered.size) { "Duplisert lønnstabell-ID." }
        ordered.zipWithNext().forEach { (previous, next) ->
            require(next.descriptor.effectiveFrom.isAfter(previous.descriptor.verifiedThrough)) {
                "Overlappende lønnstabeller: ${previous.descriptor.id} og ${next.descriptor.id}"
            }
        }
    }

    val earliestSupportedDate: LocalDate = ordered.first().descriptor.effectiveFrom
    val latestSupportedDate: LocalDate = ordered.last().descriptor.verifiedThrough

    fun descriptorForDate(date: LocalDate): SalaryTableDescriptor? =
        periodForDate(date)?.descriptor

    /**
     * Returns one salary table only when the same verified table covers the full
     * trip range. A trip crossing a salary boundary remains fail-closed until
     * split-rate calculation is explicitly implemented.
     */
    fun descriptorForRange(start: LocalDate, end: LocalDate): SalaryTableDescriptor? {
        if (end.isBefore(start)) return null
        val first = periodForDate(start) ?: return null
        val last = periodForDate(end) ?: return null
        return first.descriptor.takeIf {
            it.id == last.descriptor.id && it.coversRange(start, end)
        }
    }

    fun supportsRange(start: LocalDate, end: LocalDate): Boolean =
        descriptorForRange(start, end) != null

    fun annualSalaryForRange(
        step: Int,
        start: LocalDate,
        end: LocalDate,
    ): BigDecimal? {
        val descriptor = descriptorForRange(start, end) ?: return null
        return requireNotNull(byId[descriptor.id]).annualSalary(step)
    }

    fun requireSupportedRange(start: LocalDate, end: LocalDate): SalaryTableDescriptor =
        requireNotNull(descriptorForRange(start, end)) {
            "Ingen enkelt verifisert innebygd lønnstabell dekker hele turperioden $start–$end. " +
                "Verifisert katalogperiode er $earliestSupportedDate–$latestSupportedDate. " +
                "En tur som krysser en lønnstabellgrense må behandles med eksplisitt " +
                "split-rate-støtte før den kan beregnes."
        }

    private fun periodForDate(date: LocalDate): SalaryTablePeriod? =
        ordered.lastOrNull { it.descriptor.covers(date) }
}

object OsloSalaryTables {
    private val catalog = SalaryTableCatalog(
        listOf(
            SalaryTablePeriod(
                descriptor = SalaryTableDescriptor(
                    id = OsloSalaryTable2026.tableId,
                    effectiveFrom = OsloSalaryTable2026.effectiveFromDate,
                    verifiedThrough = OsloSalaryTable2026.verifiedThroughDate,
                    tariffPackageId = OsloSalaryTable2026.tariffPackageId,
                    sourceLabel = OsloSalaryTable2026.sourceLabel,
                    sourcePageUrl = OsloSalaryTable2026.sourcePageUrl,
                ),
                annualSalary = OsloSalaryTable2026::annualSalary,
            ),
        ),
    )

    val earliestSupportedDate: LocalDate get() = catalog.earliestSupportedDate
    val latestSupportedDate: LocalDate get() = catalog.latestSupportedDate

    fun descriptorForDate(date: LocalDate): SalaryTableDescriptor? =
        catalog.descriptorForDate(date)

    fun descriptorForRange(start: LocalDate, end: LocalDate): SalaryTableDescriptor? =
        catalog.descriptorForRange(start, end)

    fun supportsRange(start: LocalDate, end: LocalDate): Boolean =
        catalog.supportsRange(start, end)

    fun annualSalaryForRange(
        step: Int,
        start: LocalDate,
        end: LocalDate,
    ): BigDecimal? = catalog.annualSalaryForRange(step, start, end)

    fun requireSupportedRange(start: LocalDate, end: LocalDate): SalaryTableDescriptor =
        catalog.requireSupportedRange(start, end)
}
