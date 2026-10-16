package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .testTag(CATEGORY_PICKER_CREATION_TAG)
              .padding(horizontal = TallybookTheme.spacing.space22),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
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
    // The expanded icon grid and "More options" can outgrow the sheet, so only the fields scroll.
    CategoryForm(
        state = draft,
        actions = actions,
        mode = CategoryFormMode.COMPACT,
        modifier =
            Modifier.heightIn(max = TallybookTheme.dimensions.categoryPickerResultsMaximumHeight)
                .verticalScroll(rememberScrollState()),
        duplicateAction =
            draft.duplicate?.let { duplicate ->
              CategoryDuplicateAction(
                  label = stringResource(Res.string.category_duplicate_use, duplicate.nombre),
                  onClick = actions::onUseDuplicate,
              )
            },
    )
    if (saveFailed) {
      TallybookText(
          text = stringResource(Res.string.category_save_error),
          color = TallybookTheme.colors.status.danger.base,
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          style = TallybookTheme.typography.body,
      )
    }
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
  }
}
