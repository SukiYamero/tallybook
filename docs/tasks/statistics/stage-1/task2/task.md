# Statistics / Stage 1 / Task 2 — Reglas

## Objetivo

Escribir las reglas de Estadísticas como funciones puras en `commonMain`, probadas sin UI ni base:
- la selección y sus rangos;
- la variación;
- el reparto top + "Otras";
- el ritmo;
- "Lo que cambió".

La consumen el ViewModel de Stage 2 y la tarjeta de Home en Stage 3.

## Contexto y skills

- Requiere el commit de tipos de task1 (`core/model/StatisticsSnapshot.kt`).
- Leer el [README](../../README.md) (decisiones 3, 4 y 7–10), `MovimientoPeriodRanges.kt` y
  `HistorySelection.kt`.
- Skills: `android-dev`, `kotlin-api-design`, `kotlin-control-flow`, `android-testing` y
  `test-driven-development`. Los tests van en camelCase en `commonTest`.

## Archivos

- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/StatisticsRules.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/StatisticsRanges.kt`. Contiene
  `StatisticsScope` y los rangos (período, tramo, tendencia y `statisticsRequest`) como funciones de
  `(scope, anchor, moneda, today)`. Viven en `core.model` porque Home arma el request del mes en curso
  sin importar otra feature.
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/SelectableYears.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/history/viewmodel/HistorySelection.kt`
  (`availableYears` delega en `selectableYears`, sin cambio de comportamiento)
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/statistics/viewmodel/StatisticsSelection.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/model/StatisticsRulesTest.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/statistics/viewmodel/StatisticsSelectionTest.kt`

## Contratos

```kotlin
// core/model/StatisticsRanges.kt
internal enum class StatisticsScope { Month, Year }
internal fun statisticsPeriod(scope: StatisticsScope, anchor: LocalDate, today: LocalDate): MovimientoDateRange      // ends at today when current
internal fun statisticsComparison(scope: StatisticsScope, anchor: LocalDate, today: LocalDate): MovimientoDateRange  // decision 4
internal fun statisticsTrend(scope: StatisticsScope, anchor: LocalDate, today: LocalDate): MovimientoDateRange       // decision 8
internal fun statisticsRequest(scope: StatisticsScope, anchor: LocalDate, moneda: Moneda, today: LocalDate): StatisticsRequest // daily = period

// feature/statistics/viewmodel/StatisticsSelection.kt: selection state, delegates its ranges
internal data class StatisticsSelection(val scope: StatisticsScope, val anchor: LocalDate) {
  fun request(moneda: Moneda, today: LocalDate): StatisticsRequest
  fun periods(today: LocalDate): List<MovimientoDateRange> // month chips or year chips
  fun selectScope(scope: StatisticsScope, today: LocalDate): StatisticsSelection
  fun selectPeriod(period: MovimientoDateRange, today: LocalDate): StatisticsSelection
  fun selectYear(year: Int, today: LocalDate): StatisticsSelection
  fun refresh(previousToday: LocalDate, today: LocalDate): StatisticsSelection
  companion object { fun initial(today: LocalDate): StatisticsSelection }
}

internal data class Change(val percent: Int, val direction: Direction)  // rounded half away from zero
internal fun change(current: Money, previous: Money?): Change?        // null when previous is null or zero

internal data class Share(val key: StatisticsKey?, val amount: Money, val basisPoints: Int) // key null = "Otras"
internal fun exclusiveShares(totals: List<DimensionTotal>, total: Money, maxSlices: Int = 6): List<Share>
internal fun overlappingShares(totals: List<DimensionTotal>, total: Money): List<Share>      // tags, no grouping

internal data class Pace(val cumulative: List<Money>, val averageMonth: Money, val projection: Money, val change: Change)
internal fun pace(daily: List<DayGasto>, months: List<MonthTotals>, today: LocalDate): Pace?  // null unless daily is the current month

internal sealed interface Insight {
  data class CategoryRose(val key: StatisticsKey.Categoria, val change: Change, val difference: Money) : Insight
  data class CategoryFell(val key: StatisticsKey.Categoria, val change: Change, val difference: Money) : Insight
  data class BiggestDay(val fecha: LocalDate, val gasto: Money) : Insight
}
internal fun insights(snapshot: StatisticsSnapshot): List<Insight>  // max 3, in that order
```

Las firmas son orientativas. Si una no cierra con el código real, ajustarla y avisar en el handoff;
no forzarla.

## Reglas

- **Redondeo.** Los porcentajes y basis points se redondean explícitamente, con aritmética entera
  sobre `minorUnits` y sin `Double`.
- **Reparto exclusivo.** Los basis points suman exactamente 10.000: el residuo de redondeo lo absorbe
  el grupo más grande. Si hay más de 6 grupos, se muestran 5 y el resto va a "Otras".
- **Desempate** por monto DESC y luego por id.
- **Ritmo:**
  - el promedio usa hasta 3 meses completos previos con gasto mayor a 0;
  - devuelve `null` si no hay ninguno o si el período no es el mes en curso;
  - la proyección se redondea explícitamente.
- **Lo que cambió:**
  - las subas y bajas aplican los umbrales de la decisión 10, contra `snapshot.current.gastos`;
  - no se reporta una categoría sin `previous` o con `previous` en cero;
  - una categoría con `current` en cero y `previous > 0` puede ser `CategoryFell`;
  - `BiggestDay` sale de `snapshot.daily` (todo el período, en Mes y en Año) y necesita gasto en al
    menos 2 días.

## Tests

**Calendario:**
- 31 de marzo contra febrero, en año bisiesto y no bisiesto;
- 29 de febrero en Año contra el año anterior;
- enero contra diciembre del año anterior;
- un mes histórico completo;
- `refresh` cuando cambia el día y cuando cambia el mes;
- ningún período termina después de hoy (Search rechaza fechas futuras);
- Año en curso: tendencia hasta el mes actual.

**Reglas:**
- el residuo hace que los basis points sumen 10.000;
- 7 grupos dan 5 + "Otras";
- variación con anterior en cero o `null`;
- ritmo sin historia, con 1 mes y con 3 meses;
- umbrales de las frases justo debajo y justo encima;
- una categoría que cae a cero aparece como la que más bajó;
- el día de mayor gasto en un mes histórico y en Año;
- el orden y el máximo de 3 frases.

Los tests de `HistorySelection` tienen que seguir verdes sin cambios.

## Salida

Tests `commonTest` verdes (`:shared:testAndroidHostTest`), `detekt ktfmtCheck` limpio y handoff con
cualquier firma ajustada.
