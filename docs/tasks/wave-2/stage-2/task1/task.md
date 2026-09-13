# Wave 2 / Stage 2 / Task 1 — Catálogo y seed de categorías

## Objetivo

Portar las 58 categorías predefinidas a modelos tipados, resolver sus nombres desde Compose Resources e
insertarlas idempotentemente sin sobrescribir filas existentes.

## Contexto y skills

- Requiere Stage 1 completo.
- Leer `AGENTS.md`, `docs/business-logic.md`, `docs/tasks/wave-2/README.md`,
  `web/moneta/src/lib/schema.ts` y `web/moneta/src/lib/seedConfig.ts`.
- Skills requeridas: `android-dev`, `android-data-layer`, `kotlin-api-design`, `android-testing` y
  `test-driven-development`.
- La referencia web es fuente del catálogo legacy. En el producto, toda nueva copy/nombre pertenece a
  Tallybook.

## Archivos

- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/Categoria.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/CategoryIcon.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/CategoryTint.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CategorySeedCatalog.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CategorySeedProvider.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/LocalDataBootstrapper.kt`
- Modificar: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/Categoria.sq`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CategoriaRepository.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/CategoriaRepositoryImpl.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/RepositoryModule.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/CategorySeedCatalogTest.kt`
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/CategorySeedTest.kt`
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/LocalDataBootstrapperTest.kt`

## Contratos

```kotlin
enum class CategoryIcon(val storageKey: String)

enum class CategoryTint(val storageKey: String)

data class Categoria(
    val id: String,
    val nombre: String,
    val padreId: String?,
    val icono: CategoryIcon,
    val color: CategoryTint,
)

interface CategoriaRepository {
  suspend fun getAll(): DataOutcome<List<Categoria>>

  suspend fun ensureSeeded(categorias: List<Categoria>): DataOutcome<Unit>
}

class LocalDataBootstrapper(
    private val localeProvider: DeviceLocaleProvider,
    private val preferencesRepository: AppPreferencesRepository,
    private val categoriaRepository: CategoriaRepository,
    private val categorySeedProvider: CategorySeedProvider,
) {
  suspend fun initialize(): DataOutcome<Unit>
}
```

Renombrar `obtenerTodas` a `getAll` en esta tarea para cumplir el idioma de código del repo. La
taxonomía mantiene vocabulario de dominio español, no verbos de infraestructura en español.

## Reglas exactas

- Catálogo: 58 IDs, 12 raíces, 46 hijas; cada padre se declara antes que sus hijas.
- `CategoryIcon` contiene las claves realmente usadas por el catálogo; `CategoryTint` contiene
  `EMERALD`, `BLUE`, `AMBER`, `ROSE`, `PURPLE`, `SUCCESS`, `DANGER`, `INFO`.
- Las `storageKey` exactas de `CategoryIcon` son: `banknote`, `book`, `briefcase`, `building-2`, `bus`,
  `calculator`, `car`, `chef-hat`, `coffee`, `credit-card`, `dumbbell`, `film`, `fuel`, `gamepad`,
  `gift`, `graduation-cap`, `hammer`, `hand-coins`, `heart-pulse`, `hotel`, `house`, `landmark`,
  `laptop`, `luggage`, `package`, `palette`, `parking`, `party-popper`, `paw`, `percent`, `piggy-bank`,
  `pill`, `plane`, `receipt`, `school`, `scissors`, `shield`, `shirt`, `shopping-bag`, `shopping-cart`,
  `sofa`, `sparkles`, `stethoscope`, `ticket`, `trending-up`, `tv`, `utensils`, `wallet`,
  `washing-machine` y `wifi`. No portar el catálogo ampliado de iconos para categorías custom.
- SQL sigue almacenando los `storageKey` de texto; el mapper es la única conversión hacia enums.
- La query se llama `insertCategoriaIfAbsent` y usa `INSERT OR IGNORE`, dentro de una transacción para
  las 58 filas.
- Una fila existente nunca se actualiza durante seed, incluso si difiere en nombre/icono/color.
- `CategorySeedProvider` resuelve cada nombre desde `Res.string.category_*`; no contiene una segunda
  tabla de traducciones Kotlin.
- `LocalDataBootstrapper.initialize` resuelve el locale una vez, inicializa moneda y luego categorías.
  Si una operación falla, devuelve ese `DataOutcome.Failure`; repetir es seguro.
- No sembrar movimientos demo.

## Implementación TDD

- [ ] Test RED del catálogo: tamaño 58, IDs únicos, 12 raíces, padres existentes, sin ciclos y keys de
      icono/tinte no vacías.
- [ ] Portar el catálogo estructural y hacer GREEN antes de tocar SQLDelight.
- [ ] Actualizar `Categoria`/mapper/tests existentes para enums tipados.
- [ ] Test host RED: base vacía + `ensureSeeded` produce exactamente 58 filas.
- [ ] Implementar query/transacción mínima y hacer GREEN.
- [ ] Test RED/GREEN de idempotencia: dos llamadas siguen dando 58 filas.
- [ ] Test RED/GREEN de no sobrescritura: modificar manualmente una fila, reejecutar seed y conservarla.
- [ ] Test del bootstrap con fakes: llama una vez al provider, pasa la moneda correcta e interrumpe la
      secuencia al primer fallo.
- [ ] Test de integración con recursos reales en al menos un locale soportado.
- [ ] Ejecutar tests host, compilación iOS, `detekt` y `ktfmtCheck`.
- [ ] Commit sugerido: `feat(categories): seed the localized category catalog`.

## Bloqueos

Bloqueada por Stage 1 completo. Bloquea Stage 3 y el app bootstrap de Stage 6.
