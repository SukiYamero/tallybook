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
  private data class Labels(val tipos: List<String>, val medios: List<String>)

  private val expected =
      mapOf(
          "es-CO" to
              Labels(
                  listOf("Efectivo", "Banco o billetera", "Tarjeta de crédito"),
                  listOf("Efectivo", "Débito", "Transferencia", "Crédito"),
              ),
          "es-AR" to
              Labels(
                  listOf("Efectivo", "Banco o billetera virtual", "Tarjeta de crédito"),
                  listOf("Efectivo", "Débito", "Transferencia", "Crédito"),
              ),
          "en-US" to
              Labels(
                  listOf("Cash", "Bank or wallet", "Credit card"),
                  listOf("Cash", "Debit", "Bank transfer", "Credit"),
              ),
          "pt-BR" to
              Labels(
                  listOf("Dinheiro", "Banco ou carteira digital", "Cartão de crédito"),
                  listOf("Dinheiro", "Débito", "Transferência", "Crédito"),
              ),
      )

  @Test
  fun secondStringsFileResolvesThroughTheGeneratedAccessorInEveryLocale() = withLocales {
    expected.forEach { (locale, labels) ->
      Locale.setDefault(Locale.forLanguageTag(locale))
      assertEquals(labels.tipos[TipoCuenta.BANCO.ordinal], getString(Res.string.account_type_bank))
    }
  }

  @Test
  fun everyTipoCuentaAndMedioPagoHasItsLocalizedLabel() = withLocales {
    expected.forEach { (locale, labels) ->
      Locale.setDefault(Locale.forLanguageTag(locale))
      assertEquals(labels.tipos, TipoCuenta.entries.map { getString(it.labelResource) }, locale)
      assertEquals(labels.medios, MedioPago.entries.map { getString(it.labelResource) }, locale)
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
