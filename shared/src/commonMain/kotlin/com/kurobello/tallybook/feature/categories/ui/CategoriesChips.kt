package com.kurobello.tallybook.feature.categories.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.ui.category.CategoryChip
import com.kurobello.tallybook.core.ui.component.TallybookText
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.categories.viewmodel.CategoryRow
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_chip_hidden_state
import tallybook.shared.generated.resources.category_movimiento_count
import tallybook.shared.generated.resources.category_no_movimientos

private const val HIDDEN_CATEGORY_ALPHA = 0.55f

@Composable
internal fun CategoryChipBlock(rows: List<CategoryRow>, onOpen: (String) -> Unit) {
  FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
  ) {
    rows.forEach { row -> key(row.categoria.id) { CategoriesChip(row, onOpen) } }
  }
}

/** A chip that opens its categoria's sheet; the count and hidden mark are read as its state. */
@Composable
private fun CategoriesChip(row: CategoryRow, onOpen: (String) -> Unit) {
  val categoria = row.categoria
  val count = movimientoCountLabel(row.count)
  val state =
      if (categoria.oculta) stringResource(Res.string.category_chip_hidden_state, count) else count
  CategoryChip(
      categoria = categoria,
      selected = false,
      onClick = { onOpen(categoria.id) },
      modifier =
          Modifier.alpha(if (categoria.oculta) HIDDEN_CATEGORY_ALPHA else 1f).semantics {
            stateDescription = state
          },
      trailingContent = { CategoryChipTrailing(row) },
  )
}

@Composable
private fun CategoryChipTrailing(row: CategoryRow) {
  val color = TallybookTheme.colors.foreground.tertiary
  Row(
      modifier = Modifier.clearAndSetSemantics {},
      horizontalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space4),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (row.categoria.oculta) {
      Image(
          imageVector = Lucide.EyeOff,
          contentDescription = null,
          modifier = Modifier.size(TallybookTheme.dimensions.iconSmall),
          colorFilter = ColorFilter.tint(color),
      )
    }
    TallybookText(
        row.count.toString(),
        color = color,
        style = TallybookTheme.typography.controlValue,
    )
  }
}

@Composable
internal fun movimientoCountLabel(count: Long): String =
    if (count == 0L) stringResource(Res.string.category_no_movimientos)
    else
        pluralStringResource(
            Res.plurals.category_movimiento_count,
            count.toPluralQuantity(),
            count,
        )

/** Plural selection takes an Int; counts beyond it all read as "other". */
internal fun Long.toPluralQuantity(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
