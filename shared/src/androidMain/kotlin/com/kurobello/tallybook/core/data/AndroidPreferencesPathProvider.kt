package com.kurobello.tallybook.core.data

import android.content.Context

class AndroidPreferencesPathProvider(
    private val context: Context,
) : PreferencesPathProvider {
  override fun path(): String = context.filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath
}
