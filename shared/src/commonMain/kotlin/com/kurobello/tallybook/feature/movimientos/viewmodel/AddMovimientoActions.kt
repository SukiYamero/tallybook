package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.compose.runtime.Stable
import com.kurobello.tallybook.core.model.Frecuencia

@Stable
internal interface AddMovimientoActions : MovimientoFormActions, VoiceDictationActions {
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

@Stable
internal interface VoiceDictationActions {
  /** Pressing the microphone; dictated words extend the sentence already typed. */
  fun onVoiceStart()

  /** Releasing the microphone: stops listening and keeps what was heard. */
  fun onVoiceStop()

  /** Drops the session without waiting for the recognizer, as when the dialog closes. */
  fun onVoiceCancel()

  fun onVoicePermissionResult(granted: Boolean)

  fun onVoiceOpenSettings()
}
