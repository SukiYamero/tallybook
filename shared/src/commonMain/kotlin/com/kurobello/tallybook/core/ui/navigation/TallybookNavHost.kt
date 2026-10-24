package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.ui.NavDisplay
import com.kurobello.tallybook.core.ui.catalog.UiCatalogRoute
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackRoute
import com.kurobello.tallybook.core.ui.layout.TallybookAppShell
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSessionLifecycle
import com.kurobello.tallybook.feature.accounts.ui.AccountsRoute
import com.kurobello.tallybook.feature.categories.ui.CategoriesRoute
import com.kurobello.tallybook.feature.history.ui.HistoryRoute
import com.kurobello.tallybook.feature.home.HomeRoute
import com.kurobello.tallybook.feature.movimientos.ui.AddMovimientoSheetRoute
import com.kurobello.tallybook.feature.movimientos.ui.ManageMovimientoRoute
import com.kurobello.tallybook.feature.movimientos.ui.rememberOpenMovimiento
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import com.kurobello.tallybook.feature.profile.ui.ProfileRoute
import com.kurobello.tallybook.feature.search.ui.SearchRoute
import com.kurobello.tallybook.feature.tags.ui.TagsRoute
import com.kurobello.tallybook.feature.transfers.ui.TransferSheetRoute
import com.kurobello.tallybook.feature.transfers.ui.recentTransferenciasContent
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferEntry
import kotlinx.serialization.KSerializer
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TallybookNavHost(
    startDestination: Destination = Home,
    developmentToolsEnabled: Boolean = false,
) {
  val navigation = rememberPrimaryNavigation(startDestination)
  val sheetSession = remember { AddMovimientoSheetSession() }
  AddMovimientoSheetSessionLifecycle(sheetSession)
  val modals =
      remember(sheetSession, navigation) {
        PrimaryModalCoordinator(sheetSession) {
          navigation.value.selected == Search && navigation.value.secondary == null
        }
      }
  val managementOwner = rememberManagementOwner()
  val feedback = managementFeedbackQueue(managementOwner)
  val feedbackState by feedback.state.collectAsStateWithLifecycle()
  val management = rememberManageMovimientoViewModel(modals, managementOwner, feedback)
  val managementState by management.uiState.collectAsStateWithLifecycle()
  val transfers = rememberManageTransferenciaViewModel(modals, managementOwner, feedback)
  val transfersState by transfers.uiState.collectAsStateWithLifecycle()
  val navigator =
      remember(navigation, modals, developmentToolsEnabled) {
        PrimaryNavigator(navigation, modals, developmentToolsEnabled)
      }
  val openManagement = rememberOpenMovimiento(modals)
  val openTransfer = rememberOpenTransfer(modals)
  val openers =
      remember(openManagement, openTransfer) { ManagementOpeners(openManagement, openTransfer) }
  PrimaryShell(
      state = navigation.value,
      onAdd = navigator::openCreation,
      onSelect = navigator::select,
      overlay = { clearance ->
        ManagementFeedbackRoute(feedbackState, feedback, modals, clearance)
      },
  ) { clearance ->
    PrimaryNavigationDisplay(navigator, clearance, openers)
  }
  AddMovimientoSessionRoute(sheetSession)
  ManageMovimientoRoute(managementState, management)
  TransferSheetRoute(transfersState, transfers)
}

/** How primary destinations reopen records in the shell's management sheets. */
private class ManagementOpeners(
    val movimiento: (String) -> Unit,
    val transfer: (TransferEntry) -> Unit,
)

@Composable
private fun PrimaryShell(
    state: PrimaryNavigationState,
    onAdd: () -> Unit,
    onSelect: (Destination) -> Unit,
    overlay: @Composable (Dp) -> Unit,
    content: @Composable (Dp) -> Unit,
) {
  TallybookAppShell(
      overlay = overlay,
      bottomBar =
          if (state.secondary != null) null
          else
              ({
                TallybookBottomNavigation(
                    onAddMovement = onAdd,
                    selectedDestination = state.selected,
                    onSelectDestination = onSelect,
                )
              }),
      content = content,
  )
}

@Composable
private fun PrimaryNavigationDisplay(
    navigator: PrimaryNavigator,
    clearance: Dp,
    openers: ManagementOpeners,
) {
  val state = navigator.state
  val entries =
      rememberPrimaryEntries(state) { destination ->
        NavEntry(destination, contentKey = primaryContentKey(destination)) {
          PrimaryRoute(destination, navigator, clearance, openers)
        }
      }
  NavDisplay(
      entries = state.displayedKeys.map { entries[state.retainedKeys.indexOf(it)] },
      onBack = navigator::back,
      transitionSpec = { navigationContentTransform() },
      popTransitionSpec = { navigationContentTransform() },
      predictivePopTransitionSpec = { navigationContentTransform() },
  )
}

@Composable
private fun PrimaryRoute(
    destination: Destination,
    navigator: PrimaryNavigator,
    clearance: Dp,
    openers: ManagementOpeners,
) {
  val state = navigator.state
  val openManagement = openers.movimiento
  when (destination) {
    Home ->
        HomeRoute(
            onAddMovimiento = navigator::openCreation,
            bottomBarClearance = clearance,
            onOpenCatalog = navigator.openCatalog,
            onSearch = navigator::openSearch,
            onOpenMovimiento = openManagement,
            onOpenAccounts = { navigator.openSecondary(Accounts) },
        )
    History ->
        HistoryRoute(
            navigator::openCreation,
            clearance,
            onOpenMovimiento = openManagement,
            onOpenTransferencia = { id -> openers.transfer(TransferEntry.Existing(id)) },
        )
    Search ->
        SearchRoute(
            onAdd = navigator::openCreation,
            onOpenMovimiento = openManagement,
            onOpenTransferencia = { id -> openers.transfer(TransferEntry.Existing(id)) },
            bottomBarClearance = clearance,
            modals = navigator.modals,
            focusRequest = navigator.focusRequest,
            onFocusAcknowledged = navigator::acknowledgeFocus,
            searchFilterRequest = navigator.searchFilterRequest,
            onSearchFilterRequestHandled = navigator::acknowledgeSearchFilter,
            active = state.selected == Search && state.secondary == null,
        )
    Profile ->
        ProfileRoute(
            bottomBarClearance = clearance,
            modals = navigator.modals,
            active = state.selected == Profile && state.secondary == null,
            onOpenSecondary = navigator::openSecondary,
        )
    UiCatalog -> UiCatalogRoute()
    Categories ->
        CategoriesRoute(
            modals = navigator.modals,
            active = state.secondary == Categories,
            onBack = navigator::back,
            onShowCategory = { id -> navigator.showSearchFilter(categoryIds = setOf(id)) },
        )
    Tags ->
        TagsRoute(
            modals = navigator.modals,
            active = state.secondary == Tags,
            onBack = navigator::back,
            onShowTag = { id -> navigator.showSearchFilter(etiquetaIds = setOf(id)) },
        )
    Accounts -> AccountsDestination(navigator, openers.transfer)
  }
}

@Composable
private fun AccountsDestination(
    navigator: PrimaryNavigator,
    openTransfer: (TransferEntry) -> Unit,
) {
  AccountsRoute(
      modals = navigator.modals,
      active = navigator.state.secondary == Accounts,
      onBack = navigator::back,
      onTransfer = { origenId, destinoId, prefill ->
        openTransfer(TransferEntry.New(origenId, destinoId, prefill))
      },
      onViewMovements = { cuentaId, moneda ->
        navigator.showSearchFilter(cuentaIds = setOf(cuentaId), moneda = moneda)
      },
      trailingContent =
          recentTransferenciasContent { id -> openTransfer(TransferEntry.Existing(id)) },
  )
}

@Composable
private fun AddMovimientoSessionRoute(sheetSession: AddMovimientoSheetSession) {
  if (sheetSession.hasBeenOpened) {
    val origin = sheetSession.token
    key(origin) {
      CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
        val viewModel: AddMovimientoViewModel =
            koinViewModel(
                key = sheetSession.viewModelKey,
                viewModelStoreOwner = sheetSession.viewModelStoreOwner,
            )
        AddMovimientoSheetRoute(
            visible = sheetSession.isVisible,
            onDismiss = { if (sheetSession.token == origin) sheetSession.dismiss() },
            onHidden = { sheetSession.onSheetHidden(origin) },
            viewModel = viewModel,
        )
      }
    }
  }
}

internal fun primaryContentKey(destination: Destination): String =
    when (destination) {
      Home -> "primary-home"
      History -> "primary-history"
      Search -> "primary-search"
      Profile -> "primary-profile"
      UiCatalog -> "ui-catalog"
      Categories -> "secondary-categories"
      Tags -> "secondary-tags"
      Accounts -> "secondary-accounts"
    }

internal fun destinationBackStackSerializer(): KSerializer<NavBackStack<Destination>> =
    NavBackStackSerializer(Destination.serializer())

internal fun navigationContentTransform(): ContentTransform =
    fadeIn(
        animationSpec =
            tween(
                durationMillis = NAVIGATION_FADE_IN_DURATION_MILLIS,
                delayMillis = NAVIGATION_FADE_OUT_DURATION_MILLIS,
            )
    ) togetherWith
        fadeOut(animationSpec = tween(durationMillis = NAVIGATION_FADE_OUT_DURATION_MILLIS))

private const val NAVIGATION_FADE_OUT_DURATION_MILLIS = 90
private const val NAVIGATION_FADE_IN_DURATION_MILLIS = 220
