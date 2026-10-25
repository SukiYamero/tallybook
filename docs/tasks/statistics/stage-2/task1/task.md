# Statistics / Stage 2 / Task 1 — ViewModel, pantalla y hoja de detalle

## Objetivo

Convertir el snapshot y las reglas de Stage 1 en la pantalla del prototipo, con la hoja de detalle
de categoría y todos sus estados. El montaje en la navegación es de Stage 3.

## Contexto y skills

- Requiere Stage 1 revisado.
- Leer:
  - el [README](../../README.md), todas las decisiones;
  - el prototipo (<https://claude.ai/artifact/ExUs8JG2G9qFTEhyWfXxCN>);
  - `docs/ui/architecture.md` (tokens, presentación de Historial);
  - `docs/wording.md`.
- Patrones a copiar:
  - `HistoryViewModel`: `inputs` en un `MutableStateFlow`, `SavedStateHandle`, `RetainedLoadState`,
    `onResume`, captura de reloj y zona una sola vez;
  - `HistoryPeriodControls`: usa `TallybookPeriodPickerHeader`, `TallybookPeriodPickerRow`,
    `TallybookYearMenu` y `TallybookSegmentedControl`;
  - las hojas de Categorías y Accounts: `TallybookModalBottomSheet` y navegación cuando la hoja ya se
    cerró.
- Skills: `android-dev` (sección "New-project UI convention": `UiState` + `Channel` + `Actions`),
  `compose`, `compose-state-and-effects`, `compose-component-design`, `kotlin-flows`, `koin`,
  `android-testing` y `compose-ui-testing-patterns`.

## Archivos

En `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/statistics/`:

- `viewmodel/StatisticsUiState.kt`
- `viewmodel/StatisticsActions.kt`
- `viewmodel/StatisticsEffect.kt`
- `viewmodel/StatisticsViewModel.kt`
- `viewmodel/StatisticsSavedState.kt`
- `viewmodel/StatisticsPresentation.kt`: mapea snapshot + catálogos a filas, porciones y textos de
  resumen. Puro y testeable.
- `ui/StatisticsRoute.kt`
- `ui/StatisticsScreen.kt`
- `ui/StatisticsSummaryCard.kt`
- `ui/StatisticsDistributionCard.kt`
- `ui/StatisticsTrendCard.kt`
- `ui/StatisticsPaceCard.kt`
- `ui/StatisticsInsightsCard.kt`
- `ui/CategoryStatisticsSheet.kt`

Fuera de esa carpeta:

- Modificar: `di/ViewModelModule.kt`
- Modificar: `composeResources/values*/strings.xml` (es, es-rAR, en, pt-BR)
- Modificar: el UI catalog (una entrada para la pantalla con un repositorio falso)
- Test: `commonTest/.../feature/statistics/viewmodel/StatisticsViewModelTest.kt`
- Test: `commonTest/.../feature/statistics/viewmodel/StatisticsPresentationTest.kt`
- Test: `androidDeviceTest/.../feature/statistics/ui/StatisticsScreenTest.kt`

## Contratos

```kotlin
internal enum class StatisticsDimension { Categorias, Etiquetas, Cuentas, Medios }

internal data class StatisticsUiState(
    val selection: StatisticsSelection,
    val today: LocalDate,
    val moneda: Moneda?,
    val monedas: List<Moneda>,
    val years: List<Int>,
    val dimension: StatisticsDimension,
    val trendSelection: Int?,                 // null = last month
    val content: RetainedLoadState<StatisticsContent, StatisticsFailure>,
    val detail: CategoryDetailState?,         // null = sheet hidden
) { val showsMonedaSelector: Boolean get() = monedas.size > 1 }

internal interface StatisticsActions {
  fun onScopeChanged(scope: StatisticsScope)
  fun onPeriodSelected(period: MovimientoDateRange)
  fun onYearSelected(year: Int)
  fun onMonedaSelected(moneda: Moneda)
  fun onDimensionSelected(dimension: StatisticsDimension)
  fun onTrendMonthSelected(index: Int)
  fun onCategoriaOpened(rootId: String)
  fun onDetailDismissed()
  fun onViewMovimientos()                     // from the sheet
  fun onAddMovimiento()                       // empty state
  fun onRetry()
  fun onResume()
}

internal sealed interface StatisticsEffect {
  data class OpenSearch(
      val key: StatisticsKey, val moneda: Moneda, val range: MovimientoDateRange,
  ) : StatisticsEffect                        // Categoria, Etiqueta or Cuenta; from the sheet only after it hides
  data object OpenCreation : StatisticsEffect
}
```

- `StatisticsContent` es el resultado ya presentado: textos formateados con `Money.format()`,
  porciones con color resuelto por rango, `TrendPoint`, ritmo y frases. Así `StatisticsScreen` no
  calcula nada.
- `StatisticsRoute(onOpenSearch, onOpenCreation, clearance, modifier)` es la única pieza que conoce
  Koin, el lifecycle y los efectos.

## Comportamiento

- **Entrada y selección.**
  - Una entrada nueva arranca en el mes actual.
  - En `SavedStateHandle` se guardan selección, moneda y dimensión, con un `version` como
    `HistorySavedInput`.
  - La hoja abierta no se restaura después de matar el proceso. Es efímera, como las demás hojas.
- **Lecturas.**
  - `StatisticsRepository.observe(selection.request(...))` con `flatMapLatest` sobre los inputs.
  - Los nombres e íconos salen de los repositorios de categorías, etiquetas y cuentas que ya usa
    Historial.
  - Los años se arman con `selectableYears` + `ActividadRepository.observeYears(moneda)`.
- **Mientras recarga** (cambio de período o de moneda), se conserva el contenido anterior. Cambiar
  de dimensión no vuelve a leer: el snapshot ya trae las cuatro.
- **Estados:** decisión 12. El error muestra la tarjeta con reintento y no borra la selección.
- **Hoja de detalle.**
  - `onCategoriaOpened` lee `observeCategory` con el mismo período.
  - Cambiar de período o de moneda con la hoja abierta la cierra.
  - Mientras se cierra, la hoja conserva los últimos datos que mostró: el contenido no desaparece a
    mitad de la animación.
  - "Ver los N movimientos" cierra la hoja y emite `OpenSearch` recién cuando la hoja informa que se
    ocultó (el patrón de las hojas de Categorías).
- **Fila por dimensión.**
  - Categorías: abre la hoja.
  - Tags y Cuentas: abren Search directo, con ese id, moneda, rango y tipo GASTO, vía `OpenSearch`
    generalizado.
  - Medios: sin acción. Search ya filtra por medio, pero la request no lo trae (fuera de alcance).
- **Colores:** `colors.chart` por rango, salteando el índice 3. "Otras" va en `foreground.faint`.
  Las filas en cero (solo `previous > 0`) no entran en la dona ni en la lista.
- **Resúmenes accesibles:** un string por gráfico con sus valores principales, en los 4 idiomas.
- **Copy:** desde `docs/wording.md`. Usar los textos del prototipo como punto de partida, no como
  definitivos.

## Tests

- **ViewModel**, con dispatcher de test y repositorios falsos:
  - entrada nueva;
  - restauración;
  - cambio de alcance, período, año, moneda y dimensión;
  - recarga que conserva el contenido;
  - error y reintento;
  - hoja: abrir, cambiar período, cerrar y "Ver N" que emite un único `OpenSearch` después de
    ocultarse;
  - el refresco de día con `onResume`.
- **Presentación:**
  - colores por rango, sin el índice 3, y "Otras" neutral;
  - las claves con actual en cero se filtran;
  - "Otras";
  - tags sin dona;
  - variación oculta sin anterior;
  - mes parcial marcado;
  - textos con la moneda correcta.
- **UI pura** (device, compila acá y corre en la aceptación): semántica del selector, las pestañas
  de dimensión, las filas tocables y la hoja.

## Salida

Ruta montable en el catálogo, tests verdes, copy completa, `:androidApp:assembleDebug`, compilación
iOS y `detekt ktfmtCheck` limpio. Handoff con cualquier desvío de los contratos.
