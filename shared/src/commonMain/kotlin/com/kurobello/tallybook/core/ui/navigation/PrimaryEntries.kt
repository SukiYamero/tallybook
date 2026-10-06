package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

@Composable
internal fun rememberPrimaryNavigation(start: Destination): MutableState<PrimaryNavigationState> =
    rememberSerializable(stateSerializer = PrimaryNavigationState.serializer()) {
      mutableStateOf(initialPrimaryNavigation(start))
    }

@Composable
internal fun rememberPrimaryEntries(
    navigation: PrimaryNavigationState,
    entryProvider: (Destination) -> NavEntry<Destination>,
): List<NavEntry<Destination>> {
  val parent = requireNotNull(LocalViewModelStoreOwner.current)
  // Activity Intent extras contain copied Bundles, so retain extras for this owner's lifetime.
  val extras = remember(parent) { parent.defaultViewModelCreationExtras }
  val provider = rememberViewModelStoreProvider(parent = parent, defaultCreationExtras = extras)
  val viewModelDecorator =
      remember(provider) { ViewModelStoreNavEntryDecorator<Destination>(provider) }
  return rememberDecoratedNavEntries(
      backStack = navigation.retainedKeys,
      entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), viewModelDecorator),
      entryProvider = entryProvider,
  )
}
