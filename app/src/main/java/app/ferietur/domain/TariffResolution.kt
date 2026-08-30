package app.ferietur.domain

import java.time.LocalDate

data class ResolvedTariffContext(
    val tariffPackage: TariffPackage,
    val rateSet: TariffRateSet,
    val salaryTable: SalaryTableDescriptor,
)

object FerieturTariffResolver {
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

    fun requireSupportedRange(start: LocalDate, end: LocalDate): ResolvedTariffContext =
        requireNotNull(resolveRange(start, end)) {
            "Ingen komplett verifisert kombinasjon av tariffpakke, satssett og lønnstabell " +
                "dekker hele turperioden $start–$end. Ferietur stopper beregningen fremfor " +
                "å blande tariff- eller lønnsgrunnlag over en uimplementert grense."
        }
}
