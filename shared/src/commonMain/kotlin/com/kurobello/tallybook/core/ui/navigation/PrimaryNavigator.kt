package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.SearchTipo
import com.kurobello.tallybook.core.model.StatisticsKey
import com.kurobello.tallybook.feature.search.ui.SearchFilterRequest

/**
 * The shell's navigation intents, each refused while [modals] owns the screen. Search intents are
 * one-shot: the live Search route applies them and acknowledges their token, and any other
 * transition drops a pending one because it belonged to the navigation that issued it.
 */
@Stable
@Suppress("TooManyFunctions")
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

  /** [moneda] switches Search's currency first, e.g. to an account's own one. */
  @Suppress("LongParameterList")
  fun showSearchFilter(
      categoryIds: Set<String> = emptySet(),
      etiquetaIds: Set<String> = emptySet(),
      cuentaIds: Set<String> = emptySet(),
      moneda: Moneda? = null,
      range: MovimientoDateRange? = null,
      tipo: SearchTipo? = null,
  ) {
    if (navigate { select(Search) }) {
      searchFilterRequest =
          SearchFilterRequest(
              ++nextToken,
              categoryIds,
              etiquetaIds,
              cuentaIds,
              moneda,
              range,
              tipo,
          )
    }
  }

  fun openSecondary(destination: Destination) {
    navigate { openSecondary(destination) }
  }

  fun showSecondary(destination: Destination) {
    navigate { showSecondary(destination) }
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

/** Search over the gastos of one Statistics group in [moneda] and [range]. */
internal fun PrimaryNavigator.showStatisticsFilter(
    key: StatisticsKey,
    moneda: Moneda,
    range: MovimientoDateRange,
) {
  val ids = statisticsFilterIds(key) ?: return
  showSearchFilter(
      categoryIds = ids.categoryIds,
      etiquetaIds = ids.etiquetaIds,
      cuentaIds = ids.cuentaIds,
      moneda = moneda,
      range = range,
      tipo = SearchTipo.GASTO,
  )
}

private class StatisticsFilterIds(
    val categoryIds: Set<String> = emptySet(),
    val etiquetaIds: Set<String> = emptySet(),
    val cuentaIds: Set<String> = emptySet(),
)

/** A Search filter request carries no medio, so a medio opens nothing. */
private fun statisticsFilterIds(key: StatisticsKey): StatisticsFilterIds? =
    when (key) {
      is StatisticsKey.Categoria -> StatisticsFilterIds(categoryIds = setOf(key.id))
      is StatisticsKey.Etiqueta -> StatisticsFilterIds(etiquetaIds = setOf(key.id))
      is StatisticsKey.Cuenta -> StatisticsFilterIds(cuentaIds = setOf(key.id))
      is StatisticsKey.Medio -> null
    }
