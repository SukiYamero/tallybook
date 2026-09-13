# Wave 2 / Stage 1 / Task 2 — Preferences DataStore

## Objetivo

Persistir `monedaPrincipal` en una única instancia KMP de Preferences DataStore, exponerla como Flow y
permitir una inicialización idempotente desde la región del dispositivo.

## Contexto y skills

- Requiere Stage 1 / task1 completo.
- Leer `AGENTS.md`, `docs/stack.md`, `docs/tasks/wave-2/README.md` y el task anterior.
- Skills requeridas: `android-dev`, `datastore`, `kmp-boundaries`, `android-data-layer`,
  `kotlin-flows`, `android-testing` y `test-driven-development`.
- Fijar `androidx.datastore:datastore-preferences-core:1.2.1`, estable. No usar el artefacto Android
  `datastore-preferences` en `commonMain`.
- DataStore es la única fuente persistente de preferencias; no duplicar la moneda en SQLDelight ni
  `rememberSaveable`.

## Archivos

- Modificar: `gradle/libs.versions.toml`
- Modificar: `shared/build.gradle.kts`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/AppPreferences.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/AppPreferencesRepository.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/AppPreferencesRepositoryImpl.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/PreferencesPathProvider.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/PreferencesDataStoreFactory.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/PreferencesModule.kt`
- Crear: `shared/src/androidMain/kotlin/com/kurobello/tallybook/core/data/AndroidPreferencesPathProvider.kt`
- Crear: `shared/src/androidMain/kotlin/com/kurobello/tallybook/di/PreferencesModule.android.kt`
- Crear: `shared/src/iosMain/kotlin/com/kurobello/tallybook/core/data/IosPreferencesPathProvider.kt`
- Crear: `shared/src/iosMain/kotlin/com/kurobello/tallybook/di/PreferencesModule.ios.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/AppModules.kt`
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/AppPreferencesRepositoryTest.kt`
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/di/PreferencesModuleTest.kt`

## Contratos

```kotlin
data class AppPreferences(val monedaPrincipal: Moneda)

sealed interface AppPreferencesState {
  data object Uninitialized : AppPreferencesState

  data class Ready(val value: AppPreferences) : AppPreferencesState
}

interface AppPreferencesRepository {
  val preferences: Flow<DataOutcome<AppPreferencesState>>

  suspend fun initialize(monedaPrincipal: Moneda): DataOutcome<Unit>

  suspend fun setMonedaPrincipal(moneda: Moneda): DataOutcome<Unit>
}

interface PreferencesPathProvider {
  fun path(): String
}
```

La key persistida se llama `moneda_principal`; el archivo se llama
`tallybook.preferences_pb`. El factory recibe el path provider y el dispatcher IO calificado.

## Reglas exactas

- `initialize` escribe solo si la key no existe. Una región distinta en lanzamientos posteriores no
  reemplaza una preferencia ya inicializada.
- `setMonedaPrincipal` valida el enum y actualiza atómicamente; aunque la UI para cambiarla no existe
  aún, este es el único writer futuro.
- Una key ausente emite `AppPreferencesState.Uninitialized`, nunca COP silencioso. El bootstrap de
  Stage 2 inicializa primero y solo entonces habilita pantallas.
- Un nombre desconocido almacenado es corrupción/almacenamiento fallido, no una moneda por defecto.
- Capturar errores de IO de DataStore y mapearlos a `DataError.Local`; no capturar
  `CancellationException` ni errores de programación.
- Crear una sola instancia de DataStore por archivo y registrarla como `single` en Koin.
- Android usa un archivo interno de la app; iOS usa Application Support y crea el directorio si no
  existe. Ninguno usa temp ni un directorio visible del usuario.

## Implementación TDD

- [ ] Agregar la dependencia y confirmar la versión realmente resuelta con Gradle.
- [ ] Con un path temporal único por test, escribir un test RED donde `preferences` emite
      `Uninitialized` antes de inicializar.
- [ ] Implementar factory/repositorio mínimo y hacer GREEN.
- [ ] Test RED/GREEN: `initialize(COP)` dos veces conserva el valor; `initialize(USD)` posterior no lo
      reemplaza.
- [ ] Test RED/GREEN: `setMonedaPrincipal(USD)` emite USD y una instancia nueva sobre el mismo archivo
      también lee USD.
- [ ] Test de error: un fallo real del storage produce `DataOutcome.Failure(DataError.Local)`.
- [ ] Test del grafo: sustituir solo `PreferencesPathProvider` por uno temporal y resolver una única
      instancia de repositorio/DataStore.
- [ ] Cerrar scopes y borrar únicamente el directorio temporal creado por cada test.
- [ ] Ejecutar tests host, compilación iOS, `detekt` y `ktfmtCheck`.
- [ ] Commit sugerido: `feat(preferences): persist the main currency with DataStore`.

## Bloqueos

Bloqueada por Stage 1 / task1. Bloquea Stage 2 / task1.
