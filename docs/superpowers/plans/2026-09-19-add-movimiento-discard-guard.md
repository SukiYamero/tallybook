# Add Movimiento Discard Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the compact add-movimiento sheet bounded as optional details grow and protect changed drafts from accidental dismissal.

**Architecture:** `AddMovimientoViewModel` owns an immutable opening `MovimientoDraft`, exposes a derived dirty flag, and resets that draft only after confirmed discard. A composition-owned `DismissGuardState` coordinates all sheet close requests and a reusable Unstyled-backed confirmation dialog. The movement form splits its fixed save footer from a capped, scrollable details region.

**Tech Stack:** Kotlin 2.4.10, Compose Multiplatform 1.11.1, Compose Unstyled 2.9.0, StateFlow, KMP common tests, Android Compose device tests.

**Spec:** `docs/tasks/add-movimiento-redesign.md`

## Global Constraints

- Keep all production UI and state logic in `shared/src/commonMain` and use the existing split-MVI `UiState` plus `AddMovimientoActions` pattern.
- Do not add Material components to the movimiento flow; use Tallybook tokens and Compose Unstyled 2.9.0.
- Do not depend on an IME action label or presence, and do not move the footer above the keyboard.
- Do not add expansion or programmatic-scroll animations in this increment.
- Use `./gradlew :shared:testAndroidHostTest`, `detekt`, `ktfmtCheck`, and `:androidApp:assembleDebug`; validate interaction on the connected physical Xiaomi.
- Do not commit the current uncommitted worktree without explicit user authorization.

---

### Task 1: Model the add-movimiento draft and dirty-state contract

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoUiState.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoActions.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoViewModel.kt`
- Modify: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoViewModelTest.kt`

**Interfaces:**
- Produces `MovimientoDraft`, `AddMovimientoUiState.hasUnsavedChanges`, and `AddMovimientoActions.onDiscardChanges()`.
- `AddMovimientoViewModel` owns `initialDraft: MovimientoDraft` and resets only editable fields with `onDiscardChanges()`.

- [ ] **Step 1: Write failing ViewModel tests for dirty and discard behavior.**

```kotlin
@Test
fun changingADraftFieldMarksTheFormAsHavingUnsavedChanges() = runTest {
  val viewModel = createViewModel(scope = backgroundScope)
  runCurrent()

  viewModel.onAmountChanged("12.34")

  assertTrue(viewModel.uiState.value.hasUnsavedChanges)
}

@Test
fun discardingChangesRestoresTheOpeningDraft() = runTest {
  val viewModel = createViewModel(scope = backgroundScope)
  runCurrent()
  viewModel.onAmountChanged("12.34")
  viewModel.onDescripcionChanged("Lunch")

  viewModel.onDiscardChanges()

  assertEquals("", viewModel.uiState.value.amountInput)
  assertEquals("", viewModel.uiState.value.descripcion)
  assertFalse(viewModel.uiState.value.hasUnsavedChanges)
}
```

- [ ] **Step 2: Run the focused tests and confirm they fail because the state and action do not exist.**

Run: `./gradlew :shared:testAndroidHostTest --tests com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModelTest`

- [ ] **Step 3: Add the immutable draft and derive dirty state through the existing ViewModel update path.**

```kotlin
internal data class MovimientoDraft(
    val tipo: TipoMovimiento,
    val amountInput: String,
    val fecha: LocalDate,
    val selectedCategoriaId: String?,
    val descripcion: String,
)

internal fun AddMovimientoUiState.toDraft(): MovimientoDraft =
    MovimientoDraft(tipo, amountInput, fecha, selectedCategoriaId, descripcion)
```

Store the opening draft in the ViewModel, update `hasUnsavedChanges` after every editable-field update,
and implement `onDiscardChanges()` by restoring that draft while preserving repository-loaded data.

- [ ] **Step 4: Run the focused ViewModel tests and confirm they pass.**

Run: `./gradlew :shared:testAndroidHostTest --tests com.kurobello.tallybook.feature.movimientos.viewmodel.AddMovimientoViewModelTest`

### Task 2: Add reusable dismissal coordination and confirmation UI

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookDismissGuard.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookConfirmationDialog.kt`
- Create: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookDismissGuardTest.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-en/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-es-rAR/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-pt-rBR/strings.xml`

**Interfaces:**
- Consumes `hasUnsavedChanges: Boolean`, `onDiscardChanges: () -> Unit`, and final `onDismiss: () -> Unit`.
- Produces `DismissGuardState.requestDismiss()`, `continueEditing()`, `confirmDiscard()`, and `isConfirmationVisible`.
- `TallybookConfirmationDialog` accepts visible state, title, message, confirm/cancel labels, and callbacks.

- [ ] **Step 1: Write failing pure-state tests for dismissal decisions.**

```kotlin
@Test
fun cleanDraftDismissesImmediately() {
  assertEquals(DismissalDecision.Dismiss, dismissalDecision(hasUnsavedChanges = false))
}

@Test
fun changedDraftRequestsConfirmation() {
  assertEquals(DismissalDecision.RequestConfirmation, dismissalDecision(hasUnsavedChanges = true))
}
```

- [ ] **Step 2: Run the focused test and confirm it fails because the decision API does not exist.**

Run: `./gradlew :shared:testAndroidHostTest --tests com.kurobello.tallybook.core.ui.component.TallybookDismissGuardTest`

- [ ] **Step 3: Add the minimal reusable state holder and dialog.**

```kotlin
internal enum class DismissalDecision { Dismiss, RequestConfirmation }

internal fun dismissalDecision(hasUnsavedChanges: Boolean): DismissalDecision =
    if (hasUnsavedChanges) DismissalDecision.RequestConfirmation else DismissalDecision.Dismiss
```

`rememberDismissGuard` keeps only confirmation visibility in composition, calls the final dismissal
callback immediately for a clean draft, and calls discard then final dismissal only on confirm. Its
`continueEditing` callback is used by the dialog's outside-click and back dismiss callback.

Declare `com.composables:composeunstyled-dialog` directly with the existing `composeUnstyled` version.
Build `TallybookConfirmationDialog` with `UnstyledDialog`, `Scrim`, and `DialogPanel`; apply existing
Tallybook surface, radius, color, spacing, button, and semantic-pane-title primitives. Add translated
copy for discard, continue editing, title, and body.

- [ ] **Step 4: Run the focused dismissal tests and confirm they pass.**

Run: `./gradlew :shared:testAndroidHostTest --tests com.kurobello.tallybook.core.ui.component.TallybookDismissGuardTest`

### Task 3: Wire the guard into the movement sheet and bound More details

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Dimensions.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookTextField.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookAmountInput.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoAmountField.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoForm.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/AddMovimientoSheet.kt`
- Modify: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/movimientos/ui/AddMovimientoSheetTest.kt`
- Modify: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoFormTest.kt`

**Interfaces:**
- Consumes `DismissGuardState` and `AddMovimientoActions.onDiscardChanges()`.
- Produces a sheet whose fixed footer is outside the details scroll container and whose confirmation
  interception applies to drag, scrim, and system-back dismissal requests.

- [ ] **Step 1: Write failing UI tests for the bounded details region and discard interception.**

```kotlin
@Test
fun expandedDetailsScrollWithoutMovingTheSaveAction() {
  composeRule.onNodeWithText(resourceString(Res.string.more_fields)).performClick()
  val saveBounds = composeRule.onNodeWithContentDescription(resourceString(Res.string.save_expense))
      .fetchSemanticsNode().boundsInRoot

  composeRule.onNodeWithTag("movimiento_details").performTouchInput { swipeUp() }

  assertEquals(
      saveBounds,
      composeRule.onNodeWithContentDescription(resourceString(Res.string.save_expense))
          .fetchSemanticsNode().boundsInRoot,
  )
}

@Test
fun dirtySheetDismissRequestShowsDiscardConfirmation() {
  val state = DismissGuardState()
  state.update(hasUnsavedChanges = true, onDiscardChanges = {}, onDismiss = {})
  state.requestDismiss()

  assertTrue(state.isConfirmationVisible)
}
```

- [ ] **Step 2: Run the device-test class and confirm the new assertions fail before implementation.**

Run: `./gradlew :shared:connectedAndroidDeviceTest --tests com.kurobello.tallybook.feature.movimientos.ui.AddMovimientoSheetTest`

- [ ] **Step 3: Split the form body from its footer and cap only the expandable details region.**

Add a `movimientoDetailsMaximumHeight` token sized for the description section plus half of a normal
field. Keep category and primary fields outside this region. Put Description and future optional fields
in one `Column(verticalScroll(detailsScrollState))` with `heightIn(max = ...)`; do not nest it inside a
second scrollable column. Render the save action after that region so it stays fixed while details scroll.

- [ ] **Step 4: Add keyboard-safe footer behavior.**

Extend the two custom text input components with optional `KeyboardActions`. Request `ImeAction.Done`
for amount and description, and use its callback only to clear focus. Read `WindowInsets.ime` in the
form and omit the fixed footer while the IME is visible; do not use `imePadding` on the sheet or footer.

- [ ] **Step 5: Wire the generic guard around AddMovimientoSheet.**

Route the existing `ModalBottomSheetCallbacks.onDismiss` through `DismissGuardState.requestDismiss`.
Render `TallybookConfirmationDialog` beside the sheet. Its `onDismiss` calls `continueEditing`; its
confirm callback calls `actions.onDiscardChanges()` and then the original sheet dismissal callback.

- [ ] **Step 6: Run the focused device tests and confirm the new contracts pass.**

Run: `./gradlew :shared:connectedAndroidDeviceTest --tests com.kurobello.tallybook.feature.movimientos.ui.AddMovimientoSheetTest --tests com.kurobello.tallybook.feature.movimientos.ui.MovimientoFormTest`

### Task 4: Verify the complete feature on the physical device

**Files:**
- Modify: `docs/tasks/add-movimiento-redesign.md` only if verification reveals a current requirement
  that differs from the approved behavior.

- [ ] **Step 1: Apply formatter and run shared verification.**

Run: `./gradlew :shared:ktfmtFormat`

Run: `./gradlew :shared:testAndroidHostTest detekt ktfmtCheck`

- [ ] **Step 2: Build and install the debug APK.**

Run: `./gradlew :androidApp:assembleDebug`

Install the generated APK with the resolved Android SDK `adb` path on the one connected Xiaomi device.

- [ ] **Step 3: Manually verify the interaction contract.**

Open Add movement; enter a value; attempt drag, scrim, and system back dismissal; dismiss the
confirmation by outside tap/back; confirm the draft remains. Confirm discard and reopen to verify an
empty draft. Open More details and verify only the details region scrolls while Save remains fixed. Focus
Amount and Description to verify the keyboard does not expand the sheet and Save returns after it closes.

- [ ] **Step 4: Run final diff checks.**

Run: `git diff --check`

Run: `git status --short`
