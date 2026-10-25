# Estadísticas

## Contexto

Registrar a mano solo vale la pena si después la app contesta tres preguntas: en qué se va la plata,
si este período es raro y cómo viene la tendencia. Hoy ninguna pantalla lo hace. Es el ítem 1 de
`docs/backlog.md`, y los Presupuestos (ítem 4) se apoyan en estas mismas consultas.

Prototipo con datos de ejemplo (aceptado por el usuario como dirección visual):
<https://claude.ai/artifact/ExUs8JG2G9qFTEhyWfXxCN>. Tiene tres pantallas:

- la pantalla principal;
- el detalle de categoría, como hoja;
- la tarjeta en Home.

El prototipo muestra la intención. No define datos, tamaños exactos ni arquitectura: los tokens y
componentes reales mandan.

## Decisiones

Confirmadas por el usuario:

1. **Navegación.** Estadísticas pasa a ser una pestaña y ocupa el lugar de Search:
   Home · Historial · + · Estadísticas · Perfil.
   - Search sigue siendo un destino primario retenido, pero sin ícono en la barra.
   - Sus entradas son el campo "Buscar movimientos" de Home, sin cambios visuales, y los
     "Ver movimientos" que ya existen (hojas de categoría, tag y cuenta) más el nuevo de
     Estadísticas.
   - Mientras Search está abierta, ninguna pestaña aparece seleccionada. Back vuelve a Home, como hoy.
   - Si más adelante crecen los destinos, se evalúa un menú. Queda fuera de este alcance.
2. **Home suma la tarjeta "Tu mes en números"**, que abre Estadísticas.

Defaults del plan (cambiarlos implica re-planificar):

3. **Alcances: Mes (por defecto) y Año.** Una entrada nueva arranca en el mes actual, y la selección
   se retiene como en Historial. El período en curso termina hoy. Semana queda fuera de alcance: los
   bloques de Historial tienen largos distintos y no se comparan bien.
4. **Tramo comparable.**
   - Un período en curso se compara contra el mismo tramo del período anterior: del 1 al 24 de
     octubre contra el 1 al 24 de septiembre. Un día que no existe en el período anterior se ajusta a
     su último día (31 pasa a 30; el 29 de febrero pasa al 28).
   - Un período histórico se compara completo contra el anterior completo.
   - Si el período anterior no tiene gasto, no se muestra la variación.
5. **Una moneda a la vez.**
   - Se reusan `viewedMoneda` y `MonedaSelector`, con la regla de Historial: el selector se muestra
     solo si hay cuentas en más de una moneda.
   - No hay conversión FX.
6. **Qué cuenta.** Las transferencias no son gasto ni ingreso, igual que el resumen de Historial.
   Un gasto con crédito cuenta en su `fecha`. La categoría Otros aparece como una más. Un movimiento
   cuya categoría ya no existe sigue contando, y se presenta igual que en Historial.
7. **Distribución "¿En qué gastaste?"**, solo sobre gastos, agrupable por Categorías, Tags, Cuentas
   o Medios.
   - **Exclusivas** (Categorías, Cuentas y Medios): dona más lista.
     - Las categorías suman en su raíz (`padre_id`).
     - Con más de 6 grupos, la dona muestra 5 y agrupa el resto en "Otras".
     - El color sale de `colors.chart` por rango, salteando el índice 3 (rojo, se confunde con
       gasto). No usa el tint de la categoría, porque varias raíces comparten tint. "Otras" va en
       `foreground.faint`.
     - Cada fila muestra el punto de su color, el nombre, la variación, el monto y el %.
     - Al tocar una fila:
       - Categoría: abre la hoja de detalle.
       - Tag o cuenta: abre Search filtrado por ese id, moneda, tipo GASTO y rango.
       - Medio: sin acción.
   - **Tags**: solo barras, con el % calculado contra el gasto total y la nota "no suman 100%".
     Nunca dona.
8. **Evolución.** Columnas de gastos e ingresos, con el balance como línea.
   - En Mes: los 6 meses que terminan en el mes elegido.
   - En Año: los meses del año elegido, hasta el mes actual.
   - Si el último mes está en curso, se marca como parcial. Tocar una columna muestra sus valores.
9. **Ritmo del mes.** Solo en el mes en curso.
   - El acumulado diario se compara contra el promedio de hasta 3 meses completos previos con gasto.
   - Proyección = gasto a la fecha ÷ días transcurridos × días del mes.
   - Si no hay ningún mes completo previo con gasto, la tarjeta no se muestra.
10. **"Lo que cambió".** Hasta 3 frases deterministas, sin IA:
    - la categoría que más subió y la que más bajó, solo si la variación es de al menos 10% y la
      diferencia de monto llega al 5% del gasto del período;
    - el día de mayor gasto del período (en Mes y en Año), si hubo gasto en al menos 2 días.

    Una categoría que cae a cero también puede ser "la que más bajó".

    Si ninguna aplica, la sección no se muestra.
11. **Detalle de categoría.** Es una hoja (`TallybookModalBottomSheet`) que se abre desde
    Estadísticas, con el mismo patrón visual que la hoja que aparece al tocar un chip en la pantalla
    Categorías. Las pantallas Categorías y Tags de Perfil no cambian.
    - Contenido: avatar real, total, variación, 6 meses con su promedio, subcategorías (la raíz misma
      cuenta como "General") y los 3 movimientos más grandes.
    - "Ver los N movimientos" abre Search, una vez que la hoja se cerró, filtrado por categoría,
      moneda, tipo GASTO y rango del período. N coincide con lo que muestra Search.
12. **Estados.**
    - Carga: se conserva el contenido anterior mientras se refresca (`RetainedLoadState`).
    - Error: tarjeta con reintento.
    - Período sin gastos: las tarjetas de gasto muestran un vacío breve, y el resumen sigue visible.
    - Sin ningún movimiento en la moneda: un único vacío que invita a agregar uno con el mismo
      callback del +.
13. **Accesibilidad.** Ninguna librería de gráficos expone semántica. Cada gráfico recibe un
    `contentDescription` con el resumen localizado, y los números de su tarjeta cubren la misma
    información sin depender del dibujo.

## Librería

**Vico `com.patrykandpatrick.vico:compose:3.2.3`, fijado.** El AAR de Android de la 3.3.0 declara
`minCompileSdk=37`, y el proyecto compila con 36. La 3.2.3 declara 36 y pide Compose 1.11.0 (Gradle
la resuelve a nuestra 1.11.1), coroutines 1.11.0 y Kotlin stdlib 2.3.21 (resuelto a 2.4.10).
Publica `iosArm64` e `iosSimulatorArm64`. Antes de subir de versión, revisar el `minCompileSdk` del
AAR.
- Se usan dos piezas en un mismo `CartesianChart`: `ColumnCartesianLayer` + `LineCartesianLayer`.
  - Evolución: columnas de gastos e ingresos, más la línea de balance.
  - Ritmo: líneas, con la proyección como serie punteada.
  - Meses del detalle: columnas más la línea de promedio.
- Vico tiene tema propio sin Material y se pinta con los tokens.
- **Costo:** `kotlinx-coroutines` pasa a 1.11.0. El catálogo declara 1.9.0, pero Gradle ya resuelve
  1.10.2.
- La dona y las barras de distribución se hacen en Compose: la dona de Vico es experimental y estas
  formas no tienen ejes.
- Los valores de dibujo son `Double` derivados de `Money`. Nunca se hace aritmética de dinero con
  ellos.

## Stages

| Stage | Tareas | Depende de | Paralelismo |
|---|---|---|---|
| [Stage 1](stage-1/tasks.md) — fundaciones | task1 datos · task2 reglas · task3 gráficos · task4 filtro de Search por rango | — | Las 4 tocan archivos disjuntos. task2 arranca cuando task1 commitea los tipos del modelo |
| [Stage 2](stage-2/tasks.md) — pantalla | task1 ViewModel + pantalla + hoja de detalle | Stage 1 revisado | Una sola tarea: estado, pantalla y hoja comparten el ViewModel |
| [Stage 3](stage-3/tasks.md) — integración | task1 navegación + tarjeta de Home · task2 docs, review final y aceptación | Stage 2 | task2 después de task1 |

**Revisión intermedia** solo al cerrar Stage 1, porque ahí se fija el contrato
`StatisticsRepository`/modelo y entra una dependencia nueva que tiene que compilar en iOS. Después
viene una única review final independiente (Stage 3 / task2).

**Ejecución en esta máquina.** La rama de integración es `feat/statistics`. Stage 1 usa dos
worktrees en paralelo; el límite de memoria es de 2 builds de Gradle a la vez (16 GB):
- **`feat/statistics`:** task1 (los tipos primero), después task2 y después task4.
- **`feat/statistics-charts`:** task3. Sus archivos no se cruzan con los de las otras tareas, así que
  el merge de vuelta a `feat/statistics` no tiene conflictos. Se borra al cerrar Stage 1.

La revisión intermedia corre sobre `feat/statistics` con las 4 tareas ya integradas. Stage 2 y
Stage 3 trabajan en un solo worktree. Si el paralelismo complica más de lo que ahorra, el mismo orden
funciona en secuencia.

## Archivos compartidos y su dueño

| Archivo | Dueño |
|---|---|
| `Movimiento.sq`, `DatabaseSchemaTest`, `core/model/StatisticsSnapshot.kt`, `core/data/Statistics*`, `di/RepositoryModule.kt` | S1/task1 |
| `core/model/StatisticsRules.kt`, `core/model/StatisticsRanges.kt`, `core/model/SelectableYears.kt`, `HistorySelection.kt` | S1/task2 |
| `gradle/libs.versions.toml`, `shared/build.gradle.kts`, `core/ui/chart/*`, `Dimensions.kt`, UI catalog | S1/task3 |
| `SearchFilterRequest.kt`, `PrimaryNavigator.kt`, `SearchViewModel.kt`, `SearchActions.kt`, `SearchRoute.kt`, `SearchScreenPreviews.kt`, `SearchUiTestFixture.kt` | S1/task4 |
| `feature/statistics/*`, `di/ViewModelModule.kt`, `composeResources/values*/strings.xml`, entrada de la pantalla en el UI catalog | S2/task1 (en S3, task1 también toca `ViewModelModule` y strings: son stages secuenciales) |
| `Destinations.kt`, `PrimaryNavigationState.kt`, `TallybookNavHost.kt`, `TallybookBottomNavigation.kt`, `feature/home/*`, `TallybookAppShellLifecycleHostTest`, `DestinationSerializationTest`, device tests del shell | S3/task1 |
| `docs/**` | S3/task2 |

En Stage 3, task1 también suma strings (la tarjeta de Home y el nombre de la pestaña). Para entonces
S2 ya terminó, así que no hay dos escritores a la vez.

## Cobertura

| Decisión | Implementa | Verifica |
|---|---|---|
| 1 Navegación | S3/task1 | Tests de estado de navegación (host) y device tests del shell |
| 2 Tarjeta Home | S3/task1 (datos de S1/task1 y reglas de S1/task2) | `HomeViewModelTest` y smoke en el dispositivo |
| 3–4 Alcances y tramo | S1/task2 | `StatisticsRulesTest` y `StatisticsSelectionTest` (bordes de calendario) |
| 5–6 Moneda y qué cuenta | S1/task1 | Test de integración del snapshot |
| 7 Distribución | S1/task1 (sumas), S1/task2 (top y %), S1/task3 (dona y filas), S2 | Tests de reglas y de UI pura |
| 8–9 Evolución y ritmo | S1/task2, S1/task3, S2 | `StatisticsRulesTest` (proyección, promedio y parcial) |
| 10 Lo que cambió | S1/task2 | `StatisticsRulesTest` (umbrales y orden) |
| 11 Detalle y drill-down | S1/task1, S1/task4, S2, S3/task1 | Test host de igualdad entre N y el conteo de Search (S1/task1) y device test (S3/task1) |
| 12 Estados | S2 | `StatisticsViewModelTest` |
| 13 Accesibilidad | S1/task3, S2 | Semantics en tests de UI y TalkBack en el dispositivo |

## Verificación final

- **Gate de APIs externas** (S1/task3, cumplido con Vico 3.2.3): versión resuelta de Vico y coroutines en
  `./gradlew :shared:dependencies`, POM en <https://central.sonatype.com/artifact/com.patrykandpatrick.vico/compose>,
  guía en <https://guide.vico.patrykandpatrick.com/> y `:shared:compileKotlinIosSimulatorArm64`.
- **Contrato de integración** (S1/task1): un test host siembra subcategorías, tags múltiples,
  transferencias y dos monedas. El snapshot tiene que cumplir:
  - Σ categorías = gasto total, incluido un movimiento con categoría inexistente;
  - un movimiento con dos tags cuenta una vez en cada tag, pero nunca dos veces en el total;
  - las transferencias no aparecen;
  - las monedas no se mezclan.
- **Runtime en el Xiaomi confirmado** (S3/task2):
  - pestaña y tarjeta de Home;
  - Search desde el campo de Home;
  - Mes ↔ Año y cambio de período;
  - cambio de dimensión;
  - hoja de detalle y "Ver N movimientos";
  - tema oscuro y texto agrandado;
  - TalkBack lee los resúmenes;
  - las animaciones y el toque en las columnas de Vico.

  iPhone queda en el pendiente general *iOS physical end-to-end*.
- Sobre el árbol final: `./gradlew detekt ktfmtCheck :shared:testAndroidHostTest :androidApp:assembleDebug`.
