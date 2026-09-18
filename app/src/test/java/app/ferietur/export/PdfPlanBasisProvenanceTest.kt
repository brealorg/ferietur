package app.ferietur.export

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPlanBasisProvenanceTest {
    private fun source(path: String): String {
        val candidates = listOf(
            Path.of(path),
            Path.of("app/$path"),
        )
        val resolved = candidates.firstOrNull { Files.exists(it) }
            ?: error("$path not found from test working directory")
        return Files.readString(resolved)
    }

    @Test
    fun finalizedSnapshotFreezesPlanBasisAndEmployerPlanInFormatSeven() {
        val snapshot = source("src/main/java/app/ferietur/domain/FinalizedTripSnapshot.kt")
        val codec = source("src/main/java/app/ferietur/domain/FinalizedTripSnapshotCodec.kt")

        assertTrue(snapshot.contains("val workPlanBasis: TripWorkPlanBasis"))
        assertTrue(snapshot.contains("val employerWorkPlanBlocks: List<WorkBlock>"))
        assertTrue(snapshot.contains("workPlanBasis: TripWorkPlanBasis = TripWorkPlanBasis.NOT_CLARIFIED"))
        assertTrue(snapshot.contains("holidayPlans: Map<LocalDate, List<PlannedBlock>> = emptyMap()"))
        assertTrue(snapshot.contains("employerWorkPlanBlocks = employerWorkPlanBlocks"))

        assertTrue(codec.contains("private const val WORK_PLAN_BASIS_FORMAT_VERSION = 7"))
        assertTrue(codec.contains("private const val FORMAT_VERSION = WORK_PLAN_BASIS_FORMAT_VERSION"))
        assertTrue(codec.contains("writeString(snapshot.workPlanBasis.name)"))
        assertTrue(codec.contains("writeList(snapshot.employerWorkPlanBlocks)"))
        assertTrue(codec.contains("TripWorkPlanBasis.NOT_CLARIFIED"))
    }

    @Test
    fun summaryAndPdfRenderPlanBasisInsteadOfInventingHolidayPlanStatus() {
        val ui = source("src/main/java/app/ferietur/ui/FerieturApp.kt")
        val pdf = source("src/main/java/app/ferietur/export/PdfExporter.kt")

        assertTrue(ui.contains("label = \"Planbasis\""))
        assertTrue(ui.contains("\"Vanlig grunnturnus gjelder\""))
        assertTrue(ui.contains("\"Arbeidsgiver har fastsatt egen plan\""))
        assertTrue(ui.contains("snapshot.workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN"))
        assertFalse(ui.contains("label = \"Feriearbeidsplan\""))

        assertTrue(pdf.contains("workPlanBasisLabelForPdf"))
        assertTrue(pdf.contains("\"Planbasis\""))
        assertTrue(pdf.contains("\"Arbeidsgivers arbeidsplan\""))
        assertTrue(pdf.contains("s.employerWorkPlanBlocks"))
        assertTrue(pdf.contains("Arbeidsgivers plan som sammenligningsgrunnlag"))
        assertTrue(pdf.contains("Planbasis i eldre ferdigstilling"))
        assertTrue(pdf.contains("I denne ferdigstillingen er vanlig grunnturnus registrert som gjeldende planbasis."))
        assertTrue(pdf.contains("Eventuell gjennomsnittsberegning eller annen arbeidstidsordning må vurderes særskilt med arbeidsgiver."))
        assertTrue(pdf.contains("w.summaryLine(\"Planbasis\", workPlanBasisLabelForPdf(s.workPlanBasis))"))
        assertTrue(pdf.contains("\"Arbeidsgivers planstatus\""))
        assertTrue(pdf.contains("s.workPlanBasis == TripWorkPlanBasis.EMPLOYER_SET_TRIP_PLAN"))
    }
}
