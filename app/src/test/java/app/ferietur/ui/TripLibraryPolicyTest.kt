package app.ferietur.ui

import app.ferietur.domain.EmployerKind
import app.ferietur.domain.PayingParty
import app.ferietur.domain.RosterComparisonMode
import app.ferietur.domain.SavedTripDraft
import app.ferietur.domain.WeeklyBasis
import app.ferietur.domain.WeekendProfile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TripLibraryPolicyTest {
    @Test
    fun standardFlowProgressUsesNineSteps() {
        val trip = sample(screen = "TRIP_PLAN", mode = RosterComparisonMode.USE_NORMAL_ROSTER)
        assertEquals(TripLibraryProgress(5, 9), savedTripProgress(trip))
    }

    @Test
    fun separateFlowProgressUsesEightSteps() {
        val trip = sample(screen = "TRIP_PLAN", mode = RosterComparisonMode.DO_NOT_USE_NORMAL_ROSTER)
        assertEquals(TripLibraryProgress(4, 8), savedTripProgress(trip))
    }

    @Test
    fun summaryIsSeparatedFromInProgressTrips() {
        assertEquals(TripLibraryLifecycle.IN_PROGRESS, savedTripLifecycle(sample(screen = "CONTROL")))
        assertEquals(TripLibraryLifecycle.SUMMARY_READY, savedTripLifecycle(sample(screen = "SUMMARY")))
    }

    @Test
    fun copyBecomesFreshEditableDraftWithoutOldConfirmations() {
        val source = sample(screen = "SUMMARY").copy(
            title = "Solgården",
            payslipChecked = true,
            rosterGapConfirmed = true,
            settlementMode = "CUSTOM_AGREEMENT",
            settlementAmountText = "1234",
            settlementReason = "Avtalt",
        )
        val copy = copiedTripDraft(source, newId = "copy-id", now = 123L)
        assertEquals("copy-id", copy.id)
        assertEquals("Kopi av Solgården", copy.title)
        assertEquals("TRIP", copy.screen)
        assertFalse(copy.payslipChecked)
        assertFalse(copy.rosterGapConfirmed)
        assertEquals("FULL_CALCULATION", copy.settlementMode)
        assertEquals("", copy.settlementAmountText)
        assertEquals("", copy.settlementReason)
        assertEquals(source.plans, copy.plans)
        assertEquals(source.roster, copy.roster)
    }

    private fun sample(
        screen: String,
        mode: RosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
    ) = SavedTripDraft(
        id = "id",
        updatedAtEpochMillis = 1L,
        screen = screen,
        title = "Tur",
        employerKind = EmployerKind.OSLO_KOMMUNE,
        payingParty = PayingParty.RESIDENT_OR_GUARDIAN,
        rosterComparisonMode = mode,
        startDate = LocalDate.of(2026, 8, 10),
        endDate = LocalDate.of(2026, 8, 11),
        startTime = LocalTime.of(8, 0),
        endTime = LocalTime.of(18, 0),
        salaryStep = 32,
        weeklyBasis = WeeklyBasis.HOURS_35_5,
        weekendProfile = WeekendProfile.STANDARD,
        payslipChecked = false,
        rosterGapConfirmed = false,
        roster = emptyMap(),
        plans = emptyMap(),
        outboundArrival = LocalDateTime.of(2026, 8, 10, 10, 0),
        returnDeparture = LocalDateTime.of(2026, 8, 11, 16, 0),
        outboundTravelKind = null,
        returnTravelKind = null,
        settlementMode = "FULL_CALCULATION",
        settlementAmountText = "",
        settlementReason = "",
    )
}
