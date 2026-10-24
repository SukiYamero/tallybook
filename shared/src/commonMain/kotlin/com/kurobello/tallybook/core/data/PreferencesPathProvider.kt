package com.kurobello.tallybook.core.data

/**
 * Every DataStore file the app owns. [DEVICE] holds installation-scoped values that must never be
 * restored onto another device, so it stays apart from the user's [APP] preferences.
 */
enum class PreferencesFile(val fileName: String) {
  APP("tallybook.preferences_pb"),
  DEVICE("tallybook.device.preferences_pb"),
}

/** Resolves each [PreferencesFile] inside the single platform directory that owns them. */
interface PreferencesPathProvider {
  fun path(file: PreferencesFile): String
}
