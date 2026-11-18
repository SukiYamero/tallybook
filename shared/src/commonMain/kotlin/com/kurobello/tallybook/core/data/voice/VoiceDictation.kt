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

  /**
   * What the recognizer reports right now. Some recognizers repeat everything heard since the
   * start; others restart after a [Pause] or a [Segment], so callers join them with
   * [DictationTranscript].
   */
  data class Partial(val text: String) : DictationEvent

  /** A recognizer closed one stretch of speech; the next partials start a new one. */
  data class Segment(val text: String) : DictationEvent

  /** The speaker went quiet while still holding the microphone. */
  data object Pause : DictationEvent

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
