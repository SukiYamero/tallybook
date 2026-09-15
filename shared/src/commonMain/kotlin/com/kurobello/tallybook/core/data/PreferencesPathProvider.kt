package com.kurobello.tallybook.core.data

internal const val PREFERENCES_FILE_NAME = "tallybook.preferences_pb"

interface PreferencesPathProvider {
  fun path(): String
}
