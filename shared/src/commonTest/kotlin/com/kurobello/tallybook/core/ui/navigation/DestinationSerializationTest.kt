package com.kurobello.tallybook.core.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

// Leaf serializers do not prove that navigation can persist a mixed route stack as one value.
@OptIn(ExperimentalSerializationApi::class)
class DestinationSerializationTest {

  @Test
  fun everyDestinationExposesAGeneratedSerializer() {
    val serialNames =
        listOf(Placeholder1.serializer(), Placeholder2.serializer()).map {
          it.descriptor.serialName
        }

    assertEquals(
        listOf(
            "com.kurobello.tallybook.core.ui.navigation.Placeholder1",
            "com.kurobello.tallybook.core.ui.navigation.Placeholder2",
        ),
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
    val original = NavBackStack<Destination>(Placeholder1, Placeholder2)

    val encoded = Json.encodeToString(destinationBackStackSerializer(), original)
    val restored = Json.decodeFromString(destinationBackStackSerializer(), encoded)

    assertEquals(listOf(Placeholder1, Placeholder2), restored.toList())
  }
}
