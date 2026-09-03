# Backlog

Una fuente por ítem — se borra de acá en cuanto se resuelve. No es historial, es lo pendiente ahora.

## Alta

- **iOS end-to-end** — nunca se confirmó un build+deploy real al iPhone. Falta: login con Apple ID en
  Xcode, Developer Mode en el iPhone, completar `TEAM_ID` en `iosApp/Configuration/Config.xcconfig`, un
  Cmd+R de prueba.
- **Formato de montos multi-moneda** — moneta usa `Intl.NumberFormat`; Kotlin no tiene un equivalente
  directo en `commonMain`. Sin resolver, y bloquea mostrar un monto bien formateado en la primera
  feature real (agregar/ver un movimiento).
- **PEN, ¿se mantiene o se descarta?** — moneta ya soporta PEN pero el set pedido para tallybook no lo
  incluía (pedía CLP/DOP, que moneta no tiene). Confirmar antes de portar la tabla región→moneda.

## Baja

- **CI (GitHub Actions)** — build + test en cada push. Se implementa cuando haya valor real que
  proteger (colaboradores o releases reales), no antes.
