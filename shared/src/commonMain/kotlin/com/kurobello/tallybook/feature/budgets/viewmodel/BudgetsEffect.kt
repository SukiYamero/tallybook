package com.kurobello.tallybook.feature.budgets.viewmodel

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.MovimientoDateRange

internal sealed interface BudgetsEffect {
  data class OpenSearch(
      val categoriaId: String,
      val moneda: Moneda,
      val range: MovimientoDateRange,
  ) : BudgetsEffect
}
