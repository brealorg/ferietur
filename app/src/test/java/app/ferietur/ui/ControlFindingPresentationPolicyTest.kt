package app.ferietur.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ControlFindingPresentationPolicyTest {
    private val engineDetail =
        "Du har registrert 138 t arbeid i løpet av sju dager, " +
            "fra tirsdag 11. august kl. 06:00 til mandag 17. august kl. 22:00. " +
            "Arbeidsmiljøloven § 10-6 åttende ledd setter grenser for samlet arbeidstid. " +
            "I noen arbeidstidsordninger kan 48-timersgrensen gjennomsnittsberegnes over åtte uker."

    @Test
    fun readsHoursAndWindowFromTheEngineSentence() {
        val window = ControlFindingPresentationPolicy.sevenDayWindow(engineDetail)!!
        assertEquals("138 t", window.hours)
        assertEquals("tirsdag 11. august kl. 06:00", window.start)
        assertEquals("mandag 17. august kl. 22:00", window.end)
    }

    @Test
    fun keepsHoursWithMinutes() {
        val window = ControlFindingPresentationPolicy.sevenDayWindow(
            "Du har registrert 49 t 30 min arbeid i løpet av sju dager, " +
                "fra onsdag 1. juli kl. 07:00 til tirsdag 7. juli kl. 15:30. Resten.",
        )!!
        assertEquals("49 t 30 min", window.hours)
        assertEquals("tirsdag 7. juli kl. 15:30", window.end)
    }

    @Test
    fun unknownSentenceShapeFallsBackToGenericLayout() {
        assertNull(ControlFindingPresentationPolicy.sevenDayWindow(""))
        assertNull(ControlFindingPresentationPolicy.sevenDayWindow("Samlet arbeidstid er høy."))
        assertNull(
            ControlFindingPresentationPolicy.sevenDayWindow(
                "Du har registrert 50 t arbeid i løpet av sju dager, fra en ukjent periode. Resten.",
            ),
        )
    }

    @Test
    fun titleMatchesTheDomainFinding() {
        assertEquals("Mer enn 48 timer i en sju-dagersperiode", ControlFindingPresentationPolicy.SEVEN_DAY_TITLE)
    }
}
