package com.kurobello.tallybook

import androidx.compose.ui.window.ComposeUIViewController
import com.kurobello.tallybook.core.model.ThemeMode
import com.kurobello.tallybook.di.initKoin
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

fun MainViewController(
    developmentToolsEnabled: Boolean,
    uiCatalogRequested: Boolean,
): UIViewController {
  initKoin()
  val launchMode = resolveAppLaunchMode(developmentToolsEnabled, uiCatalogRequested)
  lateinit var controller: UIViewController
  controller = ComposeUIViewController {
    App(
        launchMode = launchMode,
        developmentToolsEnabled = developmentToolsEnabled,
        onThemeModeChanged = { controller.overrideUserInterfaceStyle = it.userInterfaceStyle() },
    )
  }
  return controller
}

private fun ThemeMode.userInterfaceStyle(): UIUserInterfaceStyle =
    when (this) {
      ThemeMode.SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
      ThemeMode.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
      ThemeMode.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }
