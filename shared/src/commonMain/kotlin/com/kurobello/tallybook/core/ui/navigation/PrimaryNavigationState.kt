package com.kurobello.tallybook.core.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
internal data class PrimaryNavigationState(
    val selected: Destination = Home,
    val historyVisited: Boolean = false,
    val catalogOpen: Boolean = false,
    val searchVisited: Boolean = false,
) {
  init {
    require(selected == Home || selected == History || selected == Search)
    require(selected != History || historyVisited)
    require(selected != Search || searchVisited)
  }

  val retainedKeys: List<Destination>
    get() = buildList {
      add(Home)
      if (historyVisited) add(History)
      if (searchVisited) add(Search)
      if (catalogOpen) add(UiCatalog)
    }

  val displayedKeys: List<Destination>
    get() = buildList {
      add(Home)
      if (selected == History) add(History)
      if (selected == Search) add(Search)
      if (catalogOpen) add(UiCatalog)
    }

  fun select(destination: Destination): PrimaryNavigationState =
      when {
        destination != Home && destination != History && destination != Search -> this
        destination == selected && !catalogOpen -> this
        else ->
            copy(
                selected = destination,
                historyVisited = historyVisited || destination == History,
                searchVisited = searchVisited || destination == Search,
                catalogOpen = false,
            )
      }

  fun openCatalog(): PrimaryNavigationState = if (catalogOpen) this else copy(catalogOpen = true)

  fun back(): PrimaryNavigationState =
      when {
        catalogOpen -> copy(catalogOpen = false)
        selected == History || selected == Search -> copy(selected = Home)
        else -> this
      }
}

internal fun initialPrimaryNavigation(start: Destination): PrimaryNavigationState =
    when (start) {
      Home -> PrimaryNavigationState()
      History -> PrimaryNavigationState(History, historyVisited = true)
      Search -> PrimaryNavigationState(Search, searchVisited = true)
      UiCatalog -> PrimaryNavigationState(catalogOpen = true)
    }
