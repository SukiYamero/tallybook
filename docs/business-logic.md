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
- **Opcionales**: descripción (por ahora el único campo opcional). `createdAt` es metadata técnica para
  ordenar de forma estable, no un campo solicitado al usuario.

## Moneda e idioma

Multi-moneda: COP, MXN, ARS, CLP, BRL, USD, DOP, PEN. Moneta hoy soporta COP, MXN, ARS, BRL, USD, PEN —
al portar, agregar CLP y DOP a la tabla de mapeo.

**Mecanismo** (portado de moneta, `src/lib/i18n/regionCurrency.ts` + `detectLocale.ts`): se lee el
locale del sistema una sola vez; idioma y región salen del mismo tag (`es-CO` → idioma `es`, región
`CO`); la región mapea a moneda por tabla fija (default `COP` si no matchea); el idioma determina los
strings de la UI. No hay selector de moneda al inicio — es inferido, editable después a mano.

Home recent movimientos use the primary currency before applying the six-row limit, ordered by
`fecha DESC, created_at DESC, id DESC`. Newer foreign-currency rows cannot hide eligible movements.

## Categorías

Lista predefinida (no las crea el usuario en el MVP), portada de moneta — no se retipea acá para no
tener una segunda copia que se desincronice; fuente real: `web/moneta/src/lib/schema.ts`
(`CATEGORIAS_SEMILLA`).

- 12 categorías padre, 58 en total (cada padre con 3–6 hijas). Estructura por entrada: `{id, nombre,
  padreId?, icono, color}`.
- `icono` es un nombre de Lucide — compatible directo con `icons-lucide`, ya elegido.
- `color` es un tinte con nombre (amber, blue, purple, rose, success...), no un hex — se resuelve contra
  los tokens de tema (`docs/stack.md`), no es un color fijo grabado por categoría.
- Los ingresos son una categoría padre más (`cat_ingresos`, con `cat_sueldo`/`cat_freelance`/
  `cat_ventas`/`cat_regalo`/`cat_reembolso` como hijas) — no hay un campo `tipo` en la categoría, el
  tipo (gasto/ingreso) lo determina el movimiento, no la categoría.
- Nombres de categoría traducidos por idioma en moneta (`src/lib/seedConfig.ts`,
  `SEED_CATEGORY_NAMES`) — mismo criterio a portar, ver `docs/wording.md`.

## Tracking and History

History browses movimientos with three scopes: Day, Week and Month. A year selector supplies
calendar context; Year is not a fourth scope. Search is a separate future destination. Text search,
non-date filters and charts are later capabilities and do not belong to History selection.

- Fresh History entry selects Day at today. Scope and one civil-date anchor own selection; context,
  selected chips, options and read range are derived.
- Week uses month-bound blocks 1–7, 8–14, 15–21, 22–28 and 29–month end. Blocks never cross months.
  Current-month Day and Week options stop at today; historical months are complete. Month chips
  represent full calendar months, while History's current-month read range ends at today.
- Scope changes preserve anchor. Choosing a different Week chip anchors at that block's start;
  choosing the selected chip preserves its interior anchor. A Day chip selects its exact date.
  Year/month changes and Month-chip selection preserve day-of-month where valid, clamp month-end
  and leap dates, and clamp future dates. A future month in the current year uses the current month.
- Allowed years include every year from the shared `FIRST_SELECTABLE_YEAR` (2019) through the current
  year, descending. Earlier years with persisted movimientos in the primary currency or a
  selected/restored anchor remain available, so the selection floor never hides existing records.
  Selected/restored past years remain available for the retained entry even after their last row
  disappears and another year is selected. Future years are excluded after backward date refresh.
  Fresh entries derive years again; metadata changes never reset selection.
- Each date refresh captures the clock and timezone once. An anchor equal to the previous today
  follows the new today; historical anchors stay fixed unless they become future, then clamp to
  today. Resume refresh makes newly available years visible without a background midnight timer.
- Reads use inclusive civil dates in `Movimiento.fecha`, with no UTC-instant conversion. Group by
  `fecha`, preserving `fecha DESC, created_at DESC, id DESC`; `createdAt` is only the secondary order.
- Period count, income, expense and balance describe every movimiento in the selected
  date/currency range, derived from the same bounded snapshot. Period balance is income minus
  expense. History presents only the selected period; it does not display or subscribe to a global
  balance. Home owns the global summary over all dates in the primary currency.
- Saved selection contains only schema version, scope and ISO anchor. Unknown versions/scopes or
  malformed/future anchors recover to Day/today; valid inputs recompute derived state against current
  today/currency. Repository rows and query state are never saved. Android supported saved-owner
  recreation and iOS retained-controller/saveable recreation define restoration; fresh cold launches
  use normal defaults, with no iOS cross-process persistence bridge.

## Search and filters (planned Wave 4)

Search is a separate destination over the primary currency. Its implementation contract lives in
[Wave 4](tasks/wave-4/README.md); this section owns agreed product rules, not delivery status.

- A fresh entry has an empty query, All history, all types and no selected categories. All history
  removes the date predicate; it never imposes a year floor or result limit.
- Text matches one contiguous substring in description or the displayed category name. Trim and
  lowercase with the existing supported-locale accent folding; preserve `ñ` versus `n`. Blank text
  matches everything. Internal spaces are literal; no token/fuzzy/amount/date/category-ID search.
  Unknown-category display labels are searchable.
- Nonblank text settles after 250 ms without another edit; clearing or whitespace-only text settles
  immediately. Text edits do not resubscribe to the database. Clear text keeps filters; Clear filters
  keeps raw text.
- Inclusive date presets: All; today minus six days through today; today minus 29 days through today;
  first of this month through today; January 1 through today; Custom. Relative presets resolve from
  one captured civil today/timezone on resume and Apply. Custom remains absolute.
- Custom starts with today/today. Preserve its last valid endpoints when choosing another preset.
  Normalize reversed endpoints on Apply and display the normalized range. Future input cannot apply.
  Restored malformed/future Custom input recovers to default filters. If the clock moves backward,
  keep an existing now-future Custom selection visible with an invalid-range error until corrected;
  never silently clamp it or run its invalid query.
- Date/type/category dimensions compose AND; selected IDs compose OR. No IDs means all categories;
  selecting a parent matches its own ID only. All real categories, including children, remain
  selectable regardless of type. Missing selected IDs remain visible with a localized fallback and
  explicit remove action.
- Filter edits form a draft. Apply atomically commits and closes; Back, scrim or drag discards and
  closes. Clear atomically commits default filters and closes, resetting Custom endpoints to current
  today and preserving raw search text. Apply is available for
  valid unchanged filters and zero results; its label/accessibility name contains no quantity.
- Removing an applied chip commits just that dimension. Draft edits never update results, applied
  chips or count. Closing sheets accept no edits or repeated commits.
- Rows use `fecha DESC, created_at DESC, id DESC`, description-first titles with category fallback,
  date/category subtitles and signed exact amounts. Search has no global balance or detail/edit action.
- Count derives from the exact settled result list. Pending/stale/failed work never reports an old
  count as current. No-data means no stored movements in this currency; no-results means currency
  data exists but criteria/text match none. Currency presence metadata and structural rows come from
  the same database read snapshot.
- Applied filters/raw text survive supported saved-owner recreation. Results, categories, loading,
  jobs, focus, keyboard and open/closing drafts are not saved. Tab changes retain the entry and scroll.
  Raw text never enters preferences, analytics or logs. Restoration follows the existing platform
  scope; no iOS cross-process persistence bridge is added.

## Tema

Tema claro y oscuro — de dónde salen los colores/tokens concretos, ver `docs/stack.md`.

## Alcance del MVP

Login con Google + acceso a Drive desde la primera versión usable — no es local-only. El criterio de
"anda": el usuario inicia sesión, agrega un movimiento, y persiste (local + Drive). El motor de sync
completo (HLC, outbox, resolución de conflictos) es una feature propia, no un bloqueante del primer
movimiento persistido.
