package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoriaSelection
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category
import tallybook.shared.generated.resources.category_picker_done
import tallybook.shared.generated.resources.category_picker_limit_reached
import tallybook.shared.generated.resources.category_tag_field_label

internal const val CATEGORY_PICKER_DONE_TAG = "category_picker_done"

internal sealed interface CategoryPickerMode {
  val selectedIds: List<String>
  val defaultPaneTitle: StringResource

  data class Single(val selectedId: String?, val onSelect: (String) -> Unit) : CategoryPickerMode {
    override val selectedIds: List<String> = listOfNotNull(selectedId)
    override val defaultPaneTitle: StringResource = Res.string.category
  }

  data class Multiple(
      override val selectedIds: List<String>,
      val max: Int,
      val onSelectionChange: (List<String>) -> Unit,
  ) : CategoryPickerMode {
    override val defaultPaneTitle: StringResource = Res.string.category_tag_field_label
  }
}

internal fun CategoryPickerMode.choose(id: String, catalog: List<Categoria>, dismiss: () -> Unit) {
  when (this) {
    is CategoryPickerMode.Single -> {
      onSelect(id)
      dismiss()
    }
    is CategoryPickerMode.Multiple -> {
      val updated = toggled(id, catalog)
      if (updated != selectedIds) onSelectionChange(updated)
    }
  }
}

internal fun CategoryPickerMode.Multiple.toggled(
    id: String,
    catalog: List<Categoria>,
): List<String> =
    if (id in selectedIds) CategoriaSelection.remove(selectedIds, id)
    else CategoriaSelection.add(selectedIds, id, catalog, max)

@Composable
internal fun CategoryPickerFooter(mode: CategoryPickerMode.Multiple, onDone: () -> Unit) {
  Column(
      modifier = Modifier.padding(bottom = TallybookTheme.spacing.space8),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    if (mode.selectedIds.size >= mode.max) {
      TallybookText(
          text = stringResource(Res.string.category_picker_limit_reached, mode.max),
          color = TallybookTheme.colors.foreground.tertiary,
          style = TallybookTheme.typography.caption,
      )
    }
    TallybookButton(
        onClick = onDone,
        modifier = Modifier.fillMaxWidth().testTag(CATEGORY_PICKER_DONE_TAG),
    ) {
      TallybookText(stringResource(Res.string.category_picker_done, mode.selectedIds.size))
    }
  }
}
