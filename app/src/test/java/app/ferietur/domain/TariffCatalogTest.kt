package app.ferietur.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffCatalogTest {
    private val start = LocalDate.of(2026, 5, 1)
    private val end = LocalDate.of(2028, 4, 30)

    @Test
    fun currentDok25PackageHasExplicitAgreementPeriodAndRuleset() {
        val tariff = FerieturTariffs.dok25_2026_2028

        assertEquals("oslo-dok25-2026-2028", tariff.id)
        assertEquals(start, tariff.effectiveFrom)
        assertEquals(end, tariff.effectiveTo)
        assertEquals(FerieturTariffs.DOK25_2026_2028_RULESET_VERSION, tariff.rulesetVersion)
        assertTrue(tariff.sourcePageUrl.startsWith("https://www.oslo.kommune.no/"))
    }

    @Test
    fun catalogDoesNotLeakCurrentTariffOutsideItsAgreementPeriod() {
        assertNull(FerieturTariffs.packageForDate(start.minusDays(1)))
        assertEquals(
            FerieturTariffs.DOK25_2026_2028_ID,
            FerieturTariffs.packageForRange(start, end)?.id,
        )
        assertNull(FerieturTariffs.packageForDate(end.plusDays(1)))
        assertThrows(IllegalArgumentException::class.java) {
            FerieturTariffs.requireSupportedRange(end, end.plusDays(1))
        }
    }
}
