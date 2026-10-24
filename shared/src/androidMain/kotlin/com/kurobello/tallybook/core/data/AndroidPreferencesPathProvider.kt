package com.kurobello.tallybook.core.data

import android.content.Context

class AndroidPreferencesPathProvider(
    private val context: Context,
) : PreferencesPathProvider {
  override fun path(file: PreferencesFile): String =
      context.filesDir.resolve(file.fileName).absolutePath
}
