package com.kurobello.tallybook.feature.history.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kurobello.tallybook.core.model.Actividad
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Direccion
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.MovimientoSummary
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.TipoMovimiento
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.core.ui.state.RetainedLoadState
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.history.viewmodel.HistoryActions
import com.kurobello.tallybook.feature.history.viewmodel.HistoryFailure
import com.kurobello.tallybook.feature.history.viewmodel.HistoryProjection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryScope
import com.kurobello.tallybook.feature.history.viewmodel.HistorySelection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Preview
@Composable
internal fun HistoryContentLightPreview() = HistoryPreview(historyPreviewState(), dark = false)

@Preview
@Composable
internal fun HistoryContentDarkPreview() = HistoryPreview(historyPreviewState(), dark = true)

@Preview
@Composable
internal fun HistoryEmptyPreview() {
  val content =
      HistoryProjection.Content(
          MovimientoSummary(Money(0, Moneda.COP), Money(0, Moneda.COP)),
          emptyList(),
      )
  HistoryPreview(
      historyPreviewState().copy(period = RetainedLoadState.Ready(content)),
      dark = false,
  )
}

@Preview
@Composable
internal fun HistoryLoadingPreview() =
    HistoryPreview(
        historyPreviewState().copy(period = RetainedLoadState.Loading()),
        dark = false,
    )

@Preview
@Composable
internal fun HistoryReadFailurePreview() =
    HistoryPreview(
        historyPreviewState().copy(period = RetainedLoadState.Failed(historyPreviewFailure)),
        dark = false,
    )

@Preview
@Composable
internal fun HistoryCalculationFailurePreview() =
    HistoryPreview(
        historyPreviewState()
            .copy(period = RetainedLoadState.Failed(HistoryFailure.CalculationOverflow)),
        dark = true,
    )

@Preview
@Composable
internal fun HistoryLargeTextPreview() {
  val density = LocalDensity.current
  CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
    HistoryPreview(historyPreviewState(), dark = false)
  }
}

@Composable
private fun HistoryPreview(state: HistoryUiState, dark: Boolean) {
  TallybookTheme(darkTheme = dark) {
    HistoryScreen(state, PreviewActions, HistoryNavigationCallbacks({}), 0.dp)
  }
}

private val historyPreviewFailure =
    HistoryFailure.Repository(DataError.Local(IllegalStateException("Preview read failure")))

@Suppress("MagicNumber")
private fun historyPreviewState(): HistoryUiState {
  val date = LocalDate(2026, 9, 15)
  val movement =
      Movimiento(
          "preview",
          TipoMovimiento.GASTO,
          Money(128_500, Moneda.COP),
          date,
          "food",
          "Lunch with Ana",
          null,
          Instant.parse("2026-09-15T12:00:00Z"),
          Cuenta.defaultCashId(Moneda.COP),
          MedioPago.EFECTIVO,
      )
  val summary = MovimientoSummary(Money(200_000, Moneda.COP), movement.monto)
  val content =
      HistoryProjection.Content(
          summary,
          listOf(
              HistoryDayGroupUi(
                  date,
                  listOf(
                      previewTransferRow,
                      HistoryMovimientoRowUi(
                          movement,
                          null,
                          checkNotNull(movement.titulo),
                          "Food",
                      ),
                  ),
              )
          ),
      )
  return HistoryUiState(
      HistorySelection(HistoryScope.Day, date),
      date,
      Moneda.COP,
      RetainedLoadState.Ready(content),
      primary = Moneda.COP,
      monedas = listOf(Moneda.COP, Moneda.USD),
  )
}

@Suppress("MagicNumber")
private val previewTransferRow: HistoryTransferenciaRowUi =
    Transferencia(
            "preview-transfer",
            "usd",
            "nequi",
            Money(10_000, Moneda.USD),
            Money(40_000_000, Moneda.COP),
            LocalDate(2026, 9, 15),
            "Ahorro del mes",
            Instant.parse("2026-09-15T13:00:00Z"),
        )
        .let { transfer ->
          val created = Instant.parse("2026-01-01T00:00:00Z")
          HistoryTransferenciaRowUi(
              Actividad.DeTransferencia(transfer, transfer.montoDestino, Direccion.ENTRADA),
              Cuenta(
                  "usd",
                  "Dólares",
                  TipoCuenta.EFECTIVO,
                  Money(0, Moneda.USD),
                  null,
                  false,
                  created,
              ),
              Cuenta(
                  "nequi",
                  "Nequi",
                  TipoCuenta.BANCO,
                  Money(0, Moneda.COP),
                  null,
                  false,
                  created,
              ),
          )
        }

private object PreviewActions : HistoryActions {
  override fun onScopeChanged(scope: HistoryScope) = Unit

  override fun onYearSelected(year: Int) = Unit

  override fun onPeriodSelected(period: MovimientoDateRange) = Unit

  override fun onMonedaSelected(moneda: Moneda) = Unit

  override fun onRetry() = Unit

  override fun onResume() = Unit

  override fun onPresentationChanged(unknownCategoryLabel: String) = Unit
}
