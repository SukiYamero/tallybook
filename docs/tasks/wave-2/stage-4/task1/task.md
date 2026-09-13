# Wave 2 / Stage 4 / Task 1 — Reglas y AddMovimientoViewModel

## Objetivo

Implementar el motor testeable del formulario: estado editable, validación, carga de preferencias y
categorías, construcción del `Movimiento` y escritura única. Ningún composable se crea en esta tarea.

## Contexto y skills

- Requiere Stage 3 revisado.
- Leer `AGENTS.md`, `docs/business-logic.md`, `docs/tasks/wave-2/README.md` y
  `web/moneta/src/features/movimientos/useMovimientoForm.ts` como referencia de comportamiento.
- Skills requeridas: `android-dev`, `compose-state-and-effects`, `kotlin-flows`,
  `kotlin-concurrency-and-flow`, `kotlin-api-design`, `android-testing` y
  `test-driven-development`.
- Mantener MVVM con split MVI ya establecido por `HomeViewModel`: estado durable en `UiState`, efecto
  one-shot en `Channel`, acciones mediante interfaz estable.

## Archivos

- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/IdGenerator.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/TimeZoneProvider.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/TimeModule.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AmountInput.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoUiState.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoActions.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoEffect.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoViewModel.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/AppModules.kt`
- Modificar: `shared/src/commonMain/kotlin/com/kurobello/tallybook/di/ViewModelModule.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AmountInputTest.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoViewModelTest.kt`

## Contratos

```kotlin
fun interface IdGenerator {
  fun next(): String
}

fun interface TimeZoneProvider {
  fun current(): TimeZone
}

sealed interface AmountInputResult {
  data class Valid(val money: Money) : AmountInputResult

  data class Invalid(val reason: AmountInputError) : AmountInputResult
}

enum class AmountInputError {
  EMPTY,
  MALFORMED,
  NOT_POSITIVE,
  TOO_MANY_DECIMALS,
}

internal fun parseAmountInput(raw: String, moneda: Moneda): AmountInputResult

enum class MovimientoFormError {
  CATEGORIES_UNAVAILABLE,
  SAVE_FAILED,
}

data class AddMovimientoUiState(
    val tipo: TipoMovimiento,
    val amountInput: String,
    val fecha: LocalDate,
    val moneda: Moneda,
    val categorias: List<Categoria>,
    val selectedCategoriaId: String?,
    val descripcion: String,
    val isLoading: Boolean,
    val isSubmitting: Boolean,
    val submitAttempted: Boolean,
    val error: MovimientoFormError?,
)

@Stable
interface AddMovimientoActions {
  fun onTipoChanged(tipo: TipoMovimiento)
  fun onAmountChanged(value: String)
  fun onFechaChanged(value: LocalDate)
  fun onCategoriaSelected(id: String)
  fun onDescripcionChanged(value: String)
  fun onRetryLoad()
  fun onSubmit()
}

sealed interface AddMovimientoEffect {
  data object Saved : AddMovimientoEffect
}
```

`AddMovimientoUiState` expone como getters derivados `amountError`, `isCategoriaMissing` y
`canSubmit`; ninguno es parámetro del constructor. Si esos getters requieren una representación
interna adicional, mantenerla privada al archivo y evitar dos parsers.

## Reglas exactas

- Producción: `IdGenerator` usa `Uuid.random().toString()`, estable en Kotlin 2.4; `Clock` usa
  `Clock.System`; `TimeZoneProvider` usa `TimeZone.currentSystemDefault()`.
- Tests inyectan ID, Clock y zona fija. No parchear tiempo global ni comparar con el reloj real.
- Input acepta dígitos y un único separador decimal de `Moneda`; no acepta separadores de miles,
  notación científica, signo, espacios internos, `NaN` ni `Infinity`.
- Rechazar vacío, cero/negativo, exceso de decimales y overflow de `Long` con razones distintas.
- Fecha por defecto: `clock.todayIn(timeZoneProvider.current())`. Fecha posterior a ese mismo día no
  guarda aunque una UI defectuosa la envíe.
- Descripción: máximo `MOVIMIENTO_DESCRIPTION_MAX_LENGTH` caracteres de entrada; al guardar, trim y
  espacios consecutivos/newlines se convierten en un solo espacio; resultado vacío → `null`.
- Categoría seleccionada debe existir en la última lista cargada.
- `onSubmit` marca intento, valida y lanza como máximo una coroutine mientras `isSubmitting` sea true.
- Error de repositorio conserva campos, pone `SAVE_FAILED` en estado y no emite `Saved`.
- Editar cualquier campo después de un error limpia `SAVE_FAILED`; no limpia los indicadores de campos
  obligatorios mientras `submitAttempted` siga activo.
- Éxito emite `Saved` una vez. El cierre es efecto; la escritura confirmada ya vive en SQLDelight.
- Preferencias/categorías se combinan de forma pura. No lanzar coroutines ni efectos dentro de
  `combine`/`map`; evitar pantalla eterna en loading asegurando emisión inicial de cada fuente.
- Exponer estado con explicit backing field o `stateIn(WhileSubscribed(5_000))` según el ownership real;
  no crear dos `MutableStateFlow` con la misma información.

## Implementación TDD

- [ ] Tests RED/GREEN table-driven de parser: escalas 0/2, separadores coma/punto, malformed,
      not-positive, demasiados decimales y overflow.
- [ ] Tests de defaults con Clock/zona fija, incluyendo un instante que cae en días distintos en UTC y
      `America/Bogota`.
- [ ] Test RED/GREEN de carga de moneda/categorías y selección válida.
- [ ] Test RED/GREEN por cada campo obligatorio: repositorio no recibe inserts.
- [ ] Test RED/GREEN de descripción normalizada y construcción exacta de `Movimiento`, incluido
      `createdAt` del Clock e ID fake.
- [ ] Test de doble submit con repositorio suspendido controladamente: exactamente una llamada.
- [ ] Test de failure durable que conserva campos y de success con exactamente un efecto `Saved`.
- [ ] Test de cancelación: cancelar ViewModel/scope no convierte cancelación en error de guardado.
- [ ] Registrar bindings de producción y actualizar test del grafo Koin.
- [ ] Ejecutar `commonTest`, `testAndroidHostTest`, compilación iOS, `detekt` y `ktfmtCheck`.
- [ ] Commit sugerido: `feat(movements): add the movement form state and validation`.

## Bloqueos

Bloqueada por Stage 3. Bloquea las tareas UI de Stage 5.
