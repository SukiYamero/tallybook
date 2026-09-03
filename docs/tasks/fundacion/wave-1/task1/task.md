# Wave 1 / Task 1 — Dominio + Dinero

## Objetivo

Crear los tipos de dominio puros de Tallybook (sin dependencias de framework, 100% `commonMain`) que
todo lo demás va a usar: moneda, tipo de movimiento, categoría, movimiento, dinero (`Money`) y el
modelo de error/resultado (`DataError`/`DataOutcome`) que va a usar la capa de datos desde Wave 2.

## Contexto

- Reglas de negocio fuente: `docs/business-logic.md` (secciones Movimiento, Moneda, Categorías).
- Decisión de librería: `docs/stack.md` — `com.ionspin.kotlin:bignum` para precisión decimal (Kotlin
  no tiene `BigDecimal` en `commonMain`). Nunca usar `Double` para montos.
- Arquitectura de error decidida para este spec: el repositorio (Wave 2) es el único lugar que hace
  `catch` de excepciones de plataforma; todo lo de arriba (ViewModel, UI) consume `DataOutcome<T>`,
  nunca una excepción cruda.
- **IDs como `String` (UUID), no autoincrement**: el motor de sync futuro (HLC/outbox, mencionado en
  `docs/business-logic.md` como feature propia) va a necesitar IDs globalmente únicos generados en el
  cliente. Decidirlo ahora evita migrar de Int autoincrement a UUID más adelante, que sí sería un
  refactor caro. Generar con `kotlin.uuid.Uuid` — confirmar si ya salió de `@ExperimentalUuidApi` en
  Kotlin 2.4.10; si no, mantener el opt-in.
- **Formato de montos — no inventar los separadores/símbolos**: `docs/business-logic.md` dice que el
  mecanismo se porta de moneta (`Intl.NumberFormat`). Antes de escribir la tabla de formato en
  `Moneda`, revisar el código real de moneta (buscar en el repo hermano `web/moneta` el archivo de
  formato de montos, algo bajo `src/lib/i18n/` o similar) y portar los valores reales (símbolo,
  posición del símbolo, separador de miles/decimales por moneda). Si el repo moneta no está accesible
  desde acá, preguntar antes de inventar valores — no asumir convenciones de memoria.
- Las 8 monedas y sus decimales ISO 4217 (esto sí es dato objetivo, no requiere portar nada): COP=2,
  MXN=2, ARS=2, CLP=0, BRL=2, USD=2, DOP=2, PEN=2.

## Archivos

- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/Moneda.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/TipoMovimiento.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/Categoria.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/Movimiento.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/Money.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/DataError.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/DataOutcome.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/model/MoneyTest.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/model/DataOutcomeTest.kt`
- Modificar: `gradle/libs.versions.toml` — agregar `bignum` y `kotlinx-datetime` (verificar la última
  versión estable en Maven Central antes de fijarla, no asumir de memoria).
- Modificar: `shared/build.gradle.kts` — agregar ambas libs a `commonMain.dependencies`.
- Borrar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/Greeting.kt`, `GreetingUtil.kt`
  (placeholders del wizard). **No borrar `Platform.kt`/`.android.kt`/`.ios.kt` todavía** — eso lo
  evalúa Wave 3 / task1 con un `grep` (puede que algo lo siga usando).

## Interfaces (lo que Wave 2 y Wave 3 van a consumir)

```kotlin
enum class Moneda(val isoCode: String, val scale: Int) {
    COP("COP", 2), MXN("MXN", 2), ARS("ARS", 2), CLP("CLP", 0),
    BRL("BRL", 2), USD("USD", 2), DOP("DOP", 2), PEN("PEN", 2)
    // símbolo/separadores: agregar como propiedades acá una vez confirmados contra moneta (ver Contexto)
}

enum class TipoMovimiento { GASTO, INGRESO }

data class Categoria(
    val id: String,
    val nombre: String,
    val padreId: String?,
    val icono: String,   // nombre Lucide, ej. "shopping-cart"
    val color: String,   // tinte con nombre (amber, blue...), resuelto contra tokens de tema — no es hex
)

data class Movimiento(
    val id: String,
    val tipo: TipoMovimiento,
    val monto: Money,
    val fecha: LocalDate,   // kotlinx.datetime.LocalDate
    val categoriaId: String,
    val descripcion: String?,
)

class Money(val amount: BigDecimal, val moneda: Moneda) : Comparable<Money> {
    operator fun plus(other: Money): Money    // require(moneda == other.moneda)
    operator fun minus(other: Money): Money   // require(moneda == other.moneda)
    operator fun unaryMinus(): Money
    override fun compareTo(other: Money): Int // require(moneda == other.moneda)
    fun format(): String                      // tabla en Moneda, sin locale APIs de plataforma
}

sealed class DataError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Local(cause: Throwable) : DataError("Error de almacenamiento local", cause)
    class Unknown(cause: Throwable) : DataError("Error inesperado", cause)
    // Network/Server se agregan en la feature de Auth+Drive (ver docs/backlog.md)
}

sealed class DataOutcome<out T> {
    data class Success<T>(val data: T) : DataOutcome<T>()
    data class Failure(val error: DataError) : DataOutcome<Nothing>()
}

inline fun <T, R> DataOutcome<T>.fold(onSuccess: (T) -> R, onFailure: (DataError) -> R): R
inline fun <T, R> DataOutcome<T>.map(transform: (T) -> R): DataOutcome<R>
```

## Implementación (TDD)

- [ ] **Step 1** — agregar `bignum` y `kotlinx-datetime` a `libs.versions.toml` (verificar versión
      estable actual) y a `commonMain.dependencies` en `shared/build.gradle.kts`. Correr
      `./gradlew :shared:compileKotlinMetadata` para confirmar que resuelven antes de escribir código.

- [ ] **Step 2** — test que falla, `MoneyTest.kt`:
```kotlin
class MoneyTest {
    @Test
    fun sumaMontosDeLaMismaMoneda() {
        val a = Money(BigDecimal.parseString("10.50"), Moneda.USD)
        val b = Money(BigDecimal.parseString("5.25"), Moneda.USD)
        assertEquals(BigDecimal.parseString("15.75"), (a + b).amount)
    }

    @Test
    fun sumarMonedasDistintasFalla() {
        val a = Money(BigDecimal.parseString("10"), Moneda.USD)
        val b = Money(BigDecimal.parseString("10"), Moneda.COP)
        assertFailsWith<IllegalArgumentException> { a + b }
    }
}
```
Correr: `./gradlew :shared:testAndroidHostTest --tests "*.MoneyTest"` → esperar FAIL (clase no existe).

- [ ] **Step 3** — implementar `Money.kt` mínimo para pasar esos dos tests (constructor, `plus`, `require`).

- [ ] **Step 4** — correr el test de nuevo → PASS.

- [ ] **Step 5** — repetir el ciclo test-primero para `compareTo`, `unaryMinus`, y `format()` (una vez
      confirmados los valores reales de moneta para el formato).

- [ ] **Step 6** — test que falla, `DataOutcomeTest.kt`:
```kotlin
class DataOutcomeTest {
    @Test
    fun foldEjecutaRamaCorrectaEnSuccess() {
        val outcome: DataOutcome<Int> = DataOutcome.Success(5)
        assertEquals(10, outcome.fold({ it * 2 }, { -1 }))
    }

    @Test
    fun foldEjecutaRamaCorrectaEnFailure() {
        val error = DataError.Unknown(RuntimeException("boom"))
        val outcome: DataOutcome<Int> = DataOutcome.Failure(error)
        assertEquals(-1, outcome.fold({ it * 2 }, { -1 }))
    }
}
```
Correr → FAIL → implementar `DataOutcome`/`fold`/`map` → correr → PASS.

- [ ] **Step 7** — implementar `TipoMovimiento`, `Categoria`, `Movimiento`, `DataError` (sin test
      dedicado — son data classes/enums sin lógica, cubiertos indirectamente por los tests de Wave 2
      que sí los usan).

- [ ] **Step 8** — borrar `Greeting.kt`/`GreetingUtil.kt`. Confirmar que nada los importa
      (`grep -r "Greeting" shared/`).

- [ ] **Step 9** — `./gradlew detekt ktfmtCheck` limpio sobre los archivos nuevos.

- [ ] **Step 10** — commit:
```bash
git add shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/ \
        shared/src/commonTest/kotlin/com/kurobello/tallybook/core/model/ \
        gradle/libs.versions.toml shared/build.gradle.kts
git commit -m "feat(core): add domain model, Money, and DataError/DataOutcome"
```
(borrar `Greeting.kt`/`GreetingUtil.kt` en el mismo commit si ya no los referencia nada)

## Bloqueante

No bloquea a ninguna otra tarea de Wave 1. **Bloquea Wave 2 completo** (repositorios necesitan
`Movimiento`/`Categoria`/`DataOutcome`) y por extensión Wave 3.
