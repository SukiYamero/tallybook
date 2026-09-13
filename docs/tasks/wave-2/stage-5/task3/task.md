# Wave 2 / Stage 5 / Task 3 — Formulario y AddMovimientoSheet

## Objetivo

Renderizar el estado de Stage 4 como un formulario puro dentro del bottom sheet de Stage 5, incluyendo
validaciones, teclado, fecha, descripción opcional y commit sin pérdida de datos.

## Contexto y skills

- Requiere Stage 5 / task2 y Stage 4 completa.
- Leer `docs/tasks/wave-2/README.md`, `docs/wording.md`,
  `web/moneta/src/features/movimientos/MovimientoFormFields.tsx`, `MovimientoAmountInput.tsx` y
  `AddMovimientoSheet.tsx` como referencias legacy de orden/behavior.
- Skills requeridas: `android-dev`, `compose-state-and-effects`, `compose-component-design`,
  `android-ux`, `compose-ui-testing-patterns`, `android-testing` y `test-driven-development`.

## Archivos

- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoTypeSelector.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoAmountField.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoDateField.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoForm.kt`
- Crear: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/AddMovimientoSheet.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoFormTest.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/movimientos/ui/AddMovimientoSheetTest.kt`

## Contratos

```kotlin
@Composable
fun MovimientoForm(
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    modifier: Modifier = Modifier,
)

@Composable
fun AddMovimientoSheet(
    visible: Boolean,
    uiState: AddMovimientoUiState,
    actions: AddMovimientoActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
fun AddMovimientoSheetRoute(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddMovimientoViewModel = koinViewModel(),
)
```

Solo `Route` conoce Koin, lifecycle y efectos. `MovimientoForm`/`AddMovimientoSheet` aceptan estado y
acciones, son previewables y se prueban sin grafo.

## Reglas exactas

- Orden: selector gasto/ingreso, monto centrado, fecha, categoría, disclosure `more_fields`,
  descripción y CTA.
- El CTA usa `save_expense`/`save_income`, se deshabilita durante submit y conserva tamaño aunque
  muestre progreso.
- El monto usa teclado decimal nativo y no reescribe el cursor con formato de display mientras el
  usuario edita. El parser de Stage 4 es la única autoridad.
- Fecha abre un date picker Compose compatible con ambos targets; `maxDate` es hoy y el ViewModel
  vuelve a validar. `DatePicker`/`DatePickerDialog` son APIs comunes pero experimentales: limitar
  `@OptIn` al archivo del componente y comprobar el punto de integración en iOS. Fuente primaria:
  <https://developer.android.com/reference/kotlin/androidx/compose/material3/DatePicker.composable>.
- `DatePickerState.selectedDateMillis` representa el día seleccionado en UTC; convertirlo a
  `LocalDate` con UTC para no desplazar la fecha por el timezone del dispositivo.
- Descripción limita entrada a `MOVIMIENTO_DESCRIPTION_MAX_LENGTH` y el contador es accesible si se
  muestra.
- Errores aparecen después del primer submit, asociados semánticamente al campo. Submit inválido mueve
  foco/scroll al primer campo fallido, no cierra teclado de forma incondicional.
- `isLoading` mantiene chrome del sheet y muestra progreso; error de categorías muestra retry.
- `SAVE_FAILED` permanece visible y los valores escritos no cambian.
- `Saved` se colecciona en `LaunchedEffect(viewModel)` con lifecycle STARTED y llama el callback actual
  vía `rememberUpdatedState`.
- Cerrar manualmente descarta el draft solo cuando el owner retira el sheet. Reabrir crea/restaura el
  estado según el lifecycle real del ViewModel; documentar y probar la decisión, sin segundo store.
- No renderizar receipt scan, voz, método de pago, edit/delete ni botones inertes.

## Implementación TDD

- [ ] Construir primero previews de estado vacío, validación, cargando, failure y submitting.
- [ ] UI test de selector tipo y callbacks de campos sobre un fake `Actions`.
- [ ] UI test de submit vacío: errores de monto/categoría visibles y CTA no provoca cierre.
- [ ] UI test de input válido y submit: callback único; doble tap con submitting no genera otro.
- [ ] UI test de disclosure/descripcion y límite 180.
- [ ] UI test de fecha futura bloqueada por picker; unit test de ViewModel ya cubre bypass.
- [ ] UI test de load/save error, retry y preservación visual de valores.
- [ ] Un único wiring test de Route prueba colección de `Saved`; no repetir todos los casos con VM real.
- [ ] Prueba manual de teclado, scroll, scrim, gesto y Back en Android físico.
- [ ] Ejecutar tests pertinentes, compilación iOS, `detekt` y `ktfmtCheck`.
- [ ] Commit sugerido: `feat(movements): add the movement entry sheet`.

## Bloqueos

Bloqueada por Stage 4 y Stage 5 / task2. Bloquea toda integración de Stage 6.
