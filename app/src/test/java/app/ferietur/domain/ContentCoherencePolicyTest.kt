package app.ferietur.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ContentCoherencePolicyTest {
    private val monday = LocalDate.of(2026, 8, 10)

    @Test
    fun activeAdditionalWorkExplainsAppComparisonModelSeparatelyFromTariffWording() {
        val plans = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.ACTIVE_WORK, LocalTime.of(8, 0), LocalTime.of(10, 0)),
            ),
        )
        val result = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            emptyMap(),
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(8, 0)),
            LocalDateTime.of(monday, LocalTime.of(10, 0)),
        )

        val line = result.lines.first { it.id == "active" }
        assertTrue(line.explanation.contains("grunnturnusen som sammenligningsgrunnlag"))
        assertTrue(line.explanation.contains("arbeidstid ut over ordinær arbeidstid etter kapittel 8"))
        assertTrue(line.explanation.contains("må avklares med arbeidsgiver"))
        assertFalse(line.explanation.startsWith("Arbeid som ligger utenfor grunnturnusen beregnes"))
    }

    @Test
    fun coveredRosterSupplementIsExplicitlyControlOnly() {
        val roster = mapOf(monday to "LV")
        val plans = TripPlanEngine.planFromRoster(listOf(monday), roster)
        val result = TripPlanEngine.calculatePreliminary(
            FundingMode.TURNUS_PLUS_EXTERNAL,
            listOf(monday),
            roster,
            plans,
            OsloSalaryTable2026.annualSalary(32),
            WeeklyBasis.HOURS_35_5,
            WeekendProfile.STANDARD,
            LocalDateTime.of(monday, LocalTime.of(9, 0)),
            LocalDateTime.of(monday, LocalTime.of(21, 0)),
        )

        val line = result.lines.first { it.id == "evening-night" }
        assertEquals(PaymentTreatment.ALREADY_COVERED_BY_NORMAL_ROSTER, line.paymentTreatment)
        assertTrue(line.explanation.contains("vises bare for kontroll"))
        assertTrue(line.explanation.contains("ikke med i betalingsgrunnlaget for turen"))
        assertFalse(line.explanation.contains("Hvem som faktisk skal dekke"))
    }

    @Test
    fun openRulesAffectAmountNotPaymentScenarioAndUseCorrectSingular() {
        val blocks = listOf(
            WorkBlock(
                LocalDateTime.of(monday, LocalTime.of(8, 0)),
                LocalDateTime.of(monday, LocalTime.of(9, 0)),
                TimeKind.ACTIVE_WORK,
            ),
        )
        val findings = TripPlanEngine.controlFindings(blocks, unresolvedRuleCount = 1)
        val ruleFinding = findings.first { it.severity == FindingSeverity.OPEN }

        assertEquals("1 regel må fortsatt avklares", ruleFinding.title)
        assertTrue(ruleFinding.detail.contains("kan påvirke det endelige beregnede beløpet"))
        assertFalse(ruleFinding.detail.contains("hvem som skal betale"))
    }

    @Test
    fun unresolvedNightTravelExplainsRequiredSleepPermissionWithoutInternalSliceName() {
        val plans = mapOf(
            monday to listOf(
                PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(23, 0), LocalTime.of(1, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY),
            ),
        )
        val findings = TripPlanEngine.controlFindings(
            TripPlanEngine.projectRange(listOf(monday), plans),
            unresolvedRuleCount = 1,
        )
        val night = findings.first { it.title == "Søvntillatelse under nattreisen må avklares" }
        assertTrue(night.detail.contains("tillatelse til å sove"))
        assertTrue(night.detail.contains("Ja, Nei eller Ikke avklart"))
        assertFalse(night.detail.contains("A4.0"))
    }

}
