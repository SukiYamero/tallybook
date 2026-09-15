# UI Foundation Components Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the tokenized, Material-independent UI foundation required by the Moneta-inspired Home shell without changing Home behavior yet.

**Architecture:** `TallybookTheme` becomes the owner of semantic Compose locals while preserving a temporary Material compatibility bridge for screens that have not migrated. Foundation-based text, surface, button, and screen-frame components consume only Tallybook tokens. Common Compose UI tests run on the iOS simulator so component behavior can follow a real red-green cycle without an Android device.

**Tech Stack:** Kotlin 2.4.10, Compose Multiplatform 1.11.1, Compose UI Test v2, Material3 compatibility bridge only, Manrope resources.

**Spec:** `docs/ui/architecture.md`

## Global Constraints

- Production composables contain no anonymous static `dp`, `sp`, color, radius, alpha, duration, or easing literals.
- Compose Foundation owns new visual components; do not add another component library.
- Existing Material screens must remain functional until they migrate.
- New declarations start `private` and widen only to `internal` when another file in `shared` consumes them.
- Every reusable layout accepts and applies a caller `Modifier` at its root.
- Safe-area values come from `WindowInsets.safeDrawing`; no device model or hardcoded notch value is allowed.
- Use TDD for observable component behavior and do not add tests that only assert constant values.
- Run Gradle only through the `gradle-run` compact-output wrapper.

---

### Task 1: Multiplatform theme and token foundation

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Color.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Typography.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/TallybookTheme.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Spacing.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Dimensions.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Radius.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Motion.kt`
- Create: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/theme/TallybookThemeTest.kt`
- Delete after migration: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/theme/TallybookThemeTest.kt`

**Interfaces:**
- Produces: `TallybookTheme(...)`, `TallybookTheme.colors`, `TallybookTheme.typography`, `TallybookTheme.spacing`, `TallybookTheme.dimensions`, `TallybookTheme.radii`, and `TallybookTheme.motion`.
- Preserves: existing `TallybookTheme { ... }` call sites and the Material color/typography bridge used by legacy components.

- [ ] **Step 1: Add the common Compose UI test dependency**

Add a `compose-uiTest` alias using `composeMultiplatform` and add it to `commonTest.dependencies`. Keep the Android device-test dependencies because existing device tests still use JUnit rules.

- [ ] **Step 2: Write the failing theme-provider test**

Create a v2 `runComposeUiTest` test that captures `TallybookTheme.colors.canvas` under light and dark themes and asserts the two semantic colors differ. The production mutation it catches is a theme provider that always installs the same palette.

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun lightAndDarkThemesProvideDifferentSemanticCanvases() = runComposeUiTest {
  var darkTheme by mutableStateOf(false)
  var currentCanvas = Color.Unspecified

  setContent {
    TallybookTheme(darkTheme = darkTheme) { currentCanvas = TallybookTheme.colors.canvas }
  }
  waitForIdle()
  val lightCanvas = currentCanvas

  runOnIdle { darkTheme = true }
  waitForIdle()

  assertNotEquals(Color.Unspecified, lightCanvas)
  assertNotEquals(lightCanvas, currentCanvas)
}
```

- [ ] **Step 3: Run the iOS test and verify RED**

Run `:shared:iosSimulatorArm64Test` through the compact Gradle wrapper. Expected: compilation fails because `TallybookTheme.colors` and the full semantic palette do not exist.

- [ ] **Step 4: Implement the token files and theme locals**

Implement the documented spacing and type scales, semantic dimensions, radii, and motion values. Expand `TallybookColors` so it owns canvas, surfaces, foreground tiers, brand colors, borders, status colors, chart colors, and bottom-navigation colors. Refactor `TallybookTheme` into an object with an `operator fun invoke` so current call syntax remains valid while composables gain typed theme accessors.

The compatibility bridge remains inside `TallybookTheme`:

```kotlin
@Composable
operator fun invoke(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
  val colors = if (darkTheme) DarkTallybookColors else LightTallybookColors
  val typography = rememberTallybookTypography()
  CompositionLocalProvider(
      LocalTallybookColors provides colors,
      LocalTallybookTypography provides typography,
      LocalContentColor provides colors.foreground.primary,
  ) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = legacyMaterialTypography(typography),
    ) {
      Box(Modifier.fillMaxSize().background(colors.canvas)) { content() }
    }
  }
}
```

- [ ] **Step 5: Run the iOS test and verify GREEN**

Run the same wrapped `:shared:iosSimulatorArm64Test`. Expected: the new test and existing common tests pass.

- [ ] **Step 6: Run formatting and static analysis**

Run wrapped `ktfmtFormat`, then wrapped `detekt ktfmtCheck`.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/theme shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/theme/TallybookThemeTest.kt
git commit -m "feat(ui): add tokenized Tallybook theme"
```

### Task 2: Foundation text, surface, and button components

**Files:**
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookText.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSurface.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookButton.kt`
- Create: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookButtonTest.kt`

**Interfaces:**
- Consumes: Task 1 theme accessors and tokens.
- Produces: `TallybookText`, `TallybookSurface`, `TallybookButton`, and `TallybookButtonDefaults` for Home status cards and future forms.

- [ ] **Step 1: Write failing button behavior tests**

Use `runComposeUiTest` with real components. One test clicks an enabled button and observes the callback. A second asserts a disabled button is semantically disabled and does not invoke the callback. The production mutations caught are missing callback wiring and an ignored `enabled` state.

```kotlin
@OptIn(ExperimentalTestApi::class)
@Test
fun enabledButtonInvokesItsAction() = runComposeUiTest {
  var clicks = 0
  setContent {
    TallybookTheme {
      TallybookButton(onClick = { clicks += 1 }) { TallybookText("Continue") }
    }
  }

  onNodeWithText("Continue").performClick()
  runOnIdle { assertEquals(1, clicks) }
}
```

- [ ] **Step 2: Run the focused iOS test and verify RED**

Run `:shared:iosSimulatorArm64Test` filtered to `TallybookButtonTest` through the wrapper. Expected: compilation fails because the three components do not exist.

- [ ] **Step 3: Implement the three Foundation components**

`TallybookText` wraps `BasicText`, defaults to the local semantic content color and body text style, and applies the caller modifier at the root. `TallybookSurface` owns clipping, background, optional border, and content-color provisioning. `TallybookButton` uses Foundation `clickable` with `Role.Button`, a 48 dp tokenized minimum target, tokenized shape/padding, and a slot-based `RowScope` content API.

- [ ] **Step 4: Run the focused iOS test and verify GREEN**

Run the same focused wrapped test. Expected: both button behavior tests pass.

- [ ] **Step 5: Run all iOS common tests and quality checks**

Run wrapped `:shared:iosSimulatorArm64Test`, `ktfmtFormat`, and `detekt ktfmtCheck`.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/component
git commit -m "feat(ui): add Foundation design-system primitives"
```

### Task 3: Safe-area-aware screen frame

**Files:**
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/layout/TallybookScreenFrame.kt`
- Create: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/layout/TallybookScreenFrameTest.kt`

**Interfaces:**
- Consumes: `TallybookTheme.colors.canvas`, `TallybookTheme.dimensions.screenHorizontal`, `screenTopFloor`, and `screenBottomFloor`.
- Produces: `TallybookScreenFrame(modifier, content)` for the Home outer-shell increment.

- [ ] **Step 1: Write the failing layout contract test**

Render a tagged child in a fixed-size root with `TallybookScreenFrame`. Assert its top and left positions are at least the documented minimum screen tokens. The production mutation caught is a frame that forgets content insets and lets content touch system/UI edges.

- [ ] **Step 2: Run the focused iOS test and verify RED**

Run the filtered `TallybookScreenFrameTest` through the wrapper. Expected: compilation fails because the frame does not exist.

- [ ] **Step 3: Implement the frame**

Read `WindowInsets.safeDrawing.asPaddingValues()` and calculate start/end with the current layout direction. Apply horizontal safe insets plus the semantic screen inset, and use the maximum of each vertical safe inset and its floor token. The root uses the caller modifier first, then fills available size and paints the theme canvas. Expose one `BoxScope` content slot and no feature policy.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the same focused wrapped test. Expected: the child respects minimum top and horizontal positions.

- [ ] **Step 5: Run complete verification**

Run wrapped `:shared:iosSimulatorArm64Test`, `:shared:testAndroidHostTest`, `:androidApp:assembleDebug`, and `detekt ktfmtCheck`. Compile Android device tests so the existing JUnit UI suite remains source-compatible.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/layout shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/layout
git commit -m "feat(ui): add safe-area screen frame"
```

## Plan self-review

- The plan implements only the shared foundation required by the approved architecture; Home search, navigation, and create-action policy remain untouched.
- All public signatures use the same `TallybookTheme` token accessors introduced in Task 1.
- Observable behavior has red-green tests; token constants and human documentation are not tested as source text.
- Every new layout is caller-placeable and all static visual values originate in token files.
- No placeholder or deferred implementation step is present.
