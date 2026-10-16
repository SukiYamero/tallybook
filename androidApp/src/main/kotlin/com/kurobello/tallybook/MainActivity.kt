package com.kurobello.tallybook

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kurobello.tallybook.core.model.ThemeMode

private const val UI_CATALOG_EXTRA = "ui_catalog"

// Same scrims as the androidx.activity defaults; only the dark-mode detection is overridden.
private val NavigationLightScrim = Color.parseColor("#E6FFFFFF")
private val NavigationDarkScrim = Color.parseColor("#801B1B1B")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applySystemBars(ThemeMode.SYSTEM)
        super.onCreate(savedInstanceState)

        val launchMode =
            resolveAppLaunchMode(
                developmentToolsEnabled = BuildConfig.DEBUG,
                uiCatalogRequested = intent.getBooleanExtra(UI_CATALOG_EXTRA, false),
            )

        setContent {
            App(
                launchMode = launchMode,
                developmentToolsEnabled = BuildConfig.DEBUG,
                onThemeModeChanged = ::applySystemBars,
            )
        }
    }

    private fun applySystemBars(themeMode: ThemeMode) {
        val isDark: (Resources) -> Boolean =
            when (themeMode) {
                ThemeMode.SYSTEM -> { resources ->
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                        Configuration.UI_MODE_NIGHT_YES
                }
                ThemeMode.LIGHT -> { _ -> false }
                ThemeMode.DARK -> { _ -> true }
            }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT, isDark),
            navigationBarStyle = SystemBarStyle.auto(NavigationLightScrim, NavigationDarkScrim, isDark),
        )
        window.isNavigationBarContrastEnforced = false
    }
}
