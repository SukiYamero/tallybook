# Stage 2 — Feature surfaces

## Objective

Wire the Stage 1 contracts into the three user-facing surfaces. Each task owns a disjoint set of
files; none edits `strings.xml`, `.sq`, `core/model`, `core/data` repositories, the shared
components from Stage 1, ViewModel dependency holders or test Koin modules (frozen by Stage 1). A
missing string or contract change goes back to the orchestrator.

## Tasks

- [**task1 — Tags management screen**](task1/task.md): Profile → Tags, create/rename/delete,
  View movements.
- [**task2 — Movimiento creation and editor**](task2/task.md): category switch, tags section, tag
  detection from the sentence.
- [**task3 — Search and rows**](task3/task.md): tag filter, tag text matching, applied chips.

All three depend only on Stage 1. Authoring may run in parallel; Gradle and device runs in the shared
worktree are serialized by the orchestrator (one build owner at a time).

## Exit gate

- Each task's tests pass; `./gradlew :shared:testAndroidHostTest`, `:shared:compileAndroidDeviceTest`,
  `:androidApp:assembleDebug`,
  `:shared:compileKotlinIosSimulatorArm64`, `detekt ktfmtCheck` pass on the combined tree.
- No intermediate review: the final independent review in Stage 3 covers this stage.
