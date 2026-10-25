package com.kurobello.tallybook.core.data.export

import com.kurobello.tallybook.core.model.DataOutcome

/** A failed read fails the whole export, so a share never receives a partial one. */
internal fun interface MovimientoExportSource {
  suspend fun read(): DataOutcome<MovimientoExportContent>
}
