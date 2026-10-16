# Task 5 — Categories screen, navigation, Profile row, Search request

Depends on task3. Parallel with task4; shared file: `di/ViewModelModule.kt` (own entry only).

## Objective

A secondary Categories destination from Profile: search, sections, counts, edit/create, hide/show,
delete, and tap-to-Search.

## Files

- Navigation (`core/ui/navigation/`): `Destination.Categories`; `PrimaryNavigationState` replaces
  `catalogOpen: Boolean` with `secondary: Destination?` restricted to `{UiCatalog, Categories}`
  (`openSecondary`, `back()` closes it first); `Categories` only opens while `selected == Profile`.
  Update `initialPrimaryNavigation`, `primaryContentKey`, entry provider, bottom-bar visibility and
  every `catalogOpen` reader. `DestinationSerializationTest`/`PrimaryNavigationTest` cover the new
  state, including saved-state round trip with Categories open.
- Search request: `SearchActions.onShowCategory(id)` resets the query, sets
  `SearchFilters.initial(today)` with only that category and closes any filter session. The nav host
  passes a one-shot `SearchCategoryRequest(token, id)` to `SearchRoute`, delivered like
  `focusRequest` (applied once when Search is active, then acknowledged).
- `feature/profile/ui/ProfileScreen.kt` / `ProfileRoute.kt`: a "Categories" navigation row (chevron,
  48dp) in the Data section; `onOpenCategories` from the nav host, gated by `modals.canNavigate`.
- `feature/categories/viewmodel/CategoriesViewModel.kt` (new): `StateFlow<CategoriesUiState>`
  from `observeAll()` + `observeMovimientoCounts(primary currency from preferences)`; query
  (`normalizeSearchText`, name match); `custom: List<CategoryRow>`, `predefined: List<List<CategoryRow>>`
  via `categoryFamilies`; editor `CategoryDraftState?` (create/edit), delete confirmation, and
  `Channel` effects (snackbar text, open Search). Loading and failure states with Retry.
- `feature/categories/ui/CategoriesRoute.kt` / `CategoriesScreen.kt` (new): top bar with Back,
  search field, "New category" action, sections; each row = `CategoryAvatar`, name, description
  (custom), count, "Hidden" marker; row tap → Search. Trailing action: custom → edit (opens the Full
  form in `TallybookModalBottomSheet` with Hide/Show and Delete); predefined → Hide/Show (absent for
  Otros). Delete: unused → `TallybookModalDialog` confirm → delete; in use → "Hide instead" with the
  count. Empty search shows a message (no create action here; "New category" prefills the query).

## Edge cases

Counts update live after a movimiento edit; editing a category to a duplicate name; hiding the
category currently filtered in Search; Back with the editor open closes the editor first; navigation
locked while a modal is open; long names/descriptions wrap without overflow; currency change refreshes
counts.

## Tests

- commonTest: `CategoriesViewModelTest` (sections, search, counts default 0, create/edit/duplicate,
  hide/show, delete unused/in use, open-Search effect); `PrimaryNavigationTest`; `SearchViewModelTest`
  for `onShowCategory`.
- androidHostTest: Koin graph test for the new ViewModel (pattern of `ProfileViewModelGraphHostTest`).
- androidDeviceTest: Profile → Categories → Back (compile-only).

## Exit

`testAndroidHostTest`, `detekt`, `ktfmtCheck`, iOS compile, `compileAndroidDeviceTest`,
`assembleDebug`.
