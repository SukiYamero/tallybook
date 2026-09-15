package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleQuestionMark
import com.composables.icons.lucide.Lucide
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.category.CategoryAvatar
import com.kurobello.tallybook.core.ui.category.neutralCategoryTintColors
import com.kurobello.tallybook.core.ui.theme.LocalTallybookColors
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.unknown_category

@Composable
fun MovimientoRow(
    movimiento: Movimiento,
    categoria: Categoria?,
    modifier: Modifier = Modifier,
) {
  val amountText = movimiento.signedAmountText()
  val amountColor =
      when (movimiento.tipo) {
        TipoMovimiento.INGRESO -> LocalTallybookColors.current.status.success.base
        TipoMovimiento.GASTO -> MaterialTheme.colorScheme.onSurface
      }
  Surface(
      modifier = modifier.semantics(mergeDescendants = true) {},
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      border = BorderStroke(1.dp, LocalTallybookColors.current.border.subtle),
  ) {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      if (categoria == null) {
        CategoryAvatar(
            imageVector = Lucide.CircleQuestionMark,
            colors = neutralCategoryTintColors(),
        )
      } else {
        CategoryAvatar(categoria)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = categoria?.nombre ?: stringResource(Res.string.unknown_category),
            style = MaterialTheme.typography.titleSmall,
        )
        movimiento.descripcion?.takeIf(String::isNotBlank)?.let { description ->
          Text(
              text = description,
              color = LocalTallybookColors.current.foreground.tertiary,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
          )
        }
      }
      Text(
          text = amountText,
          color = amountColor,
          style = MaterialTheme.typography.titleMedium,
      )
    }
  }
}

private fun Movimiento.signedAmountText(): String {
  val signedMonto =
      when (tipo) {
        TipoMovimiento.INGRESO -> monto
        TipoMovimiento.GASTO -> -monto
      }
  val formatted = signedMonto.format()
  if (tipo != TipoMovimiento.INGRESO || monto.minorUnits == 0L) return formatted
  val prefix = monto.moneda.symbol + monto.moneda.symbolSeparator
  return prefix + "+" + formatted.removePrefix(prefix)
}

@Preview
@Composable
internal fun MovimientoRowLightPreview() {
  TallybookTheme(darkTheme = false) {
    MovimientoRow(
        movimiento = previewMovimiento(TipoMovimiento.GASTO),
        categoria = previewMovimientoCategoria(),
        modifier = Modifier.widthIn(max = 420.dp),
    )
  }
}

@Preview
@Composable
internal fun MovimientoRowDarkPreview() {
  TallybookTheme(darkTheme = true) {
    MovimientoRow(
        movimiento = previewMovimiento(TipoMovimiento.INGRESO),
        categoria = null,
        modifier = Modifier.widthIn(max = 420.dp),
    )
  }
}

@Suppress("MagicNumber")
@Composable
private fun previewMovimiento(tipo: TipoMovimiento) =
    Movimiento(
        id = "mov-preview",
        tipo = tipo,
        monto = Money(128_500, Moneda.COP),
        fecha = LocalDate(2026, 9, 15),
        categoriaId = previewMovimientoCategoria().id,
        descripcion = null,
        createdAt = Instant.fromEpochMilliseconds(1_757_894_400_000),
    )

@Composable
private fun previewMovimientoCategoria() =
    Categoria(
        id = "cat_comida",
        nombre = stringResource(Res.string.category_comida),
        padreId = null,
        icono = CategoryIcon.UTENSILS,
        color = CategoryTint.AMBER,
    )
