package com.kurobello.tallybook.core.data.voice

import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AndroidVoiceDictationTest {
  @Test
  fun `recognizer errors map to what the user can act on`() {
    val cases =
        mapOf(
            SpeechRecognizer.ERROR_NO_MATCH to DictationError.NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT to DictationError.SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS to DictationError.PERMISSION,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED to DictationError.LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE to DictationError.LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY to DictationError.BUSY,
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS to DictationError.BUSY,
            SpeechRecognizer.ERROR_AUDIO to DictationError.AUDIO,
            SpeechRecognizer.ERROR_NETWORK to DictationError.UNKNOWN,
            SpeechRecognizer.ERROR_CLIENT to DictationError.UNKNOWN,
        )

    cases.forEach { (code, expected) -> assertEquals(expected, dictationErrorOf(code), "$code") }
  }

  @Test
  fun `the pause limits reach a recognizer that reads them as ints`() {
    val intent = recognizerIntent("es-ES")

    listOf(
            RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
            RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
        )
        .forEach { extra -> assertEquals(HELD_PAUSE_MILLIS, intent.getIntExtra(extra, 0), extra) }
  }
}
