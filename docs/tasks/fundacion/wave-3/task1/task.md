# Wave 3 / Task 1 — Pantalla smoke-test

## Objetivo

Una pantalla real (`feature.home`), sin lógica de negocio, que atraviesa toda la plomería: Koin
inyecta su ViewModel, se navega a través del shell de Nav3, se renderiza con `TallybookTheme`, muestra
un ícono de `icons-lucide` y una primitiva de `composables:core` — para confirmar en un dispositivo
físico real que todo lo de Wave 1 y Wave 2 encaja, antes de construir la primera feature real sobre
esta base.

## Contexto

- "Sin lógica de negocio" significa: el ViewModel no llama a ningún repositorio todavía — esta
  pantalla no es "ver movimientos", es la prueba de que la arquitectura funciona. La primera feature
  real (movimientos) es un spec aparte, posterior a este.
- Patrón a seguir: MVVM con split MVI (`UiState` + `Channel` de efectos + interfaz `Actions`), per
  `AGENTS.md §3` — aunque acá el estado sea trivial, seguir el patrón desde ya evita reescribir la
  primera pantalla real con una estructura distinta.
- Agregar `icons-lucide` y `composables:core` a `libs.versions.toml` recién acá (no en Wave 1) — son
  libs de UI que esta tarea es la primera en usar de verdad.

## Archivos

- Modificar: `gradle/libs.versions.toml` — `com.composables:icons-lucide`, `com.composables:core`
  (verificar últimas versiones estables).
- Modificar: `shared/build.gradle.kts` — agregar a `commonMain`.
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeUiState.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeActions.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeViewModel.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeScreen.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ViewModelModule.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/AppModules.kt` — agregar
  `viewModelModule`.
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/Destinations.kt`
  — reemplazar `Placeholder1` por `Home`, mantener `Placeholder2`.
- Borrar (confirmar con `grep` antes): `shared/src/commonMain/kotlin/com/kurobello/tallybook/App.kt`,
  `AppInfo.kt`, `Platform.kt` + `.android.kt`/`.ios.kt` si nada los usa ya.

## Implementación

- [ ] **Step 1** — agregar `icons-lucide`/`composables:core`, confirmar que resuelven.
- [ ] **Step 2** — `HomeUiState` (trivial, ej. `data class HomeUiState(val mensaje: String = "Tallybook")`),
      `HomeActions` (interfaz vacía o con una acción trivial como `onNavigateNext()`), `HomeViewModel`
      (expone `StateFlow<HomeUiState>`, implementa `HomeActions`, sin llamar a ningún repositorio).
- [ ] **Step 3** — `HomeScreen` composable: confirmar en qué nivel se aplica `TallybookTheme`
      (probablemente en el `NavHost` o el punto de entrada de cada plataforma, no repetido por
      pantalla), muestra el `mensaje`, un ícono de `icons-lucide`, y una primitiva de
      `composables:core` (ej. un `Dialog` o `DropdownMenu` que se abre con un botón — no hace falta
      contenido real, solo confirmar que renderiza).
- [ ] **Step 4** — cablear `Home` en `TallybookNavHost` (Wave 1/task3) como `startDestination`, con un
      botón que navega al segundo destino.
- [ ] **Step 5** — `viewModelModule` con el mecanismo real de Koin-Compose vigente para inyectar
      `HomeViewModel`, agregar a `appModules`.
- [ ] **Step 6** — borrar `App.kt`/`AppInfo.kt`/`Platform.*` si el `grep` confirma que no hace falta
      nada de ahí (si `Platform.kt` todavía se usa para algo de `DatabaseDriverFactory` o similar,
      dejarlo y anotarlo en el commit).
- [ ] **Step 7** — build + instalar en Android físico:
      `./gradlew :androidApp:assembleDebug && adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n com.kurobello.tallybook/.MainActivity`.
      Confirmar visualmente: theme aplicado (colores + Manrope), ícono Lucide visible, la primitiva de
      `composables:core` se abre, navegar al segundo destino y volver con el botón atrás del sistema
      funciona.
- [ ] **Step 8** — `./gradlew detekt ktfmtCheck` limpio.
- [ ] **Step 9** — commit:
```bash
git add shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/ \
        shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ \
        shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/Destinations.kt \
        shared/build.gradle.kts gradle/libs.versions.toml
git commit -m "feat(home): add smoke-test screen wiring DI, navigation, and theme"
```

## Bloqueante

Bloqueada por Wave 2/task1 y Wave 1/task3+task4. **Bloquea Wave 4** (el cleanup final necesita que
esta pantalla ya exista y corra).
