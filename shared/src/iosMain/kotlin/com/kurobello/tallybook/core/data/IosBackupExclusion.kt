package com.kurobello.tallybook.core.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import okio.IOException
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey

/** Application Support is part of iCloud and device backups unless a file opts out. */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
class IosBackupExclusion(
    private val pathProvider: PreferencesPathProvider,
) : BackupExclusion {
  override fun exclude(file: PreferencesFile) {
    memScoped {
      val error = alloc<ObjCObjectVar<NSError?>>()
      val excluded =
          NSURL.fileURLWithPath(pathProvider.path(file))
              .setResourceValue(NSNumber(bool = true), NSURLIsExcludedFromBackupKey, error.ptr)
      if (!excluded) {
        throw IOException(
            "Cannot exclude ${file.fileName} from backup: ${error.value?.localizedDescription}"
        )
      }
    }
  }
}
