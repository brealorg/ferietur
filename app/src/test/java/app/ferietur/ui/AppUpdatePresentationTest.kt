package app.ferietur.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatePresentationTest {
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
    fun acknowledgementPersistsAndPostUpdateUsesFullscreenSurface() {
        val prefs = source("src/main/java/app/ferietur/ui/AppInfoPreferences.kt")
        val ui = source("src/main/java/app/ferietur/ui/FerieturApp.kt")
        val gradle = source("build.gradle.kts")

        assertTrue(prefs.contains("last_acknowledged_version_code"))
        assertTrue(prefs.contains("fun lastAcknowledgedVersionCode("))
        assertTrue(prefs.contains("suspend fun acknowledgeVersionCode("))

        assertTrue(ui.contains("private enum class WhatsNewMode"))
        assertTrue(ui.contains("WhatsNewMode.POST_UPDATE"))
        assertTrue(ui.contains("WhatsNewMode.ABOUT"))
        assertTrue(ui.contains("private fun BoxScope.WhatsNewScrollEdgeFade("))
        assertTrue(ui.contains("private fun WhatsNewScreen("))
        assertTrue(ui.contains("val listState = rememberLazyListState()"))
        assertTrue(ui.contains("val canScrollBackward by remember"))
        assertTrue(ui.contains("val canScrollForward by remember"))
        assertTrue(ui.contains("BackHandler(enabled = true)"))
        assertTrue(ui.contains(".align(Alignment.BottomCenter)"))
        assertTrue(ui.contains("bottom = if (mode == WhatsNewMode.POST_UPDATE) 112.dp else 32.dp"))
        assertTrue(ui.contains("val bottomFadeInset = if (mode == WhatsNewMode.POST_UPDATE) 88.dp else 0.dp"))
        assertTrue(ui.contains("WhatsNewScrollEdgeFade("))
        assertTrue(ui.contains("visible = canScrollBackward"))
        assertTrue(ui.contains("visible = canScrollForward"))
        assertTrue(ui.contains("androidx.compose.material3.Button("))
        assertTrue(ui.contains("Text(if (writeInProgress) \"Lagrer…\" else \"Forstått\")"))
        assertTrue(ui.contains("!shouldShowWhatsNew"))
        assertTrue(ui.contains("mode = WhatsNewMode.POST_UPDATE"))
        assertTrue(ui.contains("mode = WhatsNewMode.ABOUT"))
        assertTrue(ui.contains("ChangeSeverity.CALCULATION_CHANGE -> \"BEREGNING\""))
        assertTrue(ui.contains("ChangeSeverity.IMPORTANT -> \"VIKTIG\""))
        assertTrue(ui.contains("ChangeSeverity.NORMAL -> \"NYTT\""))

        assertFalse(ui.contains("private fun WhatsNewDialog("))
        assertFalse(ui.contains("androidx.compose.ui.window.Dialog("))
        assertFalse(ui.contains(".heightIn(max = 600.dp)"))

        assertTrue(gradle.contains("versionCode = 54"))
        assertTrue(gradle.contains("versionName = \"0.6.0\""))
    }
}
