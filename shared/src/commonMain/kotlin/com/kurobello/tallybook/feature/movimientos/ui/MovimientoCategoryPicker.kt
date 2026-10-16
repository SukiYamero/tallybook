package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.feature.categories.viewmodel.QuickCategoryEffect
import com.kurobello.tallybook.feature.categories.viewmodel.QuickCategoryViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState
import kotlinx.coroutines.flow.Flow
import org.koin.compose.viewmodel.koinViewModel

// Otros is the fallback for "no category", so choosing it explicitly would defeat the pending flag.
internal val PICKER_EXCLUDED_CATEGORY_IDS: Set<String> = setOf(CATEGORIA_OTROS_ID)

internal data class MovimientoCategoryPickerState(
    val isVisible: Boolean,
    val onOpen: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
internal fun rememberMovimientoCategoryPicker(
    interactionEnabled: Boolean
): MovimientoCategoryPickerState {
  var isVisible by rememberSaveable { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  return MovimientoCategoryPickerState(
      isVisible = isVisible,
      onOpen = {
        if (interactionEnabled) {
          focusManager.clearFocus(force = true)
          isVisible = true
        }
      },
      onDismiss = { isVisible = false },
  )
}

/** The picker offers real categories only; the Otros fallback is not a selection. */
internal fun List<String>.realCategoriaIds(): List<String> = filterNot {
  it in PICKER_EXCLUDED_CATEGORY_IDS
}

/**
 * What a movimiento picker leaves out: the Otros fallback and hidden categories, except a hidden
 * category already on the movimiento, which must stay visible so it can be removed.
 */
internal fun pickerExcludedCategoryIds(
    catalog: List<Categoria>,
    selectedIds: List<String>,
): Set<String> =
    PICKER_EXCLUDED_CATEGORY_IDS +
        (catalog.filter(Categoria::oculta).map(Categoria::id) - selectedIds.toSet())

/** The picker every movimiento editor opens, so each one filters and quick-creates the same way. */
@Composable
internal fun MovimientoCategoryPickerSheet(
    visible: Boolean,
    uiState: MovimientoFormState,
    onSelectionChange: (List<String>) -> Unit,
    picker: MovimientoCategoryPickerState,
    creation: CategoryPickerCreation?,
) {
  val selectedIds = listOfNotNull(uiState.categoriaId).realCategoriaIds()
  CategoryPickerSheet(
      visible = visible,
      categorias = uiState.categorias,
      selectedIds = selectedIds,
      onSelectionChange = onSelectionChange,
      onDismiss = picker.onDismiss,
      onBack = picker.onDismiss,
      excludedIds = pickerExcludedCategoryIds(uiState.categorias, selectedIds),
      creation = creation,
  )
}

internal const val QUICK_CATEGORY_ADD_KEY = "quick-category-add"
internal const val QUICK_CATEGORY_MANAGE_KEY = "quick-category-manage"

/**
 * A quick-create ViewModel scoped to the calling host's composition: it is cleared when that host
 * (one Add session, one management session) leaves, and survives configuration changes through the
 * parent owner.
 */
@Composable
internal fun rememberQuickCategoryViewModel(key: String): QuickCategoryViewModel {
  val parent = requireNotNull(LocalViewModelStoreOwner.current)
  // Intent extras may contain newly copied Bundles on every read of the same Activity.
  val extras = remember(parent) { parent.defaultViewModelCreationExtras }
  val owner =
      rememberViewModelStoreOwner(
          parent = parent,
          savedStateRegistryOwner = null,
          defaultCreationExtras = extras,
      )
  return koinViewModel(key = key, viewModelStoreOwner = owner)
}

/**
 * Selects what quick-create produced through [selection], so the picker's limit still applies. A
 * new categoria reaches [catalog] only after the repository emits it, and selecting an id the
 * catalog does not know yet would be dropped, so the selection waits for it.
 */
@Composable
internal fun QuickCategorySelectionEffect(
    effects: Flow<QuickCategoryEffect>,
    selection: CategoryPickerSelection,
    catalog: List<Categoria>,
) {
  var pendingId by remember { mutableStateOf<String?>(null) }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  LaunchedEffect(effects, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
      effects.collect { effect -> pendingId = effect.categoriaId }
    }
  }
  val currentSelection by rememberUpdatedState(selection)
  LaunchedEffect(pendingId, catalog) {
    val id = pendingId ?: return@LaunchedEffect
    if (catalog.none { it.id == id }) return@LaunchedEffect
    pendingId = null
    if (id !in currentSelection.selectedIds) currentSelection.choose(id, catalog)
  }
}
