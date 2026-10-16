package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kurobello.tallybook.feature.search.ui.SearchCategoryRequest

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

  var categoryRequest by mutableStateOf<SearchCategoryRequest?>(null)
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

  fun showCategory(id: String) {
    if (navigate { select(Search) }) categoryRequest = SearchCategoryRequest(++nextToken, id)
  }

  fun openCategories() {
    navigate { openSecondary(Categories) }
  }

  fun back() {
    navigate { back() }
  }

  fun acknowledgeFocus(token: Long) {
    if (focusRequest == token) focusRequest = null
  }

  fun acknowledgeCategory(token: Long) {
    if (categoryRequest?.token == token) categoryRequest = null
  }

  private fun navigate(transition: PrimaryNavigationState.() -> PrimaryNavigationState): Boolean {
    if (!modals.canNavigate) return false
    clearRequests()
    navigation.value = navigation.value.transition()
    return true
  }

  private fun clearRequests() {
    focusRequest = null
    categoryRequest = null
  }
}
