# Task 6 — Catalog state matrices report

## Implementation

- Added `UiCatalogControls.kt` as the catalog-only owner of all control sample values and matrices.
  `UiCatalogScreen` now only composes the existing sections plus `catalogControlMatrices()` inside
  the existing single keyed `LazyColumn`.
- Added representative icon-button, text-field, search-field, segmented-control, selection-field,
  and contained-loading matrices. The matrices cover default, populated, disabled, selected,
  supporting, and error states wherever each component API supports them.
- Editable text, query, segment, and selection samples use deterministic local `rememberSaveable`
  values. Disabled samples are fixed. No ViewModel, Koin, repository, SQLDelight, network, or other
  persistence path was introduced.
- Preserved the theme/font controls, existing foundations/actions/data samples, real bottom bar with
  `selectedDestination = null`, and the catalog's single scroll container.
- Added 24 catalog strings to default, English, Argentinian Spanish, and Brazilian Portuguese. Each
  resource catalog now contains exactly 146 keys; `Wave2ResourcesTest` asserts that exact Wave 2
  contract.

## State matrix

| Component | Catalog states |
|---|---|
| Icon button | default, disabled |
| Text field | default editable, populated editable, disabled, supporting editable, error editable |
| Search field | default editable, populated/clearable, disabled |
| Segmented control | selected/interactive, disabled |
| Selection field | default, populated with leading icon, disabled, supporting, error |
| Loading | contained, centered, localized progress and supporting copy |

## TDD and verification

- RED run `0183`: the new catalog/resource tests failed at device-test compilation because the 24
  requested generated resources did not yet exist. After adding the resource contract, a follow-up
  compile exposed and corrected one test-only `category` import collision before implementation.
- GREEN compile run `0186`: `:shared:compileAndroidDeviceTest` passed with the production matrices
  and interaction tests.
- Physical matrix run `0188` attempted `TallybookIconButtonTest`, `TallybookTextFieldTest`,
  `TallybookSearchFieldTest`, `TallybookSegmentedControlTest`, `TallybookSelectionFieldTest`,
  `TallybookLoadingStateTest`, and `UiCatalogScreenTest` on `NP05J - 16`. The device remained
  keyguard-locked after the permitted wake event; all UI tests failed before composition with
  `No compose hierarchies found in the app`. Read-only window state reported
  `isKeyguardShowing=true`. No dismiss-keyguard, biometric, unlock swipe, or security bypass was used.
- Final aggregate run `0195` passed `:shared:testAndroidHostTest`,
  `:shared:compileAndroidDeviceTest`, `detekt`, `ktfmtCheck`, `:androidApp:assembleDebug`, and
  `:shared:compileKotlinIosSimulatorArm64` (96 tasks; 14 executed, 82 up-to-date).
- XML validation passed for all four resource catalogs, and `git diff --check` passed.

## Test behavior

- Catalog tests scroll by localized family headings, assert disabled icon semantics and error
  selection semantics, type into the controlled text sample, clear the populated search and observe
  the clear action disappear, change the selected typed segment, and compare the loading indicator
  center with its tagged owned region. The interaction assertions fail if catalog callbacks are
  disconnected from local state.

## Self-review and concern

- Reviewed the final diff for one keyed list, stable item keys, local-only ownership, deterministic
  initial values, token-only component/catalog visuals, localized visible/accessibility copy, no
  component API changes, no Add movement migration, and no false bottom-navigation selection.
- The only remaining concern is physical runtime coverage while the connected tablet is locked.
  Compilation, host/static, Android assembly, and iOS compilation are green.

## Commit plan

`feat(ui): catalog form controls`
