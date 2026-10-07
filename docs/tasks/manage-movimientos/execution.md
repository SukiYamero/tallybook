# Continuation ledger

- Objective: approved detail/edit/delete/Undo feature; reusable snackbar has an independent commit.
  No tags, sync, dependency upgrades or cross-process trash.
- Baseline: `2143ce3`; managed worktree
  `/Users/sukiyamero/.codex/worktrees/0713/tallybook`. Root owns Git, builds, device commands and docs.
  SOL6.1 HIGH workers delivered snackbar, persistence and integration; an independent reviewer
  approved the final source including confirmed SQLite/IME/modal corrections.
- Commits: `8cb47ea` execution contract; `c59a4ee` independent snackbar;
  `f5262b5` persistence. Integration is the feature branch HEAD at handoff.
- Current source verification: 104 focused tests across 18 suites passed after modal corrections;
  final shell callback extraction then passed 24 navigation/owner regressions. Zero failures/errors/skips.
  Current Android production/device APK builds, iOS Arm64 production/test compilation, Detekt and
  ktfmtCheck pass. Inherited Gradle 10 deprecation remains. Device formatter task is explicit because
  shared ktfmtFormat does not include AndroidDeviceTest. Workflow `21e0b8db07b796162f33b26fbe244098`
  is finished; its compact wrapper removed only wrapper-owned logs.
- Required source gates: actual resolved versions and primary sources are recorded in the overview.
  Child ViewModel ownership has real Activity/Koin/SQLite retention/disposal tests. Dialog Back uses
  its own current window dispatcher; read-state detents cannot overwrite the detail measurement.
  These host/source proofs do not establish actual iOS scene teardown.
- Physical target: user confirmed Xiaomi M2012K11AG `aaa9cea2` and authorized temporary records.
  Current APK was installed and launched. Home/History/Search opened the sole fixture's detail.
  Actual Back closes Detail; clean Cancel returns complete detail; dirty Cancel prompts discard and
  preserves saved value. Edit opens without IME; Save/Cancel are absent with IME and restored after
  dismissal. Nested category Back returns to Edit.
- Physical mutations: only `CodexManageSmoke0713` was created/changed. Expense 123 was updated to
  Income 124; Search showed the retained query/new row and success feedback, History/Home reread it.
  Delete confirmation Cancel preserved it; Delete/Undo restored it with success feedback.
  Undo worked after 12 seconds in background and after more than 12 seconds covered by Add.
  Final delete plus Close released Undo. Retained Search query showed 0 results and no fixture row.
  No original user record was modified; no temporary movement remains.
- Automated Android device runtime is unconfirmed: instrumentation stalled before assertions at 150s;
  one bounded retry after Dozing-to-Awake correction also stalled at 45s. Do not retry unchanged
  infrastructure or treat compiled fixture tests as executed. Manual Xiaomi checks provide the
  above runtime evidence, not an assistive-technology/large-text audit.
- Outstanding acceptance: physical iOS scene teardown/safe-area/VoiceOver, actual TalkBack/large-text
  checks and user's acceptance. Connected iPhone is not the confirmed target; the pending target
  question has no answer. Keep IN_PROGRESS; no merge/push or spec compaction before user acceptance.
  Promote canonical domain/UI docs only through the repository's accepted-feature workflow.

## Current rulings

- SQLDelight is the persisted owner; one shell child ViewModel owns session/draft/Undo. No second
  form/catalog state owner, no saved-state financial snapshots and no speculative resume epoch.
- Keep the cohesive repository boundary and public mutation types. Internal helpers stay internal;
  narrowly explained TooManyFunctions annotations follow existing precedent. No global rule changes.
- An unreadable same-ID row is a preserved terminal restore conflict. Capture validates strict
  restore eligibility before deletion inside the transaction; legacy raw delete remains unchanged.
- Feedback callbacks evaluate live foreground/modal admission and token/revision at delivery.
  iOS's resolved accessibility manager returns baseline duration; VoiceOver extension is not claimed.
- Preserve Add defaults, one-shot Channel conventions and origin-safe hidden callbacks.
