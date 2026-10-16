package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.SqlDriver
import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.CategoryIcon
import com.kurobello.tallybook.core.model.CategoryTint
import com.kurobello.tallybook.core.model.Movimiento

/**
 * Creates the schema with the rows movimiento writes must reference: the seeded category catalog,
 * [categoriaIds] as extra roots and [etiquetaIds] as tags named after their ids.
 */
internal fun createSchemaWithReferences(
    driver: SqlDriver,
    categoriaIds: Collection<String> = emptyList(),
    etiquetaIds: Collection<String> = emptyList(),
): TallybookDatabase {
  TallybookDatabase.Schema.create(driver)
  return TallybookDatabase(driver).apply { seedReferences(categoriaIds, etiquetaIds) }
}

/**
 * Writes [movimiento] the way older app versions could, without the reference checks the repository
 * applies now, e.g. to keep a categoria the catalog no longer has.
 */
internal fun TallybookDatabase.insertLegacy(movimiento: Movimiento) {
  transaction {
    movimientoQueries.insertMovimiento(
        movimiento.id,
        movimiento.tipo.name,
        movimiento.monto.minorUnits,
        movimiento.monto.moneda.name,
        movimiento.fecha.toString(),
        movimiento.titulo,
        movimiento.descripcion,
        movimiento.createdAt.toString(),
        movimiento.categoriaId,
    )
    movimiento.etiquetaIds.forEachIndexed { posicion, etiquetaId ->
      movimientoQueries.insertMovimientoEtiqueta(movimiento.id, etiquetaId, posicion.toLong())
    }
  }
}

internal fun TallybookDatabase.seedReferences(
    categoriaIds: Collection<String> = emptyList(),
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
      )
    }
    categoriaIds.forEach { id ->
      categoriaQueries.insertCategoriaIfAbsent(
          id,
          id,
          null,
          CategoryIcon.TAG.storageKey,
          CategoryTint.NEUTRAL.storageKey,
      )
    }
    etiquetaIds.forEach { id -> etiquetaQueries.insertEtiqueta(id, id, "2026-01-01T00:00:00Z") }
  }
}
