# Statistics / Stage 1 / Task 4 — Filtro de Search por rango

## Objetivo

Hoy los "Ver movimientos" (hojas de categoría, tag y cuenta) abren Search con un
`SearchFilterRequest` de un solo uso que trae ids y moneda. Esta tarea le suma **rango de fechas** y
**tipo**, para que "Ver los N movimientos" de Estadísticas abra exactamente lo que se contó.

El campo "Buscar movimientos" de Home no cambia.

## Contexto y skills

- Leer `SearchFilterRequest.kt`, `PrimaryNavigator.showSearchFilter`, el `SearchFilterRequestEffect`
  de `SearchRoute.kt`, `SearchFilters.kt` (`SearchDatePreset.Custom` con `customStart`/`customEnd`) y
  la sección Search de `docs/business-logic.md`.
- Skills: `android-dev`, `compose-state-and-effects`, `kotlin-flows` y `android-testing`.

## Archivos

- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/search/ui/SearchFilterRequest.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/PrimaryNavigator.kt`
- Modificar: `feature/search/viewmodel/SearchViewModel.kt` (la aplicación del request; hoy resetea
  los demás filtros)
- Modificar: todos los que comparten la firma `onShowFilters`: `SearchActions.kt`, `SearchRoute.kt`
  (`SearchFilterRequestEffect`), `SearchScreenPreviews.kt` y
  `androidDeviceTest/.../SearchUiTestFixture.kt`
- Test: los tests host existentes de la aplicación del request (extenderlos, no crear un archivo nuevo)

## Contrato

```kotlin
internal data class SearchFilterRequest(
    val token: Long,
    val categoryIds: Set<String> = emptySet(),
    val etiquetaIds: Set<String> = emptySet(),
    val cuentaIds: Set<String> = emptySet(),
    val moneda: Moneda? = null,
    val range: MovimientoDateRange? = null,
    val tipo: SearchTipo? = null,
)
```

`PrimaryNavigator.showSearchFilter(...)` suma `range` y `tipo`, con default `null`. Las llamadas
actuales no cambian.

## Reglas

- **Con `range`:**
  - el preset queda en `Custom`, con `customStart`/`customEnd` = el rango;
  - no se ajusta nada en silencio: Search rechaza fechas futuras (`SearchDateFailure.FutureDate`),
    y Estadísticas nunca envía un fin posterior a hoy (S1/task2 lo prueba). Si igual llega uno
    futuro, se muestra el error de fecha que ya existe;
  - el año del rango ya queda seleccionable (`availableYears` incluye `customStart.year`): solo hace
    falta un test.
- **Sin `range`:** se conserva el comportamiento actual con las fechas.
- **Con `tipo`:** el filtro de tipo queda en ese valor.
- Se mantiene el resto de la semántica actual: el request se aplica una vez y se confirma por token.

## Tests

- **Request con categoría, moneda, rango y tipo:** los filtros aplicados son exactamente esos y la
  consulta resultante trae solo esos movimientos.
- Un request sin `range` mantiene el comportamiento actual (el test existente sigue verde).
- Un rango que cruza el año sigue siendo válido.

## Salida

Tests host verdes y `detekt ktfmtCheck` limpio. Los device tests de Search existentes compilan.
