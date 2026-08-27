package app.ferietur.export

import app.ferietur.domain.CalculationCertainty
import app.ferietur.domain.CalculationLine
import app.ferietur.domain.PaymentTreatment
import app.ferietur.domain.RosterComparisonMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PdfExporterPolicyTest {

    @Test
    fun weekendFormulaShowsResolvedRateWithoutApplyingPercentageTwice() {
        val line = line(
            id = "weekend",
            title = "Lørdags- og søndagstillegg",
            detail = "29 t × 76,58 kr · 23 % · min. 73 kr/t",
        )

        assertEquals(
            "29 t x 76,58 kr/t (23 % av timelønn, minst 73 kr/t)",
            PdfExporter.plainFormula(line),
        )
    }

    @Test
    fun extendedWeekendFormulaKeepsItsOwnPercentageAndMinimum() {
        val line = line(
            id = "weekend",
            title = "Lørdags- og søndagstillegg",
            detail = "5 t × 110,00 kr · 30 % · min. 110 kr/t",
        )

        assertEquals(
            "5 t x 110,00 kr/t (30 % av timelønn, minst 110 kr/t)",
            PdfExporter.plainFormula(line),
        )
    }

    @Test
    fun activeExplanationSeparatesAppComparisonModelFromTariffWording() {
        val line = line(
            id = "active",
            title = "Arbeid og reise utenfor grunnturnusen",
            detail = "77 t × 332,94 kr × 1,50",
            source = "Dok. 25 2026–28, punkt 20.2 og 20.3",
            explanation = "Arbeid som ligger utenfor grunnturnusen beregnes med timelønn pluss 50 prosent etter punkt 20.2.",
        )

        val text = PdfExporter.plainLineExplanation(line)
        assertTrue(text.contains("I denne beregningsmodellen brukes grunnturnusen som sammenligningsgrunnlag"))
        assertTrue(text.contains("arbeidstid ut over ordinær arbeidstid etter kapittel 8"))
        assertTrue(text.contains("må avklares med arbeidsgiver"))
        assertTrue(text.contains("punkt 20.3"))
        assertTrue(text.contains("legger appen ikke til kveld-/nattillegg eller lørdags-/søndagstillegg"))
        assertTrue(text.contains("punkt 13.1"))
        assertFalse(text.startsWith("Arbeid som ligger utenfor grunnturnusen beregnes"))
    }

    @Test
    fun ordinaryTravelExplanationNoLongerAssumesNoticeWasKnown() {
        val line = line(
            id = "travel-without-responsibility",
            title = "Reisetid uten tilsynsansvar",
            detail = "2 t x 332,94 kr",
        )

        val text = PdfExporter.plainLineExplanation(line)
        assertTrue(text.contains("Varseltidspunktet registreres"))
        assertTrue(text.contains("overtidsdelen for inntil to timer vises separat"))
        assertFalse(text.contains("Beregningen forutsetter at reisen var kjent"))
    }

    @Test
    fun shortNoticeExplanationSeparatesOrdinaryTravelPayFromOvertimeSupplement() {
        val line = line(
            id = "travel-short-notice-overtime",
            title = "Overtidstillegg ved kort varsel om reisen",
            detail = "2 t reisetid · 2 t tilleggsgrunnlag · 50 % overtidstillegg",
        )

        val text = PdfExporter.plainLineExplanation(line)
        assertTrue(text.contains("ordinære timelønnen står på reisetidslinjen"))
        assertTrue(text.contains("bare overtidsdelen i tillegg"))
        assertTrue(text.contains("påbegynt halvtime"))
    }

    @Test
    fun unresolvedTravelNoticePossibleAmountIsPresentedAsOpenNotKnownPay() {
        val line = line(
            id = "travel-notice-open",
            title = "Når du fikk vite om reisen må avklares",
            detail = "2 t reisetid · mulig tillegg 332,94 kr",
            amount = BigDecimal("332.94"),
            certainty = CalculationCertainty.OPEN,
            includedInKnownTotal = false,
            paymentTreatment = PaymentTreatment.OPEN,
        )

        val text = PdfExporter.plainLineExplanation(line)
        assertTrue(text.contains("mulig tillegg"))
        assertTrue(text.contains("ikke inkludert i betalingsgrunnlaget"))
    }

    @Test
    fun unresolvedTravelNoticeExplanationKeepsKnownBasePaySeparate() {
        val line = line(
            id = "travel-notice-open",
            title = "Når du fikk vite om reisen må avklares",
            detail = "2 t reisetid utenfor ordinær arbeidstid",
        )

        val text = PdfExporter.plainLineExplanation(line)
        assertTrue(text.contains("Ordinær reisetidsbetaling er allerede med"))
        assertTrue(text.contains("må avklares om reisen var kjent"))
    }

    @Test
    fun warningFollowUpDoesNotAddPeriodAfterQuestionMark() {
        assertEquals(
            "Betalingsforslaget er ikke endelig: Hadde arbeidstakeren tillatelse til å sove under nattreisen? Nattreisen er ikke beregnet.",
            PdfExporter.sentenceWithFollowUp(
                "Betalingsforslaget er ikke endelig: Hadde arbeidstakeren tillatelse til å sove under nattreisen?",
                " Nattreisen er ikke beregnet.",
            ),
        )
    }

    @Test
    fun warningFollowUpAddsPeriodWhenRuleTitleHasNoTerminalPunctuation() {
        assertEquals(
            "Betalingsforslaget er ikke endelig: En regel må avklares. Beløpet kan bli høyere.",
            PdfExporter.sentenceWithFollowUp(
                "Betalingsforslaget er ikke endelig: En regel må avklares",
                " Beløpet kan bli høyere.",
            ),
        )
    }

    @Test
    fun separateTripPdfCopyDoesNotClaimGroundRosterIsPartOfTheBasis() {
        val note = PdfExporter.separateTripCalculationNote()
        val footer = PdfExporter.shortFooterDescription(RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER)

        assertTrue(note.contains("Hele den registrerte arbeidsplanen"))
        assertTrue(note.contains("eget beregningsgrunnlag"))
        assertFalse(footer.contains("grunnturnus"))
        assertTrue(footer.contains("hele arbeidsplanen"))
    }

    @Test
    fun normalRosterPdfFooterStillDocumentsGroundRoster() {
        val footer = PdfExporter.shortFooterDescription(RosterComparisonMode.USE_NORMAL_ROSTER)
        assertTrue(footer.contains("grunnturnus"))
        assertTrue(footer.contains("hele arbeidsplanen"))
    }

    @Test
    fun paymentScenarioDisclaimerDoesNotAssignLegalLiability() {
        assertEquals(
            "Betalingsscenarioet brukes i betalingsforslaget og dokumentasjonen. Det endrer ikke selve beregningen og fastsetter ikke hvem som rettslig skal bære kostnaden.",
            PAYMENT_SCENARIO_DISCLAIMER,
        )
    }

    private fun line(
        id: String,
        title: String,
        detail: String,
        source: String = "Dok. 25 2026–28",
        explanation: String = "",
        amount: BigDecimal = BigDecimal.ZERO,
        certainty: CalculationCertainty = CalculationCertainty.CONFIRMED,
        includedInKnownTotal: Boolean = true,
        paymentTreatment: PaymentTreatment = PaymentTreatment.INCLUDED_IN_PAYMENT_BASIS,
    ) = CalculationLine(
        id = id,
        title = title,
        detail = detail,
        amount = amount,
        source = source,
        explanation = explanation,
        certainty = certainty,
        includedInKnownTotal = includedInKnownTotal,
        paymentTreatment = paymentTreatment,
    )
}
