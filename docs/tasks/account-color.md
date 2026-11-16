# Account color and cash default

## Contexto

Credit cards stay as they are; the user wants every cuenta to carry a color they pick, and entry to
default to cash unless the sentence names a card or account. This precedes recurring movimientos.

## Alcance

- `cuenta.color` holds a `CategoryTint` storage key (theme-token name, light/dark safe). Schema
  migration `9.sqm` adds the column and backfills by type: EFECTIVO→`emerald`, BANCO→`blue`,
  CREDITO→`purple`. New accounts default to their type's color, including the seeded cash account.
  A color change is a normal cuenta update with one change stamp.
- `Cuenta`, `CuentaDraft`, `CuentaWrite`, mapper, seed and repository carry `color`. The account form
  (`feature/accounts/ui/AccountForm.kt`) gets a color selector built from the category form's tint
  picker, extracted to `core/ui` rather than duplicated. In-place account creation from entry keeps the
  type default without asking for a color.
- The color shows as an avatar/swatch on Accounts rows, the account sheet header and the account
  selector/picker used in entry and editing (`core/ui/account/CuentaSelectorField.kt`). Movimiento cards
  do not show the account.
- Default selection without a medio no longer uses the newest movimiento. `selectSelectionCandidates`
  ranks: primary cash account by `Cuenta.defaultCashId` → any active EFECTIVO account in the primary
  currency → any active account in the primary currency → any other. With a medio, rank 0 (newest
  movimiento with that medio) stays first.
- Sentence detection does not change.
- `backlog.md`: drop "Credit cards: limit, due date and payment reminders" and work-order item 2.

## Waves

- **Wave 1**: data (migration, model, mapper, repository, seed, default selection) and UI (color
  selector, swatches, strings es/en/pt) in one owner, since UI consumes the new model field.
- **Wave 2**: final independent review, fixes, physical smoke check.

## Verificación

- **Contrato de integración**: host tests for the 8→9 migration backfill on existing rows, color
  round-trip through create/update, and default selection with and without medio (including a
  primary currency whose seeded cash account is missing or archived).
- **Runtime por target**: on the confirmed Xiaomi, upgrade over existing data (colors by type), edit a
  color, open entry with no sentence → Efectivo preselected, "… con NU" selects the NU account.
  iOS physical runtime stays under the backlog's iOS end-to-end item.
