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
    val serialNames =
        listOf(
                Home.serializer(),
                History.serializer(),
                Search.serializer(),
                Statistics.serializer(),
                UiCatalog.serializer(),
                Categories.serializer(),
                Tags.serializer(),
            )
            .map { it.descriptor.serialName }

    assertEquals(
        listOf(
            "com.kurobello.tallybook.core.ui.navigation.Home",
            "com.kurobello.tallybook.core.ui.navigation.History",
            "com.kurobello.tallybook.core.ui.navigation.Search",
            "com.kurobello.tallybook.core.ui.navigation.Statistics",
            "com.kurobello.tallybook.core.ui.navigation.UiCatalog",
            "com.kurobello.tallybook.core.ui.navigation.Categories",
            "com.kurobello.tallybook.core.ui.navigation.Tags",
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
    val original =
        NavBackStack<Destination>(Home, History, Search, Statistics, Profile, Categories, Tags)

    val encoded = Json.encodeToString(destinationBackStackSerializer(), original)
    val restored = Json.decodeFromString(destinationBackStackSerializer(), encoded)

    assertEquals(
        listOf(Home, History, Search, Statistics, Profile, Categories, Tags),
        restored.toList(),
    )
  }
}
