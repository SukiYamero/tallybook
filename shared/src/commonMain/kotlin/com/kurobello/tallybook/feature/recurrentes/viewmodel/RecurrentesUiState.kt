package com.kurobello.tallybook.feature.recurrentes.viewmodel

import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.model.RecurrenciaWithPending
import com.kurobello.tallybook.feature.movimientos.viewmodel.EtiquetasLoad
import kotlinx.datetime.LocalDate

/** [today] is the day the pending occurrences were computed for. */
internal data class RecurrentesUiState(
    val today: LocalDate,
    val content: RecurrentesContent = RecurrentesContent.Loading,
    val catalog: RecurrentesCatalog = RecurrentesCatalog(),
    val sheet: RecurrentesSheet? = null,
) {
  val ready: RecurrentesContent.Ready?
    get() = content as? RecurrentesContent.Ready

  /** Occurrences, not recurrencias: one recurrencia can owe several dates. */
  val pendingCount: Int
    get() = ready?.pending?.size ?: 0

  fun hasPending(recurrenciaId: String): Boolean =
      ready?.pending?.any { it.recurrencia.id == recurrenciaId } == true

  /** Frecuencia and next date wait until nothing is pending for the recurrencia. */
  fun isScheduleLocked(sheet: RecurrenciaSheet): Boolean =
      sheet.scheduleRejected || hasPending(sheet.recurrencia.id)
}

internal sealed interface RecurrentesContent {
  data object Loading : RecurrentesContent

  data object Failed : RecurrentesContent

  /**
   * [pending] lists every occurrence due through today, oldest first across recurrencias;
   * [scheduled] lists every live recurrencia by its next date.
   */
  data class Ready(
      val pending: List<PendingOccurrence>,
      val scheduled: List<Recurrencia>,
      val categoriasById: Map<String, Categoria>,
  ) : RecurrentesContent {
    val isEmpty: Boolean
      get() = scheduled.isEmpty()
  }
}

/**
 * Pagado and Saltar act on a recurrencia's oldest pending occurrence, so only that one, [isNext],
 * can be opened; a later date waits for it.
 */
internal data class PendingOccurrence(
    val recurrencia: Recurrencia,
    val dueDate: LocalDate,
    val isNext: Boolean,
)

/** What the recurrencia sheet's form chooses from, read apart from what blocks the list. */
internal data class RecurrentesCatalog(
    val cuentas: List<Cuenta> = emptyList(),
    val isCuentasLoading: Boolean = true,
    val isCuentasFailed: Boolean = false,
    val etiquetas: List<Etiqueta> = emptyList(),
    val etiquetasLoad: EtiquetasLoad = EtiquetasLoad.LOADING,
)

internal fun List<RecurrenciaWithPending>.toReady(categorias: List<Categoria>) =
    RecurrentesContent.Ready(
        pending =
            flatMap { (recurrencia, dates) ->
                  dates.mapIndexed { index, date ->
                    PendingOccurrence(recurrencia, date, index == 0)
                  }
                }
                .sortedWith(compareBy({ it.dueDate }, { it.recurrencia.createdAt })),
        scheduled = map { it.recurrencia },
        categoriasById = categorias.associateBy(Categoria::id),
    )

/**
 * The movimiento an occurrence of this recurrencia would create on [fecha], which is how rows and
 * the editor present the template without a second set of components.
 */
internal fun Recurrencia.asMovimiento(fecha: LocalDate = proximaFecha): Movimiento =
    Movimiento(
        id = id,
        tipo = tipo,
        monto = monto,
        fecha = fecha,
        categoriaId = categoriaId,
        titulo = titulo,
        descripcion = descripcion,
        createdAt = createdAt,
        cuentaId = cuentaId,
        medio = medio,
        etiquetaIds = etiquetaIds,
    )
