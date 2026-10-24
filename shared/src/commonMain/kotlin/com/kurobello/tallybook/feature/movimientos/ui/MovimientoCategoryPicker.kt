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
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.ui.account.CuentaPickerSheet
import com.kurobello.tallybook.core.ui.tag.EtiquetaPickerSheet
import com.kurobello.tallybook.feature.categories.viewmodel.QuickCategoryEffect
import com.kurobello.tallybook.feature.categories.viewmodel.QuickCategoryViewModel
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoCuentaUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState
import kotlinx.coroutines.flow.Flow
import org.koin.compose.viewmodel.koinViewModel

// Otros is the fallback for "no category", so choosing it explicitly would defeat the pending flag.
internal val PICKER_EXCLUDED_CATEGORY_IDS: Set<String> = setOf(CATEGORIA_OTROS_ID)

/** Whether a picker sheet a movimiento surface hosts is open; opening drops the keyboard. */
internal data class MovimientoPickerState(
    val isVisible: Boolean,
    val onOpen: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
internal fun rememberMovimientoPicker(interactionEnabled: Boolean): MovimientoPickerState {
  var isVisible by rememberSaveable { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  return MovimientoPickerState(
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
internal fun String?.realCategoriaId(): String? = takeUnless { it in PICKER_EXCLUDED_CATEGORY_IDS }

/**
 * What a movimiento picker leaves out: the Otros fallback and hidden categories, except a hidden
 * category already on the movimiento, which stays visible as the current choice.
 */
internal fun pickerExcludedCategoryIds(catalog: List<Categoria>, selectedId: String?): Set<String> =
    PICKER_EXCLUDED_CATEGORY_IDS +
        catalog.filter { it.oculta && it.id != selectedId }.map(Categoria::id)

/** The picker every movimiento editor opens, so each one filters and quick-creates the same way. */
@Composable
internal fun MovimientoCategoryPickerSheet(
    visible: Boolean,
    uiState: MovimientoFormState,
    onSelect: (String) -> Unit,
    picker: MovimientoPickerState,
    creation: CategoryPickerCreation?,
) {
  val selectedId = uiState.categoriaId.realCategoriaId()
  CategoryPickerSheet(
      visible = visible,
      categorias = uiState.categorias,
      selectedId = selectedId,
      onSelect = onSelect,
      onDismiss = picker.onDismiss,
      onBack = picker.onDismiss,
      excludedIds = pickerExcludedCategoryIds(uiState.categorias, selectedId),
      creation = creation,
  )
}

/**
 * The tag picker every movimiento surface opens. It only sees tags the catalog still has, so a
 * deleted tag never comes back through a toggle.
 */
@Composable
internal fun MovimientoEtiquetaPickerSheet(
    visible: Boolean,
    uiState: MovimientoFormState,
    actions: MovimientoFormActions,
    picker: MovimientoPickerState,
) {
  EtiquetaPickerSheet(
      visible = visible,
      etiquetas = uiState.etiquetas,
      selectedIds = uiState.selectedEtiquetas.map(Etiqueta::id),
      onSelectionChange = actions::onEtiquetasChanged,
      onCreate = actions::onCreateEtiqueta,
      onDismiss = picker.onDismiss,
      onBack = picker.onDismiss,
      failedCreateNombre = uiState.failedEtiquetaNombre,
  )
}

/** The account picker every movimiento surface opens, over the field's compatible options. */
@Composable
internal fun MovimientoCuentaPickerSheet(
    visible: Boolean,
    field: MovimientoCuentaUi,
    onSelect: (String) -> Unit,
    picker: MovimientoPickerState,
) {
  CuentaPickerSheet(
      visible = visible,
      cuentas = field.options,
      selectedId = field.selected?.id,
      onSelect = onSelect,
      onDismiss = picker.onDismiss,
      primaryMoneda = field.primaryMoneda,
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
 * Selects what quick-create produced through [selection]. A new categoria reaches [catalog] only
 * after the repository emits it, and selecting an id the catalog does not know yet would be
 * dropped, so the selection waits for it.
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
    currentSelection.choose(id)
  }
}
