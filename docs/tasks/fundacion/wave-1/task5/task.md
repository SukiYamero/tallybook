# Wave 1 / Task 5 — Koin base + dispatchers

## Objetivo

Agregar Koin al proyecto y dejar el mecanismo de inyección de `CoroutineDispatcher` funcionando — el
módulo del que depende todo lo que Wave 2 necesite inyectar.

## Contexto

- `AGENTS.md` prohíbe `Dispatchers.Main`/`Dispatchers.IO` a pie en código compartido — deben
  inyectarse.
- Un solo `startKoin` real por plataforma: Android desde `MainActivity` (o una `Application` propia si
  hace falta), iOS desde una función común `initKoin()` invocable desde Swift.

## Archivos

- Modificar: `gradle/libs.versions.toml` — Koin core + koin-compose (verificar última versión estable).
- Modificar: `shared/build.gradle.kts` — agregar a `commonMain`.
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/DispatcherModule.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/AppModules.kt` (lista agregadora,
  solo `dispatcherModule` por ahora — Wave 2/3 le agregan `databaseModule`/`repositoryModule`/`viewModelModule`)
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/KoinInit.kt` (`fun initKoin()`)
- Modificar: `androidApp/src/main/kotlin/com/kurobello/tallybook/MainActivity.kt` — llamar
  `initKoin()` (evaluar si hace falta una `Application` propia o alcanza con `MainActivity`).

## Interfaces (lo que Wave 2/3 van a consumir)

```kotlin
// qualifier real: usar el mecanismo de Koin vigente (named(), enum qualifier, etc. — confirmar en
// la versión resuelta) para distinguir IO / DEFAULT / MAIN
val dispatcherModule = module { /* provee los 3 dispatchers calificados */ }
val appModules = listOf(dispatcherModule)  // Wave 2 hace appModules + databaseModule + repositoryModule

fun initKoin(extraModules: List<Module> = emptyList())  // llamado por cada plataforma
```

## Implementación

- [ ] **Step 1** — agregar Koin a `libs.versions.toml`/`build.gradle.kts`, confirmar que resuelve.
- [ ] **Step 2** — implementar `dispatcherModule` con los 3 dispatchers calificados.
- [ ] **Step 3** — test, `DispatcherModuleTest.kt`: `startKoin { modules(dispatcherModule) }` seguido
      de obtener el dispatcher calificado como `IO` — confirma que no tira excepción y que no devuelve
      por accidente el de `MAIN` (evita el bug clásico de qualifier mal cableado).
- [ ] **Step 4** — implementar `initKoin()`, cablear en `MainActivity`.
- [ ] **Step 5** — verificación en Android físico: la app abre sin crashear (confirma que `startKoin`
      no explota al arrancar).
- [ ] **Step 6** — `./gradlew detekt ktfmtCheck` limpio.
- [ ] **Step 7** — commit:
```bash
git add shared/build.gradle.kts gradle/libs.versions.toml \
        shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ \
        androidApp/src/main/kotlin/com/kurobello/tallybook/MainActivity.kt
git commit -m "feat(di): add Koin base and dispatcher module"
```

## Bloqueante

No bloquea otras tareas de Wave 1. **Bloquea Wave 2 y Wave 3** (nada se puede inyectar sin esto).
