package com.kurobello.tallybook.core.data.voice

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionDenied
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.localeIdentifier
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus

// Speech reports "nothing matched" and "nothing was said" through these undocumented codes.
private const val ASSISTANT_ERROR_DOMAIN = "kAFAssistantErrorDomain"
private const val NO_MATCH_CODE = 203L
private const val NO_SPEECH_CODE = 1110L
private val noSpeechErrorCodes: Set<Long> = setOf(NO_MATCH_CODE, NO_SPEECH_CODE)

private const val TAP_BUFFER_FRAMES = 1024u

/**
 * Recognition runs on this iPhone only, like on Android: the sentence describes the user's money,
 * so a language without an on-device model fails instead of reaching Apple's servers.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosVoiceDictation(private val mainDispatcher: CoroutineDispatcher) : VoiceDictation {

  override fun permission(): DictationPermission {
    val speech = SFSpeechRecognizer.authorizationStatus()
    val microphone = AVAudioApplication.sharedInstance.recordPermission
    return when {
      speech == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusDenied ||
          speech ==
              SFSpeechRecognizerAuthorizationStatus
                  .SFSpeechRecognizerAuthorizationStatusRestricted ||
          microphone == AVAudioApplicationRecordPermissionDenied -> DictationPermission.DENIED
      speech ==
          SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized &&
          microphone == AVAudioApplicationRecordPermissionGranted -> DictationPermission.GRANTED
      else -> DictationPermission.NOT_DETERMINED
    }
  }

  override fun isAvailable(): Boolean = SFSpeechRecognizer.supportedLocales().isNotEmpty()

  override fun listen(languageTag: String, finishRequests: Flow<Unit>): Flow<DictationEvent> =
      callbackFlow {
            val recognizer = supportedRecognizer(languageTag)
            when {
              recognizer == null || !recognizer.supportsOnDeviceRecognition ->
                  fail(DictationError.LANGUAGE_UNAVAILABLE)
              !recognizer.isAvailable() -> fail(DictationError.UNAVAILABLE)
              !activateRecordingSession() -> fail(DictationError.AUDIO)
              else -> recognize(recognizer, finishRequests)
            }
          }
          .flowOn(mainDispatcher)

  private suspend fun ProducerScope<DictationEvent>.recognize(
      recognizer: SFSpeechRecognizer,
      finishRequests: Flow<Unit>,
  ) {
    val request =
        SFSpeechAudioBufferRecognitionRequest().apply {
          shouldReportPartialResults = true
          requiresOnDeviceRecognition = true
        }
    val engine = AVAudioEngine()
    val input = engine.inputNode
    input.installTapOnBus(0u, TAP_BUFFER_FRAMES, input.outputFormatForBus(0u)) { buffer, _ ->
      buffer?.let(request::appendAudioPCMBuffer)
    }
    var tapInstalled = true
    fun stopCapturing() {
      if (tapInstalled) {
        tapInstalled = false
        engine.stop()
        input.removeTapOnBus(0u)
      }
    }
    engine.prepare()
    if (!startEngine(engine)) {
      stopCapturing()
      deactivateRecordingSession()
      fail(DictationError.AUDIO)
      return
    }
    // The recognizer answers on its own queue; trySend is safe from any thread.
    val task =
        recognizer.recognitionTaskWithRequest(request) { result, error ->
          when {
            result != null && result.isFinal() -> {
              trySend(DictationEvent.Final(result.bestTranscription.formattedString))
              close()
            }
            result != null ->
                trySend(DictationEvent.Partial(result.bestTranscription.formattedString))
            error != null -> {
              trySend(DictationEvent.Failed(dictationErrorOf(error)))
              close()
            }
          }
        }
    val interruptions =
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
          trySend(DictationEvent.Failed(DictationError.AUDIO))
          close()
        }
    trySend(DictationEvent.Listening)
    launch {
      finishRequests.collect {
        stopCapturing()
        request.endAudio()
      }
    }
    awaitClose {
      NSNotificationCenter.defaultCenter.removeObserver(interruptions)
      task.cancel()
      stopCapturing()
      deactivateRecordingSession()
    }
  }

  private suspend fun ProducerScope<DictationEvent>.fail(error: DictationError) {
    send(DictationEvent.Failed(error))
    close()
  }

  /** iOS spells regions with an underscore ("es_CO"), and BCP 47 tags use a hyphen. */
  private fun supportedRecognizer(languageTag: String): SFSpeechRecognizer? {
    val wanted = languageTag.replace('-', '_').lowercase()
    val locale =
        SFSpeechRecognizer.supportedLocales().filterIsInstance<NSLocale>().firstOrNull {
          it.localeIdentifier().lowercase() == wanted
        }
    return locale?.let { SFSpeechRecognizer(locale = it) }
  }

  private fun startEngine(engine: AVAudioEngine): Boolean = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    engine.startAndReturnError(error.ptr) && error.value == null
  }

  private fun activateRecordingSession(): Boolean = memScoped {
    val session = AVAudioSession.sharedInstance()
    val error = alloc<ObjCObjectVar<NSError?>>()
    session.setCategory(
        AVAudioSessionCategoryRecord,
        mode = AVAudioSessionModeMeasurement,
        options = AVAudioSessionCategoryOptionDuckOthers,
        error = error.ptr,
    ) &&
        session.setActive(
            true,
            withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
            error = error.ptr,
        )
  }

  private fun deactivateRecordingSession() {
    AVAudioSession.sharedInstance()
        .setActive(
            false,
            withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
            error = null,
        )
  }
}

internal fun dictationErrorOf(error: NSError): DictationError =
    if (error.domain == ASSISTANT_ERROR_DOMAIN && error.code in noSpeechErrorCodes)
        DictationError.NO_MATCH
    else DictationError.UNKNOWN
