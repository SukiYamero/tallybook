package com.kurobello.tallybook.core.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
fun CategoryChip(
    categoria: Categoria,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val colors = categoria.color.tintColors()
  val shape = MaterialTheme.shapes.extraLarge
  Row(
      modifier =
          modifier
              .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
              .clip(shape)
              .background(
                  if (selected) colors.container else MaterialTheme.colorScheme.surfaceContainerLow
              )
              .border(
                  width = 1.dp,
                  color = if (selected) colors.border else MaterialTheme.colorScheme.outlineVariant,
                  shape = shape,
              )
              .selectable(selected = selected, onClick = onClick, role = Role.Button)
              .semantics(mergeDescendants = true) {}
              .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    CategoryAvatar(categoria = categoria, modifier = Modifier.size(28.dp))
    Text(
        text = categoria.nombre,
        color = if (selected) colors.content else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge,
    )
  }
}

private val chipPreviewCategoria =
    Categoria(
        id = "cat_comida",
        nombre = "Comida",
        padreId = null,
        icono = CategoryIcon.UTENSILS,
        color = CategoryTint.AMBER,
    )

@Preview
@Composable
internal fun CategoryChipLightPreview() {
  TallybookTheme(darkTheme = false) {
    CategoryChip(categoria = chipPreviewCategoria, selected = true, onClick = {})
  }
}

@Preview
@Composable
internal fun CategoryChipDarkPreview() {
  TallybookTheme(darkTheme = true) {
    CategoryChip(categoria = chipPreviewCategoria, selected = false, onClick = {})
  }
}
