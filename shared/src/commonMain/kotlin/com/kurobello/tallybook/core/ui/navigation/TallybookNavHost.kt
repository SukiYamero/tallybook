package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.ui.NavDisplay
import com.kurobello.tallybook.core.ui.catalog.UiCatalogRoute
import com.kurobello.tallybook.core.ui.layout.TallybookAppShell
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSession
import com.kurobello.tallybook.core.ui.movimiento.AddMovimientoSheetSessionLifecycle
import com.kurobello.tallybook.feature.history.ui.HistoryRoute
import com.kurobello.tallybook.feature.home.HomeRoute
import com.kurobello.tallybook.feature.movimientos.ui.AddMovimientoSheetRoute
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModel
import com.kurobello.tallybook.feature.search.ui.SearchRoute
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
          navigation.value.selected == Search && !navigation.value.catalogOpen
        }
      }
  var nextFocusToken by remember { mutableLongStateOf(0L) }
  var focusToken by remember { mutableStateOf<Long?>(null) }
  val openSearch: () -> Unit = {
    if (modals.canNavigate) {
      navigation.value = navigation.value.select(Search)
      focusToken = ++nextFocusToken
    }
  }
  val openCreation: () -> Unit = { modals.tryOpenCreation() }
  val select: (Destination) -> Unit = { destination ->
    if (modals.canNavigate) {
      if (destination != Search) focusToken = null
      navigation.value = navigation.value.select(destination)
    }
  }
  val openCatalog: (() -> Unit)? =
      if (developmentToolsEnabled) {
        {
          if (modals.canNavigate) {
            focusToken = null
            navigation.value = navigation.value.openCatalog()
          }
        }
      } else null
  TallybookAppShell(
      bottomBar =
          if (navigation.value.catalogOpen) null
          else
              ({
                TallybookBottomNavigation(
                    onAddMovement = openCreation,
                    selectedDestination = navigation.value.selected,
                    onSelectDestination = select,
                )
              }),
  ) { clearance ->
    PrimaryNavigationDisplay(
        navigation,
        clearance,
        openCatalog,
        modals,
        openCreation,
        openSearch,
        focusToken,
        { token -> if (focusToken == token) focusToken = null },
    )
  }
  AddMovimientoSessionRoute(sheetSession)
}

@Suppress("LongParameterList")
@Composable
private fun PrimaryNavigationDisplay(
    navigation: MutableState<PrimaryNavigationState>,
    clearance: Dp,
    openCatalog: (() -> Unit)?,
    modals: PrimaryModalCoordinator,
    openCreation: () -> Unit,
    openSearch: () -> Unit,
    focusToken: Long?,
    acknowledgeFocus: (Long) -> Unit,
) {
  val state = navigation.value
  val entries =
      rememberPrimaryEntries(state) { destination ->
        NavEntry(destination, contentKey = primaryContentKey(destination)) {
          when (destination) {
            Home ->
                HomeRoute(
                    onAddMovimiento = openCreation,
                    bottomBarClearance = clearance,
                    onOpenCatalog = openCatalog,
                    onSearch = openSearch,
                )
            History -> HistoryRoute(openCreation, clearance)
            Search ->
                SearchRoute(
                    onAdd = openCreation,
                    bottomBarClearance = clearance,
                    modals = modals,
                    focusRequest = focusToken,
                    onFocusAcknowledged = acknowledgeFocus,
                    active = state.selected == Search && !state.catalogOpen,
                )
            UiCatalog -> UiCatalogRoute()
          }
        }
      }
  NavDisplay(
      entries = state.displayedKeys.map { entries[state.retainedKeys.indexOf(it)] },
      onBack = {
        if (modals.canNavigate) {
          acknowledgeFocus(focusToken ?: -1L)
          navigation.value = navigation.value.back()
        }
      },
      transitionSpec = { navigationContentTransform() },
      popTransitionSpec = { navigationContentTransform() },
      predictivePopTransitionSpec = { navigationContentTransform() },
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
      UiCatalog -> "ui-catalog"
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
