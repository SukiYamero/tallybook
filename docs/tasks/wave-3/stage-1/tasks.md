# Wave 3 / Stage 1 — History contracts and pure rules

## Objective

Implement the accepted [History contract](../README.md): typed civil ranges, Day/Week/Month selection
and year context; align canonical business/picker wording. No product decision remains pending.

## Tasks and ownership

- [task1 — Period and product contract](task1/task.md): active; pure transitions/fixed-date tests;
  owns canonical business/picker alignment. No query field or Search filter specification.
- [task2 — Shared normalization](task2/task.md): completed/reviewed at `e0d667b`; preserve category
  behavior and tests. History does not consume normalization.
- [task3 — Search filter contracts](task3/task.md): deferred; no current edits or stage gate.

## Exit gate and review

- Month-bound Week blocks and three scopes/year context agree with canonical wording/picker tests.
- Domain ranges contain civil dates, no UI/platform types; raw saved dates validate before construction.
- Leap/year/month/current-date clamps and resume/restored input have exact fixed-date expectations.
- Small History saved input contains scope/anchor/version only; no query, rows or new persistence.
- Existing picker/catalog and reviewed category-search behavior remain compatible.
- Focused host tests, Native test compilation, Detekt/ktfmt and fresh cumulative review pass.

Reject duplicated range calculations, `core.ui` importing History, a guessed calendar week/fourth
scope, impossible reversed-range repository tests and untested recovery. This gate enables S2.T1;
S5.T1's early real navigation probe separately precedes Stage 3 transport selection.
