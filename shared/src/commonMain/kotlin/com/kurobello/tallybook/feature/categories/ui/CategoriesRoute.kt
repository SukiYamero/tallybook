package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarDuration
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarHost
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarMessage
import com.kurobello.tallybook.core.ui.component.TallybookSnackbarStatus
import com.kurobello.tallybook.core.ui.navigation.DestinationModalController
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesActions
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesEffect
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesFeedback
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesUiState
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesViewModel
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryEditorRequest
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_created
import tallybook.shared.generated.resources.category_deleted
import tallybook.shared.generated.resources.category_hidden_feedback
import tallybook.shared.generated.resources.category_shown_feedback
import tallybook.shared.generated.resources.category_updated
import tallybook.shared.generated.resources.category_visibility_error
import tallybook.shared.generated.resources.movimiento_notification_close

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
  val controller = remember(viewModel, state) { CategoriesEditorController(viewModel, state) }
  DisposableEffect(modals, controller, active) {
    if (active) modals.attach(controller)
    onDispose { modals.detach(controller) }
  }
  val actions =
      remember(viewModel, modals, controller) {
        object : CategoriesActions by viewModel {
          override fun onOpenEditor(request: CategoryEditorRequest): Boolean {
            controller.requested = request
            return modals.tryOpenDestinationModal(controller)
          }
        }
      }
  val listState = rememberLazyListState()
  var snackbar by remember { mutableStateOf<TallybookSnackbarMessage?>(null) }
  var nextSnackbarToken by remember { mutableLongStateOf(0L) }
  val latestShowCategory = rememberUpdatedState(onShowCategory)
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  LaunchedEffect(viewModel, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.effects.collect { effect ->
        when (effect) {
          is CategoriesEffect.OpenSearch -> latestShowCategory.value(effect.id)
          is CategoriesEffect.Reveal -> {
            val index =
                viewModel.uiState.value.listItems().indexOfFirst {
                  it is CategoriesListItem.Row && it.row.categoria.id == effect.id
                }
            if (index >= 0) listState.animateScrollToItem(index)
          }
          is CategoriesEffect.Feedback ->
              snackbar = effect.feedback.toSnackbarMessage(++nextSnackbarToken)
        }
      }
    }
  }
  Box(modifier = modifier.fillMaxSize()) {
    CategoriesScreen(uiState, actions, onBack, listState = listState)
    CategoriesSnackbar(
        message = snackbar,
        isActive = uiState.editor == null,
        onDismiss = { token -> if (snackbar?.token == token) snackbar = null },
    )
  }
}

@Composable
private fun CategoriesSnackbar(
    message: TallybookSnackbarMessage?,
    isActive: Boolean,
    onDismiss: (Long) -> Unit,
) {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .windowInsetsPadding(WindowInsets.safeDrawing)
              .padding(
                  start = TallybookTheme.dimensions.screenHorizontal,
                  end = TallybookTheme.dimensions.screenHorizontal,
                  bottom = TallybookTheme.spacing.space16,
              )
  ) {
    TallybookSnackbarHost(
        message = message,
        isActive = isActive,
        closeLabel = stringResource(Res.string.movimiento_notification_close),
        onAction = { _, _ -> },
        onDismiss = { token, _, _ -> onDismiss(token) },
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .widthIn(max = TallybookTheme.dimensions.screenContentMaxWidth)
                .fillMaxWidth(),
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
        is CategoriesFeedback.Shown -> getString(Res.string.category_shown_feedback, nombre)
        CategoriesFeedback.VisibilityFailed -> getString(Res.string.category_visibility_error)
      }
  return TallybookSnackbarMessage(
      token = token,
      revision = 0,
      message = text,
      duration =
          if (this == CategoriesFeedback.VisibilityFailed) TallybookSnackbarDuration.Long
          else TallybookSnackbarDuration.Short,
      status =
          if (this == CategoriesFeedback.VisibilityFailed) TallybookSnackbarStatus.Error
          else TallybookSnackbarStatus.Success,
  )
}

private class CategoriesEditorController(
    private val viewModel: CategoriesViewModel,
    private val state: State<CategoriesUiState>,
) : DestinationModalController {
  var requested: CategoryEditorRequest = CategoryEditorRequest.New("")

  // Reads the snapshot State so shell navigation recomposes when the editor opens or hides.
  override val hasActiveSession: Boolean
    get() = state.value.editor != null

  override fun open(): Boolean = viewModel.onOpenEditor(requested)
}
