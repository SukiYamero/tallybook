# Wave 3 / Task 1 — Pantalla smoke-test

## Objetivo

Una pantalla real (`feature.home`), sin lógica de negocio, que atraviesa toda la plomería: Koin
inyecta su ViewModel, se navega a través del shell de Nav3, se renderiza con `TallybookTheme`, muestra
un ícono de `icons-lucide-cmp` y un diálogo de `composeunstyled-dialog` — para confirmar en un dispositivo
físico real que todo lo de Wave 1 y Wave 2 encaja, antes de construir la primera feature real sobre
esta base.

## Contexto

- "Sin lógica de negocio" significa: el ViewModel no llama a ningún repositorio todavía — esta
  pantalla no es "ver movimientos", es la prueba de que la arquitectura funciona. La primera feature
  real (movimientos) es un spec aparte, posterior a este.
- Patrón a seguir: MVVM con split MVI (`UiState` + `Channel` de efectos + interfaz `Actions`), per
  `AGENTS.md §3` — aunque acá el estado sea trivial, seguir el patrón desde ya evita reescribir la
  primera pantalla real con una estructura distinta.
- Agregar `icons-lucide-cmp` 2.2.1 y `composeunstyled-dialog` 2.9.0 a `libs.versions.toml` recién acá
  (no en Wave 1) — son libs de UI que esta tarea es la primera en usar de verdad. `icons-lucide` 1.x
  es la coordenada anterior; desde 2.x el artefacto KMP oficial termina en `-cmp`. `composables:core`
  1.49.9 también es la coordenada 1.x anterior: Compose Unstyled 2.x usa módulos por primitiva y el
  paquete `com.composeunstyled`. Se fija 2.9.0 porque 2.9.2 ya requiere Compose Multiplatform 1.12,
  mientras este proyecto resuelve 1.11.1.
- Evidencia API (versiones resueltas): Kotlin 2.4.10 permite explicit backing fields estables; Koin
  4.2.2 declara ViewModels con `viewModelOf` y los inyecta en Compose Multiplatform mediante
  `koinViewModel`; Compose Unstyled publica su API modular 2.x y Compose Icons publica
  `icons-lucide-cmp` 2.2.1. Fuentes primarias:
  <https://kotlinlang.org/docs/properties.html#explicit-backing-fields>,
  <https://insert-koin.io/docs/reference/koin-compose/compose/>,
  <https://composables.com/compose-unstyled/docs/migration-to-2> y
  <https://github.com/composablehorizons/composeicons/releases/tag/2.2.1>.

## Archivos

- Modificar: `gradle/libs.versions.toml` — `com.composables:icons-lucide-cmp` 2.2.1 y
  `com.composables:composeunstyled-dialog` 2.9.0.
- Modificar: `shared/build.gradle.kts` — agregar a `commonMain`.
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeUiState.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeActions.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeEffect.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeViewModel.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeScreen.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ViewModelModule.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/AppModules.kt` — agregar
  `viewModelModule`.
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/Destinations.kt`
  — reemplazar `Placeholder1` por `Home`, mantener `Placeholder2`.
- Mantener `App.kt`: es el entry point compartido que Android e iOS consumen y el único dueño de
  `TallybookTheme`. Borrar (confirmar con `rg` antes) `AppInfo.kt`, `Platform.kt` y sus `actual` si
  nada los usa.
- Tests de ViewModel/efectos y serializers en `commonTest`; prueba visual e interacción completa en
  el Android físico.

## Implementación

- [x] **Step 1** — agregar `icons-lucide-cmp`/`composeunstyled-dialog`, confirmar que resuelven.
- [x] **Step 2** — `HomeUiState`, `HomeActions.onNavigateNext()`, `HomeEffect.NavigateNext` y
      `HomeViewModel`. El estado usa explicit backing field; el efecto usa
      `Channel(Channel.BUFFERED).receiveAsFlow()` y no llama a repositorios.
- [x] **Step 3** — separar `HomeRoute` (Koin, colección de estado/efectos) de `HomeScreen` puro. El
      estado efímero del diálogo headless vive localmente en Compose. `TallybookTheme` se mantiene
      una sola vez en `App.kt`. Mostrar el mensaje, un ícono Lucide y un diálogo de
      `composeunstyled-dialog` que pueda abrirse y cerrarse.
- [x] **Step 4** — cablear `Home` en `TallybookNavHost` (Wave 1/task3) como `startDestination`, con un
      botón que navega al segundo destino.
- [x] **Step 5** — `viewModelModule` con el mecanismo real de Koin-Compose vigente para inyectar
      `HomeViewModel`, agregar a `appModules`.
- [x] **Step 6** — mantener `App.kt` y borrar `AppInfo.kt`/`Platform.*` si `rg` confirma que no tienen
      consumidores.
- [x] **Step 7** — build + instalar en Android físico:
      `./gradlew :androidApp:assembleDebug && adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n com.kurobello.tallybook/.MainActivity`.
      Confirmar visualmente: theme aplicado (colores + Manrope), ícono Lucide visible, el diálogo de
      `composeunstyled-dialog` se abre, navegar al segundo destino y volver con el botón atrás del sistema
      funciona.
- [x] **Step 8** — `./gradlew detekt ktfmtCheck` limpio.
- [x] **Step 9** — commit:
```bash
git add -A
git commit -m "feat(home): add smoke-test screen wiring DI, navigation, and theme"
```

## Bloqueante

Bloqueada por Wave 2/task1 y Wave 1/task3+task4. **Bloquea Wave 4** (el cleanup final necesita que
esta pantalla ya exista y corra).
