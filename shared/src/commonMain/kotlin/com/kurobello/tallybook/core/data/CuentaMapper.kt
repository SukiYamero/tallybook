package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.Cuenta as DatabaseCuenta
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.TipoCuenta
import kotlin.time.Instant

internal fun DatabaseCuenta.toDomain(): Cuenta =
    Cuenta(
        id = id,
        nombre = nombre,
        tipo = TipoCuenta.valueOf(tipo),
        saldoInicial = Money(saldo_inicial_minor, Moneda.valueOf(moneda)),
        diaCorte = dia_corte?.toInt(),
        archivada = archivada == 1L,
        createdAt = Instant.parse(created_at),
        color = CategoryTint.entries.first { it.storageKey == color },
    )

/** Strict like every insert: an existing id, live or tombstoned, fails the write. */
internal fun TallybookDatabase.insertCuentaRow(cuenta: Cuenta, stamp: ChangeStamp) {
  check(
      cuentaQueries
          .insertCuenta(
              id = cuenta.id,
              nombre = cuenta.nombre,
              tipo = cuenta.tipo.name,
              moneda = cuenta.moneda.name,
              saldo_inicial_minor = cuenta.saldoInicial.minorUnits,
              dia_corte = cuenta.diaCorte?.toLong(),
              color = cuenta.color.storageKey,
              archivada = if (cuenta.archivada) 1L else 0L,
              created_at = cuenta.createdAt.toString(),
              updated_at = stamp.value,
          )
          .value == 1L
  )
}
