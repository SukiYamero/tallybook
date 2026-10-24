package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaArchive
import com.kurobello.tallybook.core.model.CuentaDelete
import com.kurobello.tallybook.core.model.CuentaDraft
import com.kurobello.tallybook.core.model.CuentaRules
import com.kurobello.tallybook.core.model.CuentaWrite
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory live cuentas with the repository's naming, currency and type lock, archive and delete
 * rules. Reference counts come from [references], which tests set directly; [failure] makes every
 * call fail with that error.
 */
internal class FakeCuentaRepository(
    initial: List<Cuenta> = emptyList(),
    private val createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
) : CuentaRepository {

  val cuentas = MutableStateFlow(initial)
  val references = MutableStateFlow<Map<String, Long>>(emptyMap())
  var failure: DataError? = null
  private var nextId = 0

  override fun observeAll(): Flow<DataOutcome<List<Cuenta>>> = cuentas.map { outcome { it } }

  override suspend fun getById(id: String): DataOutcome<Cuenta?> = outcome { find(id) }

  override suspend fun create(draft: CuentaDraft): DataOutcome<CuentaWrite> =
      validated(draft) { clean ->
        duplicateOf(clean, excludingId = null)?.let { CuentaWrite.Duplicate(it) }
            ?: Cuenta(
                    id = "cuenta-${++nextId}",
                    nombre = clean.nombre,
                    tipo = clean.tipo,
                    saldoInicial = clean.saldoInicial,
                    diaCorte = clean.diaCorte,
                    archivada = false,
                    createdAt = createdAt,
                )
                .also { cuentas.value += it }
                .let { CuentaWrite.Saved(it) }
      }

  override suspend fun update(id: String, draft: CuentaDraft): DataOutcome<CuentaWrite> {
    val current = find(id) ?: return unknown(id)
    return validated(draft) { clean ->
      val updated =
          current.copy(
              nombre = clean.nombre,
              tipo = clean.tipo,
              saldoInicial = clean.saldoInicial,
              diaCorte = clean.diaCorte,
          )
      val duplicate = duplicateOf(clean, excludingId = id)
      when {
        duplicate != null -> CuentaWrite.Duplicate(duplicate)
        updated.moneda != current.moneda &&
            (referencesOf(id) > 0L || id == Cuenta.defaultCashId(current.moneda)) ->
            CuentaWrite.CurrencyLocked
        updated.tipo != current.tipo && referencesOf(id) > 0L -> CuentaWrite.TypeLocked
        else -> {
          replace(updated)
          CuentaWrite.Saved(updated)
        }
      }
    }
  }

  override suspend fun setArchivada(id: String, archivada: Boolean): DataOutcome<CuentaArchive> {
    val current = find(id) ?: return unknown(id)
    return outcome {
      when {
        current.archivada == archivada -> CuentaArchive.Updated
        archivada && isLastActive(id) -> CuentaArchive.LastActive
        else -> {
          replace(current.copy(archivada = archivada))
          CuentaArchive.Updated
        }
      }
    }
  }

  override suspend fun delete(id: String): DataOutcome<CuentaDelete> {
    val current = find(id) ?: return unknown(id)
    return outcome {
      when {
        !current.archivada && isLastActive(id) -> CuentaDelete.LastActive
        referencesOf(id) > 0L -> CuentaDelete.InUse(referencesOf(id))
        else -> {
          cuentas.value = cuentas.value.filterNot { it.id == id }
          CuentaDelete.Deleted
        }
      }
    }
  }

  fun find(id: String): Cuenta? = cuentas.value.firstOrNull { it.id == id }

  private fun referencesOf(id: String): Long = references.value[id] ?: 0L

  private fun isLastActive(id: String): Boolean =
      cuentas.value.none { it.id != id && !it.archivada }

  private fun replace(cuenta: Cuenta) {
    cuentas.value = cuentas.value.map { if (it.id == cuenta.id) cuenta else it }
  }

  private fun validated(
      draft: CuentaDraft,
      write: (CuentaDraft) -> CuentaWrite,
  ): DataOutcome<CuentaWrite> =
      if (CuentaRules.validate(draft.nombre, draft.tipo, draft.diaCorte).isValid) {
        outcome { write(draft.copy(nombre = draft.nombre.trim())) }
      } else {
        DataOutcome.Failure(DataError.InvalidInput("Invalid cuenta draft"))
      }

  private fun duplicateOf(draft: CuentaDraft, excludingId: String?): Cuenta? {
    val key = CuentaRules.nombreKey(draft.nombre, draft.moneda)
    return cuentas.value.firstOrNull {
      it.id != excludingId && CuentaRules.nombreKey(it.nombre, it.moneda) == key
    }
  }

  private fun unknown(id: String) =
      DataOutcome.Failure(DataError.InvalidInput("Unknown cuenta $id"))

  private inline fun <T> outcome(block: () -> T): DataOutcome<T> =
      failure?.let { DataOutcome.Failure(it) } ?: DataOutcome.Success(block())
}
