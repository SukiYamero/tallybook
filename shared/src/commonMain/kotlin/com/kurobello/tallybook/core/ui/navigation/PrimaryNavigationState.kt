package com.kurobello.tallybook.core.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
internal data class PrimaryNavigationState(
    val selected: Destination = Home,
    val historyVisited: Boolean = false,
    val catalogOpen: Boolean = false,
    val searchVisited: Boolean = false,
    val profileVisited: Boolean = false,
) {
  init {
    require(selected in primaryDestinations)
    require(selected != History || historyVisited)
    require(selected != Search || searchVisited)
    require(selected != Profile || profileVisited)
  }

  val retainedKeys: List<Destination>
    get() = buildList {
      add(Home)
      if (historyVisited) add(History)
      if (searchVisited) add(Search)
      if (profileVisited) add(Profile)
      if (catalogOpen) add(UiCatalog)
    }

  val displayedKeys: List<Destination>
    get() = buildList {
      add(Home)
      if (selected != Home) add(selected)
      if (catalogOpen) add(UiCatalog)
    }

  fun select(destination: Destination): PrimaryNavigationState =
      when {
        destination !in primaryDestinations -> this
        destination == selected && !catalogOpen -> this
        else ->
            copy(
                selected = destination,
                historyVisited = historyVisited || destination == History,
                searchVisited = searchVisited || destination == Search,
                profileVisited = profileVisited || destination == Profile,
                catalogOpen = false,
            )
      }

  fun openCatalog(): PrimaryNavigationState = if (catalogOpen) this else copy(catalogOpen = true)

  fun back(): PrimaryNavigationState =
      when {
        catalogOpen -> copy(catalogOpen = false)
        selected != Home -> copy(selected = Home)
        else -> this
      }
}

private val primaryDestinations: Set<Destination> = setOf(Home, History, Search, Profile)

internal fun initialPrimaryNavigation(start: Destination): PrimaryNavigationState =
    when (start) {
      Home -> PrimaryNavigationState()
      History -> PrimaryNavigationState(History, historyVisited = true)
      Search -> PrimaryNavigationState(Search, searchVisited = true)
      Profile -> PrimaryNavigationState(Profile, profileVisited = true)
      UiCatalog -> PrimaryNavigationState(catalogOpen = true)
    }
