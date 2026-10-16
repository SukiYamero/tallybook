package com.kurobello.tallybook.core.data.export

import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

class IosMovimientoExportSharer(
    private val ioDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
) : MovimientoExportSharer {

  override suspend fun share(fileName: String, csv: String): DataOutcome<Unit> {
    invalidExportFileName(fileName)?.let {
      return it
    }
    val path = NSTemporaryDirectory().trimEnd('/') + "/" + fileName
    val written = withContext(ioDispatcher) { writeUtf8(path, csv) }
    return if (written) {
      withContext(mainDispatcher) { presentShareSheet(path) }
    } else {
      DataOutcome.Failure(DataError.Local(IllegalStateException("Could not write $path")))
    }
  }

  @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
  private fun writeUtf8(path: String, csv: String): Boolean =
      NSString.create(string = csv)
          .writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)

  private fun presentShareSheet(path: String): DataOutcome<Unit> {
    val presenter =
        topViewController()
            ?: return DataOutcome.Failure(
                DataError.Unknown(IllegalStateException("No view controller to present from")),
            )
    val sheet =
        UIActivityViewController(
            activityItems = listOf(NSURL.fileURLWithPath(path)),
            applicationActivities = null,
        )
    // iPad presents the sheet as a popover and raises an exception when it has no anchor.
    sheet.popoverPresentationController?.sourceView = presenter.view
    presenter.presentViewController(sheet, animated = true, completion = null)
    return DataOutcome.Success(Unit)
  }

  private fun topViewController(): UIViewController? {
    val keyWindow =
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .firstNotNullOfOrNull { it.keyWindow }
    var controller = keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
      controller = controller.presentedViewController
    }
    return controller
  }
}
