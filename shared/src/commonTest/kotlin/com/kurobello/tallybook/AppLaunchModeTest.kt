package com.kurobello.tallybook

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLaunchModeTest {

  @Test
  fun launchModeMatrixRequiresBothDevelopmentAndRequest() {
    val cases =
        listOf(
            Triple(false, false, AppLaunchMode.Standard),
            Triple(false, true, AppLaunchMode.Standard),
            Triple(true, false, AppLaunchMode.Standard),
            Triple(true, true, AppLaunchMode.UiCatalog),
        )

    cases.forEach { (development, requested, expected) ->
      assertEquals(expected, resolveAppLaunchMode(development, requested))
    }
  }

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
