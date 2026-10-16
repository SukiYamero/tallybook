package com.kurobello.tallybook.feature.search.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowContent
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowText
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.calendar_date_content_description
import tallybook.shared.generated.resources.date_picker_months

@Composable
internal fun SearchResultRow(
    row: SearchResultRowUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
  MovimientoRowContent(
      row.movimiento,
      row.categoria,
      MovimientoRowText(
          row.title,
          "${searchFullDate(row.movimiento.fecha)} · ${row.categoryLabel}",
          row.categoryLabel,
      ),
      modifier = modifier.fillMaxWidth(),
      maxDescriptionLines = Int.MAX_VALUE,
      maxSubtitleLines = Int.MAX_VALUE,
      onClick = onClick,
      pending = CATEGORIA_OTROS_ID in row.movimiento.categoriaIds,
  )
}

@Composable
internal fun searchFullDate(date: LocalDate): String =
    stringResource(
        Res.string.calendar_date_content_description,
        date.day,
        stringArrayResource(Res.array.date_picker_months)[date.month.ordinal],
        date.year,
    )
