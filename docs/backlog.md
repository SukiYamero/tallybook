# Backlog

Una fuente por ítem — se borra de acá en cuanto se resuelve. No es historial, es lo pendiente ahora.

## Alta

- **iOS end-to-end** — nunca se confirmó un build+deploy real al iPhone. Falta: login con Apple ID en
  Xcode, Developer Mode en el iPhone, completar `TEAM_ID` en `iosApp/Configuration/Config.xcconfig`, un
  Cmd+R de prueba.

## Media

- **Taxonomía detallada de errores** — `DataError` arranca con `Local`/`Unknown` en
  `docs/tasks/fundacion/wave-1/task1/task.md`; falta expandirlo (causas de red granulares, mapeo a
  mensajes localizados para el usuario) cuando exista una pantalla real que muestre errores — primera
  feature con red (Auth+Drive) o la primera con estados de error en UI (movimientos).

- **`sqldelight:coroutines-extensions` para queries reactivas** — las queries generadas devuelven
  `Query`, no `Flow`. El primer repositorio de Wave 2 que exponga una lista observable necesita
  `app.cash.sqldelight:coroutines-extensions` (`.asFlow().mapToList(dispatcher)`) en el catálogo. No se
  agregó en Wave 1 a propósito: no tenía consumidor todavía.

## Baja

- **CI (GitHub Actions)** — build + test en cada push. Se implementa cuando haya valor real que
  proteger (colaboradores o releases reales), no antes.
