# Statistics / Stage 3 / Task 1 — Navegación y tarjeta de Home

## Objetivo

Estadísticas pasa a ocupar el lugar de Search en la barra (decisión 1). Home suma la tarjeta "Tu mes
en números" (decisión 2). "Ver N movimientos" queda conectado con Search.

## Contexto y skills

- Requiere Stage 2.
- Leer:
  - `docs/ui/architecture.md` › Tallybook destination rule;
  - `PrimaryNavigationState.kt`, `PrimaryNavigator.kt`, `TallybookNavHost.kt`, `PrimaryEntries.kt`
    y `TallybookBottomNavigation.kt`;
  - `HomeViewModel.kt`, `HomeScreen.kt` y `HomeOuterShell.kt`;
  - los device tests de navegación.
- Skills: `android-dev`, `compose`, `compose-state-and-effects`, `koin`, `android-testing` y
  `compose-ui-testing-patterns`.
- El gate de APIs externas aplica a la restauración de navegación: el estado es `@Serializable` y se
  guarda.

## Archivos

- Modificar: `core/ui/navigation/Destinations.kt` (agrega `Statistics`)
- Modificar: `core/ui/navigation/PrimaryNavigationState.kt`
- Modificar: `core/ui/navigation/TallybookNavHost.kt` (`PrimaryRoute`, `primaryContentKey`)
- Modificar: `core/ui/navigation/TallybookBottomNavigation.kt`
- Modificar: `feature/home/HomeViewModel.kt`, `HomeScreen.kt` y `di/ViewModelModule.kt` (la dependencia
  nueva de `HomeViewModel`)
- Crear: `feature/home/HomeStatisticsCard.kt`
- Modificar: `composeResources/values*/strings.xml` (nombre de la pestaña y copy de la tarjeta)
- Tests host:
  - el test existente de `PrimaryNavigationState` y `HomeViewModelTest`;
  - `TallybookAppShellLifecycleHostTest`: su `when (destination)` exhaustivo deja de compilar;
  - `DestinationSerializationTest`: sumar `Statistics`.
- Device tests:
  - modificar: `SearchNavigationTest`, `HomeScreenTest`, `CategoriesNavigationTest`,
    `TagsNavigationTest` y `UiCatalogScreenTest`;
  - crear: `StatisticsNavigationTest`.

## Navegación

- **Estado.**
  - `primaryDestinations = {Home, History, Search, Statistics, Profile}`.
  - Se agrega `statisticsVisited: Boolean = false`. El default mantiene restaurable un estado
    guardado por la versión anterior.
  - Hay que actualizar `retainedKeys`, `select`, `initialPrimaryNavigation` y el `when` exhaustivo
    de `opensOver`: `Statistics` no se abre como secundaria de nada.
- **Barra:** `trailingTabs = [Statistics (Lucide.ChartColumn), Profile]`.
  - Search queda sin ícono. Con `selected == Search`, ninguna pestaña aparece seleccionada.
  - Se mantiene el orden Home · Historial · + · Estadísticas · Perfil.
- **Back:** desde Estadísticas vuelve a Home, igual que las demás pestañas.
- **Search** se entra desde el campo de Home (`openSearch`, sin cambios) y desde los
  `showSearchFilter`.
- **`StatisticsRoute`:**
  - `onOpenSearch(key, moneda, range)` → `navigator.showSearchFilter(...)`. La `key` decide si van
    `categoryIds`, `etiquetaIds` o `cuentaIds`, siempre con `tipo = SearchTipo.GASTO`;
  - `onOpenCreation` → `navigator.openCreation()`;
  - mientras haya un modal del shell abierto, los intents se rechazan como en todas las rutas.

## Tarjeta de Home

- `HomeViewModel` observa
  `StatisticsRepository.observe(statisticsRequest(Month, today, moneda, today))` en la moneda
  principal, la misma que ya usa el `BalanceCard`. Las funciones vienen de `core.model`, así Home no
  importa la feature de Estadísticas. Con eso arma:
  - el top 3 de categorías, con `exclusiveShares(maxSlices = 4)` (3 + resto);
  - el ritmo, con `pace(...)`.
- **Cuándo se muestra:** solo si el mes tiene gasto. La línea del ritmo, solo si `pace != null`.
  Va entre el `BalanceCard` y "Recientes".
- **Cuando falla la lectura** de estadísticas, la tarjeta no se muestra y el resto de Home sigue
  igual: es una superficie secundaria y no suma un estado de error a Home.
- **Al tocarla** se selecciona Estadísticas. La selección de Estadísticas no se toca: si estaba en
  otro período, se conserva, por la regla de retención.

## Tests

- **Host:**
  - transiciones con `Statistics`: select, back, retención y restaurar un estado sin el campo nuevo;
  - Search seleccionado sin pestaña marcada;
  - `HomeViewModel`: la tarjeta aparece y desaparece según el gasto, y el ritmo es opcional.
- **Device:**
  - `SearchNavigationTest` entra por el campo de Home;
  - `StatisticsNavigationTest`: pestaña → hoja de Comida → "Ver N movimientos" → Search con el mismo
    conteo N y los filtros aplicados;
  - tarjeta de Home → Estadísticas.
- Correr los device tests con la receta MIUI de `AGENTS.md`, solo las clases afectadas.

## Salida

Tests host verdes, device tests afectados en verde en el Xiaomi (o el bloqueo registrado con
precisión), `:androidApp:assembleDebug`, compilación iOS y `detekt ktfmtCheck` limpio.
