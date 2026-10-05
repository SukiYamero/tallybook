# Wave 3 / Stage 1 — Contracts and pure rules

## Objective

Settle the product proposals in the [wave contract](../README.md), establish typed date ranges and
selection transitions, and extract the existing search normalization for its second real consumer.

## Tasks and ownership

- [task1 — Period and product contract](task1/task.md): period invariants, canonical rule alignment and
  fixed-date tests. Owns changes to shared domain documentation.
- [task2 — Shared search semantics](task2/task.md): normalization and matching, preserving category
  search behavior. May run in parallel because it touches different files; no domain-document edits.
- [task3 — Search filter contracts](task3/task.md): confirmed Apply/dismiss semantics, date presets,
  type/category predicates and custom-range validation. Follows task1 for canonical document ownership.

## Exit gate

- Week and annual proposals are explicitly settled before dependent implementation.
- Domain ranges contain civil dates, never UI types or platform timezone objects.
- Selection transitions have exact expected outcomes, including leap-year and current-date clamps.
- Search has one normalization owner and category search retains its empty-query behavior.
- Existing picker tests/catalog remain coherent with the accepted behavior.
- Applied Search filters and temporary draft have explicit, tested ownership and recovery rules.
- Common tests run on the Android host and compile for Native; Kotlin edits pass Detekt and ktfmt.

## Dedicated review

Reject duplicated calendar logic, a domain import of `TallybookPeriod`, an assumed ISO/calendar week,
platform-specific normalization, live filter writes from a draft sheet and untested saved-input
validation. task1/task3 update canonical documents sequentially, not through duplicate rule files.
