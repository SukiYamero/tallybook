package com.kurobello.tallybook.core.data

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
class IosPreferencesPathProvider : PreferencesPathProvider {
  override fun path(file: PreferencesFile): String {
    val directory =
        requireNotNull(
            NSFileManager.defaultManager
                .URLForDirectory(
                    NSApplicationSupportDirectory,
                    NSUserDomainMask,
                    null,
                    true,
                    null,
                )
                ?.path,
        )
    return "$directory/${file.fileName}"
  }
}
