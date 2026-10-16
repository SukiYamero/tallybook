# Stage 3 — Integration and close

## Objective

Leave one coherent product and documentation state, pass the final independent review and collect
physical-device evidence.

## Tasks

- [**task1 — Catalog, copy cleanup and docs**](task1/task.md)
- **Final independent review** (fresh reviewer, per `docs/README.md` step 4): focused diff of the whole
  feature for correctness, ownership, duplication and simpler alternatives; fixes confirmed findings
  and reruns only affected checks.
- **Acceptance:** orchestrator installs on the confirmed Xiaomi and hands the user the smoke list from
  the README Verification section. iOS physical validation follows the existing backlog item.

## Exit gate

- Full host suite, Android build, iOS Native compilation, `detekt ktfmtCheck` green on the final tree.
- Upgrade install over the previous build on the Xiaomi migrates without data loss beyond the agreed
  discarded extra categories.
- User acceptance before merge; then compact this folder per `docs/README.md` step 7.
