package app.ferietur.ui

import app.ferietur.domain.ShiftCategory
import app.ferietur.domain.ShiftDefinition
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RosterTemplatePolicyTest {
    @Test
    fun previouslyUsedTemplateReplacesPrimaryDraftWithCodeAndTimes() {
        val existing = listOf(RosterWorkDraft("", null, null))
        val template = ShiftDefinition(
            code = "D1",
            start = LocalTime.of(7, 0),
            end = LocalTime.of(15, 0),
            category = ShiftCategory.DAY,
            label = "Dagvakt",
        )

        val result = applyRosterTemplateToPrimaryDraft(existing, template)

        assertEquals(1, result.size)
        assertEquals("D1", result.single().code)
        assertEquals(LocalTime.of(7, 0), result.single().start)
        assertEquals(LocalTime.of(15, 0), result.single().end)
    }

    @Test
    fun templateSelectionNeverAppendsAnotherShift() {
        val existing = listOf(
            RosterWorkDraft("OLD", LocalTime.of(8, 0), LocalTime.of(16, 0)),
            RosterWorkDraft("LEG", LocalTime.of(16, 0), LocalTime.of(20, 0)),
        )
        val template = ShiftDefinition(
            code = "A",
            start = LocalTime.of(14, 30),
            end = LocalTime.of(22, 0),
            category = ShiftCategory.EVENING,
            label = "Aftenvakt",
        )

        val result = applyRosterTemplateToPrimaryDraft(existing, template)

        assertEquals(2, result.size)
        assertEquals(RosterWorkDraft("A", LocalTime.of(14, 30), LocalTime.of(22, 0)), result[0])
        assertEquals(existing[1], result[1])
    }
}
