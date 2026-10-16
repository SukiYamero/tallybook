package com.kurobello.tallybook.core.ui.catalog

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

internal data class UiCatalogFixtures(
    val categoria: Categoria,
    val movimiento: Movimiento,
    val summary: MovimientoSummary,
)

@Suppress("MagicNumber")
internal fun uiCatalogFixtures(categoryName: String): UiCatalogFixtures {
  val categoria =
      Categoria(
          id = "catalog-food",
          nombre = categoryName,
          padreId = null,
          icono = CategoryIcon.UTENSILS,
          color = CategoryTint.AMBER,
      )
  val movimiento =
      Movimiento(
          id = "catalog-expense",
          tipo = TipoMovimiento.GASTO,
          monto = Money(128_500, Moneda.COP),
          fecha = LocalDate(2026, 9, 15),
          categoriaIds = listOf(categoria.id),
          descripcion = null,
          createdAt = Instant.parse("2026-09-15T00:00:00Z"),
      )
  return UiCatalogFixtures(
      categoria = categoria,
      movimiento = movimiento,
      summary =
          MovimientoSummary(
              ingresos = Money(2_750_000, Moneda.COP),
              gastos = Money(1_225_000, Moneda.COP),
          ),
  )
}
