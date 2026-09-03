# Backlog

Una fuente por ítem — se borra de acá en cuanto se resuelve. No es historial, es lo pendiente ahora.

## Alta

- **iOS end-to-end** — nunca se confirmó un build+deploy real al iPhone. Falta: login con Apple ID en
  Xcode, Developer Mode en el iPhone, completar `TEAM_ID` en `iosApp/Configuration/Config.xcconfig`, un
  Cmd+R de prueba.

## Media

- **Taxonomía detallada de errores** — `DataError` arranca con `Local`/`Unknown` en
  `docs/tasks/fundacion.md`; falta expandirlo (causas de red granulares, mapeo a mensajes localizados
  para el usuario) cuando exista una pantalla real que muestre errores — primera feature con red
  (Auth+Drive) o la primera con estados de error en UI (movimientos).

## Baja

- **CI (GitHub Actions)** — build + test en cada push. Se implementa cuando haya valor real que
  proteger (colaboradores o releases reales), no antes.
