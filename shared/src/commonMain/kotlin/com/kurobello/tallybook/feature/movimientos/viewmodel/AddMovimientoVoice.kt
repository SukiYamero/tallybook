package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.data.voice.DictationError
import com.kurobello.tallybook.core.data.voice.DictationEvent
import com.kurobello.tallybook.core.data.voice.DictationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What the voice session reads from and writes to the add-movimiento screen it belongs to. */
internal interface AddMovimientoVoiceHost {
  val state: AddMovimientoUiState

  fun dictationLanguageTag(): String

  fun updateVoice(transform: VoiceInputUi.() -> VoiceInputUi)

  /** Replaces the sentence without stopping dictation, unlike a keystroke. */
  fun applyDictatedPhrase(value: String)

  /** Puts back a sentence and its Detected card at once, so cancelled words never linger there. */
  fun restorePhrase(value: String)

  fun send(effect: AddMovimientoEffect)
}

/** The microphone's permission flow and its single dictation session. */
internal class AddMovimientoVoice(
    private val dictation: MovimientoDictation,
    private val scope: CoroutineScope,
    private val host: AddMovimientoVoiceHost,
) {
  private var session: Job? = null
  private var stops = Channel<Unit>(Channel.CONFLATED)
  private var phraseBeforeSession = ""
  private var permissionDenials = 0
  // Android answers a second request made while the dialog is up with an instant refusal, which
  // would count as a denial the user never gave.
  private var permissionPending = false

  val isAvailable: Boolean = dictation.isAvailable()

  fun start() {
    val state = host.state
    val isBusy = state.voice.isListening || state.isSubmitting || permissionPending
    if (!state.voice.isAvailable || isBusy) return
    when (dictation.permission()) {
      DictationPermission.GRANTED -> listen()
      DictationPermission.NOT_DETERMINED -> {
        permissionPending = true
        host.send(AddMovimientoEffect.RequestDictationPermission)
      }
      DictationPermission.DENIED ->
          host.updateVoice { copy(notice = VoiceNotice.PERMISSION_BLOCKED) }
    }
  }

  fun finish() {
    stops.trySend(Unit)
    if (host.state.voice.isListening) host.updateVoice { copy(isFinishing = true) }
  }

  fun cancel() {
    session?.cancel()
    session = null
    if (host.state.voice.isListening) {
      host.updateVoice { copy(isListening = false, isMicReady = false, isFinishing = false) }
    }
  }

  /**
   * Cancels a held session and drops everything it dictated, restoring the sentence it extended.
   */
  fun discard() {
    if (!host.state.voice.canDiscard) return
    cancel()
    host.restorePhrase(phraseBeforeSession)
  }

  // A second refusal in a session is how a permanent denial looks: the system stops asking.
  // A granted permission does not start listening: the press that asked for it is long gone, so
  // the hint invites the next one.
  fun onPermissionResult(granted: Boolean) {
    permissionPending = false
    if (granted) {
      permissionDenials = 0
      host.updateVoice { copy(notice = null) }
    } else {
      permissionDenials++
      val notice =
          if (permissionDenials > 1) VoiceNotice.PERMISSION_BLOCKED
          else VoiceNotice.PERMISSION_DENIED
      host.updateVoice { copy(notice = notice) }
    }
  }

  private fun listen() {
    val state = host.state
    if (state.step != AddMovimientoStep.PHRASE || state.voice.isListening) return
    val sessionStops = Channel<Unit>(Channel.CONFLATED).also { stops = it }
    phraseBeforeSession = state.phrase
    val typed = state.phrase.trim()
    val prefix = if (typed.isEmpty()) "" else "$typed "
    host.updateVoice {
      copy(
          isListening = true,
          isMicReady = false,
          isFinishing = false,
          detectedShownAtStart = typed.isNotEmpty(),
          notice = null,
      )
    }
    session = scope.launch {
      var heard = false
      dictation.listen(host.dictationLanguageTag(), sessionStops.receiveAsFlow()).collect { event ->
        val text = event.transcript
        if (!text.isNullOrBlank()) {
          heard = true
          host.applyDictatedPhrase(prefix + text.trim())
        }
        when (event) {
          is DictationEvent.Final ->
              host.updateVoice {
                copy(
                    isListening = false,
                    isMicReady = false,
                    isFinishing = false,
                    notice = VoiceNotice.NOT_HEARD.takeUnless { heard },
                )
              }
          is DictationEvent.Failed -> host.updateVoice { afterFailure(event.error, heard) }
          DictationEvent.Listening -> host.updateVoice { copy(isMicReady = true) }
          is DictationEvent.Partial -> Unit
        }
      }
    }
  }
}

private val DictationEvent.transcript: String?
  get() = (this as? DictationEvent.Partial)?.text ?: (this as? DictationEvent.Final)?.text

// What was heard before a timeout or a missed final word is kept without a warning.
private fun VoiceInputUi.afterFailure(error: DictationError, heard: Boolean): VoiceInputUi {
  val notice = dictationErrorNotices[error]
  return copy(
      isListening = false,
      isMicReady = false,
      isFinishing = false,
      isAvailable = isAvailable && error != DictationError.UNAVAILABLE,
      notice = notice.takeUnless { heard && it == VoiceNotice.NOT_HEARD },
  )
}
