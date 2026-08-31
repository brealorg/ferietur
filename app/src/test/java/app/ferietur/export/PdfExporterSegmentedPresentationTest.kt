package app.ferietur.export

import app.ferietur.domain.CalculationEvidence
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfExporterSegmentedPresentationTest {

    @Test
    fun evidenceDateLabelUsesActualWorkDateInsteadOfTariffSliceStart() {
        val evidence = listOf(
            CalculationEvidence(
                start = LocalDateTime.of(2027, 5, 2, 7, 0),
                end = LocalDateTime.of(2027, 5, 2, 10, 0),
                minutes = 180,
                note = "Aktivt arbeid",
            ),
        )

        assertEquals(
            "2. mai 2027",
            PdfExporter.evidenceDateLabelForPdf(evidence),
        )
    }

    @Test
    fun evidenceDateLabelTreatsMidnightEndAsHalfOpen() {
        val evidence = listOf(
            CalculationEvidence(
                start = LocalDateTime.of(2027, 4, 30, 22, 0),
                end = LocalDateTime.of(2027, 5, 1, 0, 0),
                minutes = 120,
                note = "Aktivt arbeid",
            ),
        )

        assertEquals(
            "30. april 2027",
            PdfExporter.evidenceDateLabelForPdf(evidence),
        )
    }

    @Test
    fun evidenceDateLabelShowsActualCrossMidnightRange() {
        val evidence = listOf(
            CalculationEvidence(
                start = LocalDateTime.of(2027, 4, 30, 23, 0),
                end = LocalDateTime.of(2027, 5, 1, 1, 0),
                minutes = 120,
                note = "Arbeid over midnatt",
            ),
        )

        assertEquals(
            "30. april 2027–1. mai 2027",
            PdfExporter.evidenceDateLabelForPdf(evidence),
        )
    }
}
