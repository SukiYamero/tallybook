package com.kurobello.tallybook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurobello.tallybook.core.ui.navigation.Home
import com.kurobello.tallybook.core.ui.navigation.TallybookNavHost
import com.kurobello.tallybook.core.ui.navigation.UiCatalog
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.retry
import tallybook.shared.generated.resources.startup_error_body
import tallybook.shared.generated.resources.startup_error_title

@Composable
fun App(launchMode: AppLaunchMode = AppLaunchMode.Standard) {
  TallybookTheme {
    val viewModel: AppStartupViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AppStartupContent(uiState = uiState, launchMode = launchMode, onRetry = viewModel::onRetry)
  }
}

@Composable
private fun AppStartupContent(
    uiState: AppStartupUiState,
    launchMode: AppLaunchMode,
    onRetry: () -> Unit,
) {
  when (uiState) {
    AppStartupUiState.Initializing -> StartupLoading()
    AppStartupUiState.Ready ->
        TallybookNavHost(
            startDestination =
                if (launchMode == AppLaunchMode.UiCatalog) {
                  UiCatalog
                } else {
                  Home
                }
        )
    AppStartupUiState.Error -> StartupError(onRetry)
  }
}

@Composable
private fun StartupLoading() {
  Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    CircularProgressIndicator()
  }
}

@Composable
private fun StartupError(onRetry: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = stringResource(Res.string.startup_error_title),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(text = stringResource(Res.string.startup_error_body))
    Button(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
  }
}
