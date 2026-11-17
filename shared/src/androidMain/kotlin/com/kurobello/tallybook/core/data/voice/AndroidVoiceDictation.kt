package com.kurobello.tallybook.core.data.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private val recognizerErrors: Map<Int, DictationError> =
    mapOf(
        SpeechRecognizer.ERROR_AUDIO to DictationError.AUDIO,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT to DictationError.SPEECH_TIMEOUT,
        SpeechRecognizer.ERROR_NO_MATCH to DictationError.NO_MATCH,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY to DictationError.BUSY,
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS to DictationError.BUSY,
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS to DictationError.PERMISSION,
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED to DictationError.LANGUAGE_UNAVAILABLE,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE to DictationError.LANGUAGE_UNAVAILABLE,
    )

private const val HELD_PAUSE_MILLIS = 10_000L

internal fun dictationErrorOf(code: Int): DictationError =
    recognizerErrors[code] ?: DictationError.UNKNOWN

/**
 * Only the on-device recognizer is used: the default one may stream the audio to a server, and the
 * sentence describes the user's money. Every SpeechRecognizer call must run on the main thread.
 */
internal class AndroidVoiceDictation(
    private val context: Context,
    private val mainDispatcher: CoroutineDispatcher,
) : VoiceDictation {

  override fun permission(): DictationPermission =
      if (
          context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
              PackageManager.PERMISSION_GRANTED
      )
          DictationPermission.GRANTED
      else DictationPermission.NOT_DETERMINED

  override fun isAvailable(): Boolean = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

  override fun listen(languageTag: String, finishRequests: Flow<Unit>): Flow<DictationEvent> =
      callbackFlow {
            if (!isAvailable()) {
              send(DictationEvent.Failed(DictationError.UNAVAILABLE))
              close()
              return@callbackFlow
            }
            val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            var activeTag = languageTag
            recognizer.setRecognitionListener(
                object : RecognitionListener {
                  override fun onReadyForSpeech(params: Bundle?) {
                    trySend(DictationEvent.Listening)
                  }

                  override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.bestTranscript()?.let { trySend(DictationEvent.Partial(it)) }
                  }

                  override fun onResults(results: Bundle?) {
                    trySend(DictationEvent.Final(results?.bestTranscript().orEmpty()))
                    close()
                  }

                  // Recovers a missing model the support check could not report.
                  override fun onError(error: Int) {
                    if (
                        error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                      requestDownload(recognizer, activeTag)
                    } else {
                      trySend(DictationEvent.Failed(dictationErrorOf(error)))
                      close()
                    }
                  }

                  override fun onBeginningOfSpeech() = Unit

                  override fun onRmsChanged(rmsdB: Float) = Unit

                  override fun onBufferReceived(buffer: ByteArray?) = Unit

                  override fun onEndOfSpeech() = Unit

                  override fun onEvent(eventType: Int, params: Bundle?) = Unit
                }
            )
            // Started in a child so the teardown below is registered before anything suspends:
            // a session cancelled during the language check must still release the recognizer.
            launch {
              when (val choice = languageChoice(recognizer, languageTag)) {
                is RecognizerLanguageChoice.Ready -> {
                  activeTag = choice.tag
                  recognizer.startListening(recognizerIntent(choice.tag))
                  finishRequests.collect { recognizer.stopListening() }
                }
                is RecognizerLanguageChoice.Download -> requestDownload(recognizer, choice.tag)
                RecognizerLanguageChoice.Downloading -> fail(DictationError.LANGUAGE_DOWNLOADING)
                RecognizerLanguageChoice.Unsupported -> fail(DictationError.LANGUAGE_UNAVAILABLE)
              }
            }
            // SpeechRecognizer queues its commands on the main looper; destroying it in the same
            // turn drops a queued download request and crashes Android 13 with a
            // NullPointerException, so destroy runs after the queue has passed it.
            awaitClose {
              recognizer.cancel()
              Handler(Looper.getMainLooper()).post { recognizer.destroy() }
            }
          }
          .flowOn(mainDispatcher)

  /**
   * Language models ship separately from the recognizer (a phone set to English may only have
   * en-US), so the request is matched against what is installed before listening. Before API 33
   * there is no way to ask; listening is tried directly.
   */
  private suspend fun languageChoice(
      recognizer: SpeechRecognizer,
      languageTag: String,
  ): RecognizerLanguageChoice =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        checkLanguages(recognizer, languageTag)
      } else {
        RecognizerLanguageChoice.Ready(languageTag)
      }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private suspend fun checkLanguages(
      recognizer: SpeechRecognizer,
      languageTag: String,
  ): RecognizerLanguageChoice = suspendCancellableCoroutine { continuation ->
    recognizer.checkRecognitionSupport(
        recognizerIntent(languageTag),
        context.mainExecutor,
        object : RecognitionSupportCallback {
          override fun onSupportResult(support: RecognitionSupport) {
            val languages =
                RecognizerLanguages(
                    installed = support.installedOnDeviceLanguages,
                    pending = support.pendingOnDeviceLanguages,
                    supported = support.supportedOnDeviceLanguages,
                )
            if (continuation.isActive) {
              continuation.resume(chooseRecognizerLanguage(languageTag, languages))
            }
          }

          // A service that cannot report support may still recognize: try it.
          override fun onError(error: Int) {
            if (continuation.isActive) {
              continuation.resume(RecognizerLanguageChoice.Ready(languageTag))
            }
          }
        },
    )
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun ProducerScope<DictationEvent>.requestDownload(
      recognizer: SpeechRecognizer,
      languageTag: String,
  ) {
    recognizer.triggerModelDownload(recognizerIntent(languageTag))
    fail(DictationError.LANGUAGE_DOWNLOADING)
  }

  private fun ProducerScope<DictationEvent>.fail(error: DictationError) {
    trySend(DictationEvent.Failed(error))
    close()
  }

  private fun recognizerIntent(languageTag: String): Intent =
      Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
          .putExtra(
              RecognizerIntent.EXTRA_LANGUAGE_MODEL,
              RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
          )
          .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
          .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
          // The release ends the sentence, so a pause while the mic is held should not; the
          // recognizer may still apply its own shorter limit.
          .putExtra(
              RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
              HELD_PAUSE_MILLIS,
          )
          .putExtra(
              RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
              HELD_PAUSE_MILLIS,
          )
}

private fun Bundle.bestTranscript(): String? =
    getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf {
      it.isNotBlank()
    }
