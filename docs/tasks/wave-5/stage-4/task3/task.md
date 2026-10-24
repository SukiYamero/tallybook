# Wave 5 / Stage 4 / Task 3 — Medio on movimiento rows

## Objective

Show the payment method on every movimiento card (Home, History, Search) as a small icon with a
localized accessibility label, without changing the row's text hierarchy.

## Context and skills

- Read [the wave README](../../README.md) → Decisions (proposed default for rows) and Display
  surfaces; `docs/business-logic.md` → Movimiento → Tarjetas (card text rule); `docs/ui/architecture.md`
  → Design tokens and **Movement-card amounts** (the row contract this task extends);
  `docs/ui/components.md` → `MovimientoRow`.
- Skills: `android-skills:compose`, `chrisbanes-skills:compose-component-design`,
  `chrisbanes-skills:compose-performance`, `chrisbanes-skills:compose-ui-testing-patterns`,
  `android-skills:android-ux`.
- All three surfaces render through `MovimientoRowContent(movimiento, categoria, text, …)`
  (`core/ui/movimiento/MovimientoRow.kt`, `feature/history/ui/HistoryScreen.kt`,
  `feature/search/ui/SearchResultRow.kt`), and the merged `contentDescription` comes from
  `movimientoRowDescription`. The medio comes from `Movimiento.medio` (Stage 2), so callers do not
  change.

## Files

- Consume (created by Stage 2/task1): `core/ui/account/{MedioPagoIcon,MedioPagoLabels}.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/movimiento/{MovimientoRowContent,MovimientoRowSkeleton,MovimientoRow}.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/UiCatalogFixtures.kt` only if the catalog needs a medio variety
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/ui/movimiento/MovimientoRowDescriptionTest.kt` (create, or extend the existing row-description test if `grep -rl movimientoRowDescription shared/src/*Test` finds one)
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/core/ui/movimiento/MovimientoRowMedioTest.kt`

## Contracts

The icon lookup and labels come from Stage 2/task1 (`MedioPagoIcon`, `MedioPagoLabels`); this task
only places them on the row. Débito and crédito must be visually distinct; if the icons Stage 2 chose
are not, fix them there in this task's commit and record the choice in the catalog.

## Exact rules

- Icon sits on the amount line at its start, sized by an existing icon-size token, tinted
  `foreground.secondary`; the amount keeps `movementRowAmountStyle` and its color rule. No new
  literal sizes or colors.
- The row's merged accessibility description appends the medio label after the amount, e.g.
  "… -$ 50.000,00, crédito". Category fallback, pending label and two-line title rules are unchanged.
- The skeleton reserves the same icon slot so loading does not shift the amount.
- Account names do not appear on rows (README decision).
- Update `docs/ui/architecture.md` → **Movement-card amounts** and `docs/ui/components.md` →
  `MovimientoRow` in Stage 6, not here (Stage 6 owns docs); list the exact wording change in the
  task handoff.

## TDD

- [ ] RED: description test per medio and locale (es, en), pending + medio combination.
- [ ] GREEN: icon mapping, row and skeleton slot; previews light/dark, enlarged font.
- [ ] Device test: each surface's row announces the medio (semantics, not `testTag`).
- [ ] `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64 detekt ktfmtCheck :androidApp:assembleDebug`.
- [ ] Brief visual check on the confirmed Xiaomi (Home, History, Search, dark theme).

## Blocks

Blocked by Stage 2 review (`Movimiento.medio`). Independent of task1/task2. Blocks Stage 6.
