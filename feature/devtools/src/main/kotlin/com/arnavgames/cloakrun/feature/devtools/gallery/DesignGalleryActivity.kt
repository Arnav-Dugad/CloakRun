package com.arnavgames.cloakrun.feature.devtools.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.arnavgames.cloakrun.core.designsystem.theme.CloakTheme
import com.arnavgames.cloakrun.core.designsystem.theme.ThemePreference

/** Debug-only entry point ("CloakRun Gallery" in the launcher) for reviewing the design system. */
class DesignGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            var theme by rememberSaveable { mutableStateOf(ThemePreference.Night) }
            var reduceMotion by rememberSaveable { mutableStateOf(false) }
            var performance by rememberSaveable { mutableStateOf(false) }
            CloakTheme(preference = theme, reduceMotion = reduceMotion, performanceMode = performance) {
                DesignGallery(
                    settings = GallerySettings(theme, reduceMotion, performance),
                    onThemeChange = { theme = it },
                    onReduceMotionChange = { reduceMotion = it },
                    onPerformanceChange = { performance = it },
                )
            }
        }
    }
}
