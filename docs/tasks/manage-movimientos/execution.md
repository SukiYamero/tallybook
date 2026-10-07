# Continuation ledger

- Objective: implement the approved detail/edit/delete/Undo contract; reusable snackbar has its own
  commit and can run in parallel with persistence. No tags, sync or dependency upgrades.
- Baseline: `2143ce3`; existing managed worktree `/Users/sukiyamero/.codex/worktrees/0713/tallybook`,
  detached HEAD. Planning documents are the only pre-existing dirty work and belong to this feature.
- Agents: `snackbar` owns Wave 1 Task 1; `persistence` owns Wave 1 Task 2. Both use
  `gpt-6.1-sol` / `high`. Integration starts after both producer checks; final reviewer is independent.
- Shared resources: root owns Gradle workflow `21e0b8db07b796162f33b26fbe244098`, Git index/commits,
  canonical docs and physical device commands. Workers request RED/GREEN checks and never run Gradle.
- Baseline evidence: selected Money, MovimientoRepository, AddMovimientoViewModel and
  PrimaryModalCoordinator host tests passed via compact wrapper; no Kotlin source changes yet.
  Existing Gradle 10 deprecation warning remains inherited.
- Target: user selected Xiaomi M2012K11AG, serial `aaa9cea2`, first; authorized temporary test records.
  iPhone is connected but is not the selected first target. Never use simulators or production fixtures.
- API gate: current Android/iOS configurations resolved; common Compose 1.11.1 / AndroidX1.11.2,
  SQLDelight2.3.2, Koin4.2.2, Unstyled2.9.0, Lifecycle2.11.0-beta01 and Navigation3 1.1.1 confirmed.
  Isolated diagnostic task needs `--no-configuration-cache` because it closes over Project;
  repository build settings remain unchanged. Source links live in each task/overview.
- Current task state: Wave 1 GREEN: 9 snackbar timing + 14 repository + 10 reactive tests pass.
  Formatting applied. Combined target build found snackbar test DpRect API errors and structural
  lint findings; owners corrected the cited sources. Corrected target/style run is in progress.
  Root-owner audit identifies a remembered child owner and explicit NavigationBackHandler as the
  existing-API integration path; no dependency upgrade is needed.
- Next: resolve supported dependency/platform contracts, observe focused failures, implement producers,
  verify them, then dispatch one integration owner. Required runtime gates remain unconfirmed.

## Current rulings

- Keep existing public repository boundary, so new exposed mutation parameter/result types are public;
  internal implementation helpers remain internal. Avoid an unrelated visibility migration.
- An unreadable existing same-ID row is a restore conflict, not a retryable failure, and is preserved.
  Compare safely within the pure mapping boundary; storage exceptions retain normal repository handling.
- Use common accessibility timeout recommendations without an unrequested iOS adapter. The resolved
  Skiko/iOS default returns the baseline; VoiceOver-adapted expiry is not claimed. If the physical
  action window is insufficient, target adaptation becomes required before acceptance.
- Keep the cohesive typed repository/API with narrowly explained TooManyFunctions annotations,
  following existing SearchActions/SearchViewModel precedent. No global threshold change or other
  suppression is permitted; final review must assess whether the repository remains one responsibility.
- Root management uses a remembered child ViewModel owner outside primary entries: preserve Android
  configuration retention and explicitly clear child ownership on iOS scene disposal. Use the current
  Lifecycle provider API, not a platform adapter. Prove disposal/retention at the integration gate.
- Give management an optional reason-specific Back hook in the shared sheet, preserving Add defaults.
  Use existing NavigationBackHandler APIs; deprecated CMP BackHandler and nonAndroid Unstyled
  EscapeHandler do not establish supported iOS behavior.
