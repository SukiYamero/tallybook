# UI Catalog and Core Controls Design

## Context

Tallybook now has a tokenized theme, screen frame, surfaces, buttons, typography, a redesigned Home
shell, and bottom navigation. The next visual increment needs reusable form controls for the existing
Add movement flow. Testing each control only inside a feature screen makes light/dark comparison,
large-text checks, and physical-device review unnecessarily slow.

This increment adds a shared UI catalog and the first controls required by the form. The catalog is a
development surface, not a product destination.

## Goals

- Render existing and new UI components together on Android and iOS.
- Review light, dark, normal-text, and enlarged-text states without changing production data.
- Add reusable icon-button, text-field, search-field, segmented-control, and selection-field APIs.
- Establish a centered loading-state contract for screen and container loading.
- Keep the catalog deterministic, stateless where possible, and independent from Koin, repositories,
  SQLDelight, and network services.
- Preserve the current production startup, Home, movement form, and navigation behavior.

## Non-goals

- No new product destination is exposed to users.
- No History, Search, or Profile feature is implemented.
- No database, network, sync, or ViewModel is introduced for the catalog.
- The existing Add movement form is not redesigned in this increment. It adopts these controls in the
  following increment after the component contracts are accepted.
- The catalog does not replace unit, Compose, accessibility, or physical-device tests.
- No separate demo application module is added.

## Why loading states still exist

Tallybook is a native KMP application, not a server-rendered interface. Compose draws the UI on the
device. Offline-first storage removes many network waits, but asynchronous boundaries remain:

- application startup initializes preferences and SQLDelight;
- local queries and future migrations may take measurable time;
- future Drive or network synchronization runs asynchronously;
- saving and retrying operations can be in progress;
- errors can transition back into loading while data is re-read.

A loading state therefore represents pending local or remote work, not server-side rendering.

Every loading component centers its indicator within the region it owns. Full-screen loading receives
the full available screen; card or section loading receives a bounded container. A loading indicator
must never depend on a parent layout's default top-start alignment. It exposes a localized progress
description and retains enough minimum height to remain visually centered with enlarged system text.

## Development entry architecture

The catalog is a real Navigation 3 destination so it exercises the same theme and window behavior as
the application, but release builds cannot enter it.

1. Add `UiCatalog` to the sealed `Destination` hierarchy and register its entry in
   `TallybookNavHost`.
2. Add an `AppLaunchMode` value with `Standard` and `UiCatalog` variants. `App` maps this value to the
   appropriate start destination after normal startup succeeds.
3. Android reads an explicit `ui_catalog` intent extra only when `BuildConfig.DEBUG` is true. This
   allows a physical-device launch through `adb` without placing a development button in Home.
4. iOS reads a `--ui-catalog` process argument only inside `#if DEBUG` and passes the selected mode to
   `MainViewController`.
5. Release entry points always use `Standard`, even if an external caller supplies the development
   argument.

The catalog code remains in the shared binary, but it is unreachable from release UI and release
entry points. This avoids a second app module and keeps Android/iOS rendering identical.

## Catalog screen

`UiCatalogScreen` lives in `core.ui.catalog` and uses `TallybookScreenFrame`. It is a pure composable
with local preview state only. A single `LazyColumn` groups samples into these sections:

1. Theme colors and typography.
2. Buttons and icon buttons.
3. Text and search fields.
4. Segmented and selection controls.
5. Loading, empty, and error states.
6. Category avatars and chips.
7. Balance, movement-row, and bottom-navigation samples.

The top controls switch between system, light, and dark appearance and between normal and enlarged
font scale. They affect only the catalog subtree and are not persisted. Samples use fixed values and
dates so screenshots remain deterministic.

The catalog must remain usable in portrait. Landscape is a functional compatibility check: the list
must scroll, controls must remain reachable, and no content may be clipped by safe areas.

## Component contracts

All components live in `core.ui.component`, accept a caller `Modifier`, use theme tokens, and expose
user-visible state through semantics.

### `TallybookIconButton`

- Required inputs: `onClick`, icon, and localized content description.
- Optional inputs: `enabled`, visual style, and modifier.
- Owns its shape and icon spacing; the caller owns placement.
- Maintains at least a 48 dp interactive target.

### `TallybookTextField`

- Required inputs: value and `onValueChange`.
- Supports label, placeholder, supporting text, error, enabled, read-only, leading icon, and trailing
  icon states only where the movement form needs them.
- Uses `BasicTextField` as the behavior primitive so Material appearance does not leak into the new
  component.
- Error and supporting text are mutually exclusive visual states, not independent flags.

### `TallybookSearchField`

- Builds on the text-field behavior without copying editing logic.
- Owns the search icon and optional clear action.
- Exposes query, `onQueryChange`, placeholder, enabled, and `onClear`.
- The existing non-interactive Home search surface remains unchanged until the Search destination is
  implemented.

### `TallybookSegmentedControl`

- Accepts a stable list of typed options, the selected value, and `onSelect`.
- Models selection as one value rather than Boolean flags per segment.
- Each segment has selected semantics and a minimum 48 dp target.
- The first production consumer will be the Gasto/Ingreso selector.

### `TallybookSelectionField`

- Represents a value chosen in another surface, such as category or date.
- Supports label, current value, placeholder, enabled, supporting/error state, optional leading icon,
  and a required click callback.
- Is read-only and button-like; it does not imitate an editable text field.

### `TallybookLoadingState`

- Fills the region supplied by its parent and centers the progress indicator in both axes.
- Supports a localized accessibility description and optional supporting copy.
- Uses semantic size and minimum-height tokens for screen and contained variants.
- Does not own data, retry behavior, or timers.

## Tokens

New visual values are added to the existing semantic token groups before component implementation.
Expected roles include field height, icon-button size, field icon size, segmented-control height,
selection indicator, loading-container minimum height, and field border/focus/error colors.

No component introduces raw `dp`, `sp`, brand colors, animation durations, or alpha values. A value not
covered by the current token model becomes a named token instead of a one-off literal.

## State and data flow

Catalog state is local Compose state because it exists only to manipulate samples. Production
components remain stateless: values flow down and callbacks flow up. The catalog never instantiates a
ViewModel or calls application repositories.

The future movement-form migration continues to use its existing `AddMovimientoUiState` and actions.
Adopting a new component must not create a second source of truth.

## Accessibility

- Icon-only controls always receive localized content descriptions.
- Disabled samples publish disabled semantics and do not invoke callbacks.
- Selected segments publish selection semantics; unavailable navigation items remain non-clickable
  and do not publish false selection state.
- Text fields expose error semantics and readable supporting text.
- Loading states expose progress semantics and are centered at normal and enlarged font scales.
- Touch targets remain at least 48 dp even when the visible icon is smaller.

## Testing

Implementation follows RED-GREEN-REFACTOR.

- Common tests protect new resource keys and pure option/state mapping.
- Android device Compose tests verify content descriptions, click callbacks, disabled behavior,
  selected/error semantics, centered loading bounds, and enlarged text.
- Navigation tests verify Standard launches Home and development mode launches `UiCatalog`.
- Release-entry tests or compile-time boundaries verify development flags cannot select the catalog.
- `detekt`, `ktfmtCheck`, shared host tests, all Android device tests, Android assembly, and iOS arm64
  compilation run before completion.
- Physical Android review covers portrait light/dark and landscape compatibility. Physical iPhone
  review covers portrait and bottom safe area when the device is connected.

## Delivery order

1. Add the gated catalog destination and render the current components.
2. Add centered loading and catalog coverage.
3. Add the five form-control components with their state matrices.
4. Validate the catalog on physical devices and document any platform-specific difference.
5. Migrate Add movement in a separate, behavior-preserving increment.

This order makes the catalog useful immediately and prevents the form redesign from becoming the test
bed for unfinished component APIs.
