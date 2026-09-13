# Backlog

Una fuente por ítem — se borra de acá en cuanto se resuelve. No es historial, es lo pendiente ahora.

## Alta

- **iOS end-to-end** — `commonMain` y los tests compilan para `iosSimulatorArm64`, pero este host no
  tiene runtimes ni simuladores instalados en Xcode, por lo que `iosSimulatorArm64Test` todavía no se
  puede ejecutar. Falta: instalar un runtime iOS y correr ese test; luego, para el dispositivo físico,
  iniciar sesión con Apple ID en Xcode, habilitar Developer Mode en el iPhone, completar `TEAM_ID` en
  `iosApp/Configuration/Config.xcconfig` y ejecutar con Cmd+R.

## Media

- **Taxonomía detallada de errores** — `DataError` arranca con `Local`/`Unknown` en
  `docs/tasks/fundacion/wave-1/task1/task.md`; falta expandirlo (causas de red granulares, mapeo a
  mensajes localizados para el usuario) cuando exista una pantalla real que muestre errores — primera
  feature con red (Auth+Drive) o la primera con estados de error en UI (movimientos).

- **El `actual` de `ioDispatcher` en iOS no está testeado** — `DispatcherModuleTest` vive en
  `androidHostTest` y afirma `assertNotEquals(io, default)`, que en iOS es falso (ahí `ioDispatcher` es
  `Dispatchers.Default`). Mover el test a `commonTest` requiere reescribir esa aserción.

- **`sqldelight:coroutines-extensions` para queries reactivas** — las queries generadas devuelven
  `Query`, no `Flow`. El primer repositorio de Wave 2 que exponga una lista observable necesita
  `app.cash.sqldelight:coroutines-extensions` (`.asFlow().mapToList(dispatcher)`) en el catálogo. No se
  agregó en Wave 1 a propósito: no tenía consumidor todavía.

## Baja

- **Compatibilidad de Detekt con Gradle 10** — Detekt 1.23.8 no produce hallazgos, pero su plugin
  invoca `ReportingExtension.file(String)`, API deprecada por Gradle 9 y eliminada en Gradle 10. La
  siguiente línea de Detekt todavía es 2.0 alpha; migrar cuando publique una versión estable compatible,
  antes de actualizar el wrapper a Gradle 10. Referencia:
  <https://github.com/detekt/detekt#requirements>.

- **CI (GitHub Actions)** — build + test en cada push. Se implementa cuando haya valor real que
  proteger (colaboradores o releases reales), no antes.
