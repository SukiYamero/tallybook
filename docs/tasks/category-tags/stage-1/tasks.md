# Stage 1 — Contract switch

## Objective

Move persistence, domain and shared UI contracts to "one categoría + tags" while keeping the app
building and behaving coherently with a single category. No new screens yet.

## Tasks

- [**task1 — Persistence and domain**](task1/task.md): migration, models, repositories, CSV, search
  criteria, suggester, frozen DI wiring and test doubles, and the mechanical adaptation of every
  consumer to `categoriaId`. The build is green after task1 alone.
- [**task2 — Shared UI contracts and copy**](task2/task.md): tag visuals, tag field, tag picker,
  single-category field with switch suggestions, single-select category picker mode, Search filter
  request, all new string keys. Depends on task1.

## Exit gate

- Schema-5 → 6 migration test green; `verifyMigrations` passes.
- No reference to `categoriaIds`, `MAX_CATEGORIAS_POR_MOVIMIENTO` or `CategoriaSelection` remains in
  production code.
- The app builds for Android and compiles for iOS; existing flows work with exactly one category
  (sentence → summary → save, edit, Search by category, Categories screen counts, CSV).
- New shared components exist with previews and device tests, not yet wired into features.
- `testAndroidHostTest`, `compileAndroidDeviceTest`, `assembleDebug`,
  `compileKotlinIosSimulatorArm64`, `verifyCommonMainTallybookDatabaseMigration`, `detekt` and
  `ktfmtCheck` pass after each task.

## Intermediate review

Required (migration and public-contract boundary). A reviewer independent of task1/task2 checks the
migration SQL against `.sq`, link integrity on update/delete/restore, query roll-up semantics, the
`DataOutcome` error mapping of `EtiquetaRepository`, and that shared components carry no feature
state. Fix confirmed findings before Stage 2 starts.
