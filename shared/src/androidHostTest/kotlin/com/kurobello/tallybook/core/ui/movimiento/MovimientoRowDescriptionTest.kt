package com.kurobello.tallybook.core.ui.movimiento

import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.account.labelResource
import java.util.Locale
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_pending_label

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MovimientoRowDescriptionTest {
  private val monto = Money(5_000_000, Moneda.COP)
  private val amount = signedMovimientoAmountText(monto, TipoMovimiento.GASTO)

  private val medioLabels =
      mapOf(
          "es-CO" to
              mapOf(
                  MedioPago.EFECTIVO to "Efectivo",
                  MedioPago.DEBITO to "Débito",
                  MedioPago.TRANSFERENCIA to "Transferencia",
                  MedioPago.CREDITO to "Crédito",
              ),
          "en-US" to
              mapOf(
                  MedioPago.EFECTIVO to "Cash",
                  MedioPago.DEBITO to "Debit",
                  MedioPago.TRANSFERENCIA to "Bank transfer",
                  MedioPago.CREDITO to "Credit",
              ),
      )
  private val expenseLabels = mapOf("es-CO" to "Gasto", "en-US" to "Expense")
  private val pendingLabels = mapOf("es-CO" to "Por completar", "en-US" to "To complete")

  @Test
  fun `every medio is announced once after the amount in every locale`() = inLocales { locale ->
    MedioPago.entries.forEach { medio ->
      val description =
          movimientoRowDescription(
              MovimientoRowText("Almuerzo", "Con el equipo", "Comida"),
              amount,
              Moneda.COP,
              resolvedLabels(TipoMovimiento.GASTO, medio, pending = false),
          )

      val medioLabel = medioLabels.getValue(locale).getValue(medio)
      assertEquals(
          "Almuerzo, Con el equipo, Comida, ${expenseLabels.getValue(locale)}, $amount, COP, " +
              medioLabel,
          description,
          "$locale $medio",
      )
      assertEquals(1, Regex(Regex.escape(medioLabel)).findAll(description).count())
    }
  }

  @Test
  fun `a pending row keeps its label before the type and ends with the medio`() =
      inLocales { locale ->
        val description =
            movimientoRowDescription(
                MovimientoRowText("Otros", null, "Otros"),
                amount,
                Moneda.COP,
                resolvedLabels(TipoMovimiento.GASTO, MedioPago.CREDITO, pending = true),
            )

        assertEquals(
            "Otros, ${pendingLabels.getValue(locale)}, ${expenseLabels.getValue(locale)}, $amount, " +
                "COP, ${medioLabels.getValue(locale).getValue(MedioPago.CREDITO)}",
            description,
            locale,
        )
      }

  private suspend fun resolvedLabels(tipo: TipoMovimiento, medio: MedioPago, pending: Boolean) =
      MovimientoRowLabels(
          tipo = getString(tipo.presentation.label),
          medio = getString(medio.labelResource),
          pending = if (pending) getString(Res.string.movimiento_pending_label) else null,
      )

  private fun inLocales(block: suspend (String) -> Unit) = runBlocking {
    val original = Locale.getDefault()
    try {
      medioLabels.keys.forEach { locale ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        block(locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }
}
