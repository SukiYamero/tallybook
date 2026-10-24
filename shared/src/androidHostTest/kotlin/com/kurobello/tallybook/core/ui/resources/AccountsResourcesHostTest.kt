package com.kurobello.tallybook.core.ui.resources

import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.TipoCuenta
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
import tallybook.shared.generated.resources.account_type_bank

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AccountsResourcesHostTest {
  private data class Labels(val tipos: Map<TipoCuenta, String>, val medios: Map<MedioPago, String>)

  private val expected =
      mapOf(
          "es-CO" to
              Labels(
                  tipos =
                      mapOf(
                          TipoCuenta.EFECTIVO to "Efectivo",
                          TipoCuenta.BANCO to "Banco o billetera",
                          TipoCuenta.CREDITO to "Tarjeta de crédito",
                      ),
                  medios =
                      mapOf(
                          MedioPago.EFECTIVO to "Efectivo",
                          MedioPago.DEBITO to "Débito",
                          MedioPago.TRANSFERENCIA to "Transferencia",
                          MedioPago.CREDITO to "Crédito",
                      ),
              ),
          "es-AR" to
              Labels(
                  tipos =
                      mapOf(
                          TipoCuenta.EFECTIVO to "Efectivo",
                          TipoCuenta.BANCO to "Banco o billetera virtual",
                          TipoCuenta.CREDITO to "Tarjeta de crédito",
                      ),
                  medios =
                      mapOf(
                          MedioPago.EFECTIVO to "Efectivo",
                          MedioPago.DEBITO to "Débito",
                          MedioPago.TRANSFERENCIA to "Transferencia",
                          MedioPago.CREDITO to "Crédito",
                      ),
              ),
          "en-US" to
              Labels(
                  tipos =
                      mapOf(
                          TipoCuenta.EFECTIVO to "Cash",
                          TipoCuenta.BANCO to "Bank or wallet",
                          TipoCuenta.CREDITO to "Credit card",
                      ),
                  medios =
                      mapOf(
                          MedioPago.EFECTIVO to "Cash",
                          MedioPago.DEBITO to "Debit",
                          MedioPago.TRANSFERENCIA to "Bank transfer",
                          MedioPago.CREDITO to "Credit",
                      ),
              ),
          "pt-BR" to
              Labels(
                  tipos =
                      mapOf(
                          TipoCuenta.EFECTIVO to "Dinheiro",
                          TipoCuenta.BANCO to "Banco ou carteira digital",
                          TipoCuenta.CREDITO to "Cartão de crédito",
                      ),
                  medios =
                      mapOf(
                          MedioPago.EFECTIVO to "Dinheiro",
                          MedioPago.DEBITO to "Débito",
                          MedioPago.TRANSFERENCIA to "Transferência",
                          MedioPago.CREDITO to "Crédito",
                      ),
              ),
      )

  @Test
  fun secondStringsFileResolvesThroughTheGeneratedAccessorInEveryLocale() = withLocales {
    expected.forEach { (locale, labels) ->
      Locale.setDefault(Locale.forLanguageTag(locale))
      assertEquals(labels.tipos.getValue(TipoCuenta.BANCO), getString(Res.string.account_type_bank))
    }
  }

  @Test
  fun everyTipoCuentaAndMedioPagoHasItsLocalizedLabel() = withLocales {
    expected.forEach { (locale, labels) ->
      Locale.setDefault(Locale.forLanguageTag(locale))
      assertEquals(
          labels.tipos,
          TipoCuenta.entries.associateWith { getString(it.labelResource) },
          locale,
      )
      assertEquals(
          labels.medios,
          MedioPago.entries.associateWith { getString(it.labelResource) },
          locale,
      )
    }
  }

  private fun withLocales(block: suspend () -> Unit) = runBlocking {
    val original = Locale.getDefault()
    try {
      block()
    } finally {
      Locale.setDefault(original)
    }
  }
}
