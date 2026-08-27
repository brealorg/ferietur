package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleApplicabilityWorktimePolicyTest {
    private val friday = LocalDate.of(2026, 8, 14)

    @Test
    fun chapter20RejectsSameDayTripAndAcceptsOvernightStay() {
        val start = LocalDateTime.of(friday, LocalTime.of(8, 0))
        assertTrue(TripPlanEngine.isDayTrip(start, LocalDateTime.of(friday, LocalTime.of(10, 0))))
        assertFalse(TripPlanEngine.chapter20Applies(start, LocalDateTime.of(friday, LocalTime.of(10, 0))))
        assertTrue(TripPlanEngine.chapter20Applies(start, LocalDateTime.of(friday.plusDays(1), LocalTime.of(8, 0))))
    }

    @Test
    fun travelWithoutResponsibilityInsideOrdinaryRosterCountsAsWorktime() {
        val blocks = TripPlanEngine.projectRange(
            listOf(friday),
            mapOf(friday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(8, 0), LocalTime.of(10, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY))),
        )
        val findings = TripPlanEngine.controlFindings(
            blocks = blocks,
            unresolvedRuleCount = 0,
            roster = mapOf(friday to "LV"),
        )

        val total = findings.first { it.title == "Samlet arbeidstid" }
        assertTrue(total.detail.startsWith("1 t "))
        val travel = findings.first { it.title == "Reise uten tilsynsansvar er behandlet etter reisetidsreglene" }
        assertTrue(travel.detail.contains("faller i grunnturnusen regnes fullt ut som arbeidstid"))
    }

    @Test
    fun travelWithoutResponsibilityOutsideRosterDoesNotBecomeWorktimeByItself() {
        val blocks = TripPlanEngine.projectRange(
            listOf(friday),
            mapOf(friday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, LocalTime.of(7, 0), LocalTime.of(8, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY))),
        )
        val findings = TripPlanEngine.controlFindings(
            blocks = blocks,
            unresolvedRuleCount = 0,
            roster = mapOf(friday to "LV"),
        )

        val total = findings.first { it.title == "Samlet arbeidstid" }
        assertTrue(total.detail.startsWith("0 t "))
    }
    @Test
    fun sleepAllowedNightTravelOutsideRosterCountsTimeForTimeAsWorktime() {
        val blocks = TripPlanEngine.projectRange(
            listOf(friday),
            mapOf(friday to listOf(PlannedBlock(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED, LocalTime.of(23, 0), LocalTime.of(7, 0), TravelNoticeStatus.KNOWN_BY_PREVIOUS_DAY))),
        )
        val findings = TripPlanEngine.controlFindings(blocks, unresolvedRuleCount = 0, roster = emptyMap())

        val total = findings.first { it.title == "Samlet arbeidstid" }
        assertTrue(total.detail.startsWith("8 t "))
        assertTrue(findings.any { it.title == "Passiv nattreise teller som arbeidstid" })
    }

}
