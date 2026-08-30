package app.ferietur.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FinalizedTariffContextSnapshotTest {
    @Test
    fun runtimeProvenanceMapsLosslesslyIntoSnapshotProvenance() {
        val runtime = TariffRuntimeProvenanceSlice(
            start = LocalDate.of(2027, 4, 30),
            end = LocalDate.of(2027, 4, 30),
            tariffPackageId = "tariff-a",
            rulesetVersion = "rules-1",
            tariffRateSetId = "rates-a",
            salaryTableId = "salary-a",
            salaryTableEffectiveFrom = LocalDate.of(2026, 5, 1),
            salaryTableSourceLabel = "Verified table A",
            annualSalary = BigDecimal("600000"),
            hourlyRate = BigDecimal("320.00"),
        )

        val frozen = FinalizedTariffContextSnapshot.fromRuntime(runtime)

        assertEquals(runtime.start, frozen.start)
        assertEquals(runtime.end, frozen.end)
        assertEquals(runtime.tariffPackageId, frozen.tariffPackageId)
        assertEquals(runtime.rulesetVersion, frozen.rulesetVersion)
        assertEquals(runtime.tariffRateSetId, frozen.tariffRateSetId)
        assertEquals(runtime.salaryTableId, frozen.salaryTableId)
        assertEquals(runtime.salaryTableEffectiveFrom, frozen.salaryTableEffectiveFrom)
        assertEquals(runtime.salaryTableSourceLabel, frozen.salaryTableSourceLabel)
        assertEquals(runtime.annualSalary, frozen.annualSalary)
        assertEquals(runtime.hourlyRate, frozen.hourlyRate)
    }
}
