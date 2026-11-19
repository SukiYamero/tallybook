# Budgets

## Contexto

Monthly per-category budgets that tell the user, without blocking anything, when a category is close
to its amount, past it, or spending fast enough to pass it. Rules and approved design:
[spec](../superpowers/specs/2026-11-18-budgets-design.md).

## Waves

The [implementation plan](../superpowers/plans/2026-11-18-budgets.md) owns the task breakdown; every
task builds on the previous one's interfaces, so each wave is one task, executed and reviewed in order.

- **Wave 1**: Task 1 (rules) · **Wave 2**: Task 2 (schema, repository) · **Wave 3**: Task 3 (ViewModel)
- **Wave 4**: Task 4 (screen, navigation, strings) · **Wave 5**: Task 5 (Home row)
- **Wave 6**: Task 6 (notice after saving) · **Wave 7**: Task 7 (docs, device acceptance)

## Verificación

- **APIs sensibles a versión/plataforma**: none new; SQLDelight 2.3.2 and Compose Multiplatform 1.11.1
  APIs already used in the repo.
- **Contrato de integración**: `PresupuestoRepositoryTest` over an in-memory SQLite schema with
  migration 12 (spent per budget, children, currency, tombstones); ViewModel tests prove the notice
  reaches the feedback queue after a real save path.
- **Runtime por target**: on the confirmed physical Xiaomi — create a budget, see each state, the Home
  row, the notice after creating, editing and paying a recurrente (with Undo), and "Ver movimientos".
  iOS stays compiling; its runtime pass belongs to the backlog's iOS end-to-end item.
