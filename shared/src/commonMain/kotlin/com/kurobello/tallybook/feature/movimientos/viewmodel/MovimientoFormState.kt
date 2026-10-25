package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.EtiquetaWrite
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.text.AmountInputError
import kotlinx.datetime.LocalDate

/** Tags are optional, so their catalog loads apart from what blocks entry. */
internal enum class EtiquetasLoad {
  LOADING,
  READY,
  UNAVAILABLE,
}

internal interface MovimientoFormState {
  val tipo: TipoMovimiento
  val amountInput: String
  val fecha: LocalDate
  val moneda: Moneda
  val categorias: List<Categoria>
  val categoriaId: String?
  val etiquetas: List<Etiqueta>
  val etiquetasLoad: EtiquetasLoad
  val etiquetaIds: List<String>
  val titulo: String
  val descripcion: String
  val isLoading: Boolean
  val isSubmitting: Boolean
  val submitAttempted: Boolean
  val error: MovimientoFormError?
  val amountError: AmountInputError?
  val isCategoriaMissing: Boolean
  val cuentaField: MovimientoCuentaUi

  /** The category the form presents, which is the Otros fallback while none is chosen. */
  val displayedCategoriaId: String?
    get() = categoriaId

  /** Categories offered as a switch next to the selected one. */
  val suggestions: List<Categoria>
    get() = emptyList()

  /** The chosen tags the catalog still has, in display order; a deleted tag drops out. */
  val selectedEtiquetas: List<Etiqueta>
    get() = etiquetaIds.mapNotNull { id -> etiquetas.firstOrNull { it.id == id } }

  /** The name of the last tag the picker failed to create, until another create starts. */
  val failedEtiquetaNombre: String?
    get() = null

  val maxSelectableDate: LocalDate?
    get() = null

  val isFechaInvalid: Boolean
    get() = false
}

/** Otros stands for "no category", so choosing it explicitly would hide a pending draft. */
internal fun isSelectableCategoria(id: String, catalog: List<Categoria>): Boolean =
    id != CATEGORIA_OTROS_ID && catalog.any { it.id == id }

/** A picker selection without repeats, cut at the per-movimiento limit. */
internal fun List<String>.toEtiquetaSelection(): List<String> =
    distinct().take(MAX_ETIQUETAS_POR_MOVIMIENTO)

/** Selects the tag a create produced, or the existing one it duplicated, while the limit allows. */
internal fun List<String>.withCreated(write: EtiquetaWrite): List<String> {
  val id =
      when (write) {
        is EtiquetaWrite.Saved -> write.etiqueta.id
        is EtiquetaWrite.Duplicate -> write.existing.id
      }
  return if (id in this || size >= MAX_ETIQUETAS_POR_MOVIMIENTO) this else this + id
}
