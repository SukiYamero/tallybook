package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.entry.CorrectionRules
import com.kurobello.tallybook.core.data.entry.RULE_WINDOW
import com.kurobello.tallybook.core.model.CorreccionFrase
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.OrigenFrase
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

/** Rules learned from the records of the last [RULE_WINDOW]; none while they cannot be read. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun AddMovimientoRepositories.correctionRules(clock: Clock): Flow<CorrectionRules> =
    correcciones
        .observeSince(clock.now() - RULE_WINDOW)
        .map { outcome -> (outcome as? DataOutcome.Success)?.data.orEmpty() }
        .catch { emit(emptyList()) }
        .mapLatest { records -> CorrectionRules.from(records, clock.now()) }

internal fun PhraseInterpretation.snapshot(titulo: String, dictated: Boolean): SentenceSnapshot =
    SentenceSnapshot(
        origen = if (dictated) OrigenFrase.DICTADO else OrigenFrase.ESCRITO,
        sentence = ParteSnapshot(words, readings, titulo),
        partCount = parts.size,
    )

internal fun PartInterpretation.snapshot(): ParteSnapshot = ParteSnapshot(words, readings, titulo)

/** [base] null compares the single movimiento with the whole sentence. */
internal fun AddMovimientoUiState.withStructureChange(
    guardado: String,
    base: ParteSnapshot?,
    amountHandSet: Boolean,
): AddMovimientoUiState =
    copy(
        snapshot =
            snapshot?.let {
              it.copy(
                  structure =
                      StructureChange(guardado, base ?: it.sentence, amountInput, amountHandSet)
              )
            }
    )

/** What saving the single draft as [saved] corrected; nothing for a draft without a sentence. */
internal fun AddMovimientoUiState.draftCorrections(
    saved: Movimiento,
    ids: IdGenerator,
    now: Instant,
): List<CorreccionFrase> {
  val snapshot = snapshot ?: return emptyList()
  val stamp = RecordStamp(snapshot.origen, ids, now)
  val structure = snapshot.structure
  val handSet =
      HandSet(
          tipo = tipoEdited,
          monto =
              structure?.let { it.amountHandSet || amountInput != it.amountInput } ?: amountEdited,
          fecha = fechaEdited,
          categoria = categoriasEdited && categoriaId != null,
          medio = medioEdited,
          cuenta = cuentaEdited,
          frecuencia = frecuenciaEdited,
          titulo = tituloEdited,
      )
  val structureRecords =
      listOfNotNull(structure?.let { stamp.structureRecord(snapshot, it.guardado) })
  return structureRecords +
      correctionsOf(structure?.base ?: snapshot.sentence, saved, frecuencia, handSet, stamp)
}

/** What saving every part as [saved], in the parts' order, corrected. */
internal fun AddMovimientoUiState.partsCorrections(
    saved: List<Movimiento>,
    ids: IdGenerator,
    now: Instant,
): List<CorreccionFrase> {
  val snapshot = snapshot ?: return emptyList()
  val stamp = RecordStamp(snapshot.origen, ids, now)
  val structureRecords =
      if (parts.size == snapshot.partCount) emptyList()
      else listOf(stamp.structureRecord(snapshot, parts.size.toString()))
  return structureRecords +
      parts.zip(saved).flatMap { (part, movimiento) ->
        part.snapshot
            ?.let { correctionsOf(it, movimiento, frecuencia, handSetOf(part), stamp) }
            .orEmpty()
      }
}

/** A part follows the shared hand-set campos unless its own form changed them. */
private fun AddMovimientoUiState.handSetOf(part: EntryPart): HandSet =
    HandSet(
        tipo = tipoEdited || part.overrides.tipo != null,
        monto = part.amountEdited,
        fecha = fechaEdited || part.overrides.fecha != null,
        categoria = part.categoriasEdited && part.categoriaId != null,
        medio = medioEdited || part.medioEdited,
        cuenta = cuentaEdited || part.cuentaEdited,
        frecuencia = frecuenciaEdited,
        titulo = part.titulo != part.segment,
    )
