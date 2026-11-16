package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.SqlDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.Cuenta
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import com.kurobello.tallybook.core.model.Movimiento
import com.kurobello.tallybook.core.model.TipoCuenta
import kotlin.time.Instant

/**
 * Creates the schema with the rows movimiento writes must reference: the seeded category catalog,
 * [extraCategoriaIds] as extra roots, [etiquetaIds] as tags named after their ids and the default
 * cash cuenta of every [Moneda], so [Cuenta.defaultCashId] of a movimiento's currency always
 * exists.
 */
internal fun createSchemaWithReferences(
    driver: SqlDriver,
    extraCategoriaIds: Collection<String> = emptyList(),
    etiquetaIds: Collection<String> = emptyList(),
): TallybookDatabase {
  TallybookDatabase.Schema.create(driver)
  return TallybookDatabase(driver).apply { seedReferences(extraCategoriaIds, etiquetaIds) }
}

/**
 * Writes [movimiento] the way older app versions could, without the reference checks the repository
 * applies now, e.g. to keep a categoria the catalog no longer has.
 */
internal fun TallybookDatabase.insertLegacy(
    movimiento: Movimiento,
    stamp: ChangeStamp = ChangeStamp.SEED,
) {
  transaction {
    movimientoQueries.insertMovimiento(
        movimiento.id,
        movimiento.tipo.name,
        movimiento.monto.minorUnits,
        movimiento.fecha.toString(),
        movimiento.titulo,
        movimiento.descripcion,
        movimiento.createdAt.toString(),
        movimiento.categoriaId,
        movimiento.cuentaId,
        movimiento.medio.name,
        stamp.value,
    )
    movimiento.etiquetaIds.forEachIndexed { posicion, etiquetaId ->
      movimientoQueries.insertMovimientoEtiqueta(movimiento.id, etiquetaId, posicion.toLong())
    }
  }
}

internal fun TallybookDatabase.seedReferences(
    extraCategoriaIds: Collection<String> = emptyList(),
    etiquetaIds: Collection<String> = emptyList(),
) {
  transaction {
    CategorySeedCatalog.categories.forEach { seed ->
      categoriaQueries.insertCategoriaIfAbsent(
          seed.id,
          seed.id,
          seed.padreId,
          seed.icono.storageKey,
          seed.color.storageKey,
          ChangeStamp.SEED.value,
      )
    }
    extraCategoriaIds.forEach { id ->
      categoriaQueries.insertCategoriaIfAbsent(
          id,
          id,
          null,
          CategoryIcon.TAG.storageKey,
          CategoryTint.NEUTRAL.storageKey,
          ChangeStamp.SEED.value,
      )
    }
    etiquetaIds.forEach { id ->
      etiquetaQueries.insertEtiqueta(id, id, null, "2026-01-01T00:00:00Z", ChangeStamp.SEED.value)
    }
    seedCashCuentas()
  }
}

/** The default cash cuenta of every [Moneda], the cuentas legacy movimientos were migrated to. */
internal fun TallybookDatabase.seedCashCuentas() {
  transaction {
    Moneda.entries.forEach { moneda -> insertCuentaRow(cashCuenta(moneda), ChangeStamp.SEED) }
  }
}

internal fun cashCuenta(
    moneda: Moneda,
    createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
): Cuenta =
    Cuenta(
        id = Cuenta.defaultCashId(moneda),
        nombre = "Efectivo",
        tipo = TipoCuenta.EFECTIVO,
        saldoInicial = Money(0, moneda),
        diaCorte = null,
        archivada = false,
        createdAt = createdAt,
        color = TipoCuenta.EFECTIVO.defaultColor,
    )
