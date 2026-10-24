package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.RestoreResult
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.model.TransferenciaRules
import com.kurobello.tallybook.core.model.TransferenciaUpdateResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

/**
 * In-memory live transfers validated with [TransferenciaRules] against [cuentaRepository]'s
 * cuentas. [restoreResult], when set, replaces the computed restore outcome; [failure] makes every
 * call fail with that error. [restores] counts restore calls.
 */
internal class FakeTransferenciaRepository(
    private val cuentaRepository: FakeCuentaRepository = FakeCuentaRepository(),
    private val today: LocalDate = LocalDate(2026, 10, 16),
    initial: List<Transferencia> = emptyList(),
) : TransferenciaRepository {

  val transferencias = MutableStateFlow(initial)
  var restoreResult: DataOutcome<RestoreResult>? = null
  var failure: DataError? = null
  var restores = 0
    private set

  private val tombstones = mutableMapOf<String, Transferencia>()

  override suspend fun getById(id: String): DataOutcome<Transferencia?> = outcome { find(id) }

  override suspend fun insert(transferencia: Transferencia): DataOutcome<Unit> =
      validated(transferencia) { transferencias.value += transferencia }

  override suspend fun update(
      id: String,
      changes: Transferencia,
  ): DataOutcome<TransferenciaUpdateResult> {
    val current = find(id) ?: return outcome { TransferenciaUpdateResult.NotFound }
    if (changes.id != id) {
      return DataOutcome.Failure(DataError.InvalidInput("Transferencia changes must carry id $id"))
    }
    val updated = changes.copy(createdAt = current.createdAt)
    return validated(updated) {
      transferencias.value = transferencias.value.map { if (it.id == id) updated else it }
      TransferenciaUpdateResult.Updated
    }
  }

  override suspend fun deleteAndCapture(id: String): DataOutcome<Transferencia?> = outcome {
    find(id)?.also {
      transferencias.value = transferencias.value.filterNot { row -> row.id == id }
      tombstones[id] = it
    }
  }

  override suspend fun restoreDeleted(transferencia: Transferencia): DataOutcome<RestoreResult> {
    restores++
    restoreResult?.let {
      return it
    }
    return outcome {
      val live = find(transferencia.id)
      val origen = cuentaRepository.find(transferencia.cuentaOrigenId)
      val destino = cuentaRepository.find(transferencia.cuentaDestinoId)
      val tombstone = tombstones[transferencia.id]
      when {
        live != null ->
            if (live == transferencia) RestoreResult.AlreadyPresent else RestoreResult.Conflict
        origen == null || destino == null -> RestoreResult.MissingCuenta
        origen.moneda != transferencia.montoOrigen.moneda ||
            destino.moneda != transferencia.montoDestino.moneda -> RestoreResult.Conflict
        tombstone != null && tombstone != transferencia -> RestoreResult.Conflict
        else -> {
          tombstones -= transferencia.id
          transferencias.value += transferencia
          RestoreResult.Restored
        }
      }
    }
  }

  override suspend fun getAll(): DataOutcome<List<Transferencia>> = outcome {
    transferencias.value.sortedWith(
        compareByDescending<Transferencia> { it.fecha }
            .thenByDescending { it.createdAt }
            .thenByDescending { it.id }
    )
  }

  override suspend fun defaultOrigin(primary: Moneda): DataOutcome<String?> = outcome {
    val candidates =
        cuentaRepository.cuentas.value
            .filter { !it.archivada && it.tipo != TipoCuenta.CREDITO }
            .sortedWith(compareBy<Cuenta> { it.createdAt }.thenBy { it.id })
    val newestOrigen =
        transferencias.value
            .maxWithOrNull(compareBy<Transferencia> { it.createdAt }.thenBy { it.id })
            ?.cuentaOrigenId
    (candidates.firstOrNull { it.id == newestOrigen }
            ?: candidates.firstOrNull { it.id == Cuenta.defaultCashId(primary) }
            ?: candidates.firstOrNull { it.moneda == primary }
            ?: candidates.firstOrNull())
        ?.id
  }

  private fun find(id: String): Transferencia? = transferencias.value.firstOrNull { it.id == id }

  private fun <T> validated(transferencia: Transferencia, write: () -> T): DataOutcome<T> {
    val errors =
        TransferenciaRules.validate(
            origen = cuentaRepository.find(transferencia.cuentaOrigenId),
            destino = cuentaRepository.find(transferencia.cuentaDestinoId),
            montoOrigen = transferencia.montoOrigen,
            montoDestino = transferencia.montoDestino,
            fecha = transferencia.fecha,
            descripcion = transferencia.descripcion,
            today = today,
        )
    return if (errors.isValid) outcome(write)
    else DataOutcome.Failure(DataError.InvalidInput("Invalid transferencia: $errors"))
  }

  private inline fun <T> outcome(block: () -> T): DataOutcome<T> =
      failure?.let { DataOutcome.Failure(it) } ?: DataOutcome.Success(block())
}
