package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.kurobello.tallybook.feature.home.HomeRoute
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

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
            entry<Home> { HomeRoute(onNavigateNext = { backStack.add(Placeholder2) }) }
            entry<Placeholder2> {
              PlaceholderContent(
                  label = "Placeholder 2",
                  actionLabel = "Back",
                  onAction = { backStack.removeLastOrNull() },
              )
            }
          },
  )
}

internal fun destinationBackStackSerializer(): KSerializer<NavBackStack<Destination>> = serializer()

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

@Composable
private fun PlaceholderContent(label: String, actionLabel: String, onAction: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(label)
    Button(onClick = onAction) { Text(actionLabel) }
  }
}
