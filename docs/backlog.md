# Backlog

Una fuente por ítem — se borra de acá en cuanto se resuelve. No es historial, es lo pendiente ahora.

## Alta

- **Formato de montos multi-moneda** — la primera feature a arrancar. Moneta usa `Intl.NumberFormat`;
  Kotlin no tiene un equivalente directo en `commonMain`. Bloquea mostrar un monto bien formateado en
  la primera feature real (agregar/ver un movimiento).
- **iOS end-to-end** — nunca se confirmó un build+deploy real al iPhone. Falta: login con Apple ID en
  Xcode, Developer Mode en el iPhone, completar `TEAM_ID` en `iosApp/Configuration/Config.xcconfig`, un
  Cmd+R de prueba.

## Baja

- **CI (GitHub Actions)** — build + test en cada push. Se implementa cuando haya valor real que
  proteger (colaboradores o releases reales), no antes.
