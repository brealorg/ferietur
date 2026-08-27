package app.ferietur.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A5.1: deterministic Ferietur palette.
 *
 * Oslo kommune brand accents use the official palette from the design manual:
 * - Oslo gul  #F9C66B
 * - Oslo rød  #FF8274
 * - Oslo mørk blå #2A2859
 *
 * The approved Ferietur dark concept keeps its deep navy working surfaces while
 * using Oslo yellow for completion/status accents. Dynamic wallpaper colours are
 * intentionally disabled so the app has a stable visual identity on every device.
 */
val OsloDarkBlue = Color(0xFF2A2859)
val OsloBlue = Color(0xFF6FE9FF)
val OsloLightBlue = Color(0xFFB3F5FF)
val OsloRed = Color(0xFFFF8274)
val OsloYellow = Color(0xFFF9C66B)
val OsloBlack = Color(0xFF2C2C2C)
val OsloLightBeige = Color(0xFFF8F0DD)

// Working-surface tones from the approved dark Ferietur concept.
private val FerieturNavy = Color(0xFF0D1831)
private val FerieturNavySurface = Color(0xFF17233D)
private val FerieturNavySurfaceHigh = Color(0xFF202D4B)
private val FerieturPeriwinkle = Color(0xFFAFC6FF)
private val FerieturPeriwinkleContainer = Color(0xFF33466F)
private val FerieturMuted = Color(0xFFB9C1D0)

private val DarkColors = darkColorScheme(
    primary = FerieturPeriwinkle,
    onPrimary = Color(0xFF10203D),
    primaryContainer = FerieturPeriwinkleContainer,
    onPrimaryContainer = Color(0xFFE6ECFF),
    secondary = OsloYellow,
    onSecondary = Color(0xFF2A2417),
    secondaryContainer = Color(0xFF5A4925),
    onSecondaryContainer = Color(0xFFFFE8B8),
    tertiary = OsloLightBlue,
    onTertiary = Color(0xFF0A3540),
    tertiaryContainer = OsloDarkBlue,
    onTertiaryContainer = Color(0xFFE8E7FF),
    error = OsloRed,
    onError = Color(0xFF44100B),
    errorContainer = Color(0xFF5B241F),
    onErrorContainer = Color(0xFFFFDAD5),
    background = FerieturNavy,
    onBackground = Color(0xFFF5F7FF),
    surface = FerieturNavy,
    onSurface = Color(0xFFF5F7FF),
    surfaceVariant = FerieturNavySurfaceHigh,
    onSurfaceVariant = FerieturMuted,
    surfaceContainerLowest = Color(0xFF091226),
    surfaceContainerLow = Color(0xFF111D35),
    surfaceContainer = FerieturNavySurface,
    surfaceContainerHigh = FerieturNavySurfaceHigh,
    surfaceContainerHighest = Color(0xFF293655),
    outline = Color(0xFF707B91),
    outlineVariant = Color(0xFF3B4963),
)

private val LightColors = lightColorScheme(
    primary = OsloDarkBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E3FF),
    onPrimaryContainer = OsloDarkBlue,
    secondary = OsloYellow,
    onSecondary = Color(0xFF352A12),
    secondaryContainer = Color(0xFFFFE7B4),
    onSecondaryContainer = Color(0xFF352A12),
    tertiary = Color(0xFF007184),
    onTertiary = Color.White,
    tertiaryContainer = OsloLightBlue,
    onTertiaryContainer = Color(0xFF00363F),
    error = Color(0xFFB53A30),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD5),
    onErrorContainer = Color(0xFF410001),
    background = OsloLightBeige,
    onBackground = OsloBlack,
    surface = Color.White,
    onSurface = OsloBlack,
    surfaceVariant = Color(0xFFF1E9D8),
    onSurfaceVariant = Color(0xFF5E5A52),
    surfaceContainer = Color(0xFFFFFBF2),
    surfaceContainerHigh = Color(0xFFF3ECDE),
    outline = Color(0xFF7B776F),
    outlineVariant = Color(0xFFD5CEC0),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FerieturTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors

    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp),
        ),
        content = content,
    )
}
