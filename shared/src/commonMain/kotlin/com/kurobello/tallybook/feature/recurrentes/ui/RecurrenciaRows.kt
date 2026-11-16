package com.kurobello.tallybook.feature.recurrentes.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.Categoria
import com.kurobello.tallybook.core.model.Recurrencia
import com.kurobello.tallybook.core.ui.date.fullCivilDate
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowContent
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowText
import com.kurobello.tallybook.core.ui.movimiento.rowTitle
import com.kurobello.tallybook.core.ui.recurrencia.labelResource
import com.kurobello.tallybook.feature.recurrentes.viewmodel.PendingOccurrence
import com.kurobello.tallybook.feature.recurrentes.viewmodel.asMovimiento
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.recurrencia_open
import tallybook.shared.generated.resources.recurrencia_open_pending
import tallybook.shared.generated.resources.recurrencia_pending_after_previous
import tallybook.shared.generated.resources.recurrencia_pending_since
import tallybook.shared.generated.resources.recurrencia_schedule
import tallybook.shared.generated.resources.unknown_category

/**
 * The row announces title, due date and amount as one node. A later date of the same recurrencia is
 * not interactive until the earlier one is confirmed or skipped.
 */
@Composable
internal fun PendingOccurrenceRow(
    occurrence: PendingOccurrence,
    categoria: Categoria?,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val since =
      stringResource(Res.string.recurrencia_pending_since, fullCivilDate(occurrence.dueDate))
  val secondary =
      if (occurrence.isNext) since
      else
          "$since$SUBTITLE_SEPARATOR${stringResource(Res.string.recurrencia_pending_after_previous)}"
  RecurrenciaRow(
      recurrencia = occurrence.recurrencia,
      categoria = categoria,
      secondary = secondary,
      onClick = if (occurrence.isNext) ({ onOpen(occurrence.recurrencia.id) }) else null,
      onClickLabel = stringResource(Res.string.recurrencia_open_pending),
      modifier = modifier,
  )
}

@Composable
internal fun ScheduledRecurrenciaRow(
    recurrencia: Recurrencia,
    categoria: Categoria?,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  RecurrenciaRow(
      recurrencia = recurrencia,
      categoria = categoria,
      secondary =
          stringResource(
              Res.string.recurrencia_schedule,
              stringResource(recurrencia.frecuencia.labelResource),
              fullCivilDate(recurrencia.proximaFecha),
          ),
      onClick = { onOpen(recurrencia.id) },
      onClickLabel = stringResource(Res.string.recurrencia_open),
      modifier = modifier,
  )
}

@Composable
@Suppress("LongParameterList")
private fun RecurrenciaRow(
    recurrencia: Recurrencia,
    categoria: Categoria?,
    secondary: String,
    onClick: (() -> Unit)?,
    onClickLabel: String,
    modifier: Modifier,
) {
  val categoryLabel = categoria?.nombre ?: stringResource(Res.string.unknown_category)
  val movimiento = recurrencia.asMovimiento()
  MovimientoRowContent(
      movimiento = movimiento,
      categoria = categoria,
      text = MovimientoRowText(movimiento.rowTitle(categoryLabel), secondary, categoryLabel),
      modifier = modifier,
      onClick = onClick,
      onClickLabel = onClickLabel,
  )
}

private const val SUBTITLE_SEPARATOR = " · "
