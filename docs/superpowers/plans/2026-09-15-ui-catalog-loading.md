# UI Catalog and Centered Loading Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox
> (`- [ ]`) syntax for tracking.

**Goal:** Add a development-gated shared UI catalog that renders the current component system and
replace legacy loading layouts with a reusable centered Tallybook loading component.

**Architecture:** A shared `AppLaunchMode` resolves to either the existing Home destination or a new
`UiCatalog` Navigation 3 destination. Android and iOS pass explicit development flags into the shared
resolver, so release entry points cannot honor catalog launch requests. The catalog is a pure Compose
screen with local theme/font controls and deterministic fixtures; loading is a stateless component
that fills and centers within the region supplied by its parent.

**Tech Stack:** Kotlin 2.4.10, Compose Multiplatform 1.11.1, Navigation 3, Compose Resources, Lucide
icons, Kotlin Serialization, Android instrumented Compose tests, Kotlin common tests.

**Spec:** `docs/superpowers/specs/2026-09-15-ui-catalog-core-controls-design.md`

## Global Constraints

- Production code, identifiers, comments, test names, and documentation remain in English.
- The catalog uses no Koin, ViewModel, repository, SQLDelight, network, or persisted state.
- Release launch paths always resolve to `AppLaunchMode.Standard`.
- Catalog fixtures use fixed values and dates.
- Portrait is the visual acceptance reference; landscape must remain scrollable and unclipped.
- Every loader centers horizontally and vertically inside its owned region.
- No raw `dp`, `sp`, brand colors, alpha values, or animation durations appear in component code.
- All Gradle execution goes through the installed `gradle-run` wrapper and one workflow ID.
- Each task follows RED-GREEN-REFACTOR and ends with a focused commit.
- Final review uses a read-only `gpt-5.6-terra` subagent at `high` reasoning before the final commit.

---

## File structure

- `shared/src/commonMain/kotlin/com/kurobello/tallybook/AppLaunchMode.kt` owns the cross-platform launch
  contract and release gate.
- `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogScreen.kt` owns local
  catalog controls, deterministic samples, and section layout.
- `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogFixtures.kt` owns fixed
  category, movement, and balance sample values.
- `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookLoadingState.kt`
  owns the progress indicator and centered loading layout.
- Existing platform entry files only translate launch arguments into `AppLaunchMode`; they do not own
  catalog UI.

---

### Task 1: Development-gated catalog destination

**Files:**

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/AppLaunchMode.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/Destinations.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/navigation/TallybookNavHost.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogScreen.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/AppLaunchModeTest.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/navigation/DestinationSerializationTest.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogScreenTest.kt`

**Interfaces:**

- Produces: `enum class AppLaunchMode { Standard, UiCatalog }`.
- Produces: `fun resolveAppLaunchMode(developmentToolsEnabled: Boolean, uiCatalogRequested: Boolean): AppLaunchMode`.
- Produces: `@Serializable data object UiCatalog : Destination`.
- Produces: `@Composable internal fun UiCatalogRoute()` and `UiCatalogScreen(...)`.
- Changes: `fun App(launchMode: AppLaunchMode = AppLaunchMode.Standard)`.

- [ ] **Step 1: Write failing launch-mode and serialization tests**

```kotlin
class AppLaunchModeTest {
  @Test
  fun releaseModeIgnoresCatalogRequest() {
    assertEquals(
        AppLaunchMode.Standard,
        resolveAppLaunchMode(developmentToolsEnabled = false, uiCatalogRequested = true),
    )
  }

  @Test
  fun developmentModeHonorsCatalogRequest() {
    assertEquals(
        AppLaunchMode.UiCatalog,
        resolveAppLaunchMode(developmentToolsEnabled = true, uiCatalogRequested = true),
    )
  }
}
```

Extend `DestinationSerializationTest` so the expected serializer list contains both `Home.serializer()`
and `UiCatalog.serializer()`, and round-trip a stack initialized with `UiCatalog`.

- [ ] **Step 2: Run the common tests and verify RED**

Underlying Gradle task:

```text
:shared:testAndroidHostTest --tests com.kurobello.tallybook.AppLaunchModeTest --tests com.kurobello.tallybook.core.ui.navigation.DestinationSerializationTest
```

Expected: compilation fails because `AppLaunchMode` and `UiCatalog` do not exist.

- [ ] **Step 3: Implement the minimal launch contract and destination**

```kotlin
enum class AppLaunchMode {
  Standard,
  UiCatalog,
}

fun resolveAppLaunchMode(
    developmentToolsEnabled: Boolean,
    uiCatalogRequested: Boolean,
): AppLaunchMode =
    if (developmentToolsEnabled && uiCatalogRequested) {
      AppLaunchMode.UiCatalog
    } else {
      AppLaunchMode.Standard
    }
```

Map `Standard` to `Home` and `UiCatalog` to the catalog destination only after startup reaches Ready.
Register both destinations in `TallybookNavHost`. Add a temporary catalog screen containing a
localized title in `TallybookScreenFrame`; later tasks replace its body without changing the route.

- [ ] **Step 4: Write the failing catalog route UI test**

```kotlin
@Test
fun catalogRouteDisplaysItsLocalizedTitle() {
  val title = localizedString(Res.string.ui_catalog_title)

  composeRule.setContent { TallybookTheme { TallybookNavHost(startDestination = UiCatalog) } }

  composeRule.onNodeWithText(title).assertIsDisplayed()
}
```

Add `ui_catalog_title` to the resource contract before adding it to the four localization catalogs.
Verify the resource test and UI test fail for the missing catalog implementation, then add the copy:
`UI catalog`, `Catálogo de UI`, and `Catálogo de UI` for English, Spanish, and Portuguese.

- [ ] **Step 5: Run launch, serialization, resource, and catalog UI tests and verify GREEN**

Underlying Gradle tasks:

```text
:shared:testAndroidHostTest
:shared:connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=com.kurobello.tallybook.core.ui.catalog.UiCatalogScreenTest
```

Expected: the focused suites pass and the production default still resolves to Home.

- [ ] **Step 6: Commit the catalog route**

```text
feat(ui): add development catalog route
```

---

### Task 2: Platform development launch arguments

**Files:**

- Modify: `androidApp/src/main/kotlin/com/kurobello/tallybook/MainActivity.kt`
- Modify: `shared/src/iosMain/kotlin/com/kurobello/tallybook/MainViewController.kt`
- Modify: `iosApp/iosApp/ContentView.swift`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/AppLaunchModeTest.kt`

**Interfaces:**

- Android input: Boolean intent extra named `ui_catalog`.
- iOS input: process argument named `--ui-catalog`.
- Changes: `MainViewController(developmentToolsEnabled: Boolean, uiCatalogRequested: Boolean)`.

- [ ] **Step 1: Extend the resolver test matrix**

```kotlin
@Test
fun launchModeMatrixRequiresBothDevelopmentAndRequest() {
  val cases =
      listOf(
          Triple(false, false, AppLaunchMode.Standard),
          Triple(false, true, AppLaunchMode.Standard),
          Triple(true, false, AppLaunchMode.Standard),
          Triple(true, true, AppLaunchMode.UiCatalog),
      )

  cases.forEach { (development, requested, expected) ->
    assertEquals(expected, resolveAppLaunchMode(development, requested))
  }
}
```

- [ ] **Step 2: Run the matrix and verify it is GREEN before platform wiring**

This characterizes the release gate implemented in Task 1. Underlying Gradle task:

```text
:shared:testAndroidHostTest --tests com.kurobello.tallybook.AppLaunchModeTest
```

- [ ] **Step 3: Wire Android without adding Home UI**

```kotlin
private const val UI_CATALOG_EXTRA = "ui_catalog"

val launchMode =
    resolveAppLaunchMode(
        developmentToolsEnabled = BuildConfig.DEBUG,
        uiCatalogRequested = intent.getBooleanExtra(UI_CATALOG_EXTRA, false),
    )
setContent { App(launchMode = launchMode) }
```

The developer launch command is:

```text
adb shell am start -n com.kurobello.tallybook/.MainActivity --ez ui_catalog true
```

- [ ] **Step 4: Wire iOS through the existing Swift shell**

`ContentView.swift` derives two values:

```swift
#if DEBUG
private let developmentToolsEnabled = true
#else
private let developmentToolsEnabled = false
#endif

private let uiCatalogRequested =
    ProcessInfo.processInfo.arguments.contains("--ui-catalog")
```

Pass both values into the Kotlin `MainViewController`. Do not edit `project.pbxproj` or signing files.

- [ ] **Step 5: Compile both platform entry points**

Underlying Gradle tasks:

```text
:androidApp:assembleDebug
:androidApp:assembleRelease
:shared:compileKotlinIosArm64
```

Also run `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphoneos build` when signing
configuration on the host permits it. A signing failure is reported separately from shared compilation.

- [ ] **Step 6: Launch both Android modes on the physical tablet**

Launch without the extra and verify Home. Relaunch with `--ez ui_catalog true` and verify the catalog
title. Capture both screen hierarchies and confirm the release gate remains covered by the resolver
matrix and release assembly.

- [ ] **Step 7: Commit platform launch gating**

```text
feat(ui): gate component catalog to development
```

---

### Task 3: Deterministic catalog of current components

**Files:**

- Replace: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogFixtures.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-en/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-es-rAR/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-pt-rBR/strings.xml`
- Modify: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/ui/resources/Wave2ResourcesTest.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogScreenTest.kt`

**Interfaces:**

- Produces: `enum class CatalogThemeMode { System, Light, Dark }`.
- Produces: `enum class CatalogFontScale(val multiplier: Float) { Normal(1f), Large(1.5f), ExtraLarge(2f) }`.
- `UiCatalogRoute` owns these two local values and supplies the selected theme/density to
  `UiCatalogScreen`.

- [ ] **Step 1: Write failing catalog-section tests**

```kotlin
@Test
fun catalogDisplaysCurrentComponentFamilies() {
  composeRule.setContent { UiCatalogRoute() }

  listOf(
          Res.string.ui_catalog_foundations,
          Res.string.ui_catalog_actions,
          Res.string.ui_catalog_data_display,
          Res.string.ui_catalog_navigation,
      )
      .forEach { resource ->
        composeRule.onNodeWithText(localizedString(resource)).performScrollTo().assertIsDisplayed()
      }
}

@Test
fun catalogThemeAndFontControlsUpdateSamplesWithoutPersistence() {
  composeRule.setContent { UiCatalogRoute() }

  composeRule.onNodeWithText(localizedString(Res.string.ui_catalog_dark)).performClick()
  composeRule.onNodeWithText(localizedString(Res.string.ui_catalog_text_large)).performClick()

  composeRule
      .onNodeWithText(localizedString(Res.string.ui_catalog_sample_text))
      .assertIsDisplayed()
}
```

The production mutations these tests catch are a missing component family and disconnected local
catalog controls.

- [ ] **Step 2: Run the catalog tests and verify RED**

Expected: missing resource accessors and missing section nodes.

- [ ] **Step 3: Add localized catalog copy and deterministic fixtures**

Add localized names for Foundations, Actions, Data display, Navigation, System, Light, Dark, Normal,
Large, Extra large, and Sample text. Increment the resource contract by the exact number of new keys.

Fixtures use `LocalDate(2026, 9, 15)` and `Instant.parse("2026-09-15T00:00:00Z")`; they never use the
system clock.

- [ ] **Step 4: Implement the catalog sections**

Use one `LazyColumn` with stable item keys and section helpers. Render `TallybookText` styles,
`TallybookSurface`, enabled/disabled `TallybookButton`, `CategoryAvatar`, `CategoryChip`, `BalanceCard`,
`MovimientoRow`, and `TallybookBottomNavigation`. The navigation sample occupies the real bottom-bar
slot; catalog content reserves `bottomNavigationContentHeight`.

Theme mode resolves to `isSystemInDarkTheme()` for System. Font scale is applied only to the catalog
subtree with a copied `LocalDensity`.

- [ ] **Step 5: Run catalog tests and verify GREEN at normal and enlarged text**

Underlying task:

```text
:shared:connectedAndroidDeviceTest -Pandroid.testInstrumentationRunnerArguments.class=com.kurobello.tallybook.core.ui.catalog.UiCatalogScreenTest
```

- [ ] **Step 6: Commit the current-component catalog**

```text
feat(ui): catalog existing components
```

---

### Task 4: Tokenized centered loading component

**Files:**

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookLoadingState.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Dimensions.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/theme/Motion.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/App.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/home/HomeOuterShell.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogScreen.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookLoadingStateTest.kt`
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/home/HomeScreenTest.kt`

**Interfaces:**

- Produces: `TallybookProgressIndicator(contentDescription, modifier)`.
- Produces: `TallybookLoadingState(contentDescription, modifier, supportingText)`.
- Adds semantic tokens for indicator size, stroke width, contained loading height, and rotation duration.

- [ ] **Step 1: Write failing centered-layout and semantics tests**

```kotlin
@Test
fun loadingIndicatorIsCenteredInsideItsOwnedRegion() {
  composeRule.mainClock.autoAdvance = false
  composeRule.setContent {
    TallybookTheme {
      Box(Modifier.size(width = 320.dp, height = 240.dp).testTag("loading_region")) {
        TallybookLoadingState(
            contentDescription = "Loading catalog",
            modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }

  val region = composeRule.onNodeWithTag("loading_region").fetchSemanticsNode().boundsInRoot
  val indicator =
      composeRule.onNodeWithContentDescription("Loading catalog").fetchSemanticsNode().boundsInRoot

  assertEquals(region.center.x, indicator.center.x, absoluteTolerance = 1f)
  assertEquals(region.center.y, indicator.center.y, absoluteTolerance = 1f)
}
```

Add a second test with `fontScale = 2f` and supporting text; set `autoAdvance = false` before
`setContent`, then assert the indicator and copy are displayed and remain within the owned region.

- [ ] **Step 2: Run the loading tests and verify RED**

Expected: compilation fails because `TallybookLoadingState` does not exist.

- [ ] **Step 3: Add loading tokens**

Add named dimensions for indicator size, stroke width, and contained minimum height. Add
`loadingRotationMillis` to `TallybookMotion`. Do not add literals inside the composables.

- [ ] **Step 4: Implement the progress indicator and centered state**

`TallybookProgressIndicator` uses `Canvas`, a round `Stroke`, the theme brand color, and an infinite
rotation driven by the motion token. Its semantics use `ProgressBarRangeInfo.Indeterminate` and the
supplied localized content description.

`TallybookLoadingState` uses:

```kotlin
Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
) {
  TallybookProgressIndicator(contentDescription = contentDescription)
  supportingText?.let { TallybookText(text = it, modifier = Modifier.padding(top = spacing)) }
}
```

- [ ] **Step 5: Replace legacy startup and Home loading layouts**

`StartupLoading` calls `TallybookLoadingState` with `Modifier.fillMaxSize()`. `HomeLoading` keeps its
existing card surface but gives the loading state a tokenized minimum height and full width. Remove
legacy `CircularProgressIndicator` imports.

- [ ] **Step 6: Add the loading family to the catalog and verify GREEN**

Run the loading component test and `HomeScreenTest`. The existing localized loading description and
Home callbacks must remain unchanged.

- [ ] **Step 7: Commit centered loading**

```text
feat(ui): add centered loading state
```

---

### Task 5: Verification, device evidence, and review

**Files:**

- Modify only files required by verified reviewer findings.

**Interfaces:** None.

- [ ] **Step 1: Run formatting and repository quality checks**

Underlying Gradle task:

```text
ktfmtFormat
detekt ktfmtCheck
```

- [ ] **Step 2: Run the complete automated matrix**

Underlying Gradle tasks:

```text
:shared:testAndroidHostTest
:shared:connectedAndroidDeviceTest
:androidApp:assembleDebug
:androidApp:assembleRelease
:shared:compileKotlinIosArm64
```

- [ ] **Step 3: Review the catalog on the physical Android tablet**

Verify the normal Home launch and catalog launch separately. In the catalog, capture portrait light,
portrait dark, enlarged text, and landscape. Confirm loaders are centered and every section remains
scrollable. Restore portrait and scheduled theme afterward.

- [ ] **Step 4: Review on physical iPhone when connected**

Launch with `--ui-catalog`, verify top/bottom safe areas, theme controls, scrolling, and centered
loading. If the device is offline, report the visual check as pending and retain the successful iOS
arm64 compile as compilation evidence only; do not reinstall a simulator.

- [ ] **Step 5: Request the mandated independent review**

Dispatch one read-only `gpt-5.6-terra` reviewer at `high` reasoning. Ask it to inspect the full diff for
release gating, KMP boundaries, safe-area ownership, component API cleanliness, accessibility,
recomposition cost, deterministic fixtures, and tests. Fix every Critical and Important finding with
a new RED-GREEN cycle.

- [ ] **Step 6: Repeat the complete matrix after reviewer fixes**

Do not reuse pre-review evidence if Kotlin, resources, Gradle, or platform entry code changed.

- [ ] **Step 7: Commit verified reviewer fixes if any**

```text
fix(ui): address catalog review findings
```

- [ ] **Step 8: Finish the Gradle workflow and report bounded answers**

Finish only after the last validation passes or after reporting a concrete blocker. Confirm
`git diff --check` and a clean worktree before declaring the increment complete.
