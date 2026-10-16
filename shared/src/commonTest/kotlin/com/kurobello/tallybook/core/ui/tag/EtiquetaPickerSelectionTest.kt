package com.kurobello.tallybook.core.ui.tag

import com.kurobello.tallybook.core.model.Etiqueta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class EtiquetaPickerSelectionTest {

  @Test
  fun toggleAppendsAnUnselectedTagAfterTheCurrentOnes() {
    assertEquals(listOf("trip", "work"), toggledEtiquetaIds(listOf("trip"), "work", max = 6))
  }

  @Test
  fun toggleRemovesASelectedTagEvenWhenItIsTheLastOne() {
    assertEquals(listOf("work"), toggledEtiquetaIds(listOf("trip", "work"), "trip", max = 6))
    assertEquals(emptyList<String>(), toggledEtiquetaIds(listOf("trip"), "trip", max = 6))
  }

  @Test
  fun toggleIgnoresAnAdditionAtTheLimitButStillRemoves() {
    assertEquals(listOf("a", "b"), toggledEtiquetaIds(listOf("a", "b"), "c", max = 2))
    assertEquals(listOf("b"), toggledEtiquetaIds(listOf("a", "b"), "a", max = 2))
  }

  @Test
  fun searchMatchesIgnoringCaseAndAccentsAndABlankQueryKeepsEveryTag() {
    val tags = listOf(etiqueta("Viaje Chile"), etiqueta("Empresa"), etiqueta("Médico"))

    assertEquals(listOf("Viaje Chile"), searchEtiquetas(tags, "chile").map { it.nombre })
    assertEquals(listOf("Médico"), searchEtiquetas(tags, " MEDI ").map { it.nombre })
    assertEquals(tags, searchEtiquetas(tags, "  "))
  }

  private fun etiqueta(nombre: String) =
      Etiqueta(nombre.lowercase(), nombre, Instant.parse("2026-01-01T00:00:00Z"))
}
