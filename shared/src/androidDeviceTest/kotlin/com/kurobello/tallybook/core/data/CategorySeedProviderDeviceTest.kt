package com.kurobello.tallybook.core.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalResourceApi::class)
class CategorySeedProviderDeviceTest {

  @Test
  fun seedProviderResolvesEnglishNamesFromBundledResources() = runBlocking {
    setResourceReaderAndroidContext(InstrumentationRegistry.getInstrumentation().targetContext)

    val categories = CategorySeedProvider().getCategories()

    assertEquals("Food", categories.first { it.id == "cat_comida" }.nombre)
    assertEquals("Refund", categories.first { it.id == "cat_reembolso" }.nombre)
  }
}
