package com.kurobello.tallybook.feature.budgets.viewmodel

import com.kurobello.tallybook.core.model.BudgetMonth
import com.kurobello.tallybook.core.model.BudgetStatus
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Presupuesto
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import kotlinx.datetime.YearMonth

internal data class BudgetsUiState(
    val content: RetainedLoadState<BudgetsContent, DataError> = RetainedLoadState.Loading(),
    val editor: BudgetEditorState? = null,
    val detail: BudgetDetailState? = null,
    val categorias: List<Categoria> = emptyList(),
    /** Null until preferences load; rows show their currency code only outside it. */
    val primaryMoneda: Moneda? = null,
)

internal data class BudgetsContent(
    val statuses: List<BudgetStatus>,
    val fineCount: Int,
    val daysLeft: Int,
    val month: YearMonth,
)

/**
 * [editingId] is null while creating; the categoria and moneda of an existing budget are locked.
 */
internal data class BudgetEditorState(
    val editingId: String?,
    val categoriaId: String?,
    val moneda: Moneda,
    val monedas: List<Moneda>,
    val amountInput: String,
    val suggestion: Money?,
    val amountError: Boolean,
    val duplicateOf: Presupuesto?,
    val saving: Boolean,
    val saveFailed: Boolean = false,
)

/** [history] is null until the six months load, or for good when [historyFailed]. */
internal data class BudgetDetailState(
    val status: BudgetStatus,
    val history: List<BudgetMonth>?,
    val confirmingDelete: Boolean,
    val deleteFailed: Boolean = false,
    val historyFailed: Boolean = false,
)
