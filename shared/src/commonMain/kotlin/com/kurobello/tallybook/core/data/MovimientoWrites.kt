package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.CuentaRejection
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.model.MOVIMIENTO_DESCRIPTION_MAX_LENGTH
import com.kurobello.tallybook.core.model.MOVIMIENTO_TITLE_MAX_LENGTH
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoChanges

/**
 * The reference checks and row writes of every repository that writes movimientos. Each call joins
 * the caller's transaction, so a reference cannot disappear between its check and the write.
 */
internal class MovimientoWrites(private val database: TallybookDatabase) {

  /**
   * The first reference [changes] cannot point at. [keptCategoriaId] and [keptCuentaId] are the
   * ones a stored row already has: a categoria the catalog lost or an archived cuenta stays valid
   * there.
   */
  fun rejection(
      changes: MovimientoChanges,
      keptCategoriaId: String? = null,
      keptCuentaId: String? = null,
  ): DataError.InvalidInput? =
      missingReference(changes.categoriaId, changes.etiquetaIds, keptCategoriaId)
          ?: invalidCuenta(changes.cuentaId, changes.medio, changes.monto, keptCuentaId)

  private fun missingReference(
      categoriaId: String,
      etiquetaIds: List<String>,
      keptCategoriaId: String? = null,
  ): DataError.InvalidInput? =
      when {
        categoriaId != keptCategoriaId && !categoriaExists(categoriaId) ->
            DataError.InvalidInput("Unknown categoria $categoriaId")
        existingEtiquetaIds(etiquetaIds).size != etiquetaIds.size ->
            DataError.InvalidInput("Movimiento references an unknown etiqueta")
        else -> null
      }

  // A medio never outlives its cuenta's type: the type is locked while a live movimiento uses it.
  private fun invalidCuenta(
      cuentaId: String,
      medio: MedioPago,
      monto: Money,
      keptCuentaId: String? = null,
  ): DataError.InvalidInput? {
    val cuenta =
        database.cuentaQueries.selectCuentaById(cuentaId).executeAsOneOrNull()?.toDomain()
            ?: return DataError.InvalidInput("Unknown cuenta $cuentaId")
    return when (cuenta.rejectionFor(medio, monto.moneda, keptCuentaId)) {
      null -> null
      CuentaRejection.ARCHIVED -> DataError.InvalidInput("Cuenta $cuentaId is archived")
      CuentaRejection.MEDIO_MISMATCH ->
          DataError.InvalidInput("Medio $medio does not fit a ${cuenta.tipo} cuenta")
      CuentaRejection.MONEDA_MISMATCH ->
          DataError.InvalidInput("Movimiento currency must be its cuenta's ${cuenta.moneda}")
    }
  }

  fun categoriaExists(id: String): Boolean =
      database.categoriaQueries.selectCategoriaById(id).executeAsOneOrNull() != null

  fun existingEtiquetaIds(ids: List<String>): Set<String> =
      if (ids.isEmpty()) emptySet()
      else database.etiquetaQueries.selectExistingEtiquetaIds(ids).executeAsList().toSet()

  fun insertRow(movimiento: Movimiento, stamp: ChangeStamp, recurrenciaId: String? = null) {
    check(
        database.movimientoQueries
            .insertMovimiento(
                id = movimiento.id,
                tipo = movimiento.tipo.name,
                monto_minor = movimiento.monto.minorUnits,
                fecha = movimiento.fecha.toString(),
                titulo = movimiento.titulo,
                descripcion = movimiento.descripcion,
                created_at = movimiento.createdAt.toString(),
                categoria_id = movimiento.categoriaId,
                cuenta_id = movimiento.cuentaId,
                medio_pago = movimiento.medio.name,
                updated_at = stamp.value,
                recurrencia_id = recurrenciaId,
            )
            .value == 1L
    )
    insertEtiquetas(movimiento.id, movimiento.etiquetaIds)
  }

  fun insertEtiquetas(movimientoId: String, etiquetaIds: List<String>) {
    etiquetaIds.forEachIndexed { posicion, etiquetaId ->
      check(
          database.movimientoQueries
              .insertMovimientoEtiqueta(movimientoId, etiquetaId, posicion.toLong())
              .value == 1L
      )
    }
  }

  fun tombstone(id: String, stamp: ChangeStamp) {
    val queries = database.movimientoQueries
    check(queries.tombstoneMovimiento(stamp = stamp.value, id = id).value == 1L)
    queries.deleteMovimientoEtiquetas(id)
  }
}

internal fun validateMovimiento(movimiento: Movimiento): DataError.InvalidInput? =
    if (movimiento.id.isEmpty()) DataError.InvalidInput("Movimiento id must not be empty")
    else validateMovimientoChanges(movimiento.changes())

internal fun validateMovimientoChanges(changes: MovimientoChanges): DataError.InvalidInput? =
    when {
      changes.monto.minorUnits <= 0 -> DataError.InvalidInput("Movimiento amount must be positive")
      changes.monto.minorUnits > Money.MAX_MINOR_UNITS ->
          DataError.InvalidInput("Movimiento amount exceeds the maximum")
      !changes.medio.allows(changes.tipo) ->
          DataError.InvalidInput("Medio ${changes.medio} does not allow ${changes.tipo}")
      else ->
          validateReferences(changes.categoriaId, changes.etiquetaIds)
              ?: validateTitle(changes.titulo)
              ?: validateDescription(changes.descripcion)
    }

internal fun Movimiento.changes(): MovimientoChanges =
    MovimientoChanges(
        tipo,
        monto,
        fecha,
        categoriaId,
        etiquetaIds,
        titulo,
        descripcion,
        cuentaId,
        medio,
    )

private fun validateTitle(titulo: String?): DataError.InvalidInput? =
    if ((titulo?.length ?: 0) > MOVIMIENTO_TITLE_MAX_LENGTH) {
      DataError.InvalidInput("Movimiento title is too long")
    } else null

private fun validateDescription(descripcion: String?): DataError.InvalidInput? =
    if ((descripcion?.length ?: 0) > MOVIMIENTO_DESCRIPTION_MAX_LENGTH) {
      DataError.InvalidInput("Movimiento description is too long")
    } else null

private fun validateReferences(
    categoriaId: String,
    etiquetaIds: List<String>,
): DataError.InvalidInput? =
    when {
      categoriaId.isBlank() -> DataError.InvalidInput("Categoria id must not be blank")
      etiquetaIds.size > MAX_ETIQUETAS_POR_MOVIMIENTO ->
          DataError.InvalidInput(
              "Movimiento allows $MAX_ETIQUETAS_POR_MOVIMIENTO etiquetas, got ${etiquetaIds.size}"
          )
      etiquetaIds.any { it.isBlank() } -> DataError.InvalidInput("Etiqueta id must not be blank")
      etiquetaIds.any { id -> id.any { it in VIEW_SEPARATORS } } ->
          DataError.InvalidInput("Etiqueta id must not contain a control separator")
      etiquetaIds.toSet().size != etiquetaIds.size ->
          DataError.InvalidInput("Movimiento etiquetas must be distinct")
      else -> null
    }
