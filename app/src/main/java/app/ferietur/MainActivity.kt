package app.ferietur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.ferietur.ui.FerieturApp
import app.ferietur.ui.theme.FerieturTheme
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class MainActivity : ComponentActivity() {
    // A54R3_NB_NO_ACTIVITY_RESOURCE_CONTEXT:
    // Ferietur is Norwegian-first. Material3's TimeInput resolves its internal
    // labels from LocalContext.current.resources, so the Activity base context
    // must carry nb-NO rather than only local picker Configuration overrides.
    override fun attachBaseContext(newBase: Context) {
        val norwegianConfiguration = Configuration(newBase.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("nb-NO"))
        }
        super.attachBaseContext(
            newBase.createConfigurationContext(norwegianConfiguration),
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FerieturTheme {
                FerieturApp()
            }
        }
    }
}
