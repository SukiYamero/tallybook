package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSummaryUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.PhrasePreviewUi
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_cine
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.category_otros
import tallybook.shared.generated.resources.category_transporte

/**
 * Deterministic UI models shared by previews, the UI catalog and device tests. Category names are
 * the localized seed names, so samples read like real data in every language.
 */
@Suppress("MagicNumber")
internal class MovimientoEntrySamples(
    val cinema: Categoria,
    val food: Categoria,
    val transport: Categoria,
    val other: Categoria,
) {
  private val twentyThousand = Money(2_000_000, moneda)

  val emptyPreview = PhrasePreviewUi.Empty
  val partialPreview =
      PhrasePreviewUi(
          tipo = TipoMovimiento.GASTO,
          monto = twentyThousand,
          fecha = null,
          categoria = null,
      )
  val completePreview =
      PhrasePreviewUi(
          tipo = TipoMovimiento.GASTO,
          monto = twentyThousand,
          fecha = yesterday,
          categoria = cinema,
      )
  val incomePreview =
      completePreview.copy(tipo = TipoMovimiento.INGRESO, categoria = null, fecha = today)

  val completeSummary =
      MovimientoSummaryUi(
          tipo = TipoMovimiento.GASTO,
          fecha = yesterday,
          monto = twentyThousand,
          amountNeedsReview = false,
          descripcion = SENTENCE,
          categoria = cinema,
          categoriaIsPending = false,
          suggestions = listOf(food, transport),
          isSubmitting = false,
      )
  val missingAmountSummary = completeSummary.copy(monto = null)
  val ambiguousAmountSummary = completeSummary.copy(amountNeedsReview = true)
  val pendingSummary =
      completeSummary.copy(
          categoria = other,
          categoriaIsPending = true,
          suggestions = listOf(cinema, food, transport),
      )
  val submittingSummary = completeSummary.copy(isSubmitting = true)
  val incomeSummary =
      completeSummary.copy(
          tipo = TipoMovimiento.INGRESO,
          fecha = today,
          descripcion = "Received 20.000",
      )

  companion object {
    val today = LocalDate(2026, 9, 15)
    val yesterday = LocalDate(2026, 9, 14)
    val moneda = Moneda.COP
    const val SENTENCE = "Spent 20.000 on cinema yesterday"
  }
}

@Composable
internal fun rememberMovimientoEntrySamples(): MovimientoEntrySamples {
  val cinema = stringResource(Res.string.category_cine)
  val food = stringResource(Res.string.category_comida)
  val transport = stringResource(Res.string.category_transporte)
  val other = stringResource(Res.string.category_otros)
  return remember(cinema, food, transport, other) {
    MovimientoEntrySamples(
        cinema = Categoria("cat_cine", cinema, null, CategoryIcon.FILM, CategoryTint.INFO),
        food = Categoria("cat_comida", food, null, CategoryIcon.UTENSILS, CategoryTint.AMBER),
        transport =
            Categoria("cat_transporte", transport, null, CategoryIcon.CAR, CategoryTint.BLUE),
        other =
            Categoria(
                CATEGORIA_OTROS_ID,
                other,
                null,
                CategoryIcon.CIRCLE_DASHED,
                CategoryTint.NEUTRAL,
            ),
    )
  }
}
