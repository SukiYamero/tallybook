package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetCallbacks
import com.kurobello.tallybook.core.ui.component.ModalBottomSheetLayout
import com.kurobello.tallybook.core.ui.component.TallybookButton
import com.kurobello.tallybook.core.ui.component.TallybookIconButton
import com.kurobello.tallybook.core.ui.component.TallybookModalBottomSheet
import com.kurobello.tallybook.core.ui.component.TallybookSheetTitle
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.CategoriesActions
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryDetails
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryEditor
import com.kurobello.tallybook.feature.categories.viewmodel.CategorySheet
import com.kurobello.tallybook.feature.categories.viewmodel.canChangeVisibility
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_edit_title
import tallybook.shared.generated.resources.category_hidden_marker
import tallybook.shared.generated.resources.category_hide_named
import tallybook.shared.generated.resources.category_new_title
import tallybook.shared.generated.resources.category_show_named
import tallybook.shared.generated.resources.category_view_movimientos_count
import tallybook.shared.generated.resources.category_visibility_error

private const val EDITOR_DETENT = "editor"
private const val DETAILS_DETENT = "details"
private val CATEGORY_SHEET_DETENTS = listOf(EDITOR_DETENT, DETAILS_DETENT)

/**
 * One sheet for whichever category is open, so "Open <name>" on a duplicate swaps its content in
 * place instead of stacking a second modal.
 */
@Composable
internal fun CategorySheetHost(sheet: CategorySheet?, actions: CategoriesActions) {
  if (sheet == null) return
  val title =
      when (sheet) {
        is CategoryEditor ->
            stringResource(
                if (sheet.editing == null) Res.string.category_new_title
                else Res.string.category_edit_title
            )
        is CategoryDetails -> sheet.categoria.nombre
      }
  TallybookModalBottomSheet(
      visible = !sheet.isClosing,
      paneTitle = title,
      callbacks =
          ModalBottomSheetCallbacks(
              onDismiss = actions::onRequestCloseSheet,
              onDismissRequest = {
                actions.onRequestCloseSheet()
                false
              },
              onHidden = actions::onSheetHidden,
          ),
      layout =
          ModalBottomSheetLayout(
              contentDetentIdentifiers = CATEGORY_SHEET_DETENTS,
              activeContentDetentIdentifier =
                  if (sheet is CategoryEditor) EDITOR_DETENT else DETAILS_DETENT,
          ),
  ) {
    when (sheet) {
      is CategoryEditor -> key(sheet.editing?.id) { CategoryEditorBody(sheet, title, actions) }
      is CategoryDetails -> CategoryDetailsBody(sheet, actions)
    }
  }
  if (sheet is CategoryEditor) CategoryEditorDialogs(sheet, actions)
}

@Composable
private fun CategoryDetailsBody(details: CategoryDetails, actions: CategoriesActions) {
  val categoria = details.categoria
  val enabled = !details.isClosing && !details.visibilityPending
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = TallybookTheme.dimensions.screenHorizontal)
              .padding(bottom = TallybookTheme.spacing.space8),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space16),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      CategoryAvatar(categoria)
      Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space2),
      ) {
        TallybookSheetTitle(categoria.nombre)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
          TallybookText(
              movimientoCountLabel(details.count),
              color = TallybookTheme.colors.foreground.tertiary,
              style = TallybookTheme.typography.bodySmall,
          )
          if (categoria.oculta) CategoryHiddenStatus()
        }
      }
      if (categoria.canChangeVisibility) {
        CategoryVisibilityButton(categoria, enabled, actions::onToggleHidden)
      }
    }
    if (details.visibilityFailed) {
      TallybookText(
          stringResource(Res.string.category_visibility_error),
          color = TallybookTheme.colors.status.danger.base,
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      )
    }
    if (details.count > 0) {
      TallybookButton(
          onClick = actions::onViewMovimientos,
          modifier = Modifier.fillMaxWidth(),
          enabled = enabled,
      ) {
        TallybookText(viewMovimientosLabel(details.count))
      }
    }
  }
}

/** The icon shows what a tap does: an open eye brings a hidden categoria back. */
@Composable
internal fun CategoryVisibilityButton(
    categoria: Categoria,
    enabled: Boolean,
    onToggle: (String) -> Unit,
) {
  TallybookIconButton(
      onClick = { onToggle(categoria.id) },
      icon = if (categoria.oculta) Lucide.Eye else Lucide.EyeOff,
      contentDescription =
          stringResource(
              if (categoria.oculta) Res.string.category_show_named
              else Res.string.category_hide_named,
              categoria.nombre,
          ),
      enabled = enabled,
  )
}

@Composable
internal fun CategoryHiddenStatus() {
  TallybookText(
      stringResource(Res.string.category_hidden_marker),
      modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      color = TallybookTheme.colors.foreground.secondary,
      style = TallybookTheme.typography.bodySmall,
  )
}

@Composable
internal fun viewMovimientosLabel(count: Long): String =
    pluralStringResource(
        Res.plurals.category_view_movimientos_count,
        count.toPluralQuantity(),
        count,
    )
