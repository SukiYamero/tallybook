package com.kurobello.tallybook.core.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kurobello.tallybook.core.database.Correccion_frase
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.OrigenFrase
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class CorreccionFraseRepositoryImpl(
    private val database: TallybookDatabase,
    private val dispatcher: CoroutineDispatcher,
    private val changeClock: ChangeClock,
) : CorreccionFraseRepository {

  private val queries
    get() = database.correccionFraseQueries

  override suspend fun insertAll(correcciones: List<CorreccionFrase>): DataOutcome<Unit> =
      runLocalDataOperation(dispatcher) {
        if (correcciones.isNotEmpty()) {
          database.transaction {
            val stamp = changeClock.next().value
            correcciones.forEach {
              queries.insertCorreccionFrase(
                  id = it.id,
                  origen = it.origen.name,
                  palabras = it.palabras.joinToString(" "),
                  campo = it.campo.name,
                  leido = it.leido,
                  propuesto = it.propuesto,
                  guardado = it.guardado,
                  created_at = it.createdAt.toString(),
                  updated_at = stamp,
              )
            }
          }
        }
      }

  override fun observeSince(since: Instant): Flow<DataOutcome<List<CorreccionFrase>>> =
      queries
          .selectCorreccionesFraseSince(since.toString())
          .asFlow()
          .mapToList(dispatcher)
          .map { rows -> rows.map { it.toDomain() } }
          .asLocalDataOutcome()
}

private fun Correccion_frase.toDomain(): CorreccionFrase =
    CorreccionFrase(
        id = id,
        origen = OrigenFrase.valueOf(origen),
        palabras = palabras.split(' '),
        campo = CampoCorreccion.valueOf(campo),
        leido = leido,
        propuesto = propuesto,
        guardado = guardado,
        createdAt = Instant.parse(created_at),
    )
