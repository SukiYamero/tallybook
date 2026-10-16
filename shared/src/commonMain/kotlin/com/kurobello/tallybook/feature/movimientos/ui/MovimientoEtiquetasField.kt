package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kurobello.tallybook.core.model.Etiqueta
import com.kurobello.tallybook.core.model.MAX_ETIQUETAS_POR_MOVIMIENTO
import com.kurobello.tallybook.core.ui.tag.EtiquetaSectionHeading
import com.kurobello.tallybook.core.ui.tag.EtiquetaTagField
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.movimientos.viewmodel.EtiquetasLoad
import com.kurobello.tallybook.feature.movimientos.viewmodel.MovimientoEtiquetaActions
import org.jetbrains.compose.resources.stringResource
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.tags_unavailable

internal data class MovimientoEtiquetasCallbacks(
    val onAdd: () -> Unit,
    val onRemove: (Etiqueta) -> Unit,
    val onRetry: () -> Unit,
)

/** Field callbacks backed by [MovimientoEtiquetaActions]; [onAdd] opens the caller's picker. */
internal fun MovimientoEtiquetaActions.etiquetasCallbacks(onAdd: () -> Unit) =
    MovimientoEtiquetasCallbacks(
        onAdd = onAdd,
        onRemove = { onEtiquetaRemoved(it.id) },
        onRetry = ::onRetryEtiquetas,
    )

/**
 * The optional tags section of every movimiento surface. An unavailable catalog replaces only this
 * section with a retry, so the rest of the entry stays usable and saves without tags.
 */
@Composable
internal fun MovimientoEtiquetasField(
    etiquetas: List<Etiqueta>,
    load: EtiquetasLoad,
    callbacks: MovimientoEtiquetasCallbacks,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
  if (load == EtiquetasLoad.UNAVAILABLE) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space8),
    ) {
      EtiquetaSectionHeading()
      MovimientoFormRetryError(
          message = stringResource(Res.string.tags_unavailable),
          onRetry = callbacks.onRetry,
          interactionEnabled = enabled,
      )
    }
  } else {
    EtiquetaTagField(
        etiquetas = etiquetas,
        max = MAX_ETIQUETAS_POR_MOVIMIENTO,
        onRemove = callbacks.onRemove,
        onAdd = callbacks.onAdd,
        modifier = modifier,
        enabled = enabled && load == EtiquetasLoad.READY,
    )
  }
}
