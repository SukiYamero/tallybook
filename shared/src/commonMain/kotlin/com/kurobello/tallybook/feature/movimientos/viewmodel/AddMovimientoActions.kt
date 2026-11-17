package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.compose.runtime.Stable
import com.kurobello.tallybook.core.model.Frecuencia

@Stable
internal interface AddMovimientoActions :
    MovimientoFormActions, VoiceDictationActions, MovimientoPartActions {
  fun onDiscardChanges()

  fun onPhraseChanged(value: String)

  fun onPhraseContinue()

  fun onFillManually()

  fun onEditMovimiento()

  fun onSuggestionSelected(id: String)

  fun onCategoriaCleared()

  fun onStepBack()

  /** Turning Repetir on starts with a monthly frecuencia. */
  fun onRepetirChanged(enabled: Boolean)

  fun onFrecuenciaChanged(frecuencia: Frecuencia)
}

/** The review of a sentence split into several movimientos, one part per index. */
@Stable
internal interface MovimientoPartActions {
  fun onPartCategoriaSelected(index: Int, id: String)

  fun onPartCategoriaCleared(index: Int)

  /** Removing down to one part leaves the summary of that part. */
  fun onPartRemoved(index: Int)

  /** Turns the parts into one movimiento: their amounts added, the shared values kept. */
  fun onJoinParts()
}

@Stable
internal interface VoiceDictationActions {
  /** Pressing the microphone; dictated words extend the sentence already typed. */
  fun onVoiceStart()

  /** Releasing the microphone: stops listening and keeps what was heard. */
  fun onVoiceStop()

  /** Drops the session without waiting for the recognizer, as when the dialog closes. */
  fun onVoiceCancel()

  /** Sliding away from the held microphone: stops and restores the sentence typed before it. */
  fun onVoiceDiscard()

  fun onVoicePermissionResult(granted: Boolean)

  fun onVoiceOpenSettings()
}
