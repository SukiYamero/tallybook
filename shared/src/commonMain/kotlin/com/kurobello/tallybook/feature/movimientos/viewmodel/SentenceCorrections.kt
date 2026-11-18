package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.entry.suggestionTokens
import com.kurobello.tallybook.core.model.CampoCorreccion
import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.OrigenFrase
import kotlin.time.Instant

internal const val JOINED_PARTS = "JOINED"

/** What the sentence derived at the last Continue for one movimiento it would create. */
internal data class ParteSnapshot(
    val palabras: List<String>,
    val readings: FieldReadings,
    val titulo: String,
)

/**
 * What the last Continue derived. [sentence] is the whole sentence; each part of a split sentence
 * keeps its own in [EntryPart.snapshot]. [structure] is set once Join or a removal left a single
 * movimiento.
 */
internal data class SentenceSnapshot(
    val origen: OrigenFrase,
    val sentence: ParteSnapshot,
    val partCount: Int,
    val structure: StructureChange? = null,
)

/**
 * Join or a removal turned the review into one movimiento, compared against [base]. Leaving the
 * review marks the amount edited so a currency change does not re-read it; only a later change to
 * [amount] makes it the user's.
 */
internal data class StructureChange(
    val guardado: String,
    val base: ParteSnapshot,
    val amount: Money?,
    val amountHandSet: Boolean,
)

/**
 * The compared campos the user set by hand; any other value came from the sentence or a default.
 */
internal data class HandSet(
    val tipo: Boolean,
    val monto: Boolean,
    val fecha: Boolean,
    val categoria: Boolean,
    val medio: Boolean,
    val cuenta: Boolean,
    val frecuencia: Boolean,
    val titulo: Boolean,
) {
  val campos: Set<CampoCorreccion>
    get() =
        mapOf(
                CampoCorreccion.TIPO to tipo,
                CampoCorreccion.MONTO to monto,
                CampoCorreccion.FECHA to fecha,
                CampoCorreccion.CATEGORIA to categoria,
                CampoCorreccion.MEDIO to medio,
                CampoCorreccion.CUENTA to cuenta,
                CampoCorreccion.FRECUENCIA to frecuencia,
                CampoCorreccion.TITULO to titulo,
            )
            .filterValues { it }
            .keys
}

internal class RecordStamp(
    private val origen: OrigenFrase,
    private val ids: IdGenerator,
    private val now: Instant,
) {
  fun record(
      palabras: List<String>,
      campo: CampoCorreccion,
      leido: String?,
      propuesto: String?,
      guardado: String?,
  ): CorreccionFrase =
      CorreccionFrase(ids.next(), origen, palabras, campo, leido, propuesto, guardado, now)
}

internal fun RecordStamp.structureRecord(
    sentence: SentenceSnapshot,
    guardado: String,
): CorreccionFrase =
    record(
        sentence.sentence.palabras,
        CampoCorreccion.PARTES,
        leido = null,
        propuesto = sentence.partCount.toString(),
        guardado = guardado,
    )

/** One record per hand-set campo, comparing [saved] with what [snapshot] derived. */
internal fun correctionsOf(
    snapshot: ParteSnapshot,
    saved: Movimiento,
    frecuencia: Frecuencia?,
    handSet: HandSet,
    stamp: RecordStamp,
): List<CorreccionFrase> {
  val savedValues =
      mapOf(
          CampoCorreccion.TIPO to saved.tipo.name,
          CampoCorreccion.MONTO to saved.monto.encoded(),
          CampoCorreccion.FECHA to saved.fecha.toString(),
          CampoCorreccion.CATEGORIA to saved.categoriaId,
          CampoCorreccion.MEDIO to saved.medio.name,
          CampoCorreccion.CUENTA to saved.cuentaId,
          CampoCorreccion.FRECUENCIA to frecuencia?.name,
      )
  val campos = handSet.campos
  val fields =
      savedValues
          .filterKeys { it in campos }
          .map { (campo, guardado) ->
            stamp.record(
                snapshot.palabras,
                campo,
                snapshot.readings.leido[campo],
                snapshot.readings.propuesto[campo],
                guardado,
            )
          }
  val titulo =
      if (handSet.titulo) tituloCorrection(snapshot, saved.titulo.orEmpty(), stamp) else null
  return fields + listOfNotNull(titulo)
}

/**
 * A título with more than half of its words replaced was rewritten, not corrected: it says nothing
 * about how the sentence was heard.
 */
private fun tituloCorrection(
    snapshot: ParteSnapshot,
    saved: String,
    stamp: RecordStamp,
): CorreccionFrase? {
  val before = suggestionTokens(snapshot.titulo).distinct()
  val after = suggestionTokens(saved).distinct()
  val removed = before - after.toSet()
  val added = after - before.toSet()
  val rewritten = removed.size * 2 > before.size
  if (rewritten || removed.isEmpty() && added.isEmpty()) return null
  return stamp.record(
      snapshot.palabras,
      CampoCorreccion.TITULO,
      leido = null,
      propuesto = removed.joinToString(" ").ifEmpty { null },
      guardado = added.joinToString(" ").ifEmpty { null },
  )
}
