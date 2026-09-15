package com.kurobello.tallybook.feature.movimientos.viewmodel

import androidx.compose.runtime.Stable
import com.kurobello.tallybook.core.model.TipoMovimiento
import kotlinx.datetime.LocalDate

@Stable
internal interface AddMovimientoActions {
  fun onTipoChanged(tipo: TipoMovimiento)

  fun onAmountChanged(value: String)

  fun onFechaChanged(value: LocalDate)

  fun onCategoriaSelected(id: String)

  fun onDescripcionChanged(value: String)

  fun onRetryLoad()

  fun onSubmit()
}
