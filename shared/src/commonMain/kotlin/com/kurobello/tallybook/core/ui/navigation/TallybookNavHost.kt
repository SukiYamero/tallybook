package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.ui.NavDisplay
import com.kurobello.tallybook.core.ui.catalog.UiCatalogRoute
import com.kurobello.tallybook.feature.home.HomeRoute
import kotlinx.serialization.KSerializer

@Composable
fun TallybookNavHost(startDestination: Destination = Home) {
  val backStack =
      rememberSerializable(serializer = destinationBackStackSerializer()) {
        NavBackStack(startDestination)
      }
  NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      transitionSpec = { navigationContentTransform() },
      popTransitionSpec = { navigationContentTransform() },
      predictivePopTransitionSpec = { navigationContentTransform() },
      entryProvider =
          entryProvider {
            entry<Home> { HomeRoute() }
            entry<UiCatalog> { UiCatalogRoute() }
          },
  )
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
