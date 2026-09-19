package com.kurobello.tallybook

import androidx.compose.ui.window.ComposeUIViewController
import com.kurobello.tallybook.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(
    developmentToolsEnabled: Boolean,
    uiCatalogRequested: Boolean,
): UIViewController {
  initKoin()
  val launchMode = resolveAppLaunchMode(developmentToolsEnabled, uiCatalogRequested)
  return ComposeUIViewController {
    App(
        launchMode = launchMode,
        developmentToolsEnabled = developmentToolsEnabled,
    )
  }
}
