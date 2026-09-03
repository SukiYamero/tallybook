package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration

@Composable
fun TallybookNavHost(startDestination: Destination = Placeholder1) {
  // The vararg-only rememberNavBackStack overload is androidMain-only; commonMain has to pass the
  // configuration explicitly.
  val backStack = rememberNavBackStack(SavedStateConfiguration.DEFAULT, startDestination)
  NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider =
          entryProvider {
            entry<Placeholder1> {
              PlaceholderContent(
                  label = "Placeholder 1",
                  actionLabel = "Go to Placeholder 2",
                  onAction = { backStack.add(Placeholder2) },
              )
            }
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
