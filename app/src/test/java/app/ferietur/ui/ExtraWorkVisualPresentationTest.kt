package app.ferietur.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtraWorkVisualPresentationTest {
    private fun source(): String {
        val candidates = listOf(
            Path.of("src/main/java/app/ferietur/ui/FerieturApp.kt"),
            Path.of("app/src/main/java/app/ferietur/ui/FerieturApp.kt"),
        )
        val path = candidates.firstOrNull { Files.exists(it) }
            ?: error("FerieturApp.kt not found from test working directory")
        return Files.readString(path)
    }

    @Test
    fun workOnTripUsesMaterial3ComparisonLegendAlignedLanesAndTonalExtraChip() {
        val source = source()

        assertTrue(source.contains("private data class WorkComparisonVisual"))
        assertTrue(source.contains("private fun WorkComparisonLegend()"))
        assertTrue(source.contains("private fun WorkComparisonLegendItem("))
        assertTrue(source.contains("private fun ExtraWorkSummaryChip("))
        assertTrue(source.contains("private fun WorkComparisonTimeline("))
        assertTrue(source.contains("WorkComparisonLegend()"))
        assertTrue(source.contains("label = \"Avtalt tid\""))
        assertTrue(source.contains("label = \"Ekstra arbeid\""))
        assertTrue(source.contains("TimelineLane(\"Avtalt tid\", baseline = true)"))
        assertTrue(source.contains("TimelineLane(\"Arbeid på turen\", baseline = false)"))
        assertTrue(source.contains("\"+ \${minutesLabel(minutes)} utenfor avtalt tid\""))
        assertTrue(source.contains("MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.58f)"))
        assertTrue(source.contains("comparisonBasis = if (fundingMode == FundingMode.TURNUS_PLUS_EXTERNAL) workPlanBasis else null"))
        assertTrue(source.contains("baselinePlans = holidayPlans"))

        assertTrue(source.contains("val singleEditableSource = editableSources.singleOrNull()"))
        assertTrue(source.contains("onEditPeriod("))
        assertTrue(source.contains("singleEditableSource.first"))
        assertTrue(source.contains("singleEditableSource.second"))
        assertFalse(source.contains("Rødt = arbeid utover avtalt tid"))
        assertFalse(source.contains("Grunnturnus · \${minutesLabel(inside)} innen · \${minutesLabel(outside)} utenfor · kun sammenligning"))
    }

    @Test
    fun redesignPreservesAutomaticClassificationAndDoesNotAskEmployeeToClassifyOvertime() {
        val source = source()

        assertTrue(source.contains("Vi sammenligner arbeid på turen med grunnturnusen automatisk"))
        assertTrue(source.contains("Vi sammenligner arbeid på turen med arbeidsgivers plan automatisk"))
        assertTrue(source.contains("TripWorkPlanBasis.NORMAL_ROSTER_APPLIES"))
        assertTrue(source.contains("TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN"))
        assertFalse(source.contains("Forhold til feriearbeidsplanen"))
        assertFalse(source.contains("HolidayWorkPlanRelationSelector("))
    }
}
