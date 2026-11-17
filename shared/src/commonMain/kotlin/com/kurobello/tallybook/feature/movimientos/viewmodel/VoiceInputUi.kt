package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.voice.DictationError

internal data class VoiceInputUi(
    val isAvailable: Boolean = false,
    val isListening: Boolean = false,
    /** The recognizer opened the microphone; words said before this are not heard. */
    val isMicReady: Boolean = false,
    /** Whether the sentence already had text, and so a Detected card, when listening began. */
    val detectedShownAtStart: Boolean = false,
    val notice: VoiceNotice? = null,
)

internal enum class VoiceNotice {
  NOT_HEARD,
  PERMISSION_DENIED,
  /** The system no longer asks, so only the app settings can grant the microphone. */
  PERMISSION_BLOCKED,
  LANGUAGE_UNAVAILABLE,
  LANGUAGE_DOWNLOADING,
  FAILED,
}

/** UNAVAILABLE has no notice: it disables the microphone instead. */
internal val dictationErrorNotices: Map<DictationError, VoiceNotice?> =
    mapOf(
        DictationError.PERMISSION to VoiceNotice.PERMISSION_DENIED,
        DictationError.NO_MATCH to VoiceNotice.NOT_HEARD,
        DictationError.SPEECH_TIMEOUT to VoiceNotice.NOT_HEARD,
        DictationError.LANGUAGE_UNAVAILABLE to VoiceNotice.LANGUAGE_UNAVAILABLE,
        DictationError.LANGUAGE_DOWNLOADING to VoiceNotice.LANGUAGE_DOWNLOADING,
        DictationError.BUSY to VoiceNotice.FAILED,
        DictationError.AUDIO to VoiceNotice.FAILED,
        DictationError.UNAVAILABLE to null,
        DictationError.UNKNOWN to VoiceNotice.FAILED,
    )
