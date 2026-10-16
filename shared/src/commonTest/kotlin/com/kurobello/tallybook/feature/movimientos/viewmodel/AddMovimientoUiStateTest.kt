package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class AddMovimientoUiStateTest {

  @Test
  fun summaryStaysAvailableWhenTheCatalogHasNoOtrosFallback() {
    val cine = Categoria("cat_cine", "Cine", "cat_ocio", CategoryIcon.FILM, CategoryTint.INFO)
    val state =
        AddMovimientoUiState(
            tipo = TipoMovimiento.GASTO,
            amountInput = "20000",
            fecha = LocalDate(2026, 10, 16),
            moneda = Moneda.COP,
            categorias = listOf(cine),
            categoriaId = null,
            titulo = "Gasté 20000",
            descripcion = "",
            isLoading = false,
            isSubmitting = false,
            submitAttempted = false,
            error = null,
            step = AddMovimientoStep.SUMMARY,
        )

    val summary = state.summary

    assertNull(summary.categoria)
    assertFalse(summary.categoriaIsPending)
    assertEquals("Gasté 20000", summary.titulo)
  }
}
