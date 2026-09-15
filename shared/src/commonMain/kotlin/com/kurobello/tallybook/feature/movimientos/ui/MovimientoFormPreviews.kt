@file:Suppress("UnusedPrivateMember")

package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoUiState
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoFormError
import kotlinx.datetime.LocalDate

@Preview
@Composable
private fun MovimientoFormEmptyPreview() = MovimientoFormPreview(previewUiState())

@Preview
@Composable
private fun MovimientoFormValidationPreview() =
    MovimientoFormPreview(previewUiState(submitAttempted = true))

@Preview
@Composable
private fun MovimientoFormLoadingPreview() = MovimientoFormPreview(previewUiState(isLoading = true))

@Preview
@Composable
private fun MovimientoFormFailurePreview() =
    MovimientoFormPreview(previewUiState(error = MovimientoFormError.SAVE_FAILED))

@Preview
@Composable
private fun MovimientoFormSubmittingPreview() =
    MovimientoFormPreview(previewUiState(isSubmitting = true))

@Composable
private fun MovimientoFormPreview(uiState: AddMovimientoUiState) {
  TallybookTheme { MovimientoForm(uiState = uiState, actions = PreviewAddMovimientoActions) }
}

private fun previewUiState(
    isLoading: Boolean = false,
    isSubmitting: Boolean = false,
    submitAttempted: Boolean = false,
    error: MovimientoFormError? = null,
) =
    AddMovimientoUiState(
        tipo = TipoMovimiento.GASTO,
        amountInput = "",
        fecha = LocalDate.parse("2026-09-15"),
        moneda = Moneda.COP,
        categorias = emptyList(),
        selectedCategoriaId = null,
        descripcion = "",
        isLoading = isLoading,
        isSubmitting = isSubmitting,
        submitAttempted = submitAttempted,
        error = error,
    )

private object PreviewAddMovimientoActions : AddMovimientoActions {
  override fun onTipoChanged(tipo: TipoMovimiento) = Unit

  override fun onAmountChanged(value: String) = Unit

  override fun onFechaChanged(value: LocalDate) = Unit

  override fun onCategoriaSelected(id: String) = Unit

  override fun onDescripcionChanged(value: String) = Unit

  override fun onRetryLoad() = Unit

  override fun onSubmit() = Unit
}
