package app.ferietur.ui

import app.ferietur.domain.TimeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NightTravelStateIntegrityPolicyTest {

    @Test
    fun tappingAlreadySelectedWithoutResponsibilityDoesNotEraseSleepAllowed() {
        assertEquals(
            TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED,
            travelWithoutResponsibilitySelection(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED),
        )
    }

    @Test
    fun tappingAlreadySelectedWithoutResponsibilityDoesNotEraseNoSleep() {
        assertEquals(
            TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP,
            travelWithoutResponsibilitySelection(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP),
        )
    }

    @Test
    fun switchingFromAnotherResponsibilityStateStartsUnresolved() {
        assertEquals(
            TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY,
            travelWithoutResponsibilitySelection(TimeKind.TRAVEL_WITH_RESPONSIBILITY),
        )
    }

    @Test
    fun summaryLabelsExposeNightSleepDecision() {
        assertEquals(
            "Søvntillatelse: Ja",
            nightTravelSleepStatus(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED, true),
        )
        assertEquals(
            "Søvntillatelse: Nei",
            nightTravelSleepStatus(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_NO_SLEEP, true),
        )
        assertEquals(
            "Søvntillatelse: Ikke avklart",
            nightTravelSleepStatus(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, true),
        )
        assertNull(nightTravelSleepStatus(TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY, false))
    }
    @Test
    fun reselectingTravelTypeDoesNotEraseResponsibilityOrSleepSubstate() {
        assertEquals(
            TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED,
            periodTypeSelection(
                TimeKind.TRAVEL_WITHOUT_RESPONSIBILITY_SLEEP_ALLOWED,
                TimeKind.TRAVEL_UNCERTAIN,
            ),
        )
        assertEquals(
            TimeKind.TRAVEL_WITH_RESPONSIBILITY,
            periodTypeSelection(TimeKind.TRAVEL_WITH_RESPONSIBILITY, TimeKind.TRAVEL_UNCERTAIN),
        )
    }

}
