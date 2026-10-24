# Wave 5 — Accounts, payment method and sync-ready persistence

> **For implementing subagents:** execute one `taskN/task.md` per fresh context through
> `subagent-driven-development`; finish TDD and the task checks before asking for review. Read this
> README first: it owns every rule of this wave until the close stage compacts them into
> [business logic](../../business-logic.md). Challenge the spec instead of implementing a gap blindly.

**Goal:** every movimiento records where the money came from or went to (an account) and how it was
paid (a payment method); the user sees per-account balances, an estimated credit-card debt, and moves
money between their own accounts with transfers. Before that, every persisted entity becomes
sync-ready: change stamps and tombstones, so the later Google Drive backup and the HLC/outbox engine
need no second data migration.

**Architecture:** SQLDelight stays the single source of truth; derived values (balances, debt, cycle
consumption) are SQL aggregates, never stored. New domain types live in `core.model` without framework
dependencies; repositories stay the error boundary; one feature-vertical package per new screen.

**Tech stack:** unchanged — Kotlin 2.4.10, CMP 1.11.1, SQLDelight 2.3.2 (default SQLite 3.18
dialect), Koin 4.2.2, DataStore 1.2.1. No new dependency is planned.

## Observable result

On the physical Xiaomi, after upgrading over the current mock dataset:

1. Existing movimientos are intact, assigned to an "Efectivo" account per currency, payment method
   Efectivo. Home's large figure is the **total balance** of the primary-currency accounts.
2. Profile → Accounts lists accounts with their balances; the user creates "Nequi" (bank or wallet)
   and "NU" (credit card, statement day 15) with opening balances.
3. Creating "Gasté 50 mil en cine con la NU" detects Crédito + NU; the movimiento lowers NU's balance,
   and NU shows estimated debt and this cycle's spending.
4. "Pay card" creates a transfer Nequi → NU prefilled with the debt; both balances change, income and
   expense totals do not.
5. A transfer USD → COP with sent and received amounts updates both currencies' balances.
6. Deleting a movimiento or transfer and tapping Undo restores it with the same identity; every
   mutation updated its change stamp; deleted rows remain as tombstones that no screen shows.

## Starting point (repository evidence)

| Observation | Evidence | Consequence |
| --- | --- | --- |
| Schema version 6, five migrations, `verifyMigrations` on | `shared/src/commonMain/sqldelight/.../database/{1..5}.sqm`, `databases/6.db`, `shared/build.gradle.kts` | Stage 1 adds `6.sqm` (→ v7), Stage 2 adds `7.sqm` (→ v8). Rebuilt tables must match fresh DDL, as `5.sqm` does. |
| No change metadata; deletes are hard | `Movimiento.sq` `deleteMovimiento`, `Etiqueta.sq` `deleteEtiqueta`, `Categoria.sq` `deleteCustomCategoria` | Tombstones change every read and the Undo restore contract (Stage 1). |
| Every movimiento read goes through one view | `movimiento_con_etiquetas` | The view is the single place that excludes tombstones and later exposes the account's currency. Aggregates/counts that read `movimiento` directly must be updated one by one. |
| Currency is a movimiento column | `movimiento.monto_moneda`; all reads filter by it | With accounts the currency belongs to the account. The column is dropped and the view exposes `cuenta.moneda AS monto_moneda`, keeping one owner. |
| Undo snapshots are movimiento-only | `MovimientoRepository.deleteAndCapture/restoreDeleted`, `ManageMovimientoViewModel`, `MovimientoFeedback` | Transfers reuse the same token/snapshot mechanism generalized over a sealed snapshot type — never a parallel Undo queue. |
| Entry parsing is lexicon-based and tag detection matches existing names | `core/data/entry/MovimientoPhraseParser.kt`, `PhraseLexicon.kt`, `EtiquetaMatcher.kt` | Payment-method words go into the lexicon; account names are matched like tags. |
| One `strings.xml` per locale (558 lines) | `composeResources/values*/strings.xml` | CMP documents multiple XML files per `values` directory. Parallel stages add their own `strings_<area>.xml`; Stage 2/task1 proves the generated accessors on 1.11.1 before others rely on it. |
| Android auto backup enabled, no rules | `androidApp/src/main/AndroidManifest.xml` `allowBackup="true"` | The installation node ID must not be restored on another device (Stage 1/task1 excludes its file). |

Sources: [CMP resources usage](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html),
[localizing strings](https://kotlinlang.org/docs/multiplatform/compose-localize-strings.html).

## Decisions

Confirmed by the user:

- Home's main figure is the **total balance**: the sum of the primary-currency accounts' balances
  (opening balance + ingresos − gastos ± transfers). History keeps showing the period's flow.
- Transfers between accounts of **different currencies** are allowed with a sent and a received
  amount; no automatic exchange rate.
- **Payment method (`MedioPago`) is its own, prominent field** on every movimiento: Efectivo, Débito,
  Crédito, Transferencia. The account is chosen within the method. Débito draws from a bank/wallet
  account; Crédito uses a credit-card account whose balance is debt.
- **Paying a card is a transfer**, never an expense: the purchases were already recorded as gastos.
- The app estimates what is owed on each card; it is labelled as an estimate because the user may not
  record every purchase.
- Without an account named in the sentence, the **last used** method and account are preselected; the
  sentence wins when it names them, and alternatives stay visible to tap.

Proposed defaults, accepted unless the user objects during visual review:

- Account types: Efectivo, Banco o billetera (Nequi, Daviplata, Mercado Pago, debit cards), Tarjeta de
  crédito. No card numbers or other sensitive data — only a name.
- An optional statement day (`diaCorte`) on credit cards makes the cycle estimate follow the card;
  without it the cycle is the calendar month. Due date, limit and reminders are out of scope.
- Home's secondary values become this month's ingresos and gastos (all-time totals next to a balance
  that includes opening balances would read as inconsistent).
- Movimiento rows show the payment method as a small icon with an accessibility label; the account
  name appears in the editor and summary, not on rows.
- Internal transfers are created from Accounts, never from the sentence dialog in this wave.

## Domain rules

### Change stamps and tombstones (Stage 1)

- Every synchronizable root entity — `movimiento`, `categoria`, `etiqueta`, and from Stage 2 `cuenta`
  and `transferencia` — has `updated_at` (non-null) and `deleted_at` (null while live).
- Both hold a **`ChangeStamp`**: a hybrid logical clock value encoded as sortable text,
  `<13-digit epoch millis>-<4 hex counter>-<16 hex node>`. Text order equals stamp order. A stamp is
  never parsed back into a wall-clock time for business logic.
- One `ChangeClock` per process issues stamps: `next = max(wallClock, last.millis)` with the counter
  incremented on ties or when the wall clock moves backward. `prepare` seeds it from the maximum stored
  stamp across every stamped table (Stage 2/task2 adds `cuenta` and `transferencia` to that read)
  before the first mutation, so stamps never go backward across restarts. `prepare` is idempotent and
  only ever advances the clock; it is called again after any future dataset replacement (Drive
  restore).
- The node is a random per-installation ID kept in a **device-scoped** DataStore file, separate from
  `tallybook.preferences_pb`, excluded from Android auto backup/device transfer and from iOS backups
  (`isExcludedFromBackup`). It is created on first use and never copied into data rows other than
  inside stamps.
- An aggregate is a root row plus the child rows it owns (`movimiento_etiqueta` belongs to its
  movimiento). Any change to a child bumps the root's `updated_at`. Child rows carry no stamps.
- Every successful insert, update, hide/show, rename, delete and restore issues **one** stamp per
  transaction, shared by every row it writes (a tag deletion touching many movimientos stamps them all
  alike). A normalized no-op writes nothing.
- Stamps stay in the persistence layer: domain models (`Movimiento`, `Categoria`, `Etiqueta`, `Cuenta`,
  `Transferencia`) do not carry them, so content equality (Undo) is unaffected. The Drive codec adds a
  stamped read when it needs one. Deriving localized seed names (`localizeSeeded`) is
  presentation, not a user change, and issues no stamp.
- Seeded system rows (predefined categories, default cash accounts) are inserted with the constant
  `ChangeStamp.SEED` (`0000000000000-0000-0000000000000000`) so every installation agrees on their
  initial version.
- **Deleting sets `deleted_at = updated_at = next stamp`.** The row and its own fields stay; owned child
  rows are removed. Every read, count, aggregate, uniqueness check, export and suggestion source
  excludes tombstones. Tombstones are never purged in this wave; compaction belongs to the sync engine.
- A deleted name can be reused: creating a category, tag or account with a tombstoned name creates a
  new ID; it never revives the tombstone.
- Undo revives: restoring a captured movimiento/transfer whose row is a tombstone with identical
  content clears `deleted_at`, issues a new stamp and re-creates surviving child links. A live identical
  row is `AlreadyPresent`; a live different row, or a tombstone with different content, is `Conflict`.
  Account-specific restore outcomes are in [Movimiento changes](#movimiento-changes).
- Deleting a tag tombstones it, removes its links from live movimientos and bumps each affected
  movimiento's stamp in the same transaction.
- The future Drive snapshot includes stamps and tombstones (see the envelope rule in
  [Google Drive persistence](../google-drive-persistence.md#local-first-backup)).

### Cuenta (account)

- Fields: `id`, `nombre` (trimmed, up to 30 characters, unique per currency among live accounts —
  the pair (nombre, moneda) ignoring case and supported accents), `tipo` (`EFECTIVO`, `BANCO`,
  `CREDITO`), `moneda`, `saldoInicial` (signed exact `Money` in the account's currency, stored as
  received; for a credit card the form asks for a positive opening debt and the form state negates
  it — the only place that conversion happens), optional `diaCorte` (1–31, credit cards only),
  `archivada`, `createdAt`, stamps.
- Pickers, selectors and rows show the currency code next to accounts that are not in the primary
  currency, so same-named accounts stay distinguishable.
- Account names are not translated. The default cash account is created with the name localized at
  creation time ("Efectivo", "Cash", "Dinheiro"); the migration of existing data uses "Efectivo" for
  every present currency (unique because uniqueness is per currency).
- Default cash accounts have the deterministic ID `cuenta_efectivo_<moneda in lowercase>`, so two
  installations seeding the same default produce the same entity. Bootstrap creates one in the
  primary currency **only when no live account exists** (a fresh install); changing the primary
  currency never creates an account.
- The currency is locked while any live movimiento or transfer references the account
  (`CuentaWrite.CurrencyLocked`).
- Archiving removes the account from pickers; its balance still counts in totals and it stays visible
  under Archived in Accounts. Unarchiving is allowed. The last non-archived account cannot be archived
  or deleted.
- Deletion is allowed only while no live movimiento or transfer references the account; otherwise the
  sheet offers archiving, like custom categories.

### MedioPago (payment method)

| Medio | Compatible account types | Allowed for |
| --- | --- | --- |
| `EFECTIVO` | `EFECTIVO` | gasto, ingreso |
| `DEBITO` | `BANCO` | gasto |
| `TRANSFERENCIA` (paying or receiving from someone else) | `BANCO` | gasto, ingreso |
| `CREDITO` | `CREDITO` | gasto, ingreso (refund) |

- The repository rejects an incompatible medio/account/tipo combination; the UI never offers one.
- Switching a draft from gasto to ingreso while the medio is `DEBITO` changes it to `TRANSFERENCIA`
  on the same account.
- `TRANSFERENCIA` as a payment method (money sent to or received from a third party) is distinct from
  a **transfer between own accounts** (`Transferencia`). Copy must keep them apart; see
  [wording](../../wording.md), which Stage 2/task1 extends.

### Movimiento changes

- Required now: tipo, monto, fecha, **cuenta**, **medio**, exactly one categoría. The currency is the
  account's currency; there is no separate currency field to keep in sync.
- Creation: the amount is read with the separators of the selected account's currency. While the
  amount is sentence-owned, changing the account re-derives it from the sentence; a typed amount is
  re-validated against the new currency's scale (CLP has none) on Save.
- Editing may change medio and account only to an account with the **same currency**, keeping the
  existing rule that a stored movimiento's currency never changes. A movimiento whose account is
  archived keeps it while untouched.
- Restoring a deleted movimiento or transfer (one `RestoreResult` shared by both kinds):
  - into an archived account is allowed; the account stays archived;
  - when an account no longer exists → `RestoreResult.MissingCuenta`: the existing restore-failed
    informational feedback, the snapshot is released, and the money never moves to another account;
  - when an account's currency now differs from the snapshot's currency → `RestoreResult.Conflict`.

### Transferencia (transfer between own accounts)

- Fields: `id`, `cuentaOrigenId`, `cuentaDestinoId` (different, both live), `montoOrigen` (positive,
  origin currency), `montoDestino` (positive, destination currency; equal to `montoOrigen` and not
  asked when currencies match), `fecha` (civil date, not future), optional `descripcion` (≤180),
  `createdAt`, stamps. No category, tags or payment method.
- A transfer is neither income nor expense: it never enters ingresos/gastos, category totals,
  suggestions or any future budget. It changes the two account balances only.
- Create, edit and delete with confirmation; Undo after delete follows the movimiento contract through
  the same feedback queue. Editing may change every field; currency rules apply per side.
- "Pay card" on a credit card opens a transfer with the card as destination and the estimated debt as
  the received amount (`montoDestino`); with a same-currency origin that is the only amount, with a
  different-currency origin `montoOrigen` starts empty. The origin defaults to the newest live
  transfer's origin when active and not a credit card, else the primary-currency cash account.

### Balances and credit-card estimate

- `saldo(cuenta) = saldoInicial + Σ ingresos − Σ gastos + Σ montoDestino of incoming transfers −
  Σ montoOrigen of outgoing transfers`, over live rows of every date.
- Total balance for a currency = Σ saldo of every live account in that currency, archived included.
  Home shows the primary currency's total; Accounts shows one total per currency present.
- Credit card: `deuda = max(0, −saldo)`; a positive balance is shown as a credit in the user's favour.
- Cycle spending = gastos − ingresos (refunds) of the card inside the current cycle. With `diaCorte = d`
  the cycle runs from the day after the most recent statement date through the next statement date,
  where the statement day clamps to the month's last day (31 → 30 Apr, 28/29 Feb). Without it the
  cycle is the current calendar month. Both values are labelled as estimates.
- All arithmetic is exact `Long` minor units in SQL `SUM()`; currencies are never added together.

### Entry and selection

- Creation and editing show a **Medio** section before **Account**; medio is a single-choice control
  with the four methods (Débito/Transferencia hidden for gasto/ingreso as the table says).
- The account selector lists live, non-archived accounts compatible with the chosen medio; with one
  compatible account it shows that account without a picker. With none it offers creating one in
  place (quick form: name, type preset by the medio, currency defaulting to primary).
- Preselection without sentence evidence has one producer,
  `MovimientoRepository.defaultSelection(primary, medio?)`, a SQL read (no stored preference) that
  returns a resolved `CuentaSelection` (none only when a medio is given and no active account is
  compatible with it; without a medio, an account found by type implies its default medio — `BANCO` →
  `DEBITO`). Fallback order, restricted to the given medio when there is one: the medio and account of the most recently created live movimiento if the account is live,
  active and compatible → the primary-currency cash account if live and active → the first live
  active compatible account in the primary currency → the first live active compatible account.
  Entry layers sentence/user ownership on top and never re-implements the fallback.
- Sentence detection, in every supported language, follows the tag ownership model: what the sentence
  detects stays sentence-owned until the user picks by hand.
  - Method words ("en efectivo", "con débito", "con crédito", "tarjeta de crédito", "por
    transferencia", and their es-AR/en/pt-BR equivalents) select the medio.
  - A live account name appearing as whole words (shared search normalization) selects that account
    and, when unambiguous, its medio (`EFECTIVO`/`CREDITO` accounts; `BANCO` defaults to `DEBITO`
    unless a transfer word is present).
  - "con tarjeta" alone is ambiguous: no medio is decided; débito and crédito are offered. Two
    different explicit method words in one sentence are ambiguous too: both are offered, none is
    decided. Negated mentions ("no con débito", "sin tarjeta") are never method words.
  - The same account name in several currencies prefers the primary-currency account; without one
    there, the match is ambiguous and the candidates are offered.
  - An account mention whose type contradicts an explicit method word keeps the account and drops the
    method word.
- The summary sheet's choices become medio and account, category with suggestions, and optional tags.

### Display surfaces

- **Home:** the balance card shows the total balance (primary currency), with this month's ingresos and
  gastos as secondary values; tapping it opens Accounts.
- **Accounts** (Profile → Accounts, and from Home): totals per currency, accounts grouped by type, each
  with its balance; credit cards show estimated debt and cycle spending. Each account opens its sheet:
  edit, archive/delete, "View movements" (Search filtered by that account; wired in Stage 5 with the
  account filter), "Transfer", and for credit cards "Pay card".
- **History:** day groups include transfers touching an account in the selected currency as neutral
  rows ("Nequi → NU"), showing that currency's side of the amount. Period totals exclude them.
  Movimientos and transfers are read in one transaction, so rows, totals and years share a snapshot.
- **Search:** new Account and Medio filter dimensions (AND across dimensions, OR within). The type
  dimension gains Transfer (`SearchTipo` in `core.model`; `TipoMovimiento` stays gasto/ingreso);
  transfers appear when the type filter is empty or Transfer and no category, tag or medio filter is
  applied; an account filter matches either side.
- **Currency selector** (proposed default): History and Search show a currency selector only when live
  accounts exist in more than one currency; it defaults to the primary currency and follows each
  screen's existing restoration rules. "View movements" opens Search with that account's currency
  selected plus the account filter.
- **CSV export:** new columns cuenta, medio, cuenta_destino, monto_destino, moneda_destino; transfers
  are rows of type transferencia with empty category/tags/medio.

## Schema plan

Exact DDL is owned by the stage tasks; this is the target shape.

```sql
-- v7 (Stage 1): on movimiento, categoria, etiqueta
updated_at TEXT NOT NULL,   -- ChangeStamp; backfilled from created_at / SEED
deleted_at TEXT             -- ChangeStamp; NULL = live

-- v8 (Stage 2)
CREATE TABLE cuenta (
  id TEXT NOT NULL PRIMARY KEY, nombre TEXT NOT NULL,
  tipo TEXT NOT NULL CHECK (tipo IN ('EFECTIVO', 'BANCO', 'CREDITO')),
  moneda TEXT NOT NULL, saldo_inicial_minor INTEGER NOT NULL DEFAULT 0,
  dia_corte INTEGER CHECK (dia_corte BETWEEN 1 AND 31),
  archivada INTEGER NOT NULL DEFAULT 0 CHECK (archivada IN (0, 1)),
  created_at TEXT NOT NULL, updated_at TEXT NOT NULL, deleted_at TEXT
);
CREATE TABLE transferencia (
  id TEXT NOT NULL PRIMARY KEY, cuenta_origen_id TEXT NOT NULL, cuenta_destino_id TEXT NOT NULL,
  monto_origen_minor INTEGER NOT NULL CHECK (monto_origen_minor > 0),
  monto_destino_minor INTEGER NOT NULL CHECK (monto_destino_minor > 0),
  fecha TEXT NOT NULL, descripcion TEXT, created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL, deleted_at TEXT,
  CHECK (cuenta_origen_id <> cuenta_destino_id)
);
-- movimiento rebuilt: + cuenta_id TEXT NOT NULL, + medio_pago TEXT NOT NULL, − monto_moneda
```

Indexes are part of each task's contract: partial indexes `WHERE deleted_at IS NULL` where a read
filters live rows (supported by the 3.18 dialect), `movimiento(cuenta_id)`, transfer indexes on both
account columns and `fecha`. Existing comments explaining index choices stay valid or are updated.

## Ownership and data flow

```text
ChangeClock ── stamps ──> every *RepositoryImpl mutation (one transaction each)
                                │
CuentaRepository ──────────┐    ▼
TransferenciaRepository ───┼──> SQLDelight (cuenta, transferencia, movimiento, categoria, etiqueta)
MovimientoRepository ──────┘          │ live-row views/aggregates
                                       ▼
AccountsViewModel / HomeViewModel / HistoryViewModel / SearchViewModel / entry + manage ViewModels
```

- Writes for an aggregate happen in one repository transaction; cross-table invariants (medio vs
  account type, account currency immutability while referenced, live references) are enforced there,
  not in ViewModels. SQLite foreign keys stay disabled, as today.
- No use-case layer unless a rule is reused by more than one ViewModel or coordinates repositories
  (the "last used" read and the transfer Undo may justify a small shared component; justify it in the
  task).
- New packages: `feature.accounts.{ui,viewmodel}` (accounts screen and sheets),
  `feature.transfers.{ui,viewmodel}` (transfer form/management), `core.ui.account` (shared account
  chip/selector/medio control used by entry and accounts). History/Search activity reads (movimientos
  plus transfers in one snapshot) live in a new `ActividadRepository` (Stage 5/task1).

## Tracks, stages and parallelism

| Track | Branch / worktree | Content | Depends on |
| --- | --- | --- | --- |
| A | `fix/ui-adjustments` | Stage 0 — minor adjustments the user lists (snackbar review and others); **no spec** | nothing |
| B | `feat/sync-ready-schema` | Stage 1 — change stamps and tombstones | nothing |
| C | `feat/accounts` | Stages 2–6 — accounts, medio, transfers | Stage 2/task1 starts now; the rest after B merges |

At most three worktrees at once, within the four-worktree limit.

- **Stage 0 — UI adjustments**: no spec; the user lists the adjustments when it starts and accepts
  them visually. Runs in parallel with Stage 1 under the "copy, spacing or reversible visual
  adjustment" row of the [proportional verification policy](../../README.md#proportional-verification).
  Must merge before Stage 3 starts, because Stage 3 generalizes the feedback/snackbar owner.
- [**Stage 1 — Sync-ready persistence**](stage-1/tasks.md): task1's stamp contract commits first →
  the rest of task1 ∥ task2 → task3. Intermediate review (migration and repository contract boundary).
- [**Stage 2 — Accounts data model**](stage-2/tasks.md): task1 (pure domain + wording + resource
  split proof) runs in parallel with Stage 1. After B merges and C rebases: task2 (migration, plus the
  minimal call-site edits that keep the build green) → task3 (write repositories) ∥ task4 (balance read
  models). Intermediate review (migration and data contract).
- [**Stage 3 — Accounts and transfers UI**](stage-3/tasks.md) ∥ [**Stage 4 — Entry and editing with
  medio/account**](stage-4/tasks.md): both start after Stage 2's review; disjoint file ownership below.
- [**Stage 5 — History, Search and export integration**](stage-5/tasks.md): after Stages 3 and 4
  (consumes the transfer row and the medio/account filters' shared components).
- [**Stage 6 — Verification, final review and close**](stage-6/tasks.md).

### Shared-file ownership

| Shared resource | Owner |
| --- | --- |
| `*.sqm`, `.sq` DDL, `databases/*.db` | Stage 1/task2, then Stage 2/task2 only |
| Existing queries in `Movimiento.sq`/`Categoria.sq`/`Etiqueta.sq` | Stage 1/task2 → task3, Stage 2/task2 (rewrites broken by dropping `monto_moneda`) → task3, Stage 5/task1 (filters) — sequential |
| `Cuenta.sq`, `Transferencia.sq` | Stage 2/task2 (DDL, minimal insert/select-by-id queries, row→domain mappers) → Stage 2/task3 (write queries, `getAll`); transfer range/year/filter reads: Stage 5/task1 (`ActividadRepository`) |
| `Saldo.sq` (query-only) and `SaldoRepository` | Stage 2/task4 |
| Test fakes in `commonTest` | Whoever changes a repository interface updates every existing test fake of it in the same commit (the movimiento fakes live inside `HomeViewModelTest`, `HistoryViewModelTest`, `SearchViewModelTest`, `ProfileViewModelTest`, `AddMovimientoFixture`, `ManageMovimientoFixture`, `AddMovimientoViewModelTest`, plus host-test fakes). Stage 2/task3 creates `commonTest/.../core/data/{FakeCuentaRepository,FakeTransferenciaRepository}.kt`; Stage 2/task4 creates `FakeSaldoRepository.kt` there. Stages 3–5 consume or extend them in their own commits |
| `MovimientoRepository(Impl)` | Stage 1/task2 → task3, Stage 2/task2 → task3, Stage 5/task1 — sequential |
| Call sites in `feature/movimientos/**` broken by the new required fields | Stage 2/task2 (defaults: cash account + Efectivo), before Stage 4 owns the package |
| `di/RepositoryModule.kt`, `di/ViewModelModule.kt` | the active task of each stage; Stage 3 and 4 append in separate commits, rebasing in order Stage 3 → Stage 4 |
| `core/ui/navigation/*`, `feature/home/**`, `core/ui/movimiento/BalanceCard*`, `feature/profile/ui/ProfileScreen.kt` | Stage 3; `PrimaryNavigator` search-filter request for accounts: Stage 5/task1 |
| `feature/movimientos/**`, `core/data/entry/**`, `core/ui/movimiento/**` (except `BalanceCard*`) | Stage 4 |
| `core/ui/account/*` | Stage 2/task1: `CuentaTypeIcon`, `CuentaTypeLabels`, `MedioPagoIcon`, `MedioPagoLabels`; Stage 3: `CuentaBalanceRow`, `TransferenciaRowContent`; Stage 4: `MedioPagoControl`, `CuentaSelectorField`, `CuentaPickerSheet` |
| `strings.xml` (existing keys) | no stage renames existing keys; new keys go to `strings_accounts.xml` (Stages 2–3), `strings_entry_accounts.xml` (Stage 4), `strings_integration.xml` (Stage 5) in every locale |
| `MovimientoFeedback`, snackbar components, the queue in `ManageMovimientoViewModel` | Stage 0, then Stage 3/task2 commit A (extracts the queue to `core/ui/feedback/**`), which lands before Stage 4 edits `ManageMovimientoViewModel` |
| `docs/business-logic.md`, `docs/features/*` | Stage 6 only |

## Boundary matrix

| Input or failure | Expected outcome | Owner / smallest verification |
| --- | --- | --- |
| Upgrade from v6 with mock data, any currency mix | Rows intact; stamps backfilled; one cash account per present currency; medio Efectivo; balances equal previous ingresos − gastos | Stage 1/2 migration tasks / `DatabaseSchemaTest` from `6.db` and `7.db` fixtures |
| Wall clock moves backward, two mutations in the same millisecond, process restart | Stamps strictly increase | Stage 1/task1 / table-driven clock tests + reseed from stored max |
| Delete → Undo; delete → edit elsewhere → Undo; tag deleted meanwhile; category deleted meanwhile | Revive with same ID and new stamp; Conflict on differing row; surviving tags only; Otros fallback | Stage 1/task3 / real SQLDelight host tests |
| Name of a tombstoned tag/category/account reused | New live row, new ID; tombstone untouched | Stage 1/task3, Stage 2/task3 / repository tests |
| Incompatible medio/account/tipo, archived or deleted account, account in another currency on edit | Rejected by the repository with `InvalidInput`; UI never offers it | Stage 2/task3 + Stage 4 / repository and ViewModel tests |
| Change account currency while referenced; archive/delete last active account; delete referenced account | `CurrencyLocked`, `LastActive`, `InUse`; sheet shows inline errors and offers archive where applicable | Stage 2/task3 + Stage 3/task1 |
| Undo into an archived account; Undo after the account's currency changed; Undo after the account was deleted | Restored and still archived; `Conflict`; `MissingCuenta` with informational feedback | Stage 2/task3 / repository tests |
| Transfer same account, zero/negative amount, future date, cross-currency without received amount | Rejected with field errors | Stage 2/task1 rules + Stage 3/task2 form |
| Statement day 31 in April, February leap/non-leap, today equal to statement day, no statement day | Cycle boundaries as specified, inclusive civil dates | Stage 2/task1 pure tests |
| CLP account (scale 0) chosen after typing "12,50" | Save blocked with amount error; sentence-owned amount re-derived | Stage 4/task1 |
| Sentence with "con tarjeta" only, with two accounts sharing a word, with an account name that is also a tag | Ambiguity offers choices; whole-word match on full name; tag and account both detected independently | Stage 4/task2 parser tests per language |
| Large dataset (existing 2,000-row Search fixture) | Search and History reads stay within today's behaviour after the view join | Stage 5 / extend the existing scale fixture; bounded check only if the runner works |
| Long account names, enlarged text, TalkBack on medio control | Labels readable, medio announced with its name | Stage 3/4 UI tests + Stage 6 physical check |

## Verification

Per task: tests for the changed rules, `./gradlew :shared:testAndroidHostTest` (narrowed with
`--tests` while iterating), `./gradlew :shared:compileKotlinIosSimulatorArm64` for shared changes,
`./gradlew detekt ktfmtCheck`. Migration tasks also run the SQLDelight migration verification
(`verifyMigrations` runs with the build; discover the exact `verify*Migration` task from
`./gradlew :shared:tasks --all`). UI tasks add `./gradlew :androidApp:assembleDebug`.

Acceptance (Stage 6): install on the confirmed physical Xiaomi over the existing mock dataset and walk
the observable result above. iOS physical runtime remains the existing backlog item; shared Native
compilation is required, runtime is not claimed. Per [workflow](../../README.md), unexecuted checks are
reported as such.

## Out of scope (follow-ups recorded at close)

Credit-card limit, due date and payment reminders · exchange rates and FX conversion · internal
transfers from the sentence dialog · tombstone compaction and the HLC/outbox engine itself · budgets
and statistics (they will consume the account and medio dimensions) · investment/asset accounts ·
excluding an account from totals · Google login and Drive backup.
