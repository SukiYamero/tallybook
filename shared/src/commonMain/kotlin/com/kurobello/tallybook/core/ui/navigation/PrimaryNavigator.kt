package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kurobello.tallybook.feature.search.ui.SearchFilterRequest

/**
 * The shell's navigation intents, each refused while [modals] owns the screen. Search intents are
 * one-shot: the live Search route applies them and acknowledges their token, and any other
 * transition drops a pending one because it belonged to the navigation that issued it.
 */
@Stable
internal class PrimaryNavigator(
    private val navigation: MutableState<PrimaryNavigationState>,
    val modals: PrimaryModalCoordinator,
    catalogEnabled: Boolean,
) {
  private var nextToken = 0L

  var focusRequest by mutableStateOf<Long?>(null)
    private set

  var searchFilterRequest by mutableStateOf<SearchFilterRequest?>(null)
    private set

  val state: PrimaryNavigationState
    get() = navigation.value

  val openCatalog: (() -> Unit)? =
      if (catalogEnabled) ({ navigate { openSecondary(UiCatalog) } }) else null

  fun openCreation() {
    modals.tryOpenCreation()
  }

  fun select(destination: Destination) {
    if (!modals.canNavigate) return
    if (destination != Search) clearRequests()
    navigation.value = navigation.value.select(destination)
  }

  fun openSearch() {
    if (navigate { select(Search) }) focusRequest = ++nextToken
  }

  fun showSearchFilter(
      categoryIds: Set<String> = emptySet(),
      etiquetaIds: Set<String> = emptySet(),
  ) {
    if (navigate { select(Search) }) {
      searchFilterRequest = SearchFilterRequest(++nextToken, categoryIds, etiquetaIds)
    }
  }

  fun openSecondary(destination: Destination) {
    navigate { openSecondary(destination) }
  }

  fun back() {
    navigate { back() }
  }

  fun acknowledgeFocus(token: Long) {
    if (focusRequest == token) focusRequest = null
  }

  fun acknowledgeSearchFilter(token: Long) {
    if (searchFilterRequest?.token == token) searchFilterRequest = null
  }

  private fun navigate(transition: PrimaryNavigationState.() -> PrimaryNavigationState): Boolean {
    if (!modals.canNavigate) return false
    clearRequests()
    navigation.value = navigation.value.transition()
    return true
  }

  private fun clearRequests() {
    focusRequest = null
    searchFilterRequest = null
  }
}
