package com.kurobello.tallybook.core.data.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.kurobello.tallybook.core.data.runLocalDataOperation
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import com.kurobello.tallybook.core.model.fold
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher

private const val EXPORTS_DIRECTORY = "exports"
private const val CSV_MIME_TYPE = "text/csv"

class AndroidMovimientoExportSharer(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : MovimientoExportSharer {

  private val context = context.applicationContext

  override suspend fun share(fileName: String, csv: String): DataOutcome<Unit> =
      invalidExportFileName(fileName)
          ?: runLocalDataOperation(ioDispatcher) { writeExport(fileName, csv) }
              .fold(onSuccess = ::openChooser, onFailure = { DataOutcome.Failure(it) })

  private fun openChooser(file: File): DataOutcome<Unit> =
      try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.exports", file)
        context.startActivity(chooserIntent(uri))
        DataOutcome.Success(Unit)
      } catch (error: IllegalArgumentException) {
        DataOutcome.Failure(DataError.Unknown(error))
      } catch (error: ActivityNotFoundException) {
        DataOutcome.Failure(DataError.Unknown(error))
      }

  private fun writeExport(fileName: String, csv: String): File {
    val directory = File(context.cacheDir, EXPORTS_DIRECTORY).apply { mkdirs() }
    // Only the newest copy of the user's financial data stays on disk.
    directory.listFiles()?.forEach { it.delete() }
    return File(directory, fileName).apply { writeText(csv, Charsets.UTF_8) }
  }

  private fun chooserIntent(uri: Uri): Intent {
    // ClipData is what createChooser forwards, with the read grant, to the chooser and the target.
    val send =
        Intent(Intent.ACTION_SEND)
            .setType(CSV_MIME_TYPE)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = ClipData.newRawUri(null, uri) }
    // Started from the application context, which has no task of its own to join.
    return Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
}
