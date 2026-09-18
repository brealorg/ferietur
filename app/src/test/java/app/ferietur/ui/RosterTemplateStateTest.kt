package app.ferietur.ui

import app.ferietur.domain.SolhaugenShiftCatalog
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RosterTemplateStateTest {
    @Test
    fun templateAndSubsequentCodeEmissionPreserveTemplateTimes() {
        val template = requireNotNull(SolhaugenShiftCatalog.byCode("D"))
        val initial = listOf(RosterWorkDraft("", null, null))

        val templated = applyRosterTemplateToPrimaryDraft(initial, template)
        val afterTextFieldEmission = updateRosterWorkDraftAt(templated, 0) {
            it.copy(code = "D")
        }

        assertEquals("D", afterTextFieldEmission.single().code)
        assertEquals(LocalTime.of(7, 0), afterTextFieldEmission.single().start)
        assertEquals(LocalTime.of(15, 0), afterTextFieldEmission.single().end)
    }

    @Test
    fun editingOneFieldTransformsCurrentDraftInsteadOfCapturedStaleDraft() {
        val template = requireNotNull(SolhaugenShiftCatalog.byCode("A"))
        val templated = applyRosterTemplateToPrimaryDraft(emptyList(), template)

        val changedStart = updateRosterWorkDraftAt(templated, 0) {
            it.copy(start = LocalTime.of(14, 0))
        }
        val changedEnd = updateRosterWorkDraftAt(changedStart, 0) {
            it.copy(end = LocalTime.of(22, 30))
        }

        assertEquals("A", changedEnd.single().code)
        assertEquals(LocalTime.of(14, 0), changedEnd.single().start)
        assertEquals(LocalTime.of(22, 30), changedEnd.single().end)
    }
}
