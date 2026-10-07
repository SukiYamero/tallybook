# Wave 1 / Task 1 — reusable snackbar

## Objective and boundary

Create domain-neutral snackbar presentation and its usable-window controller, with a standalone
catalog example and meaningful tests. Read the [feature contract](../../README.md) and canonical
UI/token/wording owners linked there. This task can run in parallel with persistence.
Required skills: `android-dev`, `compose-component-design`, `compose-state-and-effects`, relevant
Compose UI testing guidance and `android-testing` for Kotlin changes.

The component never imports Movimiento, calls a repository, keeps an Undo snapshot or decides
whether an action is accepted. It receives localized content, identity, duration and callbacks.

## Ownership and proposed files

Paths below are relative to `shared/src/` unless prefixed with `docs/`.

- Create `commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSnackbar.kt`.
- Create `commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSnackbarHost.kt`.
- Create `commonMain/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSnackbarMessage.kt`.
- Create a focused timer/state helper only if needed for deterministic pause/reveal testing; keep
  it alongside the host, not in a global event bus.
- Modify `core/ui/theme/Motion.kt` under the same commonMain Kotlin root for named duration tokens;
  reuse existing spacing/radii/colors/type. Add a missing semantic color only with a concrete need.
- Create `core/ui/catalog/UiCatalogSnackbar.kt`; modify `UiCatalogScreen.kt` for its demo.
- Create `commonTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSnackbarTimingTest.kt`.
- Create `androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/component/TallybookSnackbarTest.kt`.
- Update `docs/ui/components.md` only for the implemented reusable API. Catalog-specific copy, if
  necessary, stays in the existing locale resources with `ui_catalog_snackbar_` keys.

Do not edit navigation/shell, movement form/ViewModels, repository/SQL, DI, production movement copy
or domain documentation. Wave 2 owns them. You are not alone in the codebase: accommodate other
workers' edits and never revert their work. Orchestrator owns shared commands/index/commits.

## Presentation and callback contract

Proposed shared types, all internal to the module:

```kotlin
enum class TallybookSnackbarDuration { Short, Long, Indefinite }
enum class TallybookSnackbarDismissReason { Timeout, Close }
data class TallybookSnackbarMessage(
    val token: Long,
    val revision: Int,
    val message: String,
    val duration: TallybookSnackbarDuration,
    val actionLabel: String? = null,
    val isBusy: Boolean = false,
)
```

`TallybookSnackbar(message, modifier, actionContent, dismissContent)` is plain previewable rendering:
message text is a semantic string; optional action/dismiss regions are nullable composable slots.
Apply caller modifier to the root. Component owns surface/spacing, wrapping and polite live-region
semantics; callers supply existing styled buttons, localized accessibility labels and busy content.

`TallybookSnackbarHost(message: TallybookSnackbarMessage?, isActive: Boolean, closeLabel: String,
onAction: (Long, Int) -> Unit, onDismiss: (Long, Int, TallybookSnackbarDismissReason) -> Unit,
modifier: Modifier = Modifier)` renders one item and manages its visual timeout. Placement belongs
to the caller. No hidden singleton, queue, MutableStateFlow bus or database access.

Callbacks carry token and revision; the caller rejects either stale dimension. The host renders
standard text-action/Close controls using the caller's localized closeLabel. A null actionLabel removes that region;
busy replaces action interaction with progress and disables Close. Disabled actions expose no click
semantics. Tokens/revisions belong to the caller; message changes do not mutate domain state.

## Timing, layout and accessibility

- Short/Long use 4,000/10,000 ms tokens. Indefinite/busy does not schedule expiry. Adapt finite timeout
  through LocalAccessibilityManager with text and actual controls correctly flagged, never shortening
  the baseline. Handle Long.MAX_VALUE as no auto-timeout; avoid arithmetic overflow.
- `isActive=false` hides the snackbar from pixels, accessibility and pointer input and cancels its
  timer. On each false→true usable reveal, start a full duration. The production caller combines
  foreground state and no active/closing modal. A background event alone never reports dismissal.
- Key timing to token/revision/reveal, not the full render object or every recomposition. Busy
  cancels expiry. Leaving busy for retry uses a new revision and full Long duration.
- Invoke a timer dismissal once for that generation. A captured old timeout/action carries its old
  token/revision. Domain owner can reject it; the host never assumes that callback deleted a record.
- Use theme surface/text tokens, rounded surface and existing motion. Message wraps; action moves
  below it when width/font size requires. Do not truncate the label or amount-containing text.
- Expose a polite message announcement, action and Close as separate reachable controls, minimum
  48dp hit areas. Do not steal keyboard/focus or continuously announce duration. Controls use
  localized labels, including a caller/resource supplied Close accessibility name.
- Component does not add safe insets. The shell caller places it above measured navigation clearance
  and available IME; catalog caller supplies its own clearance. Never double-count safe bottom.

## Test-first increments and exit condition

- [x] Write failing deterministic timing tests: null/inactive/busy/indefinite do not expire; reveal
      starts its full duration; repeated composition inputs do not restart; hide/background cancels;
      new token/revision invalidates an old timer; extended/indefinite accessibility timeout works.
- [x] Implement the smallest helper/host needed; use coroutine test scheduling rather than sleeps.
- [x] Add device component tests for message/action/Close semantics, one callback per click, busy
      disabling, large text/long action wrapping and caller modifier root placement.
- [x] Add a catalog fixture that can show plain, actionable and busy messages without financial data.
- [ ] Run only relevant host/device-fixture compilation, narrow Android build and mandatory style
      checks through the orchestrator. Physical verification follows the repository's device policy.
- [ ] Produce the independent `feat(ui): add reusable snackbar` diff/commit boundary; no movement
      integration is required for the catalog to compile or the component tests to execute.

Consult the versioned accessibility source linked in the overview before implementing the timeout;
confirm the actual resolved platform behavior at the gate. Record blocked device checks accurately.

## Implementation evidence

- The composition-scoped `TallybookSnackbarTimer` keys its usable window by token/revision. Render
  changes do not restart it; inactive/busy/disposal cancels without dismissal. Host remount is a new
  usable reveal with a full window. The caller still owns acceptance of action/dismiss identities.
- RED: orchestrator ran `:shared:testAndroidHostTest --tests
  'com.kurobello.tallybook.core.ui.component.TallybookSnackbarTimingTest'`; nine tests executed and six
  intended timing/accessibility assertions failed against the compile-only skeleton.
- Resolved by the orchestrator: common Compose UI 1.11.1 and Android Compose UI 1.11.2. The
  [official Android 1.11.2 source archive](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.11.2/ui-android-1.11.2-sources.jar),
  `androidMain/androidx/compose/ui/platform/AndroidAccessibilityManager.android.kt`, calls the system
  recommendation with content flags and maps `Int.MAX_VALUE` to `Long.MAX_VALUE`. The host reports
  text, icon and controls, clamps to the baseline, and schedules no timeout for `Long.MAX_VALUE`.
- In Compose UI 1.11.1,
  [RootNodeOwner](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/ui/ui/src/skikoMain/kotlin/androidx/compose/ui/node/RootNodeOwner.skiko.kt)
  supplies
  [DefaultAccessibilityManager](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/ui/ui/src/skikoMain/kotlin/androidx/compose/ui/platform/DefaultAccessibilityManager.skiko.kt),
  which returns the baseline unchanged;
  [CompositionLocals](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/platform/CompositionLocals.kt)
  provides that owner manager. Do not claim VoiceOver-adapted timeouts. An insufficient usable window
  on a physical iPhone requires a targeted platform adaptation; current iPhone usability remains
  unconfirmed.
- GREEN: the orchestrator reran the focused timing class against the implementation; all nine tests
  passed with zero errors/skips. Shared formatting was applied by the orchestrator. Device-fixture
  compilation, Android/Native builds and mandatory style checks are in progress. Device assertions
  and physical action/announcement/wrapping remain unexecuted until that target evidence is supplied.
