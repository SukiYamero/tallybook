package com.kurobello.tallybook.core.data.export

import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome

private val PATH_SEPARATORS = charArrayOf('/', '\\')

interface MovimientoExportSharer {

  /**
   * Writes [csv] as UTF-8 to an app-private temporary file named [fileName] and opens the platform
   * share sheet for it. Success means the sheet was presented, not that the user shared the file:
   * the platforms do not report a cancelled sheet back reliably.
   */
  suspend fun share(fileName: String, csv: String): DataOutcome<Unit>
}

// The name is joined onto a platform directory, so a separator or dot segment would escape it.
internal fun invalidExportFileName(fileName: String): DataOutcome.Failure? =
    if (fileName.isBlank() || fileName.startsWith('.') || fileName.any { it in PATH_SEPARATORS }) {
      DataOutcome.Failure(DataError.InvalidInput("Invalid export file name: '$fileName'"))
    } else {
      null
    }
