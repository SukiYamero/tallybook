# Backlog

Una fuente por ítem — se borra de acá en cuanto se resuelve. No es historial, es lo pendiente ahora.

## Alta

- **iOS physical end-to-end** — Shared production/test sources compile for Native, but physical
  iPhone runtime validation remains pending and was explicitly deferred for History. Confirm the
  authorized connected iPhone and developer services, then run the app from Xcode using the existing
  shared `TEAM_ID`. Validate Home/History/Search, filters, creation (sentence dialog keyboard
  placement, review sheet, the Category section switch and single-select
  picker, the Tags section and tag picker with create), lifecycle and safe areas,
  including management scene teardown, edit/delete/Undo and VoiceOver action-window usability, and
  Profile: theme with status-bar style, the Categories screen (chips, category sheets, keyboard
  scrolling in the category form) and quick-create from the picker, the Tags screen (create, rename,
  delete, View movements), the Search tag filter, in-app language switch (`AppleLanguages`; the subtree reset returns to Home and drops retained
  screen state — hoist navigation state if that is not acceptable) and
  CSV share sheet presentation. Accounts surfaces too: the upgrade to schema 9 over existing data,
  Accounts and the account sheet (locked fields, archive, delete, View movements), the transfer form
  and Pay card with transfer Undo, the Medio and Account sections with the account picker and in-place
  creation, sentence medio/account detection, History/Search currency selector and transfer rows, the
  Search medio/account/type filters, VoiceOver announcement of a row's medio, and the installation
  node's exclusion from iOS backups.
  Precise management evidence lives in [Manage movimientos](features/manage-movimientos.md#verification-boundaries).
  Compilation does not establish runtime behavior; follow the physical-device policy in `AGENTS.md`.

## Media

- **Android automated device checks and Search scale** — The 2,000-row isolated scale fixture is prepared
  and compiled but unexecuted; dataset authorization and a usable runner remain prerequisites.
  The Xiaomi UI runner stalls before the first assertion because MIUI rejects the test APK's
  `ComponentActivity` launch (`ActivityTaskManager: Abort background activity starts`); enabling
  MIUI's "USB debugging (Security settings)" developer option on the device is the expected
  prerequisite. Prioritize runner repair before retrying, then execute the agreed bounded scale check
  with correctness, latency, responsiveness and subscription/allocation evidence. Existing manual
  Android evidence and precise limits live in [Search](features/search.md#verification-boundaries).
  Smart entry fixtures (sentence dialog, review, form), management sheet fixtures, custom categories
  fixtures (category form, Categories screen and sheets, quick-create, Profile → Categories
  navigation, sheet-form keyboard scrolling), movimiento title fixtures (Search day
  headings, signed amounts) and single category and tags fixtures (`CategorySingleFieldTest`,
  `EtiquetaTagFieldTest`, `EtiquetaPickerSheetTest`, `TagsScreenTest`, `TagsNavigationTest`, the tag
  filter in `SearchFilterSheetTest`, the catalog sections in `UiCatalogScreenTest`) and the screen snackbar lifecycle gate
  (`TallybookScreenSnackbarTest`) also compile but remain
  unexecuted after a bounded stalled-runner retry; execute them after runner repair, preserving their Back, reread-height and IME assertions.
  Accounts fixtures (`AccountsScreenTest`, `AccountsNavigationTest`, `TransferFormTest`,
  `MovimientoCuentaFieldTest`, `MovimientoRowMedioTest`, and the updated Home, History, Search, entry
  and management fixtures, including the History/Search volume fixtures with transfers) are in the
  same state: compiled, unexecuted, behind the same MIUI launch block.

- **Accounts acceptance gaps on Android** — On the confirmed Xiaomi, upgrade over the existing mock
  dataset from schema 8 to 9 (`transferencia_con_monedas`) and check History/Search transfers and the
  CSV export of accounts, medio and transfers. TalkBack (a row's medio, `CuentaBalanceRow`, the medio
  control) and enlarged-text/dark-theme passes on Accounts and the medio control are pending. The
  installation node's exclusion from auto backup and device transfer is covered only by host tests of
  the extraction rules. See [Accounts](features/accounts.md#verification-boundaries).

- **Management accessibility on Android** — Verify TalkBack announcements/focus and enlarged-text
  snackbar, Undo, Close and sheet-action reachability on a confirmed physical device. Manual Xiaomi
  main-flow checks do not establish these cases; see
  [Manage movimientos](features/manage-movimientos.md#verification-boundaries).

- **Taxonomía detallada de errores** — `DataError` arranca con `Local`/`Unknown` en
  `docs/tasks/fundacion/wave-1/task1/task.md`; falta expandirlo (causas de red granulares, mapeo a
  mensajes localizados para el usuario) cuando exista una pantalla real que muestre errores — primera
  feature con red (Auth+Drive) o la primera con estados de error en UI (movimientos).

- **El `actual` de `ioDispatcher` en iOS no está testeado** — `DispatcherModuleTest` vive en
  `androidHostTest` y afirma `assertNotEquals(io, default)`, que en iOS es falso (ahí `ioDispatcher` es
  `Dispatchers.Default`). Mover el test a `commonTest` requiere reescribir esa aserción.

## Baja

- **Smart entry polish** — Voice dictation is a placeholder (UI only). The sentence step's root is
  focusable to keep Android from auto-focusing the input, which adds an unlabeled focus stop;
  replace it during the TalkBack pass. A sheet session left open across midnight resets Fill manually
  and Discard to the session's opening date. Native-speaker review of es-AR and pt-BR entry copy is
  pending. See [Add movimiento](features/add-movimiento.md#verification-boundaries).


- **Profile polish** — A Dark/Light override on the opposite system theme shows default status-bar
  icons until the first composed frame; language options do not expose their locale to TalkBack;
  the Android export share step has no automated test (the `FileProvider` lives in `androidApp`).
  TalkBack and enlarged-text passes on Profile are pending. See [Profile](features/profile.md).

- **Unified Custom date-range picker** — Deferred by the user. Replace Search's two labeled date
  controls with a reusable range selector when prioritized. Existing API and compilation evidence,
  adaptation requirements and runtime limits live in the
  [range-picker analysis](features/search.md#custom-range-picker-feasibility).

- **Accounts follow-ups** — Not in the delivered scope, still wanted:
  - Credit cards: limit, due date and payment reminders.
  - Exchange rates and FX conversion between currencies.
  - Transfers between own accounts from the sentence dialog.
  - Budgets and statistics, which consume the account and medio dimensions.
  - Investment and asset accounts.
  - Excluding an account from totals.

- **Sync engine** — Tombstones are never purged and stamps are issued without an outbox. Tombstone
  compaction and the HLC/outbox/conflict engine itself are a separate feature after the Google + Drive
  backup ([plan](tasks/google-drive-persistence.md)).

- **Sentence amount above the cap** — The phrase parser still detects amounts above
  `Money.MAX_MINOR_UNITS`; only Save routes them to the form's amount error. Reject or flag them at
  detection so the dialog and the review do not show an amount that cannot be saved.

- **Compatibilidad de Detekt con Gradle 10** — Detekt 1.23.8 no produce hallazgos, pero su plugin
  invoca `ReportingExtension.file(String)`, API deprecada por Gradle 9 y eliminada en Gradle 10. La
  siguiente línea de Detekt todavía es 2.0 alpha; migrar cuando publique una versión estable compatible,
  antes de actualizar el wrapper a Gradle 10. Referencia:
  <https://github.com/detekt/detekt#requirements>.

- **CI (GitHub Actions)** — build + test en cada push. Se implementa cuando haya valor real que
  proteger (colaboradores o releases reales), no antes.
