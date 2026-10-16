package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoMovimiento
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
    onClick: (() -> Unit)? = null,
    pending: Boolean = false,
) {
  val categoryLabel = categoria?.nombre ?: stringResource(Res.string.unknown_category)
  MovimientoRowContent(
      movimiento = movimiento,
      categoria = categoria,
      text =
          MovimientoRowText(
              categoryLabel,
              movimiento.descripcion?.takeIf(String::isNotBlank),
              categoryLabel,
          ),
      modifier = modifier,
      onClick = onClick,
      pending = pending,
  )
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
        categoriaIds = listOf(previewMovimientoCategoria().id),
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
