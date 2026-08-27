package app.ferietur.ui

import app.ferietur.domain.CalculationCertainty
import app.ferietur.domain.DayCalculationAudit
import app.ferietur.domain.DayCalculationContribution
import app.ferietur.domain.PaymentTreatment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class CalculationResultHierarchyPolicyTest {

    @Test
    fun coveredRosterSummaryExplicitlyKeepsAmountOutsidePaymentBasis() {
        val text = normalized(coveredRosterControlSummary(BigDecimal("532.72")))
        assertTrue(text.contains("532,72"))
        assertTrue(text.contains("i turnustillegg ligger i grunnturnusen og påvirker ikke beløpet over."))
        assertFalse(text.contains("allerede dekket"))
    }

    @Test
    fun collapsedDaySummaryDoesNotPromoteCoveredRosterAmount() {
        val audit = audit(
            payment = "0.00",
            covered = "532.72",
            open = "0.00",
        )

        val text = dayAuditCollapsedAmountSummary(audit)
        assertEquals("0,00 kr i tillegg", text)
        assertFalse(text.contains("532,72"))
        assertFalse(text.contains("allerede dekket"))
    }

    @Test
    fun collapsedDaySummaryStillPrioritizesAdditionalAndOpenAmounts() {
        val audit = audit(
            payment = "332.94",
            covered = "532.72",
            open = "100.00",
        )

        val text = normalized(dayAuditCollapsedAmountSummary(audit))
        assertTrue(text.startsWith("kommer i tillegg:"))
        assertTrue(text.contains("332,94"))
        assertTrue(text.contains("· må avklares:"))
        assertTrue(text.contains("100,00"))
        assertFalse(text.contains("532,72"))
    }

    @Test
    fun expandedDayDetailsExcludeGroundRosterControlRows() {
        val audit = audit(
            payment = "332.94",
            covered = "532.72",
            open = "0.00",
        )

        val visible = dayAuditPaymentContributions(audit)
        assertTrue(visible.isEmpty())
        assertFalse(visible.any { it.paymentTreatment == PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER })
    }

    private fun normalized(value: String): String = value.replace('\u00A0', ' ').replace('\u202F', ' ')

    private fun audit(payment: String, covered: String, open: String): DayCalculationAudit =
        DayCalculationAudit(
            date = LocalDate.of(2026, 8, 14),
            holidayLabels = emptyList(),
            contributions = listOf(
                DayCalculationContribution(
                    lineId = "covered-evening-night",
                    title = "Kveld- og nattillegg",
                    minutes = 240,
                    amount = BigDecimal(covered),
                    source = "Dok. 25 2026–28, punkt 12.1.1",
                    certainty = CalculationCertainty.CONFIRMED,
                    includedInKnownTotal = true,
                    paymentTreatment = PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER,
                    evidence = emptyList(),
                ),
            ),
            knownSubtotal = BigDecimal(payment).add(BigDecimal(covered)),
            paymentSubtotal = BigDecimal(payment),
            alreadyCoveredSubtotal = BigDecimal(covered),
            openSubtotal = BigDecimal(open),
        )
}
