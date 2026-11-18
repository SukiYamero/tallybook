package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Frecuencia
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoActions
import kotlinx.datetime.LocalDate

/** A base for fakes that record only the actions a test is about. */
@Suppress("TooManyFunctions")
internal open class NoOpAddMovimientoActions : AddMovimientoActions {
  override fun onTipoChanged(tipo: TipoMovimiento) = Unit

  override fun onAmountChanged(value: String) = Unit

  override fun onFechaChanged(value: LocalDate) = Unit

  override fun onCategoriaSelected(id: String) = Unit

  override fun onCategoriaCleared() = Unit

  override fun onEtiquetasChanged(ids: List<String>) = Unit

  override fun onEtiquetaRemoved(id: String) = Unit

  override fun onCreateEtiqueta(nombre: String) = Unit

  override fun onRetryEtiquetas() = Unit

  override fun onTituloChanged(value: String) = Unit

  override fun onDescripcionChanged(value: String) = Unit

  override fun onRetryLoad() = Unit

  override fun onSubmit() = Unit

  override fun onMedioSelected(medio: MedioPago) = Unit

  override fun onCuentaSelected(id: String) = Unit

  override fun onDiscardChanges() = Unit

  override fun onPhraseChanged(value: String) = Unit

  override fun onPhraseContinue() = Unit

  override fun onFillManually() = Unit

  override fun onEditMovimiento() = Unit

  override fun onSuggestionSelected(id: String) = Unit

  override fun onStepBack() = Unit

  override fun onRepetirChanged(enabled: Boolean) = Unit

  override fun onFrecuenciaChanged(frecuencia: Frecuencia) = Unit

  override fun onPartCategoriaSelected(index: Int, id: String) = Unit

  override fun onPartCategoriaCleared(index: Int) = Unit

  override fun onPartRemoved(index: Int) = Unit

  override fun onJoinParts() = Unit

  override fun onPartEdit(index: Int) = Unit

  override fun onPartDone() = Unit

  override fun onVoiceStart() = Unit

  override fun onVoiceStop() = Unit

  override fun onVoiceCancel() = Unit

  override fun onVoiceDiscard() = Unit

  override fun onVoicePermissionResult(granted: Boolean) = Unit

  override fun onVoiceOpenSettings() = Unit
}
