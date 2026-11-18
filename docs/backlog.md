# Backlog

Una fuente por ítem — se borra de acá en cuanto se resuelve. No es historial, es lo pendiente ahora.

## Orden de trabajo

Qué se ataca primero; el detalle de cada ítem vive en su sección de abajo.

1. Presupuestos (sobre las consultas de Estadísticas) → *Accounts follow-ups*.
2. Accesibilidad y verificaciones pendientes en Android → *Accounts acceptance gaps*, *Management
   and Statistics accessibility*, *Profile polish*.
3. Google login + backup en Drive, cuando 1 esté cerrado → [plan](tasks/google-drive-persistence.md).
4. Avisos de recurrentes por confirmar → *Producto*.
5. Pulido chico → *Sentence amount above the cap*, *`ioDispatcher` en iOS*, *Unified Custom
   date-range picker*, *Recurrentes copy*.
6. Resto de *Accounts follow-ups* (transferencias desde la frase, FX, inversiones, excluir del total).
7. Infra → *Taxonomía de errores* (con Drive), *Sync engine* (después de Drive), *Detekt con Gradle 10*, *CI*.
8. Prueba completa en iPhone físico, en una sola pasada cuando Android esté terminado (Android es el
   objetivo primero; cada feature mantiene compilando iOS mientras tanto) → *iOS physical end-to-end*.

## Producto

- **Avisos de recurrentes por confirmar** — Notificaciones locales programadas para la próxima fecha
  de cada recurrencia (sin trabajo en segundo plano), que abren Recurrentes. Hoy los pendientes solo
  se ven al abrir la app (fila de Inicio). Ver [Recurrentes](business-logic.md#recurrentes).

- **Recurrentes copy** — A pending row reads "Pendiente desde el 15 de noviembre" for a single due
  date ("Vence el …" reads better), and the second pending row of a recurrence truncates its
  "confirm the earlier date first" note.

## Al cierre de Android

- **iOS physical end-to-end** — Shared production/test sources compile for Native, but physical
  iPhone runtime validation remains pending and was explicitly deferred for History. Before the first
  build, install the iOS 26.5 platform in Xcode › Settings › Components (Xcode 26 ships without it
  and refuses every iOS destination, including a physical iPhone; it bundles the simulator runtime,
  about 8 GB). Check first that the static Kotlin framework links Speech and AVFAudio: a missing
  link fails the whole app build, not only dictation (fix with `OTHER_LDFLAGS` in
  `iosApp/Configuration/Config.xcconfig`). Confirm the authorized connected iPhone and developer
  services, then run the app from Xcode using the existing shared `TEAM_ID`. Validate Home/History/Search, filters, creation (sentence dialog keyboard
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
  node's exclusion from iOS backups. Statistics too: tab, Home card, scopes, dimensions, category sheet
  with View movements, and the Vico charts' animation and column taps. Voice dictation too, with its
  own plan in [tasks/voice-dictation.md](tasks/voice-dictation.md).
  Precise management evidence lives in [Manage movimientos](features/manage-movimientos.md#verification-boundaries).
  Compilation does not establish runtime behavior; follow the physical-device policy in `AGENTS.md`.

## Media

- **Search scale budgets** — The instrumented suite runs on the Xiaomi (runner recipe in
  [AGENTS.md](../AGENTS.md#2-comandos--exactos-con-flags)): 1228/1228 pass. `SearchVolumeDeviceTest`
  seeds 2,000 movimientos and 200 transfers in test-owned storage and asserts complete row counts,
  scroll checkpoints, bottom-bar clearance and subscription start/stop counts. It only records latency
  and memory (last run: reads 14–67 ms, filter settle 154–268 ms, rapid text 2.7 ms on the main queue,
  Java heap 38–49 MB, PSS 199–243 MB); pending is agreeing on budgets for them and asserting them.

- **Accounts acceptance gaps on Android** — On the confirmed Xiaomi, check the CSV export of
  accounts, medio and transfers. TalkBack (a row's medio, `CuentaBalanceRow`, the medio
  control) and enlarged-text/dark-theme passes on Accounts and the medio control are pending. The
  installation node's exclusion from auto backup and device transfer is covered only by host tests of
  the extraction rules. See [Accounts](features/accounts.md#verification-boundaries).

- **Management and Statistics accessibility on Android** — Verify TalkBack announcements/focus and
  enlarged-text snackbar, Undo, Close and sheet-action reachability on a confirmed physical device.
  Manual Xiaomi main-flow checks do not establish these cases; see
  [Manage movimientos](features/manage-movimientos.md#verification-boundaries). Statistics needs the
  same passes: TalkBack on the chart summaries and the category sheet, and enlarged text on its cards
  and rows, plus a light-theme and es-AR/pt-BR visual pass
  (only dark theme in English was checked). TalkBack reads change labels like "▲ 12%" literally.

- **Taxonomía detallada de errores** — `DataError` arranca con `Local`/`Unknown` en
  `docs/tasks/fundacion/wave-1/task1/task.md`; falta expandirlo (causas de red granulares, mapeo a
  mensajes localizados para el usuario) cuando exista una pantalla real que muestre errores — primera
  feature con red (Auth+Drive) o la primera con estados de error en UI (movimientos).

- **El `actual` de `ioDispatcher` en iOS no está testeado** — `DispatcherModuleTest` vive en
  `androidHostTest` y afirma `assertNotEquals(io, default)`, que en iOS es falso (ahí `ioDispatcher` es
  `Dispatchers.Default`). Mover el test a `commonTest` requiere reescribir esa aserción.

## Baja

- **Search query-change scroll** — `SearchRoute` resets the list with `scrollToItem(0)` when the
  query changes; like the first-entry focus bug, it can wait forever if the list was never measured
  (a restored Search showing a changed query). Switch it to `requestScrollToItem(0)`.

- **Smart entry polish** — The iOS microphone and speech-recognition prompts are English only: localizing
  them needs an InfoPlist string catalog added to the target in Xcode. The sentence step's root is
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
  - Exchange rates and FX conversion between currencies.
  - Transfers between own accounts from the sentence dialog.
  - Budgets, built on the Statistics queries (`StatisticsRepository`) and their account and medio
    dimensions.
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
