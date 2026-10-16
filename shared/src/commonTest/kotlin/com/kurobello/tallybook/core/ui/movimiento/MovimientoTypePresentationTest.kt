package com.kurobello.tallybook.core.ui.movimiento

import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.component.TallybookSegmentTone
import com.kurobello.tallybook.core.ui.theme.DarkTallybookColors
import com.kurobello.tallybook.core.ui.theme.LightTallybookColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MovimientoTypePresentationTest {
  @Test
  fun everyTypeHasAPresentation() {
    TipoMovimiento.entries.forEach { tipo -> tipo.presentation }
  }

  @Test
  fun typesUseTheirStatusTone() {
    assertEquals(TallybookSegmentTone.Expense, TipoMovimiento.GASTO.presentation.tone)
    assertEquals(TallybookSegmentTone.Income, TipoMovimiento.INGRESO.presentation.tone)
  }

  @Test
  fun typesResolveToTheDangerAndSuccessAccents() {
    listOf(LightTallybookColors, DarkTallybookColors).forEach { colors ->
      assertEquals(colors.status.danger.base, TipoMovimiento.GASTO.presentation.accent(colors))
      assertEquals(colors.status.success.base, TipoMovimiento.INGRESO.presentation.accent(colors))
    }
  }

  @Test
  fun typesDifferInIconAndLabel() {
    val expense = TipoMovimiento.GASTO.presentation
    val income = TipoMovimiento.INGRESO.presentation

    assertNotEquals(expense.icon, income.icon)
    assertNotEquals(expense.label, income.label)
  }
}
