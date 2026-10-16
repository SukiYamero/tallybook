# Stage 2 / Task 1 — Tags management screen

## Objective

Give tags a dedicated screen from Profile, modeled on Categories: browse, search, create, rename,
delete, and open Search filtered by a tag.

## Context and skills

- Requires Stage 1 reviewed. Read `../../README.md`, `docs/features/custom-categories.md`,
  `docs/ui/architecture.md` (secondary destinations over Profile, sheets, snackbar policy).
- Skills: `android-skills:android-dev` (UiState + Actions + effects `Channel`),
  `android-skills:compose`, `chrisbanes-skills:compose-state-and-effects`, `android-skills:koin`,
  `android-skills:android-testing`, `superpowers:test-driven-development`.

## Files (owned by this task)

Paths relative to `shared/src/commonMain/kotlin/com/kurobello/tallybook/`.

- New `feature/tags/viewmodel/TagsUiState.kt`, `TagsActions.kt`, `TagsViewModel.kt`.
- New `feature/tags/ui/TagsRoute.kt`, `TagsScreen.kt`, `TagSheetHost.kt`.
- `core/ui/navigation/Destinations.kt` (`Tags`), `PrimaryNavigationState.kt` (`opensOver` Profile,
  same as `Categories`), `PrimaryNavigator.kt` (`openTags`), `TallybookNavHost.kt` (entry +
  `primaryContentKey`).
- `feature/profile/ui/ProfileScreen.kt`, `ProfileRoute.kt`: a Tags card below the Categories card.
- `di/ViewModelModule.kt`: only the `TagsViewModel` registration (the only Stage 2 editor of this
  file).
- Tests: new `commonTest/.../feature/tags/viewmodel/TagsViewModelTest.kt`, new
  `androidDeviceTest/.../feature/tags/ui/TagsScreenTest.kt`, update
  `core/ui/navigation/PrimaryNavigationTest.kt`, `DestinationSerializationTest.kt`,
  `CategoriesNavigationTest.kt` (or a sibling `TagsNavigationTest.kt`).

## Behavior

- Screen: top bar with title and a New action; search field; flat chip list sorted by name using
  `EtiquetaChip` with the movimiento count in the primary currency; empty state (`tags_empty`) and
  no-results state (`tags_search_empty`); load error with retry.
- Tapping a chip opens one sheet: name field (rename), "View movements" (→
  `showSearchFilter(etiquetaIds = setOf(id))`), Delete.
- New: same sheet empty; Save creates; a duplicate offers the existing tag (open it) instead of
  creating.
- Delete: confirmation dialog with the count (`tag_delete` plural message; zero uses a no-impact
  message), then delete, close the sheet and show `tag_deleted`; failures keep the sheet and show
  `tag_delete_error`.
- Rename: blank/too long/duplicate keep Save disabled or show the inline error; a no-change save
  closes without writing.
- Back closes the sheet first, then the screen (same modal controller pattern as Categories).

## TDD steps

- [ ] ViewModel: browse/search projection, create/duplicate, rename validation, delete count →
      confirm → deleted effect, failure keeps state, View movements effect.
- [ ] UI: chips with counts, sheet actions, confirmation dialog, Back order.
- [ ] Navigation: Profile → Tags; View movements leaves the pending `SearchFilterRequest` with the tag
      on the navigator (Search consuming it is task3's test); serialization of `Tags`.

## Exit

Tags lifecycle works end to end on host tests; screen previewed light/dark.
