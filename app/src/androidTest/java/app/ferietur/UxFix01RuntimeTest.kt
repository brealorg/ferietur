package app.ferietur

import android.content.Context
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import app.ferietur.domain.EmployerKind
import app.ferietur.domain.PayingParty
import app.ferietur.domain.RosterComparisonMode
import app.ferietur.domain.SavedTripDraft
import app.ferietur.domain.SavedTripDraftCodec
import app.ferietur.domain.WeeklyBasis
import app.ferietur.domain.WeekendProfile
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import org.junit.After
import org.junit.Rule
import org.junit.Test

class UxFix01RuntimeTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val createdDraftIds = linkedSetOf<String>()

    @After
    fun cleanupTestDrafts() {
        Thread.sleep(500)
        createdDraftIds.forEach(::deleteDraftFiles)
        cleanupPrefixedDraftFiles()
    }

    @Test
    fun tripTitleShowsNeutralExample() {
        val title = testTitle("placeholder")
        launchAndNormalizeDisclaimer().use {
            compose.onNodeWithText("Ny tur", useUnmergedTree = true).performClick()
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(TRIP_INTRO) }
            compose.onNodeWithText("For eksempel Høsttur 2026", useUnmergedTree = true).assertExists()
            compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(title)
            Thread.sleep(700)
        }
    }

    @Test
    fun tappingEmptyWorkPlanDayOpensEditorForThatDate() {
        val start = LocalDate.of(2026, 8, 15)
        val title = testTitle("empty-day")
        val id = writeDraft(title, start)
        createdDraftIds += id

        launchAndNormalizeDisclaimer().use {
            scrollToText(title)
            compose.onNodeWithText(title).performClick()
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists("Turoversikt") }

            // The overview row was renamed from "Arbeidsplan" to "Arbeid på turen". Navigate by the
            // stable debug tag instead of by user-facing copy so a wording change cannot break this.
            scrollToTag("overview-nav-trip-plan")
            compose.onNodeWithTag("overview-nav-trip-plan", useUnmergedTree = true).performClick()

            // Prove navigation into the actual work-plan screen before exercising
            // the empty-day interaction. The earlier test clicked a Text node and
            // timed out before the day itself was ever tapped.
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                tagExists("workplan-screen")
            }

            // Exercise the same path a person uses: a physical tap inside the empty
            // day card rather than invoking the semantics OnClick action directly.
            compose.onNodeWithTag("workplan-day-2026-08-15", useUnmergedTree = true)
                .performTouchInput { click() }

            // The physical tap lands over the timeline area in the middle of the card.
            // On an empty day that timeline must not consume the gesture; the parent
            // day-card click must open the editor for the exact tapped date.
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                tagExists("plan-period-editor-2026-08-15") && nodeExists("Velg type")
            }
            compose.onNodeWithTag(
                "plan-period-editor-2026-08-15",
                useUnmergedTree = true,
            ).assertExists()
            compose.onNodeWithText("Velg type", useUnmergedTree = true).assertExists()
            // The dated editor tag above is the authoritative date assertion.
            // The same human-readable date is intentionally present both in the
            // underlying day card and the open editor sheet, so a global
            // onNodeWithText(...) assertion is ambiguous by design.
        }
    }

    private fun launchAndNormalizeDisclaimer(): ActivityScenario<MainActivity> {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            nodeExists(DISCLAIMER_TITLE) || nodeExists(HOME_SECTION)
        }
        if (nodeExists(DISCLAIMER_TITLE)) {
            compose.onNode(hasText("Jeg har lest og forstått") and hasClickAction()).performClick()
            compose.onNodeWithText("Fortsett").performClick()
            compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { !nodeExists(DISCLAIMER_TITLE) }
        }
        compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(HOME_SECTION) }
        return scenario
    }

    private fun nodeExists(text: String): Boolean =
        compose.onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()

    private fun tagExists(tag: String): Boolean =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()

    private fun scrollToTag(tag: String) {
        compose.onAllNodes(hasScrollToNodeAction()).onFirst()
            .performScrollToNode(hasTestTag(tag))
        compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { tagExists(tag) }
    }

    private fun scrollToText(text: String) {
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text))
        compose.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { nodeExists(text) }
    }

    private fun writeDraft(title: String, start: LocalDate): String {
        val id = UUID.randomUUID().toString()
        val end = start.plusDays(3)
        val draft = SavedTripDraft(
            id = id,
            updatedAtEpochMillis = System.currentTimeMillis(),
            screen = "TRIP_PLAN",
            title = title,
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.OSLO_KOMMUNE,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            startDate = start,
            endDate = end,
            startTime = LocalTime.of(7, 0),
            endTime = LocalTime.of(20, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
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
        val file = File(targetContext.filesDir, "trip-drafts/$id.properties")
        file.parentFile?.mkdirs()
        file.outputStream().bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            SavedTripDraftCodec.write(draft, writer)
        }
        return id
    }

    private fun cleanupPrefixedDraftFiles() {
        val directory = File(targetContext.filesDir, "trip-drafts")
        directory.listFiles { file -> file.isFile && file.extension == "properties" }
            .orEmpty()
            .forEach { file ->
                val draft = runCatching {
                    file.inputStream().bufferedReader(StandardCharsets.UTF_8).use(SavedTripDraftCodec::read)
                }.getOrNull()
                if (draft?.title?.startsWith(TEST_TITLE_PREFIX) == true) deleteDraftFiles(draft.id)
            }
    }

    private fun deleteDraftFiles(id: String) {
        File(targetContext.filesDir, "trip-drafts/$id.properties").delete()
        File(targetContext.filesDir, "trip-drafts/$id.properties.bak").delete()
        File(targetContext.filesDir, "trip-draft-backups/$id").deleteRecursively()
    }

    private fun testTitle(kind: String): String = "$TEST_TITLE_PREFIX$kind-${UUID.randomUUID().toString().take(8)}"

    private companion object {
        const val TEST_TITLE_PREFIX = "__FERIETUR_UXFIX01__"
        const val DISCLAIMER_TITLE = "Kontroller alltid beregningen"
        const val HOME_SECTION = "Mine turer"
        const val TRIP_INTRO = "Gi turen et navn og velg når den starter og slutter."
        const val UI_TIMEOUT_MS = 20_000L
    }
}
