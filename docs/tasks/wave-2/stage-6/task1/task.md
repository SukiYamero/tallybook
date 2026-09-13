# Wave 2 / Stage 6 / Task 1 — Home reactivo y app bootstrap

## Objetivo

Reemplazar el smoke test por estados reales de inicio/Home, observar balance y movimientos recientes y
montar el formulario desde Home sin crear una segunda copia de los datos persistidos.

## Contexto y skills

- Requiere Stage 5 revisado.
- Leer `AGENTS.md`, `docs/business-logic.md`, `docs/tasks/wave-2/README.md`, los componentes de Stage 5
  y `web/moneta/src/features/home/` como referencia legacy de contenido, no como arquitectura a copiar.
- Skills requeridas: `android-dev`, `compose-state-and-effects`, `kotlin-flows`,
  `kotlin-concurrency-and-flow`, `compose-component-design`, `compose-ui-testing-patterns`,
  `android-testing` y `test-driven-development`.
- El scope es Home mínimo: balance, ingresos/gastos, recientes, vacío y agregar. Week strip, chart,
  greeting dinámico, Areas, Search e History quedan fuera.

## Archivos

- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/AppStartupUiState.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/AppStartupActions.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/AppStartupViewModel.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/App.kt`
- Reemplazar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeUiState.kt`
- Eliminar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeEffect.kt`
- Eliminar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeActions.kt`
- Reemplazar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeViewModel.kt`
- Reemplazar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeScreen.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ViewModelModule.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/AppStartupViewModelTest.kt`
- Reemplazar: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/home/HomeViewModelTest.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/home/HomeScreenTest.kt`

## Contratos

```kotlin
sealed interface AppStartupUiState {
  data object Initializing : AppStartupUiState

  data object Ready : AppStartupUiState

  data object Error : AppStartupUiState
}

interface AppStartupActions {
  fun onRetry()
}

data class HomeUiState(
    val isLoading: Boolean,
    val summary: MovimientoSummary,
    val recent: List<Movimiento>,
    val categoriasById: Map<String, Categoria>,
    val error: DataError?,
) {
  val isEmpty: Boolean
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onAddMovimiento: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Si `AppStartupActions` solo conserva `onRetry`, usar lambda en el composable de error; la interfaz
puede pertenecer únicamente al ViewModel o eliminarse si no mejora el límite. No conservar estructura
por simetría.

## Ownership y data flow

- `AppStartupViewModel` ejecuta `LocalDataBootstrapper.initialize()` una vez por instancia. Error queda
  durable con retry; Ready habilita `TallybookNavHost`.
- `App.kt` mantiene un solo `TallybookTheme`, colecciona startup state y muestra contenido puro de
  inicialización/error/ready. No usa `runBlocking`.
- `HomeViewModel` combina `observeSummary(moneda)`, `observeRecent(limit = 6)`, `observeAll()` y
  preferencias. Solo `AppPreferencesState.Ready` alimenta Home; `Uninitialized` después de startup se
  trata como estado inconsistente recuperable. Las transforms son puras y cada upstream tiene emisión
  inicial.
- `HomeViewModel.retry()` incrementa un trigger y vuelve a suscribir el pipeline completo mediante
  `flatMapLatest`; no crea collectors acumulados ni loops de retry automáticos.
- `HomeRoute` colecciona el UiState, posee `addSheetVisible` como estado UI local y monta
  `AddMovimientoSheetRoute`. No agregar un store global con un único consumidor.
- `HomeScreen` no ve ViewModels/Koin/Flows; solo estado y `onAddMovimiento`.
- Después de insert, SQLDelight invalida queries y Home se actualiza. Prohibido hacer
  `recent + movimiento` o ajustar balance manualmente en UI/ViewModel.

## Reglas exactas

- Startup loading no muestra Home vacío detrás; startup error explica que datos existentes siguen
  guardados y permite retry.
- Home loading conserva el chrome básico; error llama `onRetry`, sin loop automático infinito.
- Estado vacío usa la copy de Stage 1 y el mismo callback de agregar que el FAB.
- Lista reciente muestra máximo 6, con keys estables por `movimiento.id`.
- Resumen siempre pertenece a `monedaPrincipal`; otras monedas no se ocultan mediante suma incorrecta.
- FAB tiene label/content description localizado y touch target mínimo.
- La sheet no forma parte del back stack de Navigation 3: su primitive modal maneja Back/dismiss.
- Al cerrar manualmente y volver a abrir durante la misma ruta se presenta un formulario limpio. Si
  esto exige recrear el ViewModel key, hacerlo en Route y cubrirlo con test; no agregar `reset` oculto a
  la UI.

## Implementación TDD

- [ ] Tests RED/GREEN de startup success, failure durable, retry y prohibición de doble initialize
      concurrente.
- [ ] Tests RED/GREEN de Home: initial loading, empty, summary/recientes, category map y error upstream.
- [ ] Test de combine donde cada upstream emite en distinto orden; Home no queda bloqueado ni produce
      un estado parcialmente incoherente.
- [ ] Construir `HomeScreen` puro con previews de loading, empty, populated y error.
- [ ] UI tests de cada branch y callbacks del FAB, empty CTA y retry.
- [ ] Integrar sheet con estado local y confirmar cierre manual/éxito/reapertura limpia.
- [ ] Confirmar con un fake repository observable que un insert actualiza Home por Flow, no por mutación
      local.
- [ ] Actualizar Koin y sus tests de resolución.
- [ ] Ejecutar tests, compilación iOS, `detekt` y `ktfmtCheck`.
- [ ] Commit sugerido: `feat(home): show the local balance and recent movements`.

## Bloqueos

Bloqueada por Stage 5. Bloquea Stage 6 / task2.
