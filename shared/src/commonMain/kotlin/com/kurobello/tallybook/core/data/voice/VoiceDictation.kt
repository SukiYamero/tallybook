package com.kurobello.tallybook.core.data.voice

import kotlinx.coroutines.flow.Flow

/** Platform speech-to-text for one utterance at a time. */
interface VoiceDictation {
  fun permission(): DictationPermission

  fun isAvailable(): Boolean

  /**
   * Cold: collecting starts listening and cancelling the collection aborts it. An emission of
   * [finishRequests] stops capturing audio and the flow still ends with the [DictationEvent.Final]
   * of what was heard. Every session ends with exactly one [DictationEvent.Final] or
   * [DictationEvent.Failed]; failures are events, never exceptions.
   */
  fun listen(languageTag: String, finishRequests: Flow<Unit>): Flow<DictationEvent>
}

/**
 * Android cannot tell a first request from a denial without an Activity, so it never reports
 * DENIED.
 */
enum class DictationPermission {
  GRANTED,
  NOT_DETERMINED,
  DENIED,
}

sealed interface DictationEvent {
  data object Listening : DictationEvent

  data class Partial(val text: String) : DictationEvent

  data class Final(val text: String) : DictationEvent

  data class Failed(val error: DictationError) : DictationEvent
}

enum class DictationError {
  PERMISSION,
  NO_MATCH,
  SPEECH_TIMEOUT,
  LANGUAGE_UNAVAILABLE,
  /** The device is fetching the language model; dictation works once it is installed. */
  LANGUAGE_DOWNLOADING,
  BUSY,
  AUDIO,
  UNAVAILABLE,
  UNKNOWN,
}
