# Statistics / Stage 1 / Task 1 — Datos

## Objetivo

Exponer todo lo que Estadísticas y la tarjeta de Home necesitan como un snapshot consistente por
moneda, leído en una sola transacción. Bloquea a task2 solo hasta el primer commit, el de los tipos.

## Contexto y skills

- Leer `AGENTS.md`, el [README](../../README.md) (decisiones 4–7 y 11) y `docs/business-logic.md`
  (Category and tags, Accounts).
- Skills: `android-dev`, `android-data-layer`, `kotlin-flows`, `android-testing` y
  `test-driven-development`.
- Patrón a seguir: `ActividadRepositoryImpl`. Usa `transactionWithResult` y vuelve a leer cuando
  cambian las tablas consultadas (`combine(queries.map { it.asFlow() })`). Emite
  `Flow<DataOutcome<…>>` con el dispatcher inyectado.

## Archivos

- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/StatisticsSnapshot.kt` (**primer commit**)
- Modificar: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/Movimiento.sq`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/StatisticsRepository.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/StatisticsRepositoryImpl.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/RepositoryModule.kt` (binding del repositorio)
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/StatisticsRepositoryTest.kt`
- Modificar: `shared/src/androidHostTest/.../DatabaseSchemaTest.kt`. Hoy usa
  `sumMovimientosPorCategoria` en tres tests, uno de ellos el de query plans con
  `PlanRecordingDriver`. Migrarlos a las consultas nuevas y sumarlas a ese mismo test de query plans.
  Actualizar también los comentarios de `Movimiento.sq` que nombran la consulta vieja.

## Contratos

```kotlin
internal data class StatisticsRequest(
    val moneda: Moneda,
    val period: MovimientoDateRange,
    val comparison: MovimientoDateRange?,
    val trend: MovimientoDateRange,      // whole months, first day to period end
    val daily: MovimientoDateRange,      // the selected period (≤ 366 days): pace and biggest day
)

internal sealed interface StatisticsKey {
  data class Categoria(val id: String) : StatisticsKey
  data class Etiqueta(val id: String) : StatisticsKey
  data class Cuenta(val id: String) : StatisticsKey
  data class Medio(val medio: MedioPago) : StatisticsKey
}

internal data class DimensionTotal(val key: StatisticsKey, val current: Money, val previous: Money?)

internal data class MonthTotals(val month: YearMonth, val summary: MovimientoSummary)

internal data class DayGasto(val fecha: LocalDate, val gasto: Money)

internal data class StatisticsSnapshot(
    val current: MovimientoSummary,
    val previous: MovimientoSummary?,
    val categorias: List<DimensionTotal>,   // root categorias, gasto only
    val etiquetas: List<DimensionTotal>,
    val cuentas: List<DimensionTotal>,
    val medios: List<DimensionTotal>,
    val months: List<MonthTotals>,          // every month of trend, zero-filled
    val daily: List<DayGasto>,              // every day of daily, zero-filled
    val hasAnyMovimiento: Boolean,          // in this moneda, ever
)

internal data class CategoryStatisticsRequest(
    val moneda: Moneda,
    val rootId: String,
    val period: MovimientoDateRange,
    val comparison: MovimientoDateRange?,
    val trend: MovimientoDateRange,
)

internal data class CategoryStatisticsSnapshot(
    val current: Money,
    val previous: Money?,
    val count: Long,                        // same semantics as the Search drill-down
    val subcategorias: List<DimensionTotal>,
    val months: List<Pair<YearMonth, Money>>,
    val largest: List<Movimiento>,          // top 3 by monto, then fecha DESC, id
)

internal interface StatisticsRepository {
  fun observe(request: StatisticsRequest): Flow<DataOutcome<StatisticsSnapshot>>
  fun observeCategory(request: CategoryStatisticsRequest): Flow<DataOutcome<CategoryStatisticsSnapshot>>
}
```

Las listas de dimensión llegan ordenadas por `current` DESC y luego por id. Incluyen cada clave con
`current > 0` o `previous > 0`: así una categoría que cae a cero puede ser "la que más bajó". La
presentación filtra los ceros antes de armar la dona y la lista. `previous` es `null` cuando no hay
`comparison`, y `Money(0, moneda)` cuando hay tramo pero no hubo gasto.

## Consultas en `Movimiento.sq`

Todas filtran `cuenta.moneda = ?`, `deleted_at IS NULL` y `fecha BETWEEN ? AND ?`, y solo
`tipo = 'GASTO'` salvo los totales mensuales. `sumMovimientosPorCategoria` hoy no tiene uso: se
reemplaza.

- `sumGastoPorCategoriaRaiz`: `LEFT JOIN categoria`, agrupado por
  `COALESCE(categoria.padre_id, movimiento.categoria_id)`, sin filtrar por `categoria.deleted_at`.
  Un movimiento cuya categoría no existe (no hay foreign key) cuenta bajo su propio `categoria_id`, y
  la suma sigue igual al gasto total.
- `sumGastoPorSubcategoria(rootId)`, `sumGastoCategoriaPorMes(rootId)`, `countGastoCategoria(rootId)`
  y `selectGastosMasGrandesCategoria(rootId, limit)` copian el predicado de categoría de
  `selectFilteredMovimientos*`:
  `+categoria_id IN (:root) OR +categoria_id IN (SELECT id FROM categoria WHERE padre_id IN (:root))`.
  Las subcategorías se agrupan por `categoria_id`.
- `sumGastoPorEtiqueta`: `JOIN movimiento_etiqueta`, agrupado por etiqueta. Cada tag suma el
  movimiento completo, a propósito.
- `sumGastoPorCuenta` y `sumGastoPorMedio`.
- `sumPorMes`: `substr(fecha, 1, 7)` y `tipo`. Se mapea a `YearMonth`.
- `sumGastoPorDia`: agrupado por `fecha`.
- `existsMovimientoEnMoneda`.

El rango usa `movimiento_recent`, y el uso del índice se verifica en el test de query plans existente.
No se agregan índices por adelantado.

## Bordes

- Un SUM que pasa `Money.MAX_MINOR_UNITS`, o el overflow de `INTEGER` de SQLite, sale como
  `DataOutcome` de error, nunca como crash. Con los topes actuales no es alcanzable por uso normal;
  no hace falta un fixture que lo provoque.
- Una categoría oculta sigue sumando (business-logic: las ocultas siguen en History y Search).
- `countGastoCategoria` usa la misma semántica que el filtro de Search por categoría (la raíz incluye
  a sus hijas) con `tipo = GASTO` y el rango.

## Tests (host, fixtures mínimos)

Un solo `StatisticsRepositoryTest`:

- **Integración:** dos monedas, una subcategoría, un movimiento con dos tags, un movimiento con
  categoría inexistente, una transferencia y un borrado. Cumple:
  - Σ categorías = `current.gastos`;
  - un tag suma el movimiento completo;
  - la transferencia y el borrado no aparecen;
  - la otra moneda no aparece.
- Los meses y días sin movimientos aparecen en cero.
- `previous` para un tramo vacío.
- **Reactividad:** insertar con un collector activo emite un snapshot nuevo y coherente.
- Detalle de categoría: subcategorías, conteo y top 3 con desempate estable.
- **N igual al conteo de Search:** con el mismo fixture, `countGastoCategoria` coincide con las filas de
  `ActividadRepository.observeFiltered` para categoría raíz + moneda + rango + `SearchTipo.GASTO`.
- Una categoría con anterior > 0 y actual = 0 aparece en la lista.

## Salida

Tipos commiteados primero. Repositorio registrado en Koin. Tests host verdes, `detekt ktfmtCheck`
limpio. Handoff corto con las consultas agregadas.
