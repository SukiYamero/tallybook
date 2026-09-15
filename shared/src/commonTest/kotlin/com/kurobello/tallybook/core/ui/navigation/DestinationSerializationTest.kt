package com.kurobello.tallybook.core.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

// A leaf serializer alone does not prove Navigation 3 can restore a persisted back stack.
@OptIn(ExperimentalSerializationApi::class)
class DestinationSerializationTest {

  @Test
  fun everyDestinationExposesAGeneratedSerializer() {
    val serialNames = listOf(Home.serializer()).map { it.descriptor.serialName }

    assertEquals(
        listOf("com.kurobello.tallybook.core.ui.navigation.Home"),
        serialNames,
    )
  }

  @Test
  fun destinationResolvesPolymorphicallyFromTheSealedParent() {
    assertEquals(
        "com.kurobello.tallybook.core.ui.navigation.Destination",
        serializer<Destination>().descriptor.serialName,
    )
  }

  @Test
  fun destinationBackStackSurvivesSerializationRoundTrip() {
    val original = NavBackStack<Destination>(Home)

    val encoded = Json.encodeToString(destinationBackStackSerializer(), original)
    val restored = Json.decodeFromString(destinationBackStackSerializer(), encoded)

    assertEquals(listOf(Home), restored.toList())
  }
}
