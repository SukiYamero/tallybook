# Wave 2 / Stage 3 / Task 1 — Migración, queries y repositorios reactivos

## Objetivo

Añadir el metadata necesario para orden estable y exponer recientes, resumen y categorías como queries
SQLDelight observables que reaccionan a inserts sin refresco manual.

## Contexto y skills

- Requiere Stage 2 revisado.
- Leer `AGENTS.md`, `docs/business-logic.md`, `docs/tasks/wave-2/README.md` y los repositorios actuales.
- Skills requeridas: `android-dev`, `android-data-layer`, `kotlin-flows`, `kotlin-coroutines`,
  `android-testing`, `test-driven-development` y `gradle-run` para los comandos Gradle.
- Añadir `app.cash.sqldelight:coroutines-extensions:2.3.2`, exactamente la versión del runtime.

## Archivos

- Modificar: `gradle/libs.versions.toml`
- Modificar: `shared/build.gradle.kts`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/Movimiento.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/MovimientoSummary.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/DataError.kt`
- Modificar: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/Movimiento.sq`
- Crear: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/1.sqm`
- Actualizar: `shared/src/commonMain/sqldelight/databases/2.db` mediante la tarea de schema, no a mano.
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoMapper.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoRepository.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoRepositoryImpl.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CategoriaRepository.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CategoriaRepositoryImpl.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/FlowDataOperation.kt`
- Modificar tests existentes de modelo/repositorio/schema.
- Crear: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/ReactiveRepositoryTest.kt`
- Modificar: `docs/business-logic.md` — documentar `createdAt` como metadata técnica, no como campo
  solicitado al usuario, y corregir el total del catálogo a 58.

## Contratos

```kotlin
data class Movimiento(
    val id: String,
    val tipo: TipoMovimiento,
    val monto: Money,
    val fecha: LocalDate,
    val categoriaId: String,
    val descripcion: String?,
    val createdAt: Instant,
)

const val MOVIMIENTO_DESCRIPTION_MAX_LENGTH: Int = 180

data class MovimientoSummary(
    val ingresos: Money,
    val gastos: Money,
) {
  val balance: Money
}

interface MovimientoRepository {
  fun observeRecent(limit: Int): Flow<DataOutcome<List<Movimiento>>>

  fun observeSummary(moneda: Moneda): Flow<DataOutcome<MovimientoSummary>>

  suspend fun insert(movimiento: Movimiento): DataOutcome<Unit>

  suspend fun getAll(): DataOutcome<List<Movimiento>>

  suspend fun delete(id: String): DataOutcome<Unit>
}

interface CategoriaRepository {
  fun observeAll(): Flow<DataOutcome<List<Categoria>>>

  suspend fun getAll(): DataOutcome<List<Categoria>>

  suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit>
}
```

Usar `kotlin.time.Instant`, estable con Kotlin 2.4.10. Renombrar en esta tarea los métodos existentes
`insertar`, `obtenerTodos` y `eliminar` y actualizar todas sus pruebas/call sites.

## Schema y queries

- Migración 1→2: agregar `created_at TEXT NOT NULL` con un default técnico solo para filas legacy y
  asignar a esas filas un instante derivado de `fecha` a medianoche UTC. Los inserts nuevos siempre
  envían `created_at` explícito.
- Índice compuesto para recientes: `fecha DESC, created_at DESC, id DESC` si el query plan demuestra
  que evita ordenar toda la tabla; conservar el índice de fecha si otra query lo necesita.
- `selectRecentMovimientos(limit)` con `LIMIT ?` y ese orden exacto.
- `sumMovimientosByTipo(moneda)` filtra moneda antes de `SUM`, agrupa por tipo y devuelve enteros.
- Reusar las queries generadas como `Query`; convertir con `asFlow().mapToList(dispatcher)`.

## Reglas exactas

- `limit <= 0` devuelve `DataError.InvalidInput`, no ejecuta SQL.
- `insert` rechaza `minorUnits <= 0`, ID/categoría vacíos y descripción que exceda
  `MOVIMIENTO_DESCRIPTION_MAX_LENGTH`; no corrige ni trunca entradas.
- Una moneda sin movimientos produce tres `Money(0, moneda)` coherentes.
- El balance se deriva como `ingresos - gastos`; no se almacena ni se pasa por constructor.
- No sumar monedas diferentes ni convertir con `Double`.
- El `Flow` emite `DataOutcome.Failure(DataError.Local)` ante error upstream. La cancelación no se
  transforma en failure.
- No usar `stateIn` en repositorio; el lifecycle del collector pertenece al consumidor.
- Conservar la query por categoría existente para una wave posterior, sin exponerla si no tiene
  consumidor.

## Implementación TDD

- [ ] Escribir tests de migración 1→2 con una fila legacy y confirmar que todos los campos sobreviven.
- [ ] Implementar `.sqm`, generar `2.db` con SQLDelight y verificar migraciones.
- [ ] Test RED/GREEN del mapper con `createdAt` ISO válido e inválido.
- [ ] Test RED/GREEN de resumen vacío, gasto, ingreso, combinación y exclusión de otra moneda.
- [ ] Test RED/GREEN del orden reciente con empates de fecha/instante y límite.
- [ ] Test reactivo con collector activo antes del insert: debe recibir estado inicial y segunda emisión.
      Un `first()` ejecutado después del insert no demuestra invalidación y no sirve como evidencia.
- [ ] Repetir el contrato reactivo para categorías después de `ensureSeeded`.
- [ ] Forzar un error de query/storage y confirmar `DataError.Local`; cancelar el collector y confirmar
      que no se emite Failure por cancelación.
- [ ] Actualizar tests de Koin y todos los constructores de `Movimiento`.
- [ ] Ejecutar `./gradlew :shared:testAndroidHostTest`, verificación SQLDelight, compilación iOS,
      `detekt` y `ktfmtCheck`.
- [ ] Commit sugerido: `feat(data): expose reactive movement queries`.

## Bloqueos

Bloqueada por Stage 2. Bloquea Stage 4 y Home en Stage 6.
