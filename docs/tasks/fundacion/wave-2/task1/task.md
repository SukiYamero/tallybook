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
  prohibido por `AGENTS.md`).
- Los dispatchers vienen inyectados por Koin (Wave 1 / task5) — nunca `withContext(Dispatchers.IO)` a
  pie.

## Archivos

- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoRepository.kt` (interfaz)
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoRepositoryImpl.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CategoriaRepository.kt` (interfaz)
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CategoriaRepositoryImpl.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoMapper.kt` (fila SQLDelight ↔ `Movimiento`)
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/DatabaseModule.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/RepositoryModule.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/AppModules.kt` — agregar ambos
  módulos a la lista.
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/MovimientoRepositoryTest.kt`

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

- [ ] **Step 1** — test que falla, `MovimientoRepositoryTest.kt` (driver en memoria, igual que Wave
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
        val movimiento = Movimiento("1", TipoMovimiento.GASTO, Money(BigDecimal.parseString("10"), Moneda.USD),
            LocalDate(2026, 9, 3), "cat1", null)
        repo.insertar(movimiento)
        val resultado = repo.obtenerTodos()
        assertTrue(resultado is DataOutcome.Success)
        assertEquals(movimiento, (resultado as DataOutcome.Success).data.first())
    }
}
```
- [ ] **Step 2** — correr, confirmar FAIL.
- [ ] **Step 3** — implementar `MovimientoMapper` (fila ↔ dominio, incluyendo el parseo de
      `Money`/`Moneda`/`LocalDate` desde texto) e `MovimientoRepositoryImpl` mínimo para pasar el test.
- [ ] **Step 4** — correr, confirmar PASS.
- [ ] **Step 5** — test adicional forzando el path de error (ej. insertar con un `id` duplicado que
      viole `PRIMARY KEY`), confirmar que devuelve `DataOutcome.Failure` con `DataError.Local`, no una
      excepción sin capturar.
- [ ] **Step 6** — repetir el ciclo para `CategoriaRepository` (más simple, solo lectura por ahora).
- [ ] **Step 7** — implementar `databaseModule` (provee `DatabaseDriverFactory` → `TallybookDatabase`)
      y `repositoryModule` (bind interfaces a impls, inyectando el dispatcher de Wave 1/task5).
      Agregar ambos a `AppModules.appModules`.
- [ ] **Step 8** — test de verificación de Koin (`checkModules`/`verify()` según la API vigente de la
      versión resuelta) — confirma que no falta ningún binding.
- [ ] **Step 9** — `./gradlew detekt ktfmtCheck` limpio.
- [ ] **Step 10** — commit:
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
