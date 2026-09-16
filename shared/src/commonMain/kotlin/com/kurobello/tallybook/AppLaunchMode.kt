package com.kurobello.tallybook

enum class AppLaunchMode {
  Standard,
  UiCatalog,
}

fun resolveAppLaunchMode(
    developmentToolsEnabled: Boolean,
    uiCatalogRequested: Boolean,
): AppLaunchMode =
    if (developmentToolsEnabled && uiCatalogRequested) {
      AppLaunchMode.UiCatalog
    } else {
      AppLaunchMode.Standard
    }
