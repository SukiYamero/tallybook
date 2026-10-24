package com.kurobello.tallybook.core.ui.navigation

import kotlinx.serialization.Serializable

/**
 * [secondary] is the one surface pushed over [selected] with the bottom bar hidden; Back closes it
 * before any primary rule applies.
 */
@Serializable
internal data class PrimaryNavigationState(
    val selected: Destination = Home,
    val historyVisited: Boolean = false,
    val secondary: Destination? = null,
    val searchVisited: Boolean = false,
    val profileVisited: Boolean = false,
) {
  init {
    require(selected in primaryDestinations)
    require(selected != History || historyVisited)
    require(selected != Search || searchVisited)
    require(selected != Profile || profileVisited)
    require(secondary == null || secondary.opensOver(selected))
  }

  val retainedKeys: List<Destination>
    get() = buildList {
      add(Home)
      if (historyVisited) add(History)
      if (searchVisited) add(Search)
      if (profileVisited) add(Profile)
      secondary?.let(::add)
    }

  val displayedKeys: List<Destination>
    get() = buildList {
      add(Home)
      if (selected != Home) add(selected)
      secondary?.let(::add)
    }

  fun select(destination: Destination): PrimaryNavigationState =
      when {
        destination !in primaryDestinations -> this
        destination == selected && secondary == null -> this
        else ->
            copy(
                selected = destination,
                historyVisited = historyVisited || destination == History,
                searchVisited = searchVisited || destination == Search,
                profileVisited = profileVisited || destination == Profile,
                secondary = null,
            )
      }

  fun openSecondary(destination: Destination): PrimaryNavigationState =
      if (secondary == null && destination.opensOver(selected)) copy(secondary = destination)
      else this

  fun back(): PrimaryNavigationState =
      when {
        secondary != null -> copy(secondary = null)
        selected != Home -> copy(selected = Home)
        else -> this
      }
}

private val primaryDestinations: Set<Destination> = setOf(Home, History, Search, Profile)

private fun Destination.opensOver(primary: Destination): Boolean =
    when (this) {
      UiCatalog -> true
      Categories,
      Tags -> primary == Profile
      Accounts -> primary == Home || primary == Profile
      Home,
      History,
      Search,
      Profile -> false
    }

internal fun initialPrimaryNavigation(start: Destination): PrimaryNavigationState =
    when (start) {
      Home -> PrimaryNavigationState()
      History -> PrimaryNavigationState(History, historyVisited = true)
      Search -> PrimaryNavigationState(Search, searchVisited = true)
      Profile -> PrimaryNavigationState(Profile, profileVisited = true)
      UiCatalog -> PrimaryNavigationState(secondary = UiCatalog)
      Categories,
      Tags,
      Accounts -> PrimaryNavigationState(Profile, profileVisited = true, secondary = start)
    }
