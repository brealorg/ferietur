package app.ferietur.ui

import app.ferietur.domain.CalculationEvidence
import java.time.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceFormattingPolicyTest {
    @Test
    fun crossDateEvidenceShowsBothDatesInsteadOfAmbiguousClockRange() {
        val evidence = CalculationEvidence(
            start = LocalDateTime.of(2026, 8, 14, 8, 0),
            end = LocalDateTime.of(2026, 8, 15, 10, 0),
            minutes = 26 * 60L,
            note = "Reisens samlede varighet",
        )

        val text = evidenceRowText(evidence)

        assertTrue(text.contains("fre. 14. aug"))
        assertTrue(text.contains("lør. 15. aug"))
        assertTrue(text.contains("→"))
        assertTrue(text.contains("26 t"))
        assertFalse(text.contains("08:00–10:00 · 26 t"))
    }
}
