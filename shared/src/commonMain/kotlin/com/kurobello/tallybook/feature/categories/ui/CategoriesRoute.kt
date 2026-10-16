package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.ui.component.TallybookScreenSnackbar
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarDuration
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarMessage
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarStatus
import com.kurobello.tallybook.core.ui.navigation.DestinationModalController
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesActions
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesEffect
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesFeedback
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesUiState
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesViewModel
import com.kurobello.tallybook.feature.categories.viewmodel.CategorySheetRequest
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_created
import tallybook.shared.generated.resources.category_deleted
import tallybook.shared.generated.resources.category_hidden_feedback
import tallybook.shared.generated.resources.category_updated

@Composable
internal fun CategoriesRoute(
    modals: PrimaryModalCoordinator,
    active: Boolean,
    onBack: () -> Unit,
    onShowCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val viewModel: CategoriesViewModel = koinViewModel()
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val uiState by state
  val controller = remember(viewModel, state) { CategorySheetController(viewModel, state) }
  DisposableEffect(modals, controller, active) {
    if (active) modals.attach(controller)
    onDispose { modals.detach(controller) }
  }
  val actions =
      remember(viewModel, modals, controller) {
        object : CategoriesActions by viewModel {
          override fun onOpenSheet(request: CategorySheetRequest): Boolean {
            controller.requested = request
            return modals.tryOpenDestinationModal(controller)
          }
        }
      }
  var snackbar by remember { mutableStateOf<TallybookSnackbarMessage?>(null) }
  var nextSnackbarToken by remember { mutableLongStateOf(0L) }
  val latestShowCategory = rememberUpdatedState(onShowCategory)
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  LaunchedEffect(viewModel, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.effects.collect { effect ->
        when (effect) {
          is CategoriesEffect.OpenSearch -> {
            // The shell refuses to navigate while a modal is open, and the collected State can
            // trail the ViewModel's by a frame.
            snapshotFlow { state.value.sheet == null }.first { it }
            latestShowCategory.value(effect.id)
          }
          is CategoriesEffect.Feedback ->
              snackbar = effect.feedback.toSnackbarMessage(++nextSnackbarToken)
        }
      }
    }
  }
  Box(modifier = modifier.fillMaxSize()) {
    CategoriesScreen(uiState, actions, onBack)
    TallybookScreenSnackbar(
        message = snackbar,
        isActive = uiState.sheet == null,
        onDismiss = { token -> if (snackbar?.token == token) snackbar = null },
    )
  }
}

private suspend fun CategoriesFeedback.toSnackbarMessage(token: Long): TallybookSnackbarMessage {
  val text =
      when (this) {
        CategoriesFeedback.Created -> getString(Res.string.category_created)
        CategoriesFeedback.Updated -> getString(Res.string.category_updated)
        CategoriesFeedback.Deleted -> getString(Res.string.category_deleted)
        is CategoriesFeedback.Hidden -> getString(Res.string.category_hidden_feedback, nombre)
      }
  return TallybookSnackbarMessage(
      token = token,
      revision = 0,
      message = text,
      duration = TallybookSnackbarDuration.Short,
      status = TallybookSnackbarStatus.Success,
  )
}

private class CategorySheetController(
    private val viewModel: CategoriesViewModel,
    private val state: State<CategoriesUiState>,
) : DestinationModalController {
  var requested: CategorySheetRequest = CategorySheetRequest.New("")

  // Reads the snapshot State so shell navigation recomposes when the sheet opens or hides.
  override val hasActiveSession: Boolean
    get() = state.value.sheet != null

  override fun open(): Boolean = viewModel.onOpenSheet(requested)
}
