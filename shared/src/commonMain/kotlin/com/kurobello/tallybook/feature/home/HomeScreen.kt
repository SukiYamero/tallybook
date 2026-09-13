package com.kurobello.tallybook.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Lucide
import com.composeunstyled.DialogPanel
import com.composeunstyled.Scrim
import com.composeunstyled.UnstyledDialog
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeRoute(
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val currentOnNavigateNext by rememberUpdatedState(onNavigateNext)
  LaunchedEffect(viewModel, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.effects.collect { effect ->
        when (effect) {
          HomeEffect.NavigateNext -> currentOnNavigateNext()
        }
      }
    }
  }

  HomeScreen(uiState = uiState, actions = viewModel, modifier = modifier)
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
  var dialogVisible by rememberSaveable { mutableStateOf(false) }

  Box(
      modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
      Icon(
          imageVector = Lucide.BookOpen,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(12.dp))
      Text(text = uiState.message, style = MaterialTheme.typography.headlineMedium)
      Spacer(Modifier.height(24.dp))
      Button(onClick = { dialogVisible = true }) { Text("Probar diálogo") }
      TextButton(onClick = actions::onNavigateNext) { Text("Ir al segundo destino") }
    }
  }

  UnstyledDialog(
      visible = dialogVisible,
      onDismissRequest = { dialogVisible = false },
      overlay = { Scrim() },
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      DialogPanel(
          modifier =
              Modifier.widthIn(min = 280.dp, max = 480.dp)
                  .background(
                      color = MaterialTheme.colorScheme.surface,
                      shape = RoundedCornerShape(24.dp),
                  )
                  .padding(24.dp),
          paneTitle = "Validación de componentes",
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "Compose Unstyled activo",
              style = MaterialTheme.typography.titleLarge,
          )
          Spacer(Modifier.height(16.dp))
          Button(onClick = { dialogVisible = false }) { Text("Cerrar") }
        }
      }
    }
  }
}

@Preview
@Composable
internal fun HomeScreenPreview() {
  TallybookTheme {
    HomeScreen(
        uiState = HomeUiState(),
        actions = PreviewHomeActions,
    )
  }
}

private object PreviewHomeActions : HomeActions {
  override fun onNavigateNext() = Unit
}
