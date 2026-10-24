@file:Suppress("UnusedPrivateMember")

package com.kurobello.tallybook.feature.movimientos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.ui.theme.TallybookTheme

@Composable
private fun MovimientoCuentaFieldPreviewContent() {
  val samples = rememberMovimientoEntrySamples()
  Column(
      modifier = Modifier.padding(TallybookTheme.spacing.space16),
      verticalArrangement = Arrangement.spacedBy(TallybookTheme.spacing.space32),
  ) {
    MovimientoCuentaField(samples.cashCuenta, MovimientoCuentaCallbacks.Inert, enabled = true)
    MovimientoCuentaField(samples.creditCuenta, MovimientoCuentaCallbacks.Inert, enabled = true)
    MovimientoCuentaField(
        samples.emptyCuenta,
        MovimientoCuentaCallbacks.Inert.copy(onCreateCuenta = {}),
        enabled = true,
    )
  }
}

@Preview
@Composable
private fun MovimientoCuentaFieldLightPreview() {
  TallybookTheme(darkTheme = false) { MovimientoCuentaFieldPreviewContent() }
}

@Preview
@Composable
private fun MovimientoCuentaFieldDarkPreview() {
  TallybookTheme(darkTheme = true) { MovimientoCuentaFieldPreviewContent() }
}
