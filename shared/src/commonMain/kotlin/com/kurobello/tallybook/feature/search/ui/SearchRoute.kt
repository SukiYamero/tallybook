package com.kurobello.tallybook.feature.search.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.ui.navigation.DestinationModalController
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.feature.search.viewmodel.SearchActions
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterSession
import com.kurobello.tallybook.feature.search.viewmodel.SearchFilterSessionId
import com.kurobello.tallybook.feature.search.viewmodel.SearchUiState
import com.kurobello.tallybook.feature.search.viewmodel.SearchViewModel
import com.kurobello.tallybook.feature.search.viewmodel.SearchWork
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.unknown_category

@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun SearchRoute(
    onAdd: () -> Unit,
    bottomBarClearance: Dp,
    modals: PrimaryModalCoordinator,
    focusRequest: Long?,
    onFocusAcknowledged: (Long) -> Unit,
    searchFilterRequest: SearchFilterRequest?,
    onSearchFilterRequestHandled: (Long) -> Unit,
    active: Boolean,
    modifier: Modifier = Modifier,
    onOpenMovimiento: ((String) -> Unit)? = null,
) {
  val label = stringResource(Res.string.unknown_category)
  val locale = LocalLocale.current.toLanguageTag()
  val viewModel: SearchViewModel = koinViewModel { parametersOf(label) }
  val state = rememberSearchRouteState(viewModel, label, locale)
  val list = rememberLazyListState()
  val fieldFocus = remember { FocusRequester() }
  val filterFocus = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val keyboard = LocalSoftwareKeyboardController.current
  val latestActive = rememberUpdatedState(active)
  var returnFilterFocus by remember { mutableStateOf<SearchFilterSessionId?>(null) }
  val controller = remember(viewModel, state) { SearchFilterController(viewModel, state) }
  DisposableEffect(modals, controller, active) {
    if (active) modals.attach(controller)
    onDispose { modals.detach(controller) }
  }
  val actions =
      remember(viewModel, filterFocus) {
        object : SearchActions by viewModel {
          override fun onFiltersHidden(id: SearchFilterSessionId) {
            val closing = viewModel.uiState.value.filterSession as? SearchFilterSession.Closing
            val matches = closing?.sessionId == id
            viewModel.onFiltersHidden(id)
            if (matches && latestActive.value) returnFilterFocus = id
          }
        }
      }
  var scrollCheckpoint by
      rememberSerializable(stateSerializer = SearchScrollCheckpoint.serializer()) {
        mutableStateOf(SearchScrollCheckpoint())
      }
  val currentScrollRequest =
      state.value.takeIf { it.isCurrent }?.displayResults?.request?.scrollIdentity()
  LaunchedEffect(currentScrollRequest) {
    if (currentScrollRequest != null) {
      val previous = scrollCheckpoint.request
      scrollCheckpoint = SearchScrollCheckpoint(currentScrollRequest)
      if (previous != null && previous != currentScrollRequest) list.scrollToItem(0)
    }
  }
  SearchEntryFocus(focusRequest, active, list, fieldFocus, onFocusAcknowledged)
  SearchFilterRequestEffect(searchFilterRequest, active, viewModel, onSearchFilterRequestHandled)
  LaunchedEffect(returnFilterFocus, active, state.value.filterSession) {
    if (!active) returnFilterFocus = null
    val origin = returnFilterFocus
    if (origin != null && active && state.value.filterSession == SearchFilterSession.Hidden) {
      withFrameNanos {}
      if (modals.canNavigate && filterFocus.requestFocus()) returnFilterFocus = null
    }
  }
  SearchScreen(
      uiState = state.value,
      actions = actions,
      onAdd = onAdd,
      onOpenMovimiento = onOpenMovimiento,
      onOpenFilters = {
        if (modals.tryOpenDestinationModal(controller)) {
          keyboard?.hide()
          focusManager.clearFocus()
        }
      },
      bottomBarClearance = bottomBarClearance,
      modifier = modifier,
      contentListState = list,
      fieldModifier = Modifier.focusRequester(fieldFocus),
      filterButtonModifier = Modifier.focusRequester(filterFocus),
  )
  SearchFilterSheet(state.value, actions)
}

@Composable
private fun SearchEntryFocus(
    focusRequest: Long?,
    active: Boolean,
    list: LazyListState,
    fieldFocus: FocusRequester,
    onFocusAcknowledged: (Long) -> Unit,
) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val keyboard = LocalSoftwareKeyboardController.current
  val latestActive = rememberUpdatedState(active)
  val latestAcknowledgment = rememberUpdatedState(onFocusAcknowledged)
  LaunchedEffect(focusRequest, active, lifecycle) {
    if (focusRequest != null && active) {
      lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        list.scrollToItem(0)
        snapshotFlow { list.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
        withFrameNanos {}
        if (latestActive.value && fieldFocus.requestFocus()) {
          keyboard?.show()
          latestAcknowledgment.value(focusRequest)
        }
      }
    }
  }
}

@Composable
private fun SearchFilterRequestEffect(
    request: SearchFilterRequest?,
    active: Boolean,
    actions: SearchActions,
    onHandled: (Long) -> Unit,
) {
  val latestHandled = rememberUpdatedState(onHandled)
  LaunchedEffect(request, active, actions) {
    if (request != null && active) {
      actions.onShowFilters(request.categoryIds, request.etiquetaIds)
      latestHandled.value(request.token)
    }
  }
}

@Composable
internal fun rememberSearchRouteState(
    viewModel: SearchViewModel,
    label: String,
    localeTag: String,
): State<SearchUiState> {
  val owner = LocalLifecycleOwner.current
  return key(viewModel, owner, label, localeTag) {
    produceState(initialValue = viewModel.uiState.value.routeRevalidating()) {
      owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        try {
          viewModel.onPresentationChanged(label, localeTag)
          viewModel.onResume()
          viewModel.uiState.collect { value = it }
        } finally {
          value = value.routeRevalidating()
        }
      }
    }
  }
}

private fun SearchUiState.routeRevalidating(): SearchUiState =
    copy(results = results.refreshing(), work = SearchWork.Revalidating)

internal class SearchFilterController(
    private val viewModel: SearchViewModel,
    private val state: State<SearchUiState>,
) : DestinationModalController {
  // The collected State mirrors the view model's session and is read first so shell readers
  // always subscribe to it; the view model still answers immediately for admission guards.
  override val hasActiveSession: Boolean
    get() =
        state.value.filterSession != SearchFilterSession.Hidden || viewModel.hasActiveFilterSession

  override fun open(): Boolean = viewModel.onOpenFilters() != null
}

@Serializable private data class SearchScrollCheckpoint(val request: SearchScrollIdentity? = null)

@Serializable
private data class SearchScrollIdentity(
    val moneda: String,
    val start: String?,
    val end: String?,
    val tipo: String?,
    val categoryIds: List<String>,
    val etiquetaIds: List<String> = emptyList(),
    val normalizedQuery: String,
)

private fun SearchRequest.scrollIdentity(): SearchScrollIdentity =
    SearchScrollIdentity(
        criteria.moneda.name,
        criteria.range?.start?.toString(),
        criteria.range?.end?.toString(),
        criteria.tipo?.name,
        criteria.categoryIds.sorted(),
        criteria.etiquetaIds.sorted(),
        normalizedQuery,
    )
