package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.CategoriaRules
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.ETIQUETA_DESCRIPCION_MAX
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.EtiquetaWrite
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.text.normalizeOptionalText
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory etiquetas with the repository's naming rules. Counts come from [movimientoCounts],
 * which tests set directly; [failure] makes every call fail with that error.
 */
internal class FakeEtiquetaRepository(
    initial: List<Etiqueta> = emptyList(),
    private val createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
) : EtiquetaRepository {

  val etiquetas = MutableStateFlow(initial)
  val movimientoCounts = MutableStateFlow<Map<String, Long>>(emptyMap())
  var failure: DataError? = null
  private var nextId = 0

  override fun observeAll(): Flow<DataOutcome<List<Etiqueta>>> = etiquetas.map {
    outcome { it.sorted() }
  }

  override suspend fun getAll(): DataOutcome<List<Etiqueta>> = outcome { etiquetas.value.sorted() }

  override fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>> =
      movimientoCounts.map { counts ->
        outcome { counts }
      }

  override suspend fun create(nombre: String, descripcion: String?): DataOutcome<EtiquetaWrite> =
      validated(nombre, descripcion) { clean, cleanDescripcion ->
        duplicateOf(clean, excludingId = null)?.let { EtiquetaWrite.Duplicate(it) }
            ?: run {
              val created = Etiqueta("etiqueta-${++nextId}", clean, createdAt, cleanDescripcion)
              etiquetas.value += created
              EtiquetaWrite.Saved(created)
            }
      }

  override suspend fun update(
      id: String,
      nombre: String,
      descripcion: String?,
  ): DataOutcome<EtiquetaWrite> {
    val current =
        etiquetas.value.firstOrNull { it.id == id }
            ?: return DataOutcome.Failure(DataError.InvalidInput("Unknown etiqueta $id"))
    return validated(nombre, descripcion) { clean, cleanDescripcion ->
      duplicateOf(clean, excludingId = id)?.let { EtiquetaWrite.Duplicate(it) }
          ?: current.copy(nombre = clean, descripcion = cleanDescripcion).let { updated ->
            etiquetas.value = etiquetas.value.map { if (it.id == id) updated else it }
            EtiquetaWrite.Saved(updated)
          }
    }
  }

  override suspend fun countMovimientos(id: String): DataOutcome<Long> = outcome {
    movimientoCounts.value[id] ?: 0L
  }

  override suspend fun delete(id: String): DataOutcome<Unit> =
      if (etiquetas.value.none { it.id == id }) {
        DataOutcome.Failure(DataError.InvalidInput("Unknown etiqueta $id"))
      } else {
        outcome {
          etiquetas.value = etiquetas.value.filterNot { it.id == id }
          movimientoCounts.value -= id
        }
      }

  private fun validated(
      nombre: String,
      descripcion: String?,
      write: (String, String?) -> EtiquetaWrite,
  ): DataOutcome<EtiquetaWrite> {
    val clean = nombre.trim()
    val cleanDescripcion = descripcion?.normalizeOptionalText()
    return when {
      clean.isEmpty() || clean.length > CategoriaRules.NOMBRE_MAX ->
          DataOutcome.Failure(DataError.InvalidInput("Invalid etiqueta nombre"))
      (cleanDescripcion?.length ?: 0) > ETIQUETA_DESCRIPCION_MAX ->
          DataOutcome.Failure(DataError.InvalidInput("Invalid etiqueta descripcion"))
      else -> outcome { write(clean, cleanDescripcion) }
    }
  }

  private fun duplicateOf(nombre: String, excludingId: String?): Etiqueta? =
      etiquetas.value.firstOrNull {
        it.id != excludingId &&
            CategoriaRules.nombreKey(it.nombre) == CategoriaRules.nombreKey(nombre)
      }

  private inline fun <T> outcome(block: () -> T): DataOutcome<T> =
      failure?.let { DataOutcome.Failure(it) } ?: DataOutcome.Success(block())

  private fun List<Etiqueta>.sorted() = sortedBy { CategoriaRules.nombreKey(it.nombre) }
}
