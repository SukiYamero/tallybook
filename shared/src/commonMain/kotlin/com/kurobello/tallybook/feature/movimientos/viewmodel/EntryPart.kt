package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

/**
 * One movimiento of a split sentence. Like the single draft, [amountEdited] and [categoriasEdited]
 * tell a value the user changed apart from one the sentence still owns, and [medioEdited] and
 * [cuentaEdited] a pick made in the part's form. [segment] is the part's text in the sentence: its
 * amount is always read from there, so editing [titulo] never moves it.
 */
internal data class EntryPart(
    val segment: String,
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
    val medioEdited: Boolean = false,
    val cuentaEdited: Boolean = false,
    val snapshot: ParteSnapshot? = null,
)

/**
 * What Join or removing down to one part replaced in the draft, given back when Back returns to the
 * sentence: the single sentence's category and the parts whose hand picks a new Continue keeps.
 * [summaryCategoriaId] and [summaryCategoriasEdited] are what the summary started with, so a
 * category the user then picks there is not undone.
 */
internal data class SplitStash(
    val categoriaId: String?,
    val categoriasEdited: Boolean,
    val parts: List<EntryPart>,
    val summaryCategoriaId: String?,
    val summaryCategoriasEdited: Boolean,
)

/** Values a part's form changed away from the shared ones; null keeps following them. */
internal data class PartOverrides(
    val tipo: TipoMovimiento? = null,
    val fecha: LocalDate? = null,
    val cuentaChoice: CuentaSelectionState? = null,
    val etiquetaIds: List<String>? = null,
)
