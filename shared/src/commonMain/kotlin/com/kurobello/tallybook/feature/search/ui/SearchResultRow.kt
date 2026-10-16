package com.kurobello.tallybook.feature.search.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.CATEGORIA_OTROS_ID
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowContent
import com.kurobello.tallybook.core.ui.movimiento.MovimientoRowText
import com.kurobello.tallybook.core.ui.movimiento.rowDescription

@Composable
internal fun SearchResultRow(
    row: SearchResultRowUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
  MovimientoRowContent(
      row.movimiento,
      row.categoria,
      MovimientoRowText(row.title, row.movimiento.rowDescription, row.categoryLabel),
      modifier = modifier.fillMaxWidth(),
      onClick = onClick,
      pending = CATEGORIA_OTROS_ID in row.movimiento.categoriaIds,
  )
}
