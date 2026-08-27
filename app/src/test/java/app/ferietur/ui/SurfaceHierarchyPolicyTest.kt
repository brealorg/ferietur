package app.ferietur.ui

import app.ferietur.domain.ControlFinding
import app.ferietur.domain.FindingSeverity
import org.junit.Assert.assertEquals
import org.junit.Test

class SurfaceHierarchyPolicyTest {
    @Test
    fun repeatedControlFindingsCollapseIntoOneCategory() {
        val groups = groupControlFindings(
            listOf(
                ControlFinding(FindingSeverity.REVIEW, "Kort hvile mellom arbeidsperioder", "første"),
                ControlFinding(FindingSeverity.CRITICAL, "Kort hvile mellom arbeidsperioder", "andre"),
                ControlFinding(FindingSeverity.REVIEW, "Lang sammenhengende arbeidsperiode", "tredje"),
            ),
        )

        assertEquals(2, groups.size)
        assertEquals("Kort hvile mellom arbeidsperioder", groups[0].title)
        assertEquals(2, groups[0].findings.size)
        assertEquals(FindingSeverity.CRITICAL, groups[0].severity)
    }

    @Test
    fun severityPriorityKeepsOpenAboveReviewButBelowCritical() {
        assertEquals(
            FindingSeverity.OPEN,
            highestFindingSeverity(
                listOf(
                    ControlFinding(FindingSeverity.OK, "x", "x"),
                    ControlFinding(FindingSeverity.REVIEW, "x", "x"),
                    ControlFinding(FindingSeverity.OPEN, "x", "x"),
                ),
            ),
        )
    }
}
