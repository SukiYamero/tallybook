package com.kurobello.tallybook.core.ui.navigation

import androidx.compose.runtime.Composable
import com.kurobello.tallybook.core.model.SearchTipo
import com.kurobello.tallybook.core.ui.feedback.ManagementFeedbackQueue
import com.kurobello.tallybook.feature.accounts.ui.AccountsRoute
import com.kurobello.tallybook.feature.budgets.ui.BudgetsRoute
import com.kurobello.tallybook.feature.categories.ui.CategoriesRoute
import com.kurobello.tallybook.feature.recurrentes.ui.RecurrentesRoute
import com.kurobello.tallybook.feature.tags.ui.TagsRoute
import com.kurobello.tallybook.feature.transfers.ui.recentTransferenciasContent
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferEntry

@Composable
internal fun CategoriesDestination(navigator: PrimaryNavigator) {
  CategoriesRoute(
      modals = navigator.modals,
      active = navigator.state.secondary == Categories,
      onBack = navigator::back,
      onShowCategory = { id -> navigator.showSearchFilter(categoryIds = setOf(id)) },
  )
}

@Composable
internal fun TagsDestination(navigator: PrimaryNavigator) {
  TagsRoute(
      modals = navigator.modals,
      active = navigator.state.secondary == Tags,
      onBack = navigator::back,
      onShowTag = { id -> navigator.showSearchFilter(etiquetaIds = setOf(id)) },
  )
}

@Composable
internal fun RecurrentesDestination(
    navigator: PrimaryNavigator,
    feedback: ManagementFeedbackQueue,
) {
  RecurrentesRoute(
      modals = navigator.modals,
      active = navigator.state.secondary == Recurrentes,
      onBack = navigator::back,
      feedback = feedback,
  )
}

@Composable
internal fun PresupuestosDestination(navigator: PrimaryNavigator) {
  BudgetsRoute(
      modals = navigator.modals,
      active = navigator.state.secondary == Presupuestos,
      onBack = navigator::back,
      onViewMovements = { categoriaId, moneda, range ->
        navigator.showSearchFilter(
            categoryIds = setOf(categoriaId),
            moneda = moneda,
            range = range,
            tipo = SearchTipo.GASTO,
        )
      },
  )
}

@Composable
internal fun AccountsDestination(
    navigator: PrimaryNavigator,
    openTransfer: (TransferEntry) -> Unit,
) {
  AccountsRoute(
      modals = navigator.modals,
      active = navigator.state.secondary == Accounts,
      onBack = navigator::back,
      onTransfer = openTransfer,
      onViewMovements = { cuentaId, moneda ->
        navigator.showSearchFilter(cuentaIds = setOf(cuentaId), moneda = moneda)
      },
      trailingContent =
          recentTransferenciasContent { id -> openTransfer(TransferEntry.Existing(id)) },
  )
}
