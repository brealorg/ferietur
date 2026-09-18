package app.ferietur.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppChangelogTest {
    @Test
    fun existingInstallationSeesRelease54Once() {
        assertEquals(
            UpdatePromptAction.SHOW_CHANGELOG,
            AppChangelog.updatePromptAction(
                disclaimerAcknowledgementVersion = 2,
                lastAcknowledgedVersionCode = 0,
                currentVersionCode = 54,
            ),
        )

        val pending = AppChangelog.releasesAfter(
            lastAcknowledgedVersionCode = 53,
            currentVersionCode = 54,
        )
        assertEquals(listOf(54), pending.map { it.versionCode })
    }

    @Test
    fun freshInstallSeedsCurrentVersionWithoutPretendingItWasAnUpdate() {
        assertEquals(
            UpdatePromptAction.ACKNOWLEDGE_SILENTLY,
            AppChangelog.updatePromptAction(
                disclaimerAcknowledgementVersion = 0,
                lastAcknowledgedVersionCode = 0,
                currentVersionCode = 54,
            ),
        )
    }

    @Test
    fun acknowledgedCurrentVersionDoesNotPromptAgain() {
        assertEquals(
            UpdatePromptAction.NONE,
            AppChangelog.updatePromptAction(
                disclaimerAcknowledgementVersion = 2,
                lastAcknowledgedVersionCode = 54,
                currentVersionCode = 54,
            ),
        )
    }

    @Test
    fun release54ExplicitlyCallsOutCalculationChanges() {
        val release = requireNotNull(AppChangelog.releaseForVersion(54))
        assertEquals("0.6.0", release.versionName)
        assertTrue(
            release.changes.count { it.severity == ChangeSeverity.CALCULATION_CHANGE } >= 2,
        )
        assertTrue(
            release.intro.contains("Beregningen er forbedret i denne versjonen."),
        )
    }
}
