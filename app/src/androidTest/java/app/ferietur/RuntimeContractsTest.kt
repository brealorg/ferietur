package app.ferietur

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import app.ferietur.domain.EmployerKind
import app.ferietur.domain.PayingParty
import app.ferietur.domain.RosterComparisonMode
import app.ferietur.domain.SavedTripDraft
import app.ferietur.domain.SavedTripDraftCodec
import app.ferietur.domain.WeeklyBasis
import app.ferietur.domain.WeekendProfile
import app.ferietur.export.PdfExporter
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * TEST01 / CA-010
 *
 * Deliberately small on-device contract suite. Domain/tariff mathematics remain
 * in JVM tests; these tests cover Android lifecycle/framework boundaries that
 * JVM tests cannot prove.
 *
 * The suite is non-destructive to ordinary Ferietur drafts. Any test draft uses
 * the TEST01- title prefix and/or a tracked UUID and is removed in teardown.
 */
class RuntimeContractsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val createdDraftIds = linkedSetOf<String>()
    private val createdExportFiles = linkedSetOf<File>()

    @After
    fun cleanupTestArtifacts() {
        // Give any in-flight autosave that started just before Activity teardown
        // a chance to finish before deleting our namespaced test artifacts.
        Thread.sleep(400)
        createdDraftIds.forEach(::deleteDraftFiles)
        cleanupPrefixedDraftFiles()
        createdExportFiles.forEach { it.delete() }
    }

    @Test
    fun coldLaunchDoesNotCrash() {
        launchAndNormalizeDisclaimer().use { scenario ->
            compose.onNodeWithText(HOME_SECTION).assertExists()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun disclaimerAcceptancePersistsAcrossRestart() {
        launchAndNormalizeDisclaimer().close()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                nodeExists(HOME_SECTION)
            }
            compose.onNodeWithText(DISCLAIMER_TITLE).assertDoesNotExist()
            compose.onNodeWithText(HOME_SECTION).assertExists()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun systemBackFollowsAppHierarchy() {
        val title = testTitle("back")
        launchAndNormalizeDisclaimer().use { scenario ->
            compose.onNodeWithText("Ny tur", useUnmergedTree = true).performClick()
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(TRIP_INTRO) }

            compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(title)
            compose.onNodeWithText("Lønn og betaling").assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(METHOD_PROMPT) }

            scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(TRIP_INTRO) }

            scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(HOME_SECTION) }

            compose.onNodeWithText(HOME_SECTION).assertExists()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun rotationRetainsActiveTripAndStep() {
        val title = testTitle("rotation")
        launchAndNormalizeDisclaimer().use { scenario ->
            compose.onNodeWithText("Ny tur", useUnmergedTree = true).performClick()
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(TRIP_INTRO) }
            compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(title)
            compose.onNodeWithText("Lønn og betaling").assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(METHOD_PROMPT) }

            val beforeOrientation = currentOrientation(scenario)
            scenario.onActivity { activity ->
                activity.requestedOrientation = if (beforeOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }

            compose.waitUntil(timeoutMillis = ROTATION_TIMEOUT_MS) {
                currentOrientation(scenario) != beforeOrientation && nodeExists(METHOD_PROMPT)
            }
            compose.onNodeWithText(METHOD_PROMPT).assertExists()

            // Return to the previous step after the real configuration change and
            // prove the Activity-scoped ViewModel kept the editable trip state too.
            compose.onNodeWithContentDescription("Tilbake").performClick()
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(TRIP_INTRO) }
            compose.onAllNodes(hasSetTextAction()).onFirst().assertTextContains(title)
        }
    }

    @Test
    fun storedDraftAndRecoverySurviveRealActivityLifecycle() {
        val title = testTitle("recovery")
        val id = prepareRecoverableDraft(title)
        createdDraftIds += id

        launchAndNormalizeDisclaimer().use {
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                nodeExists("Lagret tur gjenopprettet")
            }
            compose.onNodeWithText("Lagret tur gjenopprettet").assertExists()
            scrollHomeToText(title)
            compose.onNodeWithText(title).assertExists()
        }

        // A completely new Activity instance must now read the healed primary
        // draft from app-private storage, not rely on the previous Activity VM.
        launchAndNormalizeDisclaimer().use { scenario ->
            scrollHomeToText(title)
            compose.onNodeWithText(title).performClick()
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists("Turoversikt") }
            compose.onNodeWithText("Turoversikt").assertExists()
            compose.onNodeWithText(title).assertExists()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun pdfShareUsesValidFileProviderContentUri() {
        val exportDir = File(targetContext.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "TEST01-${UUID.randomUUID()}.pdf")
        file.writeBytes("%PDF-1.4\n% TEST01\n".toByteArray(StandardCharsets.UTF_8))
        createdExportFiles += file

        val capturingContext = CapturingContext(targetContext)
        PdfExporter.sharePrepared(capturingContext, file.absolutePath)

        val chooser = assertNotNullAndReturn(capturingContext.startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)

        val sendIntent = chooser.intentExtra(Intent.EXTRA_INTENT)
        assertNotNull(sendIntent)
        sendIntent!!
        assertEquals(Intent.ACTION_SEND, sendIntent.action)
        assertEquals("application/pdf", sendIntent.type)
        assertTrue(sendIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)

        val streamUri = sendIntent.uriExtra(Intent.EXTRA_STREAM)
        assertNotNull(streamUri)
        streamUri!!
        assertEquals("content", streamUri.scheme)
        assertEquals("${targetContext.packageName}.files", streamUri.authority)

        targetContext.contentResolver.openInputStream(streamUri).use { input ->
            assertNotNull(input)
            assertTrue(input!!.read() >= 0)
        }
    }

    private fun launchAndNormalizeDisclaimer(): ActivityScenario<MainActivity> {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            nodeExists(DISCLAIMER_TITLE) || nodeExists(HOME_SECTION)
        }

        if (nodeExists(DISCLAIMER_TITLE)) {
            compose.onNode(
                hasText("Jeg har lest og forstått") and hasClickAction(),
            ).performClick()
            compose.onNodeWithText("Fortsett").assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                !nodeExists(DISCLAIMER_TITLE)
            }
        }

        compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(HOME_SECTION) }
        return scenario
    }

    private fun nodeExists(text: String): Boolean =
        compose.onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()

    private fun scrollHomeToText(text: String) {
        compose.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasText(text))
        compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(text) }
    }

    private fun currentOrientation(scenario: ActivityScenario<MainActivity>): Int {
        val value = AtomicInteger(Configuration.ORIENTATION_UNDEFINED)
        scenario.onActivity { activity ->
            value.set(activity.resources.configuration.orientation)
        }
        return value.get()
    }

    private fun prepareRecoverableDraft(title: String): String {
        val id = UUID.randomUUID().toString()
        val draft = testDraft(id = id, title = title)
        val primary = File(targetContext.filesDir, "trip-drafts/$id.properties")
        val backupDir = File(targetContext.filesDir, "trip-draft-backups/$id").apply { mkdirs() }
        primary.parentFile?.mkdirs()

        val backup = File(backupDir, "${System.currentTimeMillis()}.properties")
        backup.outputStream().bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            SavedTripDraftCodec.write(draft, writer)
        }
        primary.writeText("not-a-valid-ferietur-draft", StandardCharsets.UTF_8)
        return id
    }

    private fun testDraft(id: String, title: String): SavedTripDraft {
        val start = LocalDate.of(2026, 8, 25)
        val end = start.plusDays(6)
        return SavedTripDraft(
            id = id,
            updatedAtEpochMillis = System.currentTimeMillis(),
            screen = "TRIP",
            title = title,
            employerKind = EmployerKind.UNSPECIFIED,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            startDate = start,
            endDate = end,
            startTime = LocalTime.of(7, 0),
            endTime = LocalTime.of(20, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = false,
            rosterGapConfirmed = false,
            roster = emptyMap(),
            plans = emptyMap(),
            outboundArrival = LocalDateTime.of(start, LocalTime.of(11, 0)),
            returnDeparture = LocalDateTime.of(end, LocalTime.of(16, 0)),
            outboundTravelKind = null,
            returnTravelKind = null,
            settlementMode = "FULL_CALCULATION",
            settlementAmountText = "",
            settlementReason = "",
        )
    }

    private fun cleanupPrefixedDraftFiles() {
        val directory = File(targetContext.filesDir, "trip-drafts")
        directory.listFiles { file -> file.isFile && file.extension == "properties" }
            .orEmpty()
            .forEach { file ->
                val draft = runCatching {
                    file.inputStream().bufferedReader(StandardCharsets.UTF_8).use(SavedTripDraftCodec::read)
                }.getOrNull()
                if (draft?.title?.startsWith(TEST_TITLE_PREFIX) == true) {
                    deleteDraftFiles(draft.id)
                }
            }
    }

    private fun deleteDraftFiles(id: String) {
        File(targetContext.filesDir, "trip-drafts/$id.properties").delete()
        File(targetContext.filesDir, "trip-drafts/$id.properties.bak").delete()
        File(targetContext.filesDir, "trip-draft-backups/$id").deleteRecursively()
    }

    private fun testTitle(kind: String): String =
        "$TEST_TITLE_PREFIX$kind-${UUID.randomUUID().toString().take(8)}"

    @Suppress("DEPRECATION")
    private fun Intent.intentExtra(name: String): Intent? =
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(name, Intent::class.java)
        } else {
            getParcelableExtra(name)
        }

    @Suppress("DEPRECATION")
    private fun Intent.uriExtra(name: String): Uri? =
        if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(name, Uri::class.java)
        } else {
            getParcelableExtra(name)
        }

    private fun <T : Any> assertNotNullAndReturn(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    private class CapturingContext(base: Context) : ContextWrapper(base) {
        var startedIntent: Intent? = null
            private set

        override fun startActivity(intent: Intent) {
            startedIntent = intent
        }
    }

    private companion object {
        const val TEST_TITLE_PREFIX = "__FERIETUR_TEST01_CA010__"
        const val DISCLAIMER_TITLE = "Kontroller alltid beregningen"
        const val HOME_SECTION = "Mine turer"
        const val TRIP_INTRO = "Gi turen et navn og velg når den starter og slutter."
        const val METHOD_PROMPT = "Hvordan skal turen lønnes?"
        const val UI_TIMEOUT_MS = 15_000L
        const val ROTATION_TIMEOUT_MS = 20_000L
    }
}
