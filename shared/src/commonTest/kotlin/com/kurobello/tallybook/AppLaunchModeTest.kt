package com.kurobello.tallybook

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLaunchModeTest {

  @Test
  fun releaseModeIgnoresCatalogRequest() {
    assertEquals(
        AppLaunchMode.Standard,
        resolveAppLaunchMode(developmentToolsEnabled = false, uiCatalogRequested = true),
    )
  }

  @Test
  fun developmentModeHonorsCatalogRequest() {
    assertEquals(
        AppLaunchMode.UiCatalog,
        resolveAppLaunchMode(developmentToolsEnabled = true, uiCatalogRequested = true),
    )
  }
}
