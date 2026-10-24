package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.text.AmountInputError
import com.kurobello.tallybook.core.text.AmountInputResult
import com.kurobello.tallybook.core.text.parseAmountInput
import kotlinx.datetime.LocalDate

internal data class EditMovimientoUiState(
    val original: Movimiento,
    val draft: MovimientoDraft,
    override val categorias: List<Categoria>,
    override val maxSelectableDate: LocalDate,
    override val etiquetas: List<Etiqueta> = emptyList(),
    override val etiquetasLoad: EtiquetasLoad = EtiquetasLoad.LOADING,
    override val isLoading: Boolean = false,
    override val isSubmitting: Boolean = false,
    override val submitAttempted: Boolean = false,
    override val error: MovimientoFormError? = null,
    val validationDate: LocalDate = maxSelectableDate,
    override val failedEtiquetaNombre: String? = null,
    val cuentas: List<Cuenta> = emptyList(),
    val cuentaRejected: Boolean = false,
) : MovimientoFormState {
  /**
   * The stored currency never changes; its cuenta stays offered, archived or not, while untouched.
   */
  override val cuentaField: MovimientoCuentaUi by lazy {
    movimientoCuentaUi(
        selection = CuentaSelectionState(draft.medio, draft.cuentaId, SelectionOwner.USER),
        cuentas = cuentas,
        tipo = tipo,
        submitAttempted = submitAttempted,
        cuentaRejected = cuentaRejected,
        moneda = moneda,
        kept =
            cuentas
                .firstOrNull { it.id == original.cuentaId }
                ?.takeIf { draft.cuentaId == original.cuentaId },
    )
  }

  override val tipo
    get() = draft.tipo

  override val amountInput
    get() = draft.amountInput

  override val fecha
    get() = draft.fecha

  override val moneda
    get() = original.monto.moneda

  override val categoriaId
    get() = draft.categoriaId

  override val etiquetaIds
    get() = draft.etiquetaIds

  override val titulo
    get() = draft.titulo

  override val descripcion
    get() = draft.descripcion

  override val amountError: AmountInputError?
    get() =
        (parseAmountInput(amountInput, moneda) as? AmountInputResult.Invalid)?.reason?.takeIf {
          submitAttempted
        }

  val hasValidCategoria: Boolean
    get() = categoriaId.let { id ->
      id != null && (id == original.categoriaId || categorias.any { it.id == id })
    }

  override val isCategoriaMissing
    get() = submitAttempted && !hasValidCategoria

  override val isFechaInvalid
    get() = submitAttempted && fecha > validationDate
}
