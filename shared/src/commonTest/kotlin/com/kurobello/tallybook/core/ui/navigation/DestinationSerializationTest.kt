package com.kurobello.tallybook.core.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

// Nav3 restores the back stack by serializing each NavKey, so a destination without a working
// generated serializer only fails at process death — far from this change.
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
}
