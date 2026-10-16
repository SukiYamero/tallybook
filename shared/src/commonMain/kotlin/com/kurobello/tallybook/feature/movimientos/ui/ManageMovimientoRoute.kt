package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kurobello.tallybook.core.model.MAX_CATEGORIAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.ui.navigation.PrimaryModalCoordinator
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.ManageMovimientoUiState

@Composable
internal fun ManageMovimientoRoute(
    uiState: ManageMovimientoUiState,
    actions: ManageMovimientoActions,
) {
  uiState.session?.let { session ->
    key(session.token) {
      CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
        val quickCategory = rememberQuickCategoryViewModel(QUICK_CATEGORY_MANAGE_KEY)
        val quickCategoryState by quickCategory.uiState.collectAsStateWithLifecycle()
        QuickCategorySelectionEffect(
            effects = quickCategory.effects,
            selection =
                CategoryPickerSelection(
                    selectedIds = listOfNotNull(session.form?.categoriaId).realCategoriaIds(),
                    max = MAX_CATEGORIAS_POR_MOVIMIENTO,
                    onSelectionChange = actions::onCategoriasChanged,
                ),
            catalog = session.categorias,
        )
        ManageMovimientoContent(
            session,
            actions,
            CategoryPickerCreation(quickCategoryState, quickCategory),
        )
      }
    }
  }
}

@Composable
internal fun rememberOpenMovimiento(modals: PrimaryModalCoordinator): (String) -> Unit {
  val focusManager = LocalFocusManager.current
  val keyboard = LocalSoftwareKeyboardController.current
  return remember(modals, focusManager, keyboard) {
    { id ->
      if (modals.tryOpenManagement(id)) {
        keyboard?.hide()
        focusManager.clearFocus(force = true)
      }
    }
  }
}
