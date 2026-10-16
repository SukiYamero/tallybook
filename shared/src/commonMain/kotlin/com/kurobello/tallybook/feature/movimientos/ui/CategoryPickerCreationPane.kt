package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookSheetForm
import com.kurobello.tallybook.core.ui.component.TallybookSheetFormLayout
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.ui.CategoryDuplicateAction
import com.kurobello.tallybook.feature.categories.ui.CategoryForm
import com.kurobello.tallybook.feature.categories.ui.CategoryFormMode
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryDraftState
import com.kurobello.tallybook.feature.categories.viewmodel.QuickCategoryActions
import com.kurobello.tallybook.feature.categories.viewmodel.QuickCategoryUiState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.back
import tallybook.shared.generated.resources.category_create_action
import tallybook.shared.generated.resources.category_duplicate_use
import tallybook.shared.generated.resources.category_new_title
import tallybook.shared.generated.resources.category_save_error

internal const val CATEGORY_PICKER_CREATION_TAG = "category_picker_creation"
internal const val CATEGORY_PICKER_CREATE_TAG = "category_picker_create"

/**
 * The quick-create flow a movimiento picker offers when a search finds nothing. Its owner keeps the
 * draft and the write; the picker only switches to the creation pane while [state] has a draft.
 */
internal data class CategoryPickerCreation(
    val state: QuickCategoryUiState,
    val actions: QuickCategoryActions,
)

@Composable
internal fun CategoryPickerCreationPane(
    draft: CategoryDraftState,
    saveFailed: Boolean,
    actions: QuickCategoryActions,
    onBack: () -> Unit,
) {
  val title = stringResource(Res.string.category_new_title)
  TallybookSheetForm(
      header = { CreationPaneHeader(title, onBack) },
      footer = {
        TallybookButton(
            onClick = actions::onSave,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = TallybookTheme.spacing.space8)
                    .testTag(CATEGORY_PICKER_CREATE_TAG),
            enabled = draft.canSave,
        ) {
          TallybookText(stringResource(Res.string.category_create_action))
        }
      },
      modifier = Modifier.testTag(CATEGORY_PICKER_CREATION_TAG),
      // A fixed body close to the results' height keeps the sheet from jumping between panes and
      // leaves room above the keyboard, since the sheet itself never moves for it.
      layout =
          TallybookSheetFormLayout(
              horizontalPadding = TallybookTheme.spacing.space22,
              minimumBodyHeight = TallybookTheme.dimensions.categorySheetFormMinimumHeight,
              maximumBodyHeight = TallybookTheme.dimensions.categoryPickerResultsMaximumHeight,
          ),
  ) {
    CategoryForm(
        state = draft,
        actions = actions,
        mode = CategoryFormMode.COMPACT,
        duplicateAction =
            draft.duplicate?.let { duplicate ->
              CategoryDuplicateAction(
                  label = stringResource(Res.string.category_duplicate_use, duplicate.nombre),
                  onClick = actions::onUseDuplicate,
              )
            },
        revealFocusedInput = { revealWhenFocused() },
    )
    if (saveFailed) {
      TallybookText(
          text = stringResource(Res.string.category_save_error),
          color = TallybookTheme.colors.status.danger.base,
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          style = TallybookTheme.typography.body,
      )
    }
  }
}

@Composable
private fun CreationPaneHeader(title: String, onBack: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    TallybookIconButton(
        onClick = onBack,
        icon = Lucide.ArrowLeft,
        contentDescription = stringResource(Res.string.back),
    )
    TallybookSheetTitle(text = title, modifier = Modifier.weight(1f))
  }
}
