package com.kurobello.tallybook.feature.movimientos.viewmodel

import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.CuentaSelection
import com.kurobello.tallybook.core.model.MedioPago
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import kotlin.time.Instant

internal fun testCuenta(
    id: String,
    tipo: TipoCuenta,
    moneda: Moneda = Moneda.COP,
    nombre: String = id,
    archivada: Boolean = false,
): Cuenta =
    Cuenta(
        id = id,
        nombre = nombre,
        tipo = tipo,
        saldoInicial = Money(0, moneda),
        diaCorte = null,
        archivada = archivada,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        color = tipo.defaultColor,
    )

internal fun cashCuenta(moneda: Moneda): Cuenta =
    testCuenta(Cuenta.defaultCashId(moneda), TipoCuenta.EFECTIVO, moneda, nombre = "Efectivo")

internal val COP_CASH: Cuenta = cashCuenta(Moneda.COP)
internal val NEQUI: Cuenta = testCuenta("nequi", TipoCuenta.BANCO, nombre = "Nequi")
internal val NU: Cuenta = testCuenta("nu", TipoCuenta.CREDITO, nombre = "NU")
internal val OLD_BANK: Cuenta =
    testCuenta("old-bank", TipoCuenta.BANCO, nombre = "Old bank", archivada = true)
internal val USD_CASH: Cuenta = cashCuenta(Moneda.USD)
internal val USD_CARD: Cuenta =
    testCuenta("usd-card", TipoCuenta.CREDITO, Moneda.USD, nombre = "Visa")
internal val USD_BANK: Cuenta =
    testCuenta("usd-bank", TipoCuenta.BANCO, Moneda.USD, nombre = "Chase")
internal val CLP_CASH: Cuenta = cashCuenta(Moneda.CLP)

internal fun Cuenta.selection(medio: MedioPago): CuentaSelection =
    CuentaSelection(id, medio, moneda)

/** The cached repository answers; a medio left out resolves no cuenta. */
internal fun defaultsFor(base: CuentaSelection?, vararg byMedio: Pair<MedioPago, Cuenta?>) =
    CuentaDefaults(
        base = base,
        byMedio =
            MedioPago.entries.associateWith { medio ->
              byMedio.firstOrNull { it.first == medio }?.second?.selection(medio)
            },
    )
