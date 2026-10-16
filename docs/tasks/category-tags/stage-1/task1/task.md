# Stage 1 / Task 1 — Persistence and domain

## Objective

Replace the multi-category model with `categoriaId` + `etiquetaIds`, add tag persistence and
repository, migrate existing data, freeze dependency wiring, and adapt every consumer so the build
stays green.

## Context and skills

- Read `../../README.md` (domain contracts, persistence, defaults), `docs/business-logic.md`,
  `AGENTS.md`.
- Skills: `android-skills:android-dev`, `android-skills:android-data-layer`,
  `android-skills:kotlin-flows`, `android-skills:koin`, `android-skills:android-testing`,
  `superpowers:test-driven-development`.
- Blocks: Stage 1 / task2 and all of Stage 2.

## Files

Paths relative to `shared/src/commonMain/kotlin/com/kurobello/tallybook/` unless absolute.

- Schema: `shared/src/commonMain/sqldelight/com/kurobello/tallybook/core/database/Movimiento.sq`,
  `Categoria.sq` (count queries), new `Etiqueta.sq`, new `5.sqm`, generated
  `shared/src/commonMain/sqldelight/databases/6.db`.
- Model: `core/model/Movimiento.kt` (add `MAX_ETIQUETAS_POR_MOVIMIENTO`; **keep**
  `MAX_CATEGORIAS_POR_MOVIMIENTO` until task2), `MovimientoChanges.kt`, `MovimientoReadCriteria.kt`,
  new `core/model/Etiqueta.kt`, `EtiquetaWrite.kt`. Share `nombreKey()`/`NOMBRE_MAX` from
  `CategoriaRules.kt` instead of copying them. **Keep** `CategoriaSelection.kt` (task2 deletes it).
- Data: `core/data/MovimientoRepository.kt`, `MovimientoRepositoryImpl.kt`, `MovimientoMapper.kt`,
  `CategoryHistoryRow.kt`, `CategoriaRepositoryImpl.kt`, new `EtiquetaRepository.kt`,
  `EtiquetaRepositoryImpl.kt`, `core/data/export/MovimientoCsv.kt`,
  `core/data/entry/CategorySuggester.kt`.
- DI and dependency holders: `di/RepositoryModule.kt`, `di/ViewModelModule.kt`;
  `AddMovimientoRepositories` (`feature/movimientos/viewmodel/AddMovimientoViewModel.kt`),
  `ManageMovimientoViewModel` constructor, `SearchRepositories`
  (`feature/search/viewmodel/SearchObservation.kt`), `ProfileExportDependencies` +
  `feature/profile/viewmodel/ProfileViewModel.kt` (CSV tags), `feature/profile/ui/ProfilePresentation.kt`.
- Strings: `csv_header_tags` in all four `strings.xml` (only this key).
- Consumers, mechanical only (a one-element list and limit 1 where a component still expects a list):
  `feature/movimientos/viewmodel/*` (`AddMovimientoUiState`, `AddMovimientoViewModel`,
  `MovimientoEntryEngine`, `EditMovimientoUiState`, `MovimientoFormState`, `ManageMovimientoViewModel`),
  `feature/movimientos/ui/*` call sites, `core/ui/movimiento/MovimientoRowContent.kt`,
  `MovimientoRow.kt`, `feature/home/HomeOuterShell.kt`, `HomeScreen.kt`,
  `feature/history/viewmodel/HistoryProjection.kt`, `feature/history/ui/HistoryScreen.kt`,
  `feature/search/viewmodel/SearchProjection.kt`, `feature/search/ui/SearchResultRow.kt`,
  `core/ui/catalog/*`.

## Tests

- New `commonTest/.../core/data/UnsupportedEtiquetaRepository.kt` (pattern of
  `UnsupportedCategoriaRepository.kt`) and an in-memory `FakeEtiquetaRepository` for ViewModel tests.
- Register `EtiquetaRepository` in every test Koin module that builds real ViewModels:
  `PrimaryNavigationDeviceFixture.kt`, `TallybookAppShellLifecycleHostTest.kt`,
  `ManageMovimientoOwnerHostTest.kt`, `SearchViewModelGraphHostTest.kt`, `HistoryIntegrationTest.kt`,
  `RepositoryModulesTest.kt`, `ProfileViewModelGraphHostTest.kt`, `CategoriesViewModelGraphHostTest.kt`.
- New: `androidHostTest/.../core/data/EtiquetaRepositoryTest.kt`,
  `androidHostTest/.../CategoryTagsIntegrationTest.kt`.
- Update every test using `categoriaIds` (≈30 files; find them with
  `grep -rln "categoriaIds" shared/src/*Test`), including `DatabaseSchemaTest`,
  `MovimientoRepositoryTest`, `MovimientoMapperTest`, `CategoriaRepositoryTest`,
  `SearchRepositoryTest`, `HistoryRepositoryTest`, `ReactiveRepositoryTest`, `MovimientoCsvTest`,
  `CategorySuggesterTest`, `ProfileViewModelTest`, `FirstMovimientoIntegrationTest`,
  `HistoryIntegrationTest`, `AddMovimientoSheetRouteTest`, `SearchVolumeDeviceTest`, the
  AddMovimiento/ManageMovimiento fixtures and the History/Home/Search builders.

## Rules

- Repository validation replaces `validateCategoriaIds`: `categoriaId` non-blank and existing;
  `etiquetaIds` distinct, ≤ MAX, non-blank, no `\u001E`/`\u001F`, all existing tags.
- Insert/update/delete/restore write `movimiento.categoria_id` and `movimiento_etiqueta` in the same
  transaction. `deleteAndCapture`/`restoreDeleted` snapshot and restore tags; restore falls back to
  Otros for a deleted category and skips deleted tags (README § Persistence).
- `EtiquetaRepository.delete` removes links and the tag in one transaction; movimiento observers
  re-emit.
- Filtering: four named queries (README § Persistence). Category roll-up (parent includes children,
  never upward) on `movimiento.categoria_id`; tag filter matches any selected tag; both AND-combined.
  Persisted rows are never filtered in Kotlin.
- Counts per categoría use `movimiento.categoria_id` (+ parent roll-up); custom-category delete stays
  allowed only at zero uses. Counts per tag count links, filtered by moneda like categories.
- `CategorySuggester`: each movimiento votes fully for its single category; the learned set becomes a
  learned single category; preselection is that category or the single HIGH suggestion.
- CSV: one category name, its parent column (a root repeats itself), a new tags column joined
  `" · "`, headed by `csv_header_tags`.
- Rows drop the "+N" badge (`extraCategories`) since it can no longer be non-zero; keep the plural
  string until Stage 3. No new UX in this task.

## TDD steps

- [ ] `DatabaseSchemaTest`: schema-5 fixture with movimientos having 0, 1 and 3 category links →
      `categoria_id` is Otros / that one / lowest `posicion`; tag tables exist and are empty; the view
      returns `etiqueta_ids = ''`. Update the 1–4 migration tests to migrate to `Schema.version` and
      assert the first category only.
- [ ] `MovimientoRepositoryTest`: insert/update with tags round-trips order; update replaces tags;
      delete + restore keeps tags; restore after tag deletion keeps the rest; restore after custom
      category deletion uses Otros; validation rejects >6 tags, duplicates and unknown ids.
- [ ] `EtiquetaRepositoryTest`: create, duplicate (case/accent-insensitive) offers existing, rename,
      rename-to-duplicate, counts per moneda, delete unlinks.
- [ ] `SearchRepositoryTest`: the four filter combinations, tag OR, category roll-up AND tag.
- [ ] `CategoriaRepositoryTest`: delete refused while one movimiento uses the category; counts.
- [ ] `MovimientoCsvTest`, `CategorySuggesterTest` (single vote), `ProfileViewModelTest` (tags column).
- [ ] `CategoryTagsIntegrationTest`: one category + two tags through the real repositories, filter by
      tag and by parent category, delete one tag, observe the remaining tag.

## Exit

`testAndroidHostTest`, `compileAndroidDeviceTest`, `assembleDebug`, iOS compilation,
`verifyCommonMainTallybookDatabaseMigration`, `detekt ktfmtCheck` green. Report the migration evidence
and changed test files.
