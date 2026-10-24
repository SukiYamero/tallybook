package com.kurobello.tallybook.core.ui.movimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.MedioPago
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
              movimiento.rowTitle(categoryLabel),
              movimiento.rowDescription,
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
        movimiento = previewMovimiento(TipoMovimiento.INGRESO, MedioPago.TRANSFERENCIA),
        categoria = null,
        modifier = Modifier.widthIn(max = 420.dp),
    )
  }
}

@Preview
@Composable
internal fun MovimientoRowLargeFontPreview() {
  val density = LocalDensity.current
  CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
    TallybookTheme(darkTheme = false) {
      Column(verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8)) {
        listOf(MedioPago.DEBITO, MedioPago.CREDITO).forEach { medio ->
          MovimientoRow(
              movimiento = previewMovimiento(TipoMovimiento.GASTO, medio),
              categoria = previewMovimientoCategoria(),
              modifier = Modifier.widthIn(max = 420.dp),
          )
        }
      }
    }
  }
}

@Suppress("MagicNumber")
@Composable
private fun previewMovimiento(tipo: TipoMovimiento, medio: MedioPago = MedioPago.EFECTIVO) =
    Movimiento(
        id = "mov-preview",
        tipo = tipo,
        monto = Money(128_500, Moneda.COP),
        fecha = LocalDate(2026, 9, 15),
        categoriaId = previewMovimientoCategoria().id,
        titulo = null,
        descripcion = null,
        createdAt = Instant.fromEpochMilliseconds(1_757_894_400_000),
        cuentaId = Cuenta.defaultCashId(Moneda.COP),
        medio = medio,
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
