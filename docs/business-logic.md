# Business logic — Tallybook

Única referencia de reglas de negocio del dominio, compartidas por Android e iOS vía `shared/commonMain`.
Se actualiza en el lugar a medida que la lógica cambia — no se duplica en otro `.md` ni se versiona por
decisión histórica (eso lo cubre git).

## Qué es la app

App de finanzas personales tipo budget/wallet para Android e iOS. El objetivo es que el usuario entienda
mejor sus finanzas — en qué gastó, cuánto ingresó, cómo queda su balance — no reemplaza un banco ni
sincroniza cuentas bancarias reales.

## Movimiento (gasto o ingreso)

Un **movimiento** registra un gasto o un ingreso.

- **Obligatorios**: tipo (gasto / ingreso), monto, fecha, categoría, moneda.
- **Opcionales**: descripción (por ahora el único campo opcional).

## Moneda e idioma

Multi-moneda: COP, MXN, ARS, CLP, BRL, USD, DOP, PEN. Moneta hoy soporta COP, MXN, ARS, BRL, USD, PEN —
al portar, agregar CLP y DOP a la tabla de mapeo.

**Mecanismo** (portado de moneta, `src/lib/i18n/regionCurrency.ts` + `detectLocale.ts`): se lee el
locale del sistema una sola vez; idioma y región salen del mismo tag (`es-CO` → idioma `es`, región
`CO`); la región mapea a moneda por tabla fija (default `COP` si no matchea); el idioma determina los
strings de la UI. No hay selector de moneda al inicio — es inferido, editable después a mano.

## Categorías

Lista predefinida (no las crea el usuario en el MVP), portada de moneta — no se retipea acá para no
tener una segunda copia que se desincronice; fuente real: `web/moneta/src/lib/schema.ts`
(`CATEGORIAS_SEMILLA`).

- 12 categorías padre, ~57 en total (cada padre con 3–6 hijas). Estructura por entrada: `{id, nombre,
  padreId?, icono, color}`.
- `icono` es un nombre de Lucide — compatible directo con `icons-lucide`, ya elegido.
- `color` es un tinte con nombre (amber, blue, purple, rose, success...), no un hex — se resuelve contra
  los tokens de tema (`docs/stack.md`), no es un color fijo grabado por categoría.
- Los ingresos son una categoría padre más (`cat_ingresos`, con `cat_sueldo`/`cat_freelance`/
  `cat_ventas`/`cat_regalo`/`cat_reembolso` como hijas) — no hay un campo `tipo` en la categoría, el
  tipo (gasto/ingreso) lo determina el movimiento, no la categoría.
- Nombres de categoría traducidos por idioma en moneta (`src/lib/seedConfig.ts`,
  `SEED_CATEGORY_NAMES`) — mismo criterio a portar, ver `docs/wording.md`.

## Tracking y consulta

- Ver y filtrar movimientos por día, semana, mes o año.
- Buscar movimientos dentro de esos rangos de fecha.
- Balance general (ingresos − gastos) siempre visible.
- Vista de gráfico semanal de movimientos/balance.

## Tema

Tema claro y oscuro — de dónde salen los colores/tokens concretos, ver `docs/stack.md`.

## Alcance del MVP

Login con Google + acceso a Drive desde la primera versión usable — no es local-only. El criterio de
"anda": el usuario inicia sesión, agrega un movimiento, y persiste (local + Drive). El motor de sync
completo (HLC, outbox, resolución de conflictos) es una feature propia, no un bloqueante del primer
movimiento persistido.
