package com.kurobello.tallybook.feature.accounts.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kurobello.tallybook.core.model.CreditEstimate
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.MovimientoDateRange
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.ui.account.CuentaBalance
import com.kurobello.tallybook.core.ui.theme.TallybookTheme
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountDraftState
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountGroup
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsActions
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsContent
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsUiState
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Suppress("TooManyFunctions")
private object PreviewAccountsActions : AccountsActions {
  override fun onSearchChanged(query: String) = Unit

  override fun onRetry() = Unit

  override fun onToggleArchived() = Unit

  override fun onCreate() = Unit

  override fun onOpenCuenta(id: String) = Unit

  override fun onDraftChanged(transform: AccountDraftState.() -> AccountDraftState) = Unit

  override fun onSave() = Unit

  override fun onRequestArchive(archived: Boolean) = Unit

  override fun onConfirmArchive() = Unit

  override fun onRequestDelete() = Unit

  override fun onConfirmDelete() = Unit

  override fun onCancelConfirmation() = Unit

  override fun onDismissSheet() = false

  override fun onConfirmDiscard() = Unit

  override fun onContinueEditing() = Unit

  override fun onSheetHidden() = Unit

  override fun onTransfer() = Unit

  override fun onPayCard() = Unit
}

private val previewCreatedAt = Instant.parse("2026-01-01T00:00:00Z")

private fun previewCuenta(id: String, nombre: String, tipo: TipoCuenta, saldoInicial: Money) =
    Cuenta(id, nombre, tipo, saldoInicial, diaCorte = null, archivada = false, previewCreatedAt)

@Suppress("MagicNumber")
private val previewState =
    AccountsUiState(
        content =
            AccountsContent.Ready(
                totals = listOf(Money(312_450_000, Moneda.COP), Money(25_000, Moneda.USD)),
                groups =
                    listOf(
                        AccountGroup(
                            TipoCuenta.EFECTIVO,
                            listOf(
                                CuentaBalance(
                                    previewCuenta(
                                        "efectivo",
                                        "Efectivo",
                                        TipoCuenta.EFECTIVO,
                                        Money(0, Moneda.COP),
                                    ),
                                    Money(18_000_000, Moneda.COP),
                                )
                            ),
                        ),
                        AccountGroup(
                            TipoCuenta.BANCO,
                            listOf(
                                CuentaBalance(
                                    previewCuenta(
                                        "nequi",
                                        "Nequi",
                                        TipoCuenta.BANCO,
                                        Money(0, Moneda.COP),
                                    ),
                                    Money(344_450_000, Moneda.COP),
                                ),
                                CuentaBalance(
                                    previewCuenta(
                                        "usd",
                                        "Nequi",
                                        TipoCuenta.BANCO,
                                        Money(0, Moneda.USD),
                                    ),
                                    Money(25_000, Moneda.USD),
                                    showsMoneda = true,
                                ),
                            ),
                        ),
                        AccountGroup(
                            TipoCuenta.CREDITO,
                            listOf(
                                CuentaBalance(
                                    previewCuenta(
                                        "nu",
                                        "NU",
                                        TipoCuenta.CREDITO,
                                        Money(0, Moneda.COP),
                                    ),
                                    Money(-50_000_000, Moneda.COP),
                                    CreditEstimate(
                                        deuda = Money(50_000_000, Moneda.COP),
                                        cycleSpending = Money(12_500_000, Moneda.COP),
                                        cycle =
                                            MovimientoDateRange(
                                                LocalDate(2026, 10, 16),
                                                LocalDate(2026, 11, 15),
                                            ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                archived = emptyList(),
            )
    )

@Preview
@Composable
internal fun AccountsScreenLightPreview() {
  TallybookTheme(darkTheme = false) {
    AccountsScreen(previewState, PreviewAccountsActions, onBack = {})
  }
}

@Preview
@Composable
internal fun AccountsScreenDarkPreview() {
  TallybookTheme(darkTheme = true) {
    AccountsScreen(previewState, PreviewAccountsActions, onBack = {})
  }
}

@Preview
@Composable
internal fun AccountFormLightPreview() {
  TallybookTheme(darkTheme = false) {
    AccountForm(
        state =
            AccountDraftState.forNew("NU", TipoCuenta.CREDITO, Moneda.COP)
                .copy(saldo = "500000", diaCorte = "15"),
        onDraftChanged = {},
    )
  }
}

@Preview
@Composable
internal fun AccountFormDarkPreview() {
  TallybookTheme(darkTheme = true) {
    AccountForm(
        state = AccountDraftState.forNew("Nequi", TipoCuenta.BANCO, Moneda.COP),
        onDraftChanged = {},
        mode = AccountFormMode.QUICK,
    )
  }
}
