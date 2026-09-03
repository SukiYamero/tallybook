# Wave 1 / Task 2 — Base de datos (SQLDelight)

## Objetivo

Dejar SQLDelight configurado y compilando en los 3 targets (Android, iOS, JVM-test) con el schema de
`movimiento` y `categoria`, más el `DatabaseDriverFactory` por plataforma. Sin repositorios todavía
(eso es Wave 2) — esta tarea entrega la base de datos como pieza aislada y probada.

## Contexto

- Decisión ya tomada en `AGENTS.md`: SQLDelight, no Room Multiplatform — SQL-first para el motor de
  sync futuro.
- El schema debe reflejar los campos de `docs/business-logic.md` (Movimiento, Categorías) exactamente
  — no inventar columnas de más.
- Los tipos generados por SQLDelight (row classes, queries, `Database`) **nunca deben cruzar fuera de
  `core.database`** — Wave 2 los envuelve en `core.data`. Esta tarea no expone nada fuera de
  `core.database` todavía.
- Migración desde v1: crear el mecanismo de migración desde ya (aunque no haya nada que migrar) — no
  hay que redescubrir cómo migrar la primera vez que haga falta una columna nueva.

## Archivos

- Modificar: `gradle/libs.versions.toml` — plugin `app.cash.sqldelight` (verificar última versión
  estable) + driver de test JDBC para `commonTest`/`androidHostTest`.
- Modificar: `shared/build.gradle.kts` — aplicar el plugin SQLDelight, bloque
  `sqldelight { databases { create("TallybookDatabase") { packageName = "com.kurobello.tallybook.core.database" } } }`.
- Crear: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/Movimiento.sq`
- Crear: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/Categoria.sq`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/database/DatabaseDriverFactory.kt` (`expect class`)
- Crear: `shared/src/androidMain/kotlin/com/kurobello/tallybook/core/database/DatabaseDriverFactory.android.kt` (`actual`, `AndroidSqliteDriver`)
- Crear: `shared/src/iosMain/kotlin/com/kurobello/tallybook/core/database/DatabaseDriverFactory.ios.kt` (`actual`, `NativeSqliteDriver`)
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/database/DatabaseSchemaTest.kt`
  (crea la DB en memoria y confirma que insertar+leer una fila cruda funciona — sin pasar por
  repositorio, eso es Wave 2).

## Interfaces (lo que Wave 2 va a consumir)

```kotlin
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// generado por SQLDelight a partir de los .sq, package com.kurobello.tallybook.core.database:
class TallybookDatabase(driver: SqlDriver) {
    val movimientoQueries: MovimientoQueries
    val categoriaQueries: CategoriaQueries
}
```

## Schema (`.sq`)

`Movimiento.sq`:
```sql
CREATE TABLE movimiento (
    id TEXT NOT NULL PRIMARY KEY,
    tipo TEXT NOT NULL,               -- "GASTO" | "INGRESO"
    monto_amount TEXT NOT NULL,       -- BigDecimal serializado como string decimal exacto
    monto_moneda TEXT NOT NULL,       -- código ISO, ej. "COP"
    fecha TEXT NOT NULL,              -- ISO-8601 (LocalDate.toString())
    categoria_id TEXT NOT NULL,
    descripcion TEXT
);

insertMovimiento:
INSERT INTO movimiento(id, tipo, monto_amount, monto_moneda, fecha, categoria_id, descripcion)
VALUES (?, ?, ?, ?, ?, ?, ?);

selectAllMovimientos:
SELECT * FROM movimiento ORDER BY fecha DESC;

selectMovimientoById:
SELECT * FROM movimiento WHERE id = ?;

deleteMovimiento:
DELETE FROM movimiento WHERE id = ?;
```

`Categoria.sq`:
```sql
CREATE TABLE categoria (
    id TEXT NOT NULL PRIMARY KEY,
    nombre TEXT NOT NULL,
    padre_id TEXT,
    icono TEXT NOT NULL,
    color TEXT NOT NULL
);

insertCategoria:
INSERT INTO categoria(id, nombre, padre_id, icono, color)
VALUES (?, ?, ?, ?, ?);

selectAllCategorias:
SELECT * FROM categoria;
```

Nota: `monto_amount` se guarda como `TEXT` (no `REAL`) — `BigDecimal.toPlainString()` al escribir,
`BigDecimal.parseString()` al leer. Guardar un monto como `REAL`/`Double` reintroduce el problema de
precisión que `bignum` existe para evitar.

## Implementación

- [ ] **Step 1** — agregar el plugin SQLDelight a `libs.versions.toml` + `shared/build.gradle.kts`,
      correr `./gradlew :shared:generateCommonMainTallybookDatabaseInterface` para confirmar que el
      schema genera sin errores antes de escribir ningún test.
- [ ] **Step 2** — implementar `DatabaseDriverFactory` `expect`/`actual` para Android
      (`AndroidSqliteDriver(TallybookDatabase.Schema, context, "tallybook.db")`) e iOS
      (`NativeSqliteDriver(TallybookDatabase.Schema, "tallybook.db")`).
- [ ] **Step 3** — test que falla, `DatabaseSchemaTest.kt` (driver en memoria vía el driver JDBC de
      test de SQLDelight):
```kotlin
class DatabaseSchemaTest {
    @Test
    fun insertaYLeeUnMovimiento() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TallybookDatabase.Schema.create(driver)
        val db = TallybookDatabase(driver)
        db.movimientoQueries.insertMovimiento("1", "GASTO", "10.50", "USD", "2026-09-03", "cat1", null)
        val fila = db.movimientoQueries.selectMovimientoById("1").executeAsOne()
        assertEquals("10.50", fila.monto_amount)
    }
}
```
- [ ] **Step 4** — correr, confirmar FAIL → implementar lo que falte → correr, confirmar PASS.
- [ ] **Step 5** — agregar el driver JDBC de test solo a `commonTest`/`androidHostTest`, nunca a
      `commonMain` (es un driver de test, no de producción).
- [ ] **Step 6** — `./gradlew detekt ktfmtCheck` limpio.
- [ ] **Step 7** — commit:
```bash
git add shared/build.gradle.kts gradle/libs.versions.toml \
        shared/src/commonMain/sqldelight/ \
        shared/src/commonMain/kotlin/com/kurobello/tallybook/core/database/ \
        shared/src/androidMain/kotlin/com/kurobello/tallybook/core/database/ \
        shared/src/iosMain/kotlin/com/kurobello/tallybook/core/database/ \
        shared/src/commonTest/kotlin/com/kurobello/tallybook/core/database/
git commit -m "feat(core): add SQLDelight schema and database driver factory"
```

## Bloqueante

No bloquea otras tareas de Wave 1. **Bloquea Wave 2** (los repositorios necesitan `TallybookDatabase`).
