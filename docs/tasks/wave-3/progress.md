# Wave 3 execution status

Worktree: `/Users/sukiyamero/.codex/worktrees/history-search/tallybook`  
Branch: `feat/history-search`  
Scope-revision baseline: `cfae0954d5749b00138c27999d92ceb1cae3d761`

## Current gate

History scope is accepted: Day/Week/Month plus year context, existing month-bound week blocks,
compact separately labelled global balance, no text search/filter UI. Search is a separate later
destination. No product decision remains pending for active History; the revised contract still needs
its document review, then S1.T1 execution and fresh cumulative Stage 1 review. No merge, current-tree
implementation completion or physical acceptance is claimed by this scope revision.

## Implemented and verified

| Task | Evidence | Review |
| --- | --- | --- |
| S1.T2 shared normalization, preserved | `e0d667b`; expected category regression RED; 13 focused host tests GREEN; Native test compilation, Detekt and ktfmt pass; full shared host suite and Android debug build passed at the recorded implementation tree | Independent review approved; no findings |

These checks belong to the recorded implementation tree, not to unimplemented History behavior.
The normalization still serves category search and future Search; History does not consume it.
Native compilation is not iPhone runtime evidence. No emulator/simulator was run; the generic Gradle
10 deprecation notice was not suppressed.

## Active task mapping

| Task | Status / next dependency |
| --- | --- |
| S1.T1 | Next after scope-doc review; pure selection/range and canonical business/picker alignment |
| S1.T2 | Completed/reviewed; retain code and category regression coverage |
| S2.T1 | After reviewed Stage 1; bounded range/year observation and positive read validation |
| S5.T1 early gate | After Stage 1, by Stage 2 closure; add official adapter, resolve graph, real saved-owner/Koin probe before S3 chooses transport |
| S3.T1 | After reviewed Stage 2 and early probe; pure History rows/count/totals |
| S3.T2 | After S3.T1; History state/selection/retry/resume/presentation input |
| S4.T1 | After reviewed Stage 3; pure History/reference rows and Home compatibility |
| S5.T1 full shell | After reviewed Stage 4; Home/History navigation, scroll/Back/lifecycle and one sheet/bar |
| S6.T1 | After reviewed Stage 5; production graph, physical evidence, full-wave review/History closure |
| S1.T3 / S2.T2 / S3.T3 / S4.T2 | Deferred Search proposals; no implementation or current dependency |

## Coordination and evidence boundaries

- Accepted engineering rulings remain in their active owners: validated raw date boundary, positive
  persisted movement amount, scoped year validation, fallback input, five-second timeout/re-entry,
  originating creation-session callback identity and real navigation ownership integration.
- Early S5.T1 probe is still unverified. Its actual decorated-entry/Koin/saved-owner evidence is required
  before Stage 3 transport; full-shell NavDisplay/scroll/Back/device proof remains later. Do not enable
  Search or build a Search ViewModel just to exercise the early probe; Home/History suffice.
- Android saved-owner/Activity restoration and iOS retained-controller/saveable scope are separate
  contracts. A fresh iOS process starts normally; no cross-process bridge or new persistence is planned.
- One physical Xiaomi `M2012K11AG` was previously discovered; reconfirm authorized inventory before
  installation/visual work. Installation and physical acceptance have not run.
- One owner runs implementation/Gradle at a time. Every stage receives a fresh cumulative reviewer.
- Temporary audit reports live in ignored `.superpowers/sdd/README/`; this revision records its own
  document checks in `history-scope-report.md`. Those checks do not prove runtime behavior.

## Next action

Review this revised History execution contract, execute S1.T1, then review Stage 1. Execute S2.T1 and
the early S5.T1 probe before Stage 3. Continue through active stages; deferred Search tasks are not gates.
