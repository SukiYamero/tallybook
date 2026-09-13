# Wave 2 / Task 1 — Repositorios

## Objetivo

Implementar `MovimientoRepository` y `CategoriaRepository` como la única capa que toca
`TallybookDatabase` directamente y la única que hace `catch` de excepciones de plataforma — todo lo de
arriba consume `DataOutcome<T>`.

## Contexto

- Requiere: Wave 1 / task1 (`Movimiento`, `Categoria`, `Money`, `DataError`, `DataOutcome`) y Wave 1 /
  task2 (`TallybookDatabase`, `DatabaseDriverFactory`) ya mergeados.
- Regla dura de este spec: **ningún tipo generado por SQLDelight sale de `core.data`**. Las funciones
  públicas de los repositorios solo devuelven/reciben tipos de `core.model`.
- SQLDelight no tiene un único tipo de excepción común entre Android/iOS/JVM — el `catch` en el
  repositorio debe ser sobre `Exception` genérica, nunca sobre un tipo Android-only como
  `android.database.sqlite.SQLiteException` (eso rompería la compilación de `iosMain`/`commonMain`,
  prohibido por `AGENTS.md`). `CancellationException` siempre se relanza antes de ese `catch`: la
  cancelación de coroutines no es un error de almacenamiento.
- Los dispatchers vienen inyectados por Koin (Wave 1 / task5) — nunca `withContext(Dispatchers.IO)` a
  pie.
- Evidencia API (versiones resueltas): SQLDelight 2.3.2 usa queries generadas tipadas y
  `executeAsList()`; Koin 4.2 recomienda inicializar Android desde `Application` con
  `androidContext(...)`, y `koinApplication { ... }` crea un contenedor aislado para tests.
  Fuentes primarias:
  <https://sqldelight.github.io/sqldelight/2.3.2/multiplatform_sqlite/>,
  <https://insert-koin.io/docs/reference/koin-core/starting-koin/> y
  <https://insert-koin.io/docs/reference/koin-android/start/>.

## Archivos

- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoRepository.kt` (interfaz)
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoRepositoryImpl.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CategoriaRepository.kt` (interfaz)
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CategoriaRepositoryImpl.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoMapper.kt` (fila SQLDelight ↔ `Movimiento`)
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/DatabaseModule.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/RepositoryModule.kt`
- Crear: `shared/src/androidMain/kotlin/com/kurobello/tallybook/di/DatabaseDriverModule.android.kt`
- Crear: `shared/src/iosMain/kotlin/com/kurobello/tallybook/di/DatabaseDriverModule.ios.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/AppModules.kt` — agregar ambos
  módulos a la lista.
- Crear: `androidApp/src/main/kotlin/com/kurobello/tallybook/TallybookApplication.kt`; registrar la
  clase en el manifest y retirar la inicialización de Koin de `MainActivity`.
- Tests: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/` — el driver JDBC en
  memoria es JVM y por eso estos tests no pertenecen a `commonTest`.

## Interfaces (lo que Wave 3 y features futuras van a consumir)

```kotlin
interface MovimientoRepository {
    suspend fun insertar(movimiento: Movimiento): DataOutcome<Unit>
    suspend fun obtenerTodos(): DataOutcome<List<Movimiento>>
    suspend fun eliminar(id: String): DataOutcome<Unit>
}

interface CategoriaRepository {
    suspend fun obtenerTodas(): DataOutcome<List<Categoria>>
}
```

## Implementación (TDD)

- [x] **Step 1** — test que falla, `MovimientoRepositoryTest.kt` (driver en memoria, igual que Wave
      1/task2):
```kotlin
class MovimientoRepositoryTest {
    private fun repo(): MovimientoRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TallybookDatabase.Schema.create(driver)
        return MovimientoRepositoryImpl(TallybookDatabase(driver), Dispatchers.Default)
    }

    @Test
    fun insertarYObtenerTodosDevuelveElMovimiento() = runTest {
        val repo = repo()
        val movimiento = Movimiento("1", TipoMovimiento.GASTO, Money(1_000, Moneda.USD),
            LocalDate(2026, 9, 3), "cat1", null)
        repo.insertar(movimiento)
        val resultado = repo.obtenerTodos()
        assertTrue(resultado is DataOutcome.Success)
        assertEquals(movimiento, (resultado as DataOutcome.Success).data.first())
    }
}
```
- [x] **Step 2** — correr, confirmar FAIL.
- [x] **Step 3** — implementar `MovimientoMapper` (fila ↔ dominio, incluyendo el parseo de
      `Money`/`Moneda`/`LocalDate` desde texto) e `MovimientoRepositoryImpl` mínimo para pasar el test.
- [x] **Step 4** — correr, confirmar PASS.
- [x] **Step 5** — test adicional forzando el path de error (ej. insertar con un `id` duplicado que
      viole `PRIMARY KEY`), confirmar que devuelve `DataOutcome.Failure` con `DataError.Local`, no una
      excepción sin capturar.
- [x] **Step 6** — repetir el ciclo para `CategoriaRepository` (más simple, solo lectura por ahora).
- [x] **Step 7** — implementar el módulo de driver por plataforma, `databaseModule`
      (`DatabaseDriverFactory` → `TallybookDatabase`) y `repositoryModule` (interfaces → impls con el
      dispatcher de Wave 1/task5). Agregarlos a `appModules`.
- [x] **Step 8** — test de integración con `koinApplication` aislado que sustituye únicamente el
      driver de plataforma por uno en memoria y resuelve ambos repositorios. Esto prueba el grafo y
      sus instancias reales; la compilación iOS y el arranque físico Android cubren los módulos de
      plataforma.
- [x] **Step 9** — mover el arranque Android a una subclase de `Application`, configurar
      `androidContext(...)` y verificar que el proceso inicia en el dispositivo físico.
- [x] **Step 10** — `./gradlew detekt ktfmtCheck` limpio.
- [x] **Step 11** — commit:
```bash
git add shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/ \
        shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ \
        shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/
git commit -m "feat(data): add Movimiento/Categoria repositories over SQLDelight"
```

## Bloqueante

Bloqueada por Wave 1 / task1, task2, task5. **Bloquea Wave 3** — el `viewModelModule` de Wave 3
depende de que `appModules` ya compile con estos módulos adentro, aunque la pantalla smoke-test en sí
no llame a ningún repositorio.
