package com.kurobello.tallybook.feature.recurrentes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackQueue
import com.kurobello.tallybook.core.ui.navigation.DestinationModalController
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesActions
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesEffect
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesUiState
import com.kurobello.tallybook.feature.recurrentes.viewmodel.RecurrentesViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Feedback goes to the shell's [feedback] queue, which owns Undo beyond this screen's lifetime. */
@Composable
internal fun RecurrentesRoute(
    modals: PrimaryModalCoordinator,
    active: Boolean,
    onBack: () -> Unit,
    feedback: ManagementFeedbackQueue,
    modifier: Modifier = Modifier,
) {
  val viewModel: RecurrentesViewModel = koinViewModel()
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val uiState by state
  val controller = remember(viewModel, state) { RecurrentesSheetController(viewModel, state) }
  DisposableEffect(modals, controller, active) {
    if (active) modals.attach(controller)
    onDispose { modals.detach(controller) }
  }
  LifecycleResumeEffect(viewModel) {
    viewModel.onResume()
    onPauseOrDispose {}
  }
  val actions =
      remember(viewModel, modals, controller) {
        object : RecurrentesActions by viewModel {
          override fun onOpenOccurrence(recurrenciaId: String): Boolean =
              open(RecurrentesSheetRequest.Occurrence(recurrenciaId))

          override fun onOpenRecurrencia(recurrenciaId: String): Boolean =
              open(RecurrentesSheetRequest.Recurrencia(recurrenciaId))

          private fun open(request: RecurrentesSheetRequest): Boolean {
            controller.requested = request
            return modals.tryOpenDestinationModal(controller)
          }
        }
      }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  LaunchedEffect(viewModel, lifecycle, feedback) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.effects.collect { effect ->
        when (effect) {
          is RecurrentesEffect.Feedback -> feedback.enqueue(effect.outcome)
        }
      }
    }
  }
  RecurrentesScreen(uiState, actions, onBack, modifier)
}

private sealed interface RecurrentesSheetRequest {
  val recurrenciaId: String

  data class Occurrence(override val recurrenciaId: String) : RecurrentesSheetRequest

  data class Recurrencia(override val recurrenciaId: String) : RecurrentesSheetRequest
}

private class RecurrentesSheetController(
    private val viewModel: RecurrentesViewModel,
    private val state: State<RecurrentesUiState>,
) : DestinationModalController {
  var requested: RecurrentesSheetRequest? = null

  // Reads the snapshot State so shell feedback recomposes when a sheet opens or hides.
  override val hasActiveSession: Boolean
    get() = state.value.sheet != null

  override fun open(): Boolean =
      when (val request = requested) {
        null -> false
        is RecurrentesSheetRequest.Occurrence -> viewModel.onOpenOccurrence(request.recurrenciaId)
        is RecurrentesSheetRequest.Recurrencia -> viewModel.onOpenRecurrencia(request.recurrenciaId)
      }
}
