# Backlog

Una fuente por ítem — se borra de acá en cuanto se resuelve. No es historial, es lo pendiente ahora.

## Alta

- **iOS physical end-to-end** — Shared production/test sources compile for Native, but physical
  iPhone runtime validation remains pending and was explicitly deferred for History. Confirm the
  authorized connected iPhone and developer services, then run the app from Xcode using the existing
  shared `TEAM_ID`. Validate Home/History, creation, lifecycle and safe areas. Compilation does not
  establish runtime behavior; follow the physical-device policy in `AGENTS.md`.

## Media

- **Taxonomía detallada de errores** — `DataError` arranca con `Local`/`Unknown` en
  `docs/tasks/fundacion/wave-1/task1/task.md`; falta expandirlo (causas de red granulares, mapeo a
  mensajes localizados para el usuario) cuando exista una pantalla real que muestre errores — primera
  feature con red (Auth+Drive) o la primera con estados de error en UI (movimientos).

- **El `actual` de `ioDispatcher` en iOS no está testeado** — `DispatcherModuleTest` vive en
  `androidHostTest` y afirma `assertNotEquals(io, default)`, que en iOS es falso (ahí `ioDispatcher` es
  `Dispatchers.Default`). Mover el test a `commonTest` requiere reescribir esa aserción.

## Baja

- **Compatibilidad de Detekt con Gradle 10** — Detekt 1.23.8 no produce hallazgos, pero su plugin
  invoca `ReportingExtension.file(String)`, API deprecada por Gradle 9 y eliminada en Gradle 10. La
  siguiente línea de Detekt todavía es 2.0 alpha; migrar cuando publique una versión estable compatible,
  antes de actualizar el wrapper a Gradle 10. Referencia:
  <https://github.com/detekt/detekt#requirements>.

- **CI (GitHub Actions)** — build + test en cada push. Se implementa cuando haya valor real que
  proteger (colaboradores o releases reales), no antes.
