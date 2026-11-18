package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.category.CategorySingleField
import com.kurobello.tallybook.core.ui.component.LastWhileVisible
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoStep
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.ReviewPartUi
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_picker_single_title
import tallybook.shared.generated.resources.movimiento_summary_title_quote

/** The review part whose category is being chosen; null while no part's is. */
@Stable
internal class ReviewCategoryTarget(private val state: MutableState<Int?>) {
  val index: Int?
    get() = state.value

  fun open(index: Int) {
    state.value = index
  }

  fun close() {
    state.value = null
  }
}

@Composable
internal fun rememberReviewCategoryTarget(): ReviewCategoryTarget {
  val state = rememberSaveable { mutableStateOf<Int?>(null) }
  return remember(state) { ReviewCategoryTarget(state) }
}

/**
 * Where the category picker reports: the targeted part while the review shows, otherwise the single
 * draft. A part's pick also ends the choice, so its sheet does not reopen once the picker closes.
 */
internal fun AddMovimientoUiState.categoryPickerSelection(
    actions: AddMovimientoActions,
    target: ReviewCategoryTarget,
): CategoryPickerSelection {
  val index = target.index?.takeIf { step == AddMovimientoStep.REVIEW && it in parts.indices }
  return if (index == null) {
    CategoryPickerSelection(categoriaId.realCategoriaId(), actions::onCategoriaSelected)
  } else {
    CategoryPickerSelection(parts[index].categoriaId.realCategoriaId()) { id ->
      target.close()
      actions.onPartCategoriaSelected(index, id)
    }
  }
}

internal data class ReviewPartCategoryCallbacks(
    val onSuggestion: (Categoria) -> Unit,
    val onDeselect: () -> Unit,
    val onChoose: () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * The Category section of one part, over the review: its category, its own suggestions and Choose,
 * which hands over to the category picker. [part] keeps showing while the sheet leaves.
 */
@Composable
internal fun ReviewPartCategorySheet(
    visible: Boolean,
    part: ReviewPartUi?,
    enabled: Boolean,
    callbacks: ReviewPartCategoryCallbacks,
) {
  val shown = remember { LastWhileVisible(part) }.resolve(visible && part != null, part)
  TallybookModalBottomSheet(
      visible = visible && part != null,
      paneTitle = stringResource(Res.string.category_picker_single_title),
      callbacks = ModalBottomSheetCallbacks(onDismiss = callbacks.onDismiss),
  ) {
    if (shown != null) {
      Column(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = TallybookTheme.dimensions.screenHorizontal)
                  .padding(bottom = TallybookTheme.spacing.space24),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
      ) {
        TallybookSheetTitle(stringResource(Res.string.movimiento_summary_title_quote, shown.titulo))
        CategorySingleField(
            categoria = shown.categoria,
            deselectable = true,
            onChoose = callbacks.onChoose,
            onSuggestion = callbacks.onSuggestion,
            onDeselect = callbacks.onDeselect,
            pending = shown.categoriaIsPending,
            suggestions = shown.suggestions,
            enabled = enabled,
        )
      }
    }
  }
}
