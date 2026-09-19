package com.kurobello.tallybook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

private const val UI_CATALOG_EXTRA = "ui_catalog"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
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
            )
        }
    }
}
