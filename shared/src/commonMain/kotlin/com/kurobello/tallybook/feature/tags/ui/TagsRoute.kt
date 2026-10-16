package com.kurobello.tallybook.feature.tags.ui

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
import com.kurobello.tallybook.feature.tags.viewmodel.TagSheetRequest
import com.kurobello.tallybook.feature.tags.viewmodel.TagsActions
import com.kurobello.tallybook.feature.tags.viewmodel.TagsEffect
import com.kurobello.tallybook.feature.tags.viewmodel.TagsFeedback
import com.kurobello.tallybook.feature.tags.viewmodel.TagsUiState
import com.kurobello.tallybook.feature.tags.viewmodel.TagsViewModel
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.tag_created
import tallybook.shared.generated.resources.tag_deleted
import tallybook.shared.generated.resources.tag_updated

@Composable
internal fun TagsRoute(
    modals: PrimaryModalCoordinator,
    active: Boolean,
    onBack: () -> Unit,
    onShowTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val viewModel: TagsViewModel = koinViewModel()
  val state = viewModel.uiState.collectAsStateWithLifecycle()
  val uiState by state
  val controller = remember(viewModel, state) { TagSheetController(viewModel, state) }
  DisposableEffect(modals, controller, active) {
    if (active) modals.attach(controller)
    onDispose { modals.detach(controller) }
  }
  val actions =
      remember(viewModel, modals, controller) {
        object : TagsActions by viewModel {
          override fun onOpenSheet(request: TagSheetRequest): Boolean {
            controller.requested = request
            return modals.tryOpenDestinationModal(controller)
          }
        }
      }
  var snackbar by remember { mutableStateOf<TallybookSnackbarMessage?>(null) }
  var nextSnackbarToken by remember { mutableLongStateOf(0L) }
  val latestShowTag = rememberUpdatedState(onShowTag)
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  LaunchedEffect(viewModel, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      viewModel.effects.collect { effect ->
        when (effect) {
          is TagsEffect.OpenSearch -> {
            // The shell refuses to navigate while a modal is open, and the collected State can
            // trail the ViewModel's by a frame.
            snapshotFlow { state.value.sheet == null }.first { it }
            latestShowTag.value(effect.id)
          }
          is TagsEffect.Feedback ->
              snackbar =
                  TallybookSnackbarMessage(
                      token = ++nextSnackbarToken,
                      revision = 0,
                      message = getString(effect.feedback.message),
                      duration = TallybookSnackbarDuration.Short,
                      status = TallybookSnackbarStatus.Success,
                  )
        }
      }
    }
  }
  Box(modifier = modifier.fillMaxSize()) {
    TagsScreen(uiState, actions, onBack)
    TallybookScreenSnackbar(
        message = snackbar,
        isActive = uiState.sheet == null,
        onDismiss = { token -> if (snackbar?.token == token) snackbar = null },
    )
  }
}

private val TagsFeedback.message: StringResource
  get() =
      when (this) {
        TagsFeedback.CREATED -> Res.string.tag_created
        TagsFeedback.UPDATED -> Res.string.tag_updated
        TagsFeedback.DELETED -> Res.string.tag_deleted
      }

private class TagSheetController(
    private val viewModel: TagsViewModel,
    private val state: State<TagsUiState>,
) : DestinationModalController {
  var requested: TagSheetRequest = TagSheetRequest.New("")

  // Reads the snapshot State so shell navigation recomposes when the sheet opens or hides.
  override val hasActiveSession: Boolean
    get() = state.value.sheet != null

  override fun open(): Boolean = viewModel.onOpenSheet(requested)
}
