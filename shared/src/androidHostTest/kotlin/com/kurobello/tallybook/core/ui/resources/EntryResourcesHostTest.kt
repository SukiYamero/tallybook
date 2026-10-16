package com.kurobello.tallybook.core.ui.resources

import java.util.Locale
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.movimiento_amount_not_detected
import tallybook.shared.generated.resources.movimiento_detected_label
import tallybook.shared.generated.resources.movimiento_summary_title

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EntryResourcesHostTest {
  @Test
  fun reviewTitleAndDetectedLabelFollowEachLocaleVoice() = runBlocking {
    val original = Locale.getDefault()
    try {
      val cases =
          listOf(
              Triple("es-CO", "Revisa antes de guardar", "Detectado"),
              Triple("es-AR", "Revisá antes de guardar", "Detectado"),
              Triple("en-US", "Review before saving", "Detected"),
              Triple("pt-BR", "Revise antes de salvar", "Detectado"),
          )
      cases.forEach { (locale, title, detected) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        assertEquals(title, getString(Res.string.movimiento_summary_title), locale)
        assertEquals(detected, getString(Res.string.movimiento_detected_label), locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }

  @Test
  fun missingAmountSaysItWasNotDetectedInEveryLocale() = runBlocking {
    val original = Locale.getDefault()
    try {
      val cases =
          listOf(
              "es-CO" to "No detectamos el monto",
              "es-AR" to "No detectamos el monto",
              "en-US" to "We couldn’t detect the amount",
              "pt-BR" to "Não identificamos o valor",
          )
      cases.forEach { (locale, message) ->
        Locale.setDefault(Locale.forLanguageTag(locale))
        assertEquals(message, getString(Res.string.movimiento_amount_not_detected), locale)
      }
    } finally {
      Locale.setDefault(original)
    }
  }
}
