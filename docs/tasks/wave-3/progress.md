# Wave 3 execution status

Worktree: `/Users/sukiyamero/.codex/worktrees/history-search/tallybook`  
Branch: `feat/history-search`  
Scope-revision baseline: `cfae0954d5749b00138c27999d92ceb1cae3d761`

## Current gate

History scope is accepted: Day/Week/Month plus year context, existing month-bound week blocks,
compact separately labelled global balance, no text search/filter UI. Search is a separate later
destination. No product decision remains pending. The revised contract passed independent document
review and is committed at `bebf1c5`. Stage 1 is implemented, checked and independently approved.
S2.T1 consultation reads are implemented, checked and independently approved. The early S5.T1
saved-owner/Koin probe and S3.T1 projection are independently approved. S3.T2 and cumulative Stage 3 are independently approved at `343fb1d`;
pure History UI is the next implementation increment. No whole-wave completion, merge or physical acceptance
is claimed.

## Implemented and verified

| Task | Evidence | Review |
| --- | --- | --- |
| S1.T2 shared normalization, preserved | `e0d667b`; expected category regression RED; 13 focused host tests GREEN; Native test compilation, Detekt and ktfmt pass; full shared host suite and Android debug build passed at the recorded implementation tree | Independent review approved; no findings |
| S1.T1 period rules | `e892284`; 33 focused host tests, Native test compilation, Detekt and ktfmt pass | Independent Stage 1 review approved; no findings |
| S2.T1 range/year reads | `7da1b2a`; 204 full host tests, Native production/test compilation, Android device-test compilation, Detekt and ktfmt pass | Independent review approved; no findings |
| S5.T1 early transport | `ace07b9`; four executed host probes, Native/host compilation/regression boundary in report, Detekt/ktfmt pass | Independent review approved; full shell/device proof remains later |
| S3.T1 projection | `f70a363`; 16 focused host tests, Native test compilation, Detekt/ktfmt pass | Independent review approved; no findings |
| S3.T2 state | `343fb1d`; 236 host tests, Android build, Native production/test compilation, Detekt/ktfmt pass | Independent cumulative Stage 3 review approved after readonly picker contract fix |

These checks belong to the recorded implementation tree, not to unimplemented History behavior.
The normalization still serves category search and future Search; History does not consume it.
Native compilation is not iPhone runtime evidence. No emulator/simulator was run; the generic Gradle
10 deprecation notice was not suppressed.

## Active task mapping

| Task | Status / next dependency |
| --- | --- |
| S1.T1 | Implemented/checked/reviewed |
| S1.T2 | Completed/reviewed; retain code and category regression coverage |
| S2.T1 | Implemented/checked/reviewed |
| S5.T1 early gate | Implemented/checked/reviewed; stable parent creation extras required in full shell |
| S3.T1 | Implemented/checked/reviewed; pure History rows/count/totals |
| S3.T2 | Implemented/checked/reviewed; full picker identity distinct from read range |
| S4.T1 | Active next increment; pure History/reference rows and Home compatibility |
| S5.T1 full shell | After reviewed Stage 4; Home/History navigation, scroll/Back/lifecycle and one sheet/bar |
| S6.T1 | After reviewed Stage 5; production graph, physical evidence, full-wave review/History closure |
| S1.T3 / S2.T2 / S3.T3 / S4.T2 | Deferred Search proposals; no implementation or current dependency |

## Coordination and evidence boundaries

- Accepted engineering rulings remain in their active owners: validated raw date boundary, positive
  persisted movement amount, scoped year validation, fallback input, five-second timeout/re-entry,
  originating creation-session callback identity and real navigation ownership integration.
- Early S5.T1 establishes decorated-entry/Koin/saved-owner restoration with new owners/stores and lazy
  typed-input checkpoints. Actual Activity Intent extras require stable parent creation extras; the
  full shell must retain that tested seam. Full-shell NavDisplay/scroll/Back/device proof remains later.
- Android saved-owner/Activity restoration and iOS retained-controller/saveable scope are separate
  contracts. A fresh iOS process starts normally; no cross-process bridge or new persistence is planned.
- One physical Xiaomi `M2012K11AG` was previously discovered; reconfirm authorized inventory before
  installation/visual work. Installation and physical acceptance have not run.
- One owner runs implementation/Gradle at a time. Each stage receives independent cumulative review;
  the runtime thread limit requires reusing the dedicated reviewer with a bounded assignment.
- Temporary audit reports live in ignored `.superpowers/sdd/README/`; this revision records its own
  document checks in `history-scope-report.md`. Those checks do not prove runtime behavior.

## Next action

Implement/review S4.T1, then integrate/review the S5.T1 shell and
execute S6 verification/physical acceptance. Deferred Search tasks are not gates.
