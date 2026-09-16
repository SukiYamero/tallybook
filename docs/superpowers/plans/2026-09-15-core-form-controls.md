# Core Form Controls Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox
> (`- [ ]`) syntax for tracking.

**Goal:** Add five tokenized, Material-independent form controls and render their complete state
matrices in the development UI catalog.

**Architecture:** Stateless controls live in `core.ui.component` and expose values plus callbacks.
`TallybookTextField` owns shared editing and field-decoration behavior; `TallybookSearchField` composes
it instead of copying text input logic. Segmented and selection controls model mutually exclusive state
with typed values, and the catalog supplies deterministic local state for interaction testing.

**Tech Stack:** Kotlin 2.4.10, Compose Multiplatform Foundation `BasicTextField`, Compose Resources,
Lucide icons, Android instrumented Compose tests.

**Spec:** `docs/superpowers/specs/2026-09-15-ui-catalog-core-controls-design.md`

## Global Constraints

- Execute this plan after `docs/superpowers/plans/2026-09-15-ui-catalog-loading.md` is complete.
- Do not migrate Add movement in this plan; preserve its existing behavior and Material controls.
- Components are stateless and depend on values/callbacks, never ViewModels or repositories.
- Every component accepts a caller `Modifier`; callers own external placement.
- Interactive targets remain at least 48 dp.
- Error and supporting copy use one sealed state, never independent conflicting flags.
- No raw `dp`, `sp`, brand colors, alpha values, or animation durations appear in component code.
- All visible and accessibility copy comes from callers or localized resources.
- Every task follows RED-GREEN-REFACTOR and ends with a focused commit.
- All Gradle execution uses the existing `gradle-run` workflow wrapper.

---

## File structure

- `TallybookFieldMessage.kt` owns supporting/error message state shared by editable and selection fields.
- Each control has one production file named after its primary composable.
- Each component test targets the plain composable contract with callbacks and semantics.
- `UiCatalogControls.kt` owns catalog-only local sample state and keeps `UiCatalogScreen.kt` focused on
  section composition.

---

### Task 1: Field-state foundation and icon button

**Files:**

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookFieldMessage.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookIconButton.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Dimensions.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookIconButtonTest.kt`

**Interfaces:**

```kotlin
@Immutable
internal sealed interface TallybookFieldMessage {
  val text: String

  @Immutable data class Supporting(override val text: String) : TallybookFieldMessage
  @Immutable data class Error(override val text: String) : TallybookFieldMessage
}

@Composable
internal fun TallybookIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

- [ ] **Step 1: Write failing icon-button tests**

```kotlin
@Test
fun iconButtonExposesDescriptionAndInvokesCallback() {
  var clicks = 0
  composeRule.setContent {
    TallybookTheme {
      TallybookIconButton(
          onClick = { clicks += 1 },
          icon = Lucide.X,
          contentDescription = "Close sample",
      )
    }
  }

  composeRule
      .onNodeWithContentDescription("Close sample")
      .assertHasClickAction()
      .performClick()
  composeRule.runOnIdle { assertEquals(1, clicks) }
}

@Test
fun disabledIconButtonDoesNotInvokeCallback() {
  var clicks = 0
  composeRule.setContent {
    TallybookTheme {
      TallybookIconButton(
          onClick = { clicks += 1 },
          icon = Lucide.X,
          contentDescription = "Close sample",
          enabled = false,
      )
    }
  }

  composeRule.onNodeWithContentDescription("Close sample").assertIsNotEnabled().performClick()
  composeRule.runOnIdle { assertEquals(0, clicks) }
}
```

- [ ] **Step 2: Run the test and verify RED**

Expected: compilation fails because `TallybookIconButton` does not exist.

- [ ] **Step 3: Add icon-button tokens and minimal implementation**

Add semantic dimensions for visible icon size and container size. Use `TallybookSurface`, a tokenized
rounded shape, `Modifier.clickable(enabled, role = Role.Button)`, theme navigation/content colors, and
an `Image` with a null nested description so the root owns one accessibility node.

- [ ] **Step 4: Run the tests and verify GREEN**

Underlying task:

```text
:shared:connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=com.kurobello.tallybook.core.ui.component.TallybookIconButtonTest
```

- [ ] **Step 5: Commit the icon button and field state**

```text
feat(ui): add icon button foundation
```

---

### Task 2: Tokenized text field

**Files:**

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookTextField.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Color.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Dimensions.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookTextFieldTest.kt`

**Interfaces:**

```kotlin
@Composable
internal fun TallybookTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    message: TallybookFieldMessage? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = TallybookTheme.typography.body,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
)
```

- [ ] **Step 1: Write failing editing and state tests**

Tests render real `BasicTextField` behavior through `TallybookTextField` and verify:

```kotlin
composeRule.onNodeWithText("Description").performTextInput("Lunch")
composeRule.runOnIdle { assertEquals("Lunch", value) }
```

Separate tests assert placeholder display, disabled semantics, read-only behavior, and an error message
with `SemanticsProperties.Error` plus `LiveRegionMode.Polite`. A supporting message must not publish
error semantics.

- [ ] **Step 2: Run the text-field class and verify RED**

Expected: compilation fails because the component does not exist.

- [ ] **Step 3: Add semantic field tokens and colors**

Add field minimum height, horizontal/vertical padding, icon size, and border width to Dimensions.
Extend semantic colors only if the existing border base, ring, status danger, surface card, and
foreground tiers cannot express a required state. Do not introduce component-local colors.

- [ ] **Step 4: Implement with `BasicTextField`**

Use one remembered `MutableInteractionSource` and `collectIsFocusedAsState()` to select the tokenized
border. A `Column` renders the optional label, decorated input row, and exactly one optional message.
The error variant applies `semantics { error(message.text) }`; its text applies a polite live region.
The decoration box owns placeholder and leading/trailing slots.

- [ ] **Step 5: Run the tests and verify GREEN**

Run the component test at normal font scale and at `2f`. The field must grow vertically rather than
clip label, value, placeholder, or message.

- [ ] **Step 6: Commit the text field**

```text
feat(ui): add tokenized text field
```

---

### Task 3: Search field composed from text behavior

**Files:**

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSearchField.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSearchFieldTest.kt`

**Interfaces:**

```kotlin
@Composable
internal fun TallybookSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearContentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClear: () -> Unit,
)
```

- [ ] **Step 1: Write failing search tests**

Verify input updates `query`, the localized clear action is absent for an empty query, appears for a
non-empty query, invokes `onClear` once, and is disabled with the field.

```kotlin
composeRule.onNodeWithContentDescription("Clear search").performClick()
composeRule.runOnIdle { assertEquals(1, clears) }
```

- [ ] **Step 2: Run the search tests and verify RED**

- [ ] **Step 3: Implement by composing `TallybookTextField`**

Supply a Lucide Search leading image and `TallybookIconButton` trailing content only when the query is
not empty. Do not duplicate `BasicTextField`, focus, placeholder, or border code.

- [ ] **Step 4: Run the tests and verify GREEN**

- [ ] **Step 5: Commit the search field**

```text
feat(ui): add search field
```

---

### Task 4: Typed segmented control

**Files:**

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSegmentedControl.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Dimensions.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSegmentedControlTest.kt`

**Interfaces:**

```kotlin
@Immutable
internal data class TallybookSegmentOption<T>(val value: T, val label: String)

@Composable
internal fun <T> TallybookSegmentedControl(
    options: List<TallybookSegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

- [ ] **Step 1: Write failing typed-selection tests**

Use a private test enum with Expense and Income. Assert one selected semantic, clicking Income returns
Income, disabled segments do not invoke callbacks, and an empty option list throws an actionable
`IllegalArgumentException`.

- [ ] **Step 2: Run the tests and verify RED**

- [ ] **Step 3: Implement a selectable group**

Use `Row.selectableGroup()` and equal weighted segments. Each segment uses `Modifier.selectable` with
`Role.RadioButton`, selected semantics, a 48 dp minimum target, tokenized container colors, and no
Material `FilterChip`.

- [ ] **Step 4: Run the tests and verify GREEN**

- [ ] **Step 5: Commit the segmented control**

```text
feat(ui): add segmented control
```

---

### Task 5: Read-only selection field

**Files:**

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSelectionField.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSelectionFieldTest.kt`

**Interfaces:**

```kotlin
@Composable
internal fun TallybookSelectionField(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    message: TallybookFieldMessage? = null,
    leadingContent: (@Composable () -> Unit)? = null,
)
```

- [ ] **Step 1: Write failing selection-field tests**

Verify placeholder/value branches, root button semantics, callback delivery, disabled behavior, error
semantics, polite error copy, and large-text vertical growth.

- [ ] **Step 2: Run the tests and verify RED**

- [ ] **Step 3: Implement one merged button-like field**

Use `TallybookSurface`, a tokenized border, an optional leading slot, one weighted value/placeholder,
and a decorative Lucide ChevronRight. Apply click semantics to the merged root and reuse
`TallybookFieldMessage` rendering rules from the text field rather than introducing `isError` plus a
separate message.

- [ ] **Step 4: Run the tests and verify GREEN**

- [ ] **Step 5: Commit the selection field**

```text
feat(ui): add selection field
```

---

### Task 6: Catalog state matrices

**Files:**

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogControls.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogScreen.kt`
- Modify: all four `shared/src/commonMain/composeResources/values*/strings.xml` catalogs
- Modify: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/resources/Wave2ResourcesTest.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogScreenTest.kt`

**Interfaces:**

- Produces catalog-only samples for default, populated, disabled, selected, supporting, and error
  states of every new component.

- [ ] **Step 1: Write failing catalog coverage tests**

Scroll to each localized component-family heading and assert representative semantics: disabled icon
button, editable text field, clearable search, selected segment, error selection field, and centered
loading.

- [ ] **Step 2: Run the catalog tests and verify RED**

- [ ] **Step 3: Add localized catalog labels and state samples**

Use local `rememberSaveable` only for sample values that users edit during a catalog session. Use
fixed initial values and no persistence outside the route.

- [ ] **Step 4: Run catalog and component tests and verify GREEN**

Run every new component test class plus `UiCatalogScreenTest` on the physical tablet.

- [ ] **Step 5: Commit catalog controls**

```text
feat(ui): catalog form controls
```

---

### Task 7: Full verification and independent review

**Files:**

- Modify only files required by verified reviewer findings.

**Interfaces:** None.

- [ ] **Step 1: Run `ktfmtFormat`, then `detekt ktfmtCheck` through the wrapper**

- [ ] **Step 2: Run shared host tests, all physical-device UI tests, Android debug/release assembly,
  and `compileKotlinIosArm64`**

- [ ] **Step 3: Review every control in portrait light/dark, font scale 2×, and landscape on the
  physical Android tablet**

- [ ] **Step 4: Review the same catalog on a physical iPhone when connected; otherwise report visual
  iPhone acceptance as pending without reinstalling a simulator**

- [ ] **Step 5: Dispatch one read-only `gpt-5.6-terra` reviewer at `high` reasoning**

The reviewer checks API scope, modifier ownership, BasicTextField focus behavior, semantics, large-text
layout, recomposition cost, duplicate logic, tokens, localization, and test quality. Fix every Critical
and Important finding with a new failing test before production changes.

- [ ] **Step 6: Repeat the full matrix after reviewer fixes and commit**

```text
fix(ui): address form control review findings
```

- [ ] **Step 7: Finish the Gradle workflow, confirm `git diff --check`, and report every bounded
  verification answer**
