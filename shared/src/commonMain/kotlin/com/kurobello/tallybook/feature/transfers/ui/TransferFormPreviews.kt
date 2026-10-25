@file:Suppress("UnusedPrivateMember", "MagicNumber")

package com.kurobello.tallybook.feature.transfers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferDraftState
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferEntry
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferFormActions
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Preview
@Composable
private fun TransferFormPayCardLightPreview() =
    TransferFormPreview(
        darkTheme = false,
        entry = TransferEntry.New(NEQUI.id, NU.id, Money(35_000_000, Moneda.COP)),
    )

@Preview
@Composable
private fun TransferFormCrossCurrencyDarkPreview() =
    TransferFormPreview(
        darkTheme = true,
        entry = TransferEntry.New(DOLARES.id, NEQUI.id, prefill = null),
        submitted = true,
    )

@Composable
private fun TransferFormPreview(
    darkTheme: Boolean,
    entry: TransferEntry.New,
    submitted: Boolean = false,
) {
  val form =
      TransferDraftState.forNew(PREVIEW_CUENTAS, Moneda.COP, entry, PREVIEW_TODAY)
          .copy(submitAttempted = submitted)
  TallybookTheme(darkTheme = darkTheme) {
    TransferForm(
        form = form,
        actions = PreviewTransferActions,
        enabled = true,
        modifier =
            Modifier.background(TallybookTheme.colors.surface.raised)
                .padding(TallybookTheme.dimensions.screenHorizontal),
    )
  }
}

private object PreviewTransferActions : TransferFormActions {
  override fun onOrigenSelected(id: String) = Unit

  override fun onDestinoSelected(id: String) = Unit

  override fun onMontoOrigenChanged(value: String) = Unit

  override fun onMontoDestinoChanged(value: String) = Unit

  override fun onFechaChanged(value: LocalDate) = Unit

  override fun onDescripcionChanged(value: String) = Unit
}

private val PREVIEW_TODAY = LocalDate(2026, 10, 16)
private val PREVIEW_CREATED_AT = Instant.parse("2026-01-01T00:00:00Z")

private fun previewCuenta(id: String, nombre: String, tipo: TipoCuenta, moneda: Moneda) =
    Cuenta(id, nombre, tipo, Money(0, moneda), null, archivada = false, PREVIEW_CREATED_AT)

private val NEQUI = previewCuenta("nequi", "Nequi", TipoCuenta.BANCO, Moneda.COP)
private val NU = previewCuenta("nu", "NU", TipoCuenta.CREDITO, Moneda.COP)
private val DOLARES = previewCuenta("dolares", "Dólares", TipoCuenta.BANCO, Moneda.USD)
private val PREVIEW_CUENTAS =
    listOf(
        previewCuenta("efectivo", "Efectivo", TipoCuenta.EFECTIVO, Moneda.COP),
        NEQUI,
        NU,
        DOLARES,
    )
