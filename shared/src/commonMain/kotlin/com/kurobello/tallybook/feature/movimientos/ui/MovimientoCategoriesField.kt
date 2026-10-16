package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.category.CategoryTagField
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormState
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_required
import tallybook.shared.generated.resources.unknown_category

@Composable
internal fun MovimientoCategoriesField(
    uiState: MovimientoFormState,
    onOpen: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
  val ids = listOfNotNull(uiState.displayedCategoriaId)
  val unknownName = stringResource(Res.string.unknown_category)
  val categorias =
      remember(ids, uiState.categorias, unknownName) {
        ids.map { id ->
          uiState.categorias.firstOrNull { it.id == id } ?: unknownCategoria(id, unknownName)
        }
      }
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    CategoryTagField(
        categorias = categorias,
        // The field never offers removal on its only tag; switching goes through the picker.
        onRemove = {},
        onAdd = onOpen,
        pending = CATEGORIA_OTROS_ID in ids,
        enabled = enabled,
    )
    if (uiState.isCategoriaMissing) {
      TallybookText(
          text = stringResource(Res.string.category_required),
          color = TallybookTheme.colors.status.danger.base,
          style = TallybookTheme.typography.bodySmall,
      )
    }
  }
}

// A stored category the catalog no longer lists stays visible as a neutral tag.
private fun unknownCategoria(id: String, name: String) =
    Categoria(
        id,
        name,
        padreId = null,
        icono = CategoryIcon.CIRCLE_DASHED,
        color = CategoryTint.NEUTRAL,
    )
