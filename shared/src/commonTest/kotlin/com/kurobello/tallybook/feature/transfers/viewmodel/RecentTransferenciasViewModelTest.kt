package com.kurobello.tallybook.feature.transfers.viewmodel

import com.kurobello.tallybook.core.data.FakeCuentaRepository
import com.kurobello.tallybook.core.data.FakeTransferenciaRepository
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.DataError
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import com.kurobello.tallybook.core.model.Transferencia
import com.kurobello.tallybook.feature.accounts.viewmodel.AccountsPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RecentTransferenciasViewModelTest {

  @Test
  fun listsTheNewestTransfersWithBothAccountsAndFollowsWrites() = runTest {
    val cuentas = FakeCuentaRepository(listOf(NEQUI, NU, DOLARES))
    val older = transferencia("older", NEQUI, NU, LocalDate(2026, 10, 1))
    val newer = transferencia("newer", DOLARES, NEQUI, LocalDate(2026, 10, 10))
    val transferencias = FakeTransferenciaRepository(cuentas, initial = listOf(older, newer))
    val vm =
        RecentTransferenciasViewModel(
            transferencias,
            cuentas,
            AccountsPreferences(),
            scope = backgroundScope,
        )
    runCurrent()

    assertEquals(Moneda.COP, vm.uiState.value.primary)
    assertEquals(
        listOf(
            RecentTransferencia(newer, DOLARES, NEQUI),
            RecentTransferencia(older, NEQUI, NU),
        ),
        vm.uiState.value.items,
    )

    transferencias.deleteAndCapture(newer.id)
    runCurrent()
    assertEquals(listOf("older"), vm.uiState.value.items.map { it.transferencia.id })
  }

  @Test
  fun anUnreadableSourceShowsNoTransfers() = runTest {
    val cuentas = FakeCuentaRepository(listOf(NEQUI, NU))
    val transferencias =
        FakeTransferenciaRepository(
            cuentas,
            initial = listOf(transferencia("t", NEQUI, NU, LocalDate(2026, 10, 1))),
        )
    transferencias.failure = DataError.Local(IllegalStateException())
    val vm =
        RecentTransferenciasViewModel(
            transferencias,
            cuentas,
            AccountsPreferences(),
            scope = backgroundScope,
        )
    runCurrent()

    assertTrue(vm.uiState.value.items.isEmpty())
  }
}

private val CREATED_AT = Instant.parse("2026-01-01T00:00:00Z")

private fun cuenta(id: String, tipo: TipoCuenta, moneda: Moneda) =
    Cuenta(
        id,
        id,
        tipo,
        Money(0, moneda),
        null,
        archivada = false,
        createdAt = CREATED_AT,
        color = tipo.defaultColor,
    )

private val NEQUI = cuenta("nequi", TipoCuenta.BANCO, Moneda.COP)
private val NU = cuenta("nu", TipoCuenta.CREDITO, Moneda.COP)
private val DOLARES = cuenta("dolares", TipoCuenta.BANCO, Moneda.USD)

private fun transferencia(id: String, origen: Cuenta, destino: Cuenta, fecha: LocalDate) =
    Transferencia(
        id = id,
        cuentaOrigenId = origen.id,
        cuentaDestinoId = destino.id,
        montoOrigen = Money(10_000, origen.moneda),
        montoDestino = Money(10_000, destino.moneda),
        fecha = fecha,
        descripcion = null,
        createdAt = CREATED_AT,
    )
