package com.kurobello.tallybook.feature.budgets.viewmodel

import com.kurobello.tallybook.core.model.Moneda

@Suppress("TooManyFunctions")
internal interface BudgetsActions {
  fun onCreate()

  fun onOpen(id: String)

  fun onCategorySelected(id: String)

  fun onMonedaSelected(moneda: Moneda)

  fun onAmountChanged(text: String)

  fun onUseSuggestion()

  fun onSave()

  fun onDismissEditor()

  fun onEditAmount()

  fun onRequestDelete()

  fun onConfirmDelete()

  fun onDismissDetail()

  fun onViewMovements()

  fun onRetry()
}
