package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

/**
 * One movimiento of a split sentence. Like the single draft, [amountEdited] and [categoriasEdited]
 * tell a value the user changed apart from one the sentence still owns.
 */
internal data class EntryPart(
    val titulo: String,
    val amountInput: String,
    val amountEdited: Boolean = false,
    val amountNeedsReview: Boolean,
    val categoriaId: String?,
    val categoriasEdited: Boolean = false,
    val suggestionQuery: SuggestionQuery,
    val suggestions: List<Categoria> = emptyList(),
    val descripcion: String = "",
    val overrides: PartOverrides = PartOverrides(),
)

/** Values a part's form changed away from the shared ones; null keeps following them. */
internal data class PartOverrides(
    val tipo: TipoMovimiento? = null,
    val fecha: LocalDate? = null,
    val cuentaChoice: CuentaSelectionState? = null,
    val etiquetaIds: List<String>? = null,
)
