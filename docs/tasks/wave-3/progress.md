# Wave 3 execution status

Worktree: `/Users/sukiyamero/.codex/worktrees/history-search/tallybook`  
Branch: `feat/history-search`  
Scope-revision baseline: `cfae0954d5749b00138c27999d92ceb1cae3d761`

## Current gate

History is implemented and independently source-reviewed: Day/Week/Month, year context, month-bound
week blocks and a separately labelled compact global balance. Search remains deferred. Whole-wave
production source is approved at `e093eb4`; reviewed Android fixture corrections finish at `12c5541`.

The Android/Xiaomi round is verified; the user explicitly ended further testing after the important
checks. Integration approval remains a separate step. The six UI/navigation
assertions that initially failed now have passing corrective checks, with no weakened visibility or
navigation guards. The shortened isolated volume case passed once in 8.669s, preserving all 10,000
selected rows and excluding 4,000 foreign-currency and 4,000 outside-date rows. Indexed read took
308.784ms and projection 62.835ms on the debug test APK; these are target observations, not a capacity
limit or a leak claim. Source, artifact and runtime results received independent focused review.

The installed production APK matches the built artifact. Actual MainActivity checks confirmed
historical blocks, Home/History selection retention, year-menu Back, category cancellation, amount
keyboard dismissal, landscape/portrait recreation and saved-Activity restoration in a new process.
Cold launch restored Home and fresh History Day/today defaults while visible financial values
persisted. Spanish resources/light mode were exercised and original locale/theme/rotation restored.
The real midnight resume followed October 5→6 without a fabricated date or production test data.

The user explicitly deferred iPhone runtime verification. iOS remains a follow-up; Native compilation
is not runtime proof. TalkBack, additional physical devices and every supported locale are not claimed
as exercised in this round. Product approval, merge and feature compaction remain pending.

## Implemented and verified

| Task | Evidence | Review |
| --- | --- | --- |
| S1.T2 shared normalization, preserved | `e0d667b`; expected category regression RED; 13 focused host tests GREEN; Native test compilation, Detekt and ktfmt pass; full shared host suite and Android debug build passed at the recorded implementation tree | Independent review approved; no findings |
| S1.T1 period rules | `e892284`; 33 focused host tests, Native test compilation, Detekt and ktfmt pass | Independent Stage 1 review approved; no findings |
| S2.T1 range/year reads | `7da1b2a`; 204 full host tests, Native production/test compilation, Android device-test compilation, Detekt and ktfmt pass | Independent review approved; no findings |
| S5.T1 early transport | `ace07b9`; four executed host probes, Native/host compilation/regression boundary in report, Detekt/ktfmt pass | Independent review approved; full shell/device proof remains later |
| S3.T1 projection | `f70a363`; 16 focused host tests, Native test compilation, Detekt/ktfmt pass | Independent review approved; no findings |
| S3.T2 state | `343fb1d`; 236 host tests, Android build, Native production/test compilation, Detekt/ktfmt pass | Independent cumulative Stage 3 review approved after readonly picker contract fix |
| S4.T1 UI | `7ca41d4`; 240 host tests, Android build, Native production/test compilation, device fixture compilation, Detekt/ktfmt pass | Independent source-stage review approved after visibility-aware chip fix; Android behavioral gate passed; user acceptance pending |
| S5.T1 shell | `5126f53`; 251 host tests, Android app/test APK builds, Native production/test compilation, Detekt/ktfmt pass | Independent source-stage review approved after compatible Home cache fix; Android navigation gate passed; user acceptance pending |
| S6.T1 graph | `e093eb4`; 258 host tests, Android app/test APK builds, Native production/test compilation, Detekt/ktfmt pass | Whole-wave source review approved after wrong-type checkpoint fix; Android round passed; iOS deferred; user acceptance pending |

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
| S4.T1 | Source and Android UI checks reviewed; product acceptance pending |
| S5.T1 full shell | Source and Android navigation checks reviewed; product acceptance pending |
| S6.T1 | Source/Android runtime/volume reviewed; iOS deferred; user acceptance pending |
| S1.T3 / S2.T2 / S3.T3 / S4.T2 | Deferred Search proposals; no implementation or current dependency |

## Coordination and evidence boundaries

- Accepted engineering rulings remain in their active owners: validated raw date boundary, positive
  persisted movement amount, scoped year validation, fallback input, five-second timeout/re-entry,
  originating creation-session callback identity and real navigation ownership integration.
- Early S5.T1 establishes decorated-entry/Koin/saved-owner restoration with new owners/stores and lazy
  typed-input checkpoints. Actual Activity Intent extras require stable parent creation extras; the
  full shell retains that tested seam. Actual Android shell/Back/new-owner and MainActivity recreation
  checks passed; iOS runtime remains deferred.
- Android saved-owner/Activity restoration and iOS retained-controller/saveable scope are separate
  contracts. A fresh iOS process starts normally; no cross-process bridge or new persistence is planned.
- Physical Xiaomi `M2012K11AG` / `aaa9cea2` ran the UI/navigation and bounded volume checks through
  a fresh foreground self-test process and the supported `--no-restart` option. The default runner's
  MIUI background-start block was bypassed through normal foreground launch, with no permission change.
  Failed fixtures were diagnosed and corrected; only affected cases/neighbors and one shortened volume
  repeat were run. Production data was preserved; no production fixture seeding or synthetic save.
- iPhone runtime is deferred by explicit user instruction. No iPhone installation, signing, SDK or
  developer-image change occurred. Existing Native compilation is the available iOS evidence.
- One owner runs implementation/Gradle at a time. Each stage receives independent cumulative review;
  the runtime thread limit requires reusing the dedicated reviewer with a bounded assignment.
- Temporary audit reports live in ignored `.superpowers/sdd/README/`; this revision records its own
  document checks in `history-scope-report.md`. Those checks do not prove runtime behavior.

## Next action

The testing round is closed at the user's request. Keep the implementation and evidence committed;
handle integration approval separately under `docs/README.md`, retaining deferred Search/iOS work.
No additional testing, merge or worktree cleanup is implied by ending this round.
