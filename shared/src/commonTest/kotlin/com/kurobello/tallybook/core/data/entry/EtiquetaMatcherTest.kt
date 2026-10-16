package com.kurobello.tallybook.core.data.entry

import com.kurobello.tallybook.core.model.Etiqueta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class EtiquetaMatcherTest {
  private val matcher = EtiquetaMatcher()

  @Test
  fun aTagNameMatchesIgnoringCaseAndAccents() {
    val catalog = etiquetas("Café", "EMPRESA")

    assertEquals(listOf("Café"), matcher.match("Pagué 5 mil en el CAFE", catalog))
    assertEquals(listOf("EMPRESA"), matcher.match("almuerzo empresa", catalog))
  }

  @Test
  fun aMultiWordNameMatchesOnlyWhenItsWordsAppearTogetherInOrder() {
    val catalog = etiquetas("Viaje Chile")

    assertEquals(listOf("Viaje Chile"), matcher.match("Hotel viaje, chile 200 mil", catalog))
    assertEquals(emptyList(), matcher.match("Chile viaje", catalog))
    assertEquals(emptyList(), matcher.match("Viaje a Chile", catalog))
  }

  @Test
  fun aNameInsideALongerWordDoesNotMatch() {
    val catalog = etiquetas("Casa", "Auto 2")

    assertEquals(emptyList(), matcher.match("Arriendo casamiento", catalog))
    assertEquals(emptyList(), matcher.match("Autos 20 mil", catalog))
    assertEquals(listOf("Auto 2"), matcher.match("Gasolina auto 2", catalog))
  }

  @Test
  fun matchesFollowTheirOrderInTheSentence() {
    val catalog = etiquetas("Empresa", "Viaje", "Casa")

    assertEquals(
        listOf("Viaje", "Empresa"),
        matcher.match("viaje de la empresa, 30 mil", catalog),
    )
    assertEquals(emptyList(), matcher.match("   ", catalog))
  }

  private fun etiquetas(vararg nombres: String) = nombres.map {
    Etiqueta(it, it, Instant.parse("2026-01-01T00:00:00Z"))
  }
}
