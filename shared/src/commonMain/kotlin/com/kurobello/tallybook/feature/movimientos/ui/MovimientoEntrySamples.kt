package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.feature.movimientos.viewmodel.EtiquetasLoad
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoCuentaUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSentenceUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoSummaryUi
import com.kurobello.tallybook.feature.movimientos.viewmodel.PhrasePreviewUi
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.category_cine
import tallybook.shared.generated.resources.category_comida
import tallybook.shared.generated.resources.category_hogar
import tallybook.shared.generated.resources.category_otros
import tallybook.shared.generated.resources.category_regalo
import tallybook.shared.generated.resources.category_salud
import tallybook.shared.generated.resources.category_transporte

/**
 * Deterministic UI models shared by previews, the UI catalog and device tests. Category names are
 * the localized seed names, so samples read like real data in every language; tag names are user
 * data and never translated.
 */
@Suppress("MagicNumber", "LongParameterList")
internal class MovimientoEntrySamples(
    val cinema: Categoria,
    val food: Categoria,
    val transport: Categoria,
    val other: Categoria,
    val health: Categoria,
    val home: Categoria,
    val gifts: Categoria,
) {
  private val twentyThousand = Money(2_000_000, moneda)

  val all = listOf(cinema, food, transport, other, health, home, gifts)

  val etiquetas =
      listOf("Empresa", "Viaje Chile", "Casa", "Regalos", "Salud", "Auto").map { nombre ->
        Etiqueta("tag_$nombre", nombre, Instant.parse("2026-01-01T00:00:00Z"))
      }
  val business = etiquetas.first()

  val cash = sampleCuenta(Cuenta.defaultCashId(moneda), "Efectivo", TipoCuenta.EFECTIVO)
  val nequi = sampleCuenta("cuenta_nequi", "Nequi", TipoCuenta.BANCO)
  val nu = sampleCuenta("cuenta_nu", "NU", TipoCuenta.CREDITO)
  val visa = sampleCuenta("cuenta_visa", "Visa", TipoCuenta.CREDITO, Moneda.USD)
  val cuentas = listOf(cash, nequi, nu, visa)

  val cashCuenta =
      MovimientoCuentaUi(
          medio = MedioPago.EFECTIVO,
          medioOptions = MedioPago.entries,
          selected = cash,
          options = listOf(cash),
          primaryMoneda = moneda,
      )
  val creditCuenta =
      cashCuenta.copy(
          medio = MedioPago.CREDITO,
          selected = nu,
          options = listOf(nu, visa),
          suggestions = listOf(nequi),
      )
  val incomeCuenta =
      cashCuenta.copy(medioOptions = MedioPago.entries.filter { it.allows(TipoMovimiento.INGRESO) })
  val emptyCuenta =
      cashCuenta.copy(medio = MedioPago.DEBITO, selected = null, options = emptyList())

  val emptySentence =
      MovimientoSentenceUi(
          text = "",
          detected = PhrasePreviewUi(TipoMovimiento.GASTO, monto = null, fecha = today),
          today = today,
          moneda = moneda,
          categoria = other,
          categoriaIsPending = true,
          suggestions = emptyList(),
          etiquetas = emptyList(),
          etiquetasLoad = EtiquetasLoad.READY,
          cuenta = cashCuenta,
          isEnabled = true,
          actionsEnabled = true,
      )
  val partialSentence =
      emptySentence.copy(
          text = "Spent 20.000",
          detected = emptySentence.detected.copy(monto = twentyThousand),
      )
  val completeSentence =
      partialSentence.copy(
          text = SENTENCE,
          detected = PhrasePreviewUi(TipoMovimiento.GASTO, twentyThousand, yesterday),
          categoria = cinema,
          categoriaIsPending = false,
          suggestions = listOf(food, transport),
          etiquetas = listOf(business),
          cuenta = creditCuenta,
      )
  val incomeSentence =
      completeSentence.copy(
          text = "Received 20.000",
          detected = PhrasePreviewUi(TipoMovimiento.INGRESO, twentyThousand, today),
          cuenta = incomeCuenta,
      )

  val completeSummary =
      MovimientoSummaryUi(
          tipo = TipoMovimiento.GASTO,
          fecha = yesterday,
          monto = twentyThousand,
          amountNeedsReview = false,
          titulo = SENTENCE,
          categoria = cinema,
          categoriaIsPending = false,
          suggestions = listOf(food, transport),
          etiquetas = listOf(business),
          etiquetasLoad = EtiquetasLoad.READY,
          cuenta = creditCuenta,
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
  val fullSummary = completeSummary.copy(etiquetas = etiquetas)
  val submittingSummary = completeSummary.copy(isSubmitting = true)
  val incomeSummary =
      completeSummary.copy(
          tipo = TipoMovimiento.INGRESO,
          fecha = today,
          titulo = "Received 20.000",
          cuenta = incomeCuenta,
      )

  companion object {
    private fun sampleCuenta(
        id: String,
        nombre: String,
        tipo: TipoCuenta,
        moneda: Moneda = this.moneda,
    ) =
        Cuenta(
            id,
            nombre,
            tipo,
            Money(0, moneda),
            null,
            false,
            Instant.parse("2026-01-01T00:00:00Z"),
            color = tipo.defaultColor,
        )

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
  val health = stringResource(Res.string.category_salud)
  val home = stringResource(Res.string.category_hogar)
  val gifts = stringResource(Res.string.category_regalo)
  return remember(cinema, food, transport, other, health, home, gifts) {
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
        health = Categoria("cat_salud", health, null, CategoryIcon.HEART_PULSE, CategoryTint.ROSE),
        home = Categoria("cat_hogar", home, null, CategoryIcon.HOUSE, CategoryTint.EMERALD),
        gifts = Categoria("cat_regalo", gifts, null, CategoryIcon.GIFT, CategoryTint.PURPLE),
    )
  }
}
