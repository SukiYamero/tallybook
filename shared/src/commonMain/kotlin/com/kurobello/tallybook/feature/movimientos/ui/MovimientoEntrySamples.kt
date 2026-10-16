package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSummaryUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.PhrasePreviewUi
import kotlinx.datetime.LocalDate

/** Deterministic UI models shared by previews, the UI catalog and device tests. */
@Suppress("MagicNumber")
internal object MovimientoEntrySamples {
  val today = LocalDate(2026, 9, 15)
  val yesterday = LocalDate(2026, 9, 14)
  val moneda = Moneda.COP

  val cinema = Categoria("sample-cinema", "Cinema", null, CategoryIcon.FILM, CategoryTint.PURPLE)
  val food = Categoria("sample-food", "Food", null, CategoryIcon.UTENSILS, CategoryTint.AMBER)
  val transport =
      Categoria("sample-transport", "Transport", null, CategoryIcon.BUS, CategoryTint.BLUE)
  val other = Categoria("sample-other", "Other", null, CategoryIcon.PACKAGE, CategoryTint.INFO)

  private val twentyThousand = Money(2_000_000, moneda)
  private const val SENTENCE = "Spent 20.000 on cinema yesterday"

  val emptyPreview = PhrasePreviewUi(tipo = null, monto = null, fecha = null, categoria = null)
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
}
