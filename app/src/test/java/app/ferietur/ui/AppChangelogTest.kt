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
    fun upgradeFrom54SeesOnlyRelease55AndOlderInstallsSeeBothInOrder() {
        assertEquals(
            UpdatePromptAction.SHOW_CHANGELOG,
            AppChangelog.updatePromptAction(
                disclaimerAcknowledgementVersion = 2,
                lastAcknowledgedVersionCode = 54,
                currentVersionCode = 55,
            ),
        )
        assertEquals(
            listOf(55),
            AppChangelog.releasesAfter(lastAcknowledgedVersionCode = 54, currentVersionCode = 55)
                .map { it.versionCode },
        )
        assertEquals(
            listOf(54, 55),
            AppChangelog.releasesAfter(lastAcknowledgedVersionCode = 53, currentVersionCode = 55)
                .map { it.versionCode },
        )
    }

    @Test
    fun release55DoesNotClaimCalculationChanges() {
        val release = requireNotNull(AppChangelog.releaseForVersion(55))
        assertEquals("0.6.1", release.versionName)
        assertTrue(release.changes.none { it.severity == ChangeSeverity.CALCULATION_CHANGE })
        assertTrue(release.intro.contains("endrer ingen beregninger"))
    }

    @Test
    fun versionCodesAreUnique() {
        val codes = AppChangelog.releases.map { it.versionCode }
        assertEquals(codes.distinct(), codes)
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
