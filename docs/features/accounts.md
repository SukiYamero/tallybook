# Accounts, payment method and transfers

Every movimiento records an account and a payment method; Accounts shows balances and credit-card
estimates and moves money between the user's own accounts. The same delivery made every persisted
entity sync-ready with change stamps and tombstones. Rules live in business logic:
[Cuentas](../business-logic.md#cuentas), [Medio de pago](../business-logic.md#medio-de-pago),
[Transferencias](../business-logic.md#transferencias),
[Balances](../business-logic.md#balances-and-credit-card-estimate) and
[Change stamps and tombstones](../business-logic.md#change-stamps-and-tombstones). Presentation lives
in [UI architecture](../ui/architecture.md), components in the [catalog](../ui/components.md) and copy
in [wording](../wording.md#7-glosario-cuentas-medios-de-pago-y-transferencias).

## Integration

- **Schema 7 (`6.sqm`)** adds `updated_at`/`deleted_at` to `movimiento`, `categoria` and `etiqueta`,
  backfilled from `created_at` or `ChangeStamp.SEED`. **Schema 8 (`7.sqm`)** adds `cuenta` and
  `transferencia` and rebuilds `movimiento` with `cuenta_id` and `medio_pago` and without its own
  currency; `movimiento_con_etiquetas` exposes `cuenta.moneda AS monto_moneda`, so the account is the
  only owner of the currency. **Schema 9 (`8.sqm`)** adds the `transferencia_con_monedas` view (live
  transfers with both sides' currencies) and changes no table. Every migration's DDL matches a fresh
  install.
- `HybridChangeClock` stamps every repository mutation; the installation node lives in its own
  DataStore file excluded by the Android data extraction rules and from iOS backups
  (`IosBackupExclusion`).
- `CuentaRepository`, `TransferenciaRepository` and `MovimientoRepository` enforce the cross-table
  invariants in their transactions; SQLite foreign keys stay disabled. `SaldoRepository` (`Saldo.sq`,
  query-only) derives balances, totals per currency and the credit-card estimate.
  `ActividadRepository` reads movimientos and transfers in one snapshot for History and Search. The
  CSV export reads its five lists in one transaction (`DatabaseMovimientoExportSource`).
- Packages: `feature.accounts` (screen, account sheet, `QuickCuentaViewModel` for in-place creation),
  `feature.transfers` (transfer form, management, recent transfers), `core.ui.account` (shared
  account, medio and transfer components) and `core.ui.feedback` (`ManagementFeedbackViewModel` and
  the one `ManagementFeedbackQueue` shared by movimiento and transfer management).
- Entry: `core/data/entry` detects medio (`PhraseMedio`, `PhraseLexicon.medioWords`) and accounts
  (`CuentaMatcher`); `CuentaSelectionState` layers the DEFAULT/SENTENCE/USER owners over the cached
  `defaultSelection` reads (`CuentaReads`).
- Navigation: Accounts is a `secondary` destination over Home or Profile. "View movements" reaches
  Search as a one-shot `SearchFilterRequest` carrying the account and its currency.
- Strings: new keys live in `strings_accounts.xml` (accounts and transfers), `strings_entry_accounts.xml`
  (entry and editing) and `strings_integration.xml` (History, Search, CSV) in every locale;
  `strings.xml` keeps its existing keys.

## Verification boundaries

The final reviewed tree (`d67cb53`) passed 1,158 shared host tests with zero failures, Detekt,
ktfmtCheck, `verifyCommonMainTallybookDatabaseMigration`, Native compilation, Android device-test
compilation and the Android debug APK. Host tests cover the migrations from the `6.db`, `7.db` and
`8.db` snapshots against fresh schemas, the clock and stamp encoding, tombstones and revive, every row
of the medio table, account locks and deletion, transfer validation and Undo, balances and cycle
boundaries, sentence detection per language, the selection owners, History/Search activity reads and
the CSV layout. One final independent review ran; its findings were fixed.

On the Xiaomi M2012K11AG, over the existing mock dataset: the in-place upgrades 6 → 7 → 8 → 9, delete
and Undo reviving the same row with a new stamp, creation into the cash account, Accounts with a
USD → COP transfer, the History currency selector and its movement/transfer count, the Search medio
filter, View movements, and locked type and currency shown read-only in the account sheet.

Not covered:
- CSV export on the device, and TalkBack and enlarged-text passes on Accounts, the medio control and
  rows.
- iOS runtime.
- The new and updated device tests (`AccountsScreenTest`, `AccountsNavigationTest`,
  `TransferFormTest`, `MovimientoCuentaFieldTest`, `MovimientoRowMedioTest` and the updated Home,
  History, Search and entry fixtures) compile but have not run, because MIUI blocks the
  instrumentation activity launch.

Outstanding work is tracked in the [backlog](../backlog.md).
