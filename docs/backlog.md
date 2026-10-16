# Backlog

Una fuente por ítem — se borra de acá en cuanto se resuelve. No es historial, es lo pendiente ahora.

## Alta

- **iOS physical end-to-end** — Shared production/test sources compile for Native, but physical
  iPhone runtime validation remains pending and was explicitly deferred for History. Confirm the
  authorized connected iPhone and developer services, then run the app from Xcode using the existing
  shared `TEAM_ID`. Validate Home/History/Search, filters, creation, lifecycle and safe areas,
  including management scene teardown, edit/delete/Undo and VoiceOver action-window usability, and
  Profile: theme with status-bar style, in-app language switch (`AppleLanguages`; the subtree reset returns to Home and drops retained
  screen state — hoist navigation state if that is not acceptable) and
  CSV share sheet presentation.
  Precise management evidence lives in [Manage movimientos](features/manage-movimientos.md#verification-boundaries).
  Compilation does not establish runtime behavior; follow the physical-device policy in `AGENTS.md`.

## Media

- **Android automated device checks and Search scale** — The 2,000-row isolated scale fixture is prepared
  and compiled but unexecuted; dataset authorization and a usable runner remain prerequisites.
  The Xiaomi UI runner stalled before assertions (0/20 completed) and was stopped after bounded
  diagnosis. Prioritize runner repair before retrying, then execute the agreed bounded scale check
  with correctness, latency, responsiveness and subscription/allocation evidence. Existing manual
  Android evidence and precise limits live in [Search](features/search.md#verification-boundaries).
  Management sheet fixtures also compile but remain unexecuted after a bounded stalled-runner retry;
  execute them after runner repair, preserving their Back, reread-height and IME assertions.

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

- **Profile polish** — A Dark/Light override on the opposite system theme shows default status-bar
  icons until the first composed frame; language options do not expose their locale to TalkBack;
  the Android export share step has no automated test (the `FileProvider` lives in `androidApp`).
  TalkBack and enlarged-text passes on Profile are pending. See [Profile](features/profile.md).

- **Unified Custom date-range picker** — Deferred by the user. Replace Search's two labeled date
  controls with a reusable range selector when prioritized. Existing API and compilation evidence,
  adaptation requirements and runtime limits live in the
  [range-picker analysis](features/search.md#custom-range-picker-feasibility).

- **Compatibilidad de Detekt con Gradle 10** — Detekt 1.23.8 no produce hallazgos, pero su plugin
  invoca `ReportingExtension.file(String)`, API deprecada por Gradle 9 y eliminada en Gradle 10. La
  siguiente línea de Detekt todavía es 2.0 alpha; migrar cuando publique una versión estable compatible,
  antes de actualizar el wrapper a Gradle 10. Referencia:
  <https://github.com/detekt/detekt#requirements>.

- **CI (GitHub Actions)** — build + test en cada push. Se implementa cuando haya valor real que
  proteger (colaboradores o releases reales), no antes.
