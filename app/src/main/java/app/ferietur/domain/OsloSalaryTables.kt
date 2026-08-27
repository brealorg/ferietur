package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate

data class SalaryTableDescriptor(
    val id: String,
    val effectiveFrom: LocalDate,
    val sourceLabel: String,
)

object OsloSalaryTables {
    private data class Period(
        val descriptor: SalaryTableDescriptor,
        val annualSalary: (Int) -> BigDecimal,
    )

    private val periods = listOf(
        Period(
            descriptor = SalaryTableDescriptor(
                id = OsloSalaryTable2026.tableId,
                effectiveFrom = OsloSalaryTable2026.effectiveFromDate,
                sourceLabel = OsloSalaryTable2026.sourceLabel,
            ),
            annualSalary = OsloSalaryTable2026::annualSalary,
        ),
    ).sortedBy { it.descriptor.effectiveFrom }

    val earliestSupportedDate: LocalDate =
        requireNotNull(periods.firstOrNull()).descriptor.effectiveFrom

    fun descriptorForDate(date: LocalDate): SalaryTableDescriptor? =
        periodForDate(date)?.descriptor

    /**
     * Returns one salary table only when the same known table covers the full
     * trip range. This is deliberately conservative when a future table is
     * added: a trip crossing a table boundary becomes unsupported until the
     * calculation engine explicitly supports split-rate periods.
     */
    fun descriptorForRange(start: LocalDate, end: LocalDate): SalaryTableDescriptor? {
        if (end.isBefore(start)) return null
        val first = periodForDate(start) ?: return null
        val last = periodForDate(end) ?: return null
        return first.descriptor.takeIf { it.id == last.descriptor.id }
    }

    fun supportsRange(start: LocalDate, end: LocalDate): Boolean =
        descriptorForRange(start, end) != null

    fun annualSalaryForRange(
        step: Int,
        start: LocalDate,
        end: LocalDate,
    ): BigDecimal? {
        val descriptor = descriptorForRange(start, end) ?: return null
        val period = periods.single { it.descriptor.id == descriptor.id }
        return period.annualSalary(step)
    }

    fun requireSupportedRange(start: LocalDate, end: LocalDate): SalaryTableDescriptor =
        requireNotNull(descriptorForRange(start, end)) {
            "Ingen innebygd lønnstabell dekker hele turperioden $start–$end. " +
                "Tidligste støttede dato er $earliestSupportedDate."
        }

    private fun periodForDate(date: LocalDate): Period? =
        periods.lastOrNull { !date.isBefore(it.descriptor.effectiveFrom) }
}
