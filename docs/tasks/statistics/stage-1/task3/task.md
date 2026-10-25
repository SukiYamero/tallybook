# Statistics / Stage 1 / Task 3 — Gráficos

## Objetivo

Agregar Vico 3.2.3 y construir los componentes de gráficos stateless y reutilizables en
`core/ui/chart`. Reciben valores ya calculados y un resumen accesible, y no conocen ni el dominio ni
los ViewModels.

## Contexto y skills

- Leer el [README](../../README.md) (sección Librería, decisiones 7–9 y 13), `docs/ui/architecture.md`
  (tokens; "data-driven chart values" está permitido) y `docs/ui/components.md`.
- Skills: `android-dev`, `compose`, `compose-component-design`, `compose-performance`,
  `compose-animations`, `compose-ui-testing-patterns` y `chrisbanes-skills:gradle-run`.
- **Gate de APIs externas** (`docs/README.md`), antes de construir encima:
  1. confirmar que la versión resuelta de `com.patrykandpatrick.vico:compose` es 3.2.3, que
     `kotlinx-coroutines` pasa de la 1.10.2 resuelta hoy a 1.11.0, y que el POM declara Compose 1.11.1
     y Kotlin 2.4.10;
  2. verificar `CartesianChartHost`, `rememberCartesianChart`, `rememberColumnCartesianLayer`,
     `rememberLineCartesianLayer`, `CartesianChartModelProducer` y el marker contra la guía y el
     código de la etiqueta `v3.2.3`;
  3. compilar `:shared:compileKotlinIosSimulatorArm64`.

  Si algo de esto falla, parar y reportar. No subir a otra versión.

## Archivos

- Modificar: `gradle/libs.versions.toml` (`vico = "3.2.3"`, `kotlinx-coroutines = "1.11.0"`)
- Modificar: `shared/build.gradle.kts` (`commonMain`)
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Dimensions.kt`
  (alturas y grosores de los gráficos)
- Crear en `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/chart/`:
  - `TallybookDonut.kt`
  - `TallybookDistributionRow.kt`
  - `TallybookTrendChart.kt`
  - `TallybookPaceChart.kt`
  - `TallybookCategoryTrendChart.kt`
  - `TallybookChartTheme.kt` (Vico pintado con `TallybookTheme`)
- Modificar: el UI catalog (una sección "Charts" con datos fijos, claro y oscuro)
- Test: `shared/src/androidDeviceTest/.../core/ui/chart/ChartComponentsTest.kt` (semantics y
  callbacks; compila en esta tarea y corre en la aceptación)

## Contratos

```kotlin
@Immutable data class DonutSlice(val basisPoints: Int, val color: Color)

@Composable fun TallybookDonut(
    slices: List<DonutSlice>, contentDescription: String, modifier: Modifier = Modifier,
    center: @Composable () -> Unit = {},
)

@Immutable data class ChangeIndicator(val label: String, val tone: ChangeTone)  // Unfavorable, Favorable, Neutral

@Composable fun TallybookDistributionRow(
    label: String, amount: String, share: String, fraction: Float, color: Color,
    modifier: Modifier = Modifier, change: ChangeIndicator? = null, onClick: (() -> Unit)? = null,
)

@Immutable data class TrendPoint(val label: String, val gastos: Double, val ingresos: Double, val partial: Boolean)

@Composable fun TallybookTrendChart(
    points: List<TrendPoint>, selectedIndex: Int, onSelect: (Int) -> Unit,
    contentDescription: String, modifier: Modifier = Modifier,
)

@Composable fun TallybookPaceChart(
    cumulative: List<Double>, averageMonth: Double, projection: Double, daysInMonth: Int,
    todayLabel: String, contentDescription: String, modifier: Modifier = Modifier,
)

@Composable fun TallybookCategoryTrendChart(
    values: List<Double>, labels: List<String>, average: Double, partialLast: Boolean, color: Color,
    contentDescription: String, modifier: Modifier = Modifier,
)
```

Todos aplican `modifier` en su raíz.

## Reglas visuales

- **Colores:**
  - gastos `status.danger.base`, ingresos `brand.primary`, balance `foreground.primary`;
  - promedio `foreground.faint`, punteado;
  - dona: los colores llegan desde afuera (el reparto por rango y el color de "Otras" los resuelve
    S2).

  Gastos e ingresos se distinguen también por luminosidad.
- Un mes parcial va atenuado, y su etiqueta lo marca.
- La selección de la Evolución se ve como opacidad y peso. El valor se muestra afuera del gráfico, en
  la tarjeta (S2).
- Sin leyendas embebidas ni textos fijos: las etiquetas llegan como parámetros, ya localizadas.
- Cada gráfico es un único nodo de semántica con su `contentDescription`. Las columnas de la
  Evolución tienen además una acción accesible para elegir el mes, o se reemplaza por los chips de
  meses si Vico no lo permite. Documentar cuál de las dos quedó.
- Las animaciones de entrada son las de Vico, con la duración de `TallybookTheme.motion`.

## Tests

- Previews o catálogo con vacío, un valor, nueve porciones, todo en cero y textos largos.
- `ChartComponentsTest`: la semántica expone el resumen, `onSelect` y `onClick` se llaman, y un
  `fraction` de 0 o de 1 no rompe el layout.

## Salida

- Gate completo con evidencia: versiones resueltas, enlace y compilación iOS.
- Catálogo con la sección Charts.
- `:androidApp:assembleDebug`, compilación de `androidDeviceTest`, y `detekt ktfmtCheck` limpio.
