# Wave 5 / Stage 6 — Verification, final review and close

## Objective

Prove the wave on the physical Android target, run the one final independent review, and compact
this wave's rules into their canonical documents so the wave folder can be deleted.

## Order

1. **Integrated checks** on `feat/accounts` rebased on `main` (with `feat/sync-ready-schema` and
   `fix/ui-adjustments` already merged): `./gradlew :shared:testAndroidHostTest`,
   `./gradlew :shared:compileKotlinIosSimulatorArm64`, `./gradlew detekt ktfmtCheck`,
   `./gradlew :androidApp:assembleDebug`, SQLDelight migration verification (task discovered from
   `./gradlew :shared:tasks --all`). Device-test compilation; device-test execution only if the MIUI
   runner blocker in the [backlog](../../../backlog.md) is resolved, otherwise recorded as unexecuted.
2. **Final independent review** of the whole `feat/accounts` diff against
   [the wave README](../README.md) and every stage task: money exactness, single sources of truth
   (no stored balances, no currency copy), tombstone filtering on every read, stamp issuance on every
   mutation, Undo queue generalization without a second queue, Stage 3/4 ownership respected,
   platform imports in `commonMain`, cancellation/late-result protection, accessibility. The reviewer
   fixes confirmed findings and reruns only affected checks.
3. **Physical acceptance** (confirmed Xiaomi, existing mock dataset per memory — no backup needed):
   walk the README [Observable result](../README.md#observable-result) items 1–6 in order, plus:
   archive/unarchive an account, reject deleting a referenced account, a CLP account, a sentence with
   "con tarjeta" only, History of a cross-currency transfer in both currencies, Search Account/Medio/
   Transfer filters, View movements, CSV export, dark theme and enlarged text on Accounts and the
   medio control, TalkBack announcement of a row's medio. 3–5 minute smoke per changed area, no
   full-app revalidation.
4. **User acceptance**: report to the user what to try; iterate on feedback (back to the affected
   stage task); nothing merges without the user's ok.
5. **Compaction** (after the ok, one commit `docs: close accounts and medio delivery`):
   - `docs/business-logic.md`: replace the Movimiento required-field list, add sections for Cuentas,
     Medio de pago, Transferencias, Balances and credit-card estimate, Change stamps and tombstones;
     update Natural-language entry, Editing and deletion (Undo revives), Tags deletion, Search,
     Tracking and History, CSV export and Home recent/balance rules. Remove anything now false.
   - `docs/features/accounts.md` (new, short: integration and verification boundaries) and update
     `docs/features/{add-movimiento,manage-movimientos,history,search,profile,tags,custom-categories}.md`
     only where their boundaries changed; set the rows in `docs/features/README.md` to `DONE`.
   - `docs/ui/architecture.md` (Movement-card amounts, Balance summary cards, Snackbar presentation /
     Shell ownership for the generalized queue, Search presentation) and `docs/ui/components.md`
     (new account/medio/transfer components, `MovimientoRow`) per each task handoff.
   - `docs/wording.md`: medio vs transfer copy rules finalized by Stage 2/task1.
   - `docs/tasks/google-drive-persistence.md`: confirm the envelope rule includes accounts, transfers,
     stamps and tombstones.
   - `docs/backlog.md`: add the README [out-of-scope follow-ups](../README.md#out-of-scope-follow-ups-recorded-at-close)
     that remain wanted, the unexecuted device/scale checks with their exact blocker, and iOS
     physical validation of the new surfaces inside the existing iOS item.
   - Delete `docs/tasks/wave-5/` (git keeps the history), per [workflow](../../../README.md).
6. Merge to `main`, delete branches and worktrees.

## Exit

All integrated checks green on the current tree, review findings resolved, physical evidence for each
listed scenario or an explicit unverified note, user ok, canonical docs updated, wave folder removed.
Shared Native compilation is required; iOS runtime is not claimed.
