package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.compose.runtime.Stable
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

@Stable
internal interface MovimientoEtiquetaActions {
  /** The whole selection the tag picker reports; ignored until the tags catalog is ready. */
  fun onEtiquetasChanged(ids: List<String>)

  fun onEtiquetaRemoved(id: String)

  /** Creates a tag from the picker's search and selects it while the limit allows. */
  fun onCreateEtiqueta(nombre: String)

  fun onRetryEtiquetas()
}

@Stable
internal interface MovimientoCuentaActions {
  /** Keeps a compatible cuenta, otherwise selects the default cuenta for [medio]. */
  fun onMedioSelected(medio: MedioPago)

  fun onCuentaSelected(id: String)
}

@Stable
internal interface MovimientoFormActions : MovimientoEtiquetaActions, MovimientoCuentaActions {
  fun onTipoChanged(tipo: TipoMovimiento)

  fun onAmountChanged(value: String)

  fun onFechaChanged(value: LocalDate)

  fun onCategoriaSelected(id: String)

  fun onTituloChanged(value: String)

  fun onDescripcionChanged(value: String)

  fun onRetryLoad()

  fun onSubmit()
}
