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

### Editing and deletion

Home, History and Search open the current persisted movimiento by ID. Missing records cannot be
edited or deleted; failed reads require Retry before mutation actions become available.

- Editing permits type, positive exact amount, civil date, category and optional description.
  Stored currency, ID and `createdAt` remain fixed. Amount input round-trips through the stored
  currency's scale and decimal separator without grouping or a currency symbol.
- Creation's validation and description normalization also apply to editing. Civil today/timezone
  is captured at editor entry and again on Save; future dates are rejected without silently changing
  the draft when the clock moves backward.
- The picker selects existing parent categories. An existing child or missing category remains
  visible and valid when untouched; a replacement must exist in the loaded catalog. Category updates
  cannot overwrite typed input or clear an unretried save failure.
- Unsaved-change guarding compares input with its opening baseline. A valid normalized no-op performs
  no database update and closes the editor without saved feedback. A changed successful Save closes
  management; failure retains the draft for explicit retry. Saving a missing ID never inserts it.
- Selecting a movement opens its prefilled editor directly after the read succeeds. Cancel or Back
  closes the editor. Changed input first requires Continue or Discard; Continue preserves the draft,
  while Discard closes without saving. Scrim/drag uses the same draft guard. Nested selection surfaces
  close first; submission cannot be dismissed or repeated.
- Delete requires confirmation from the editor and commits immediately against the persisted row,
  without saving the draft first. Cancelling deletion retains all unsaved input. Invalid drafts and
  unavailable category catalogs do not block deletion. Pending confirmations and submission reject
  competing edits or mutations. The transaction validates that
  its captured row can be restored before deleting it; an ineligible legacy record stays intact.
  Failure keeps the editor and draft for retry. An already-missing record produces informational feedback.
- Only the exact row captured by the delete transaction is eligible for Undo. Restore preserves all
  fields and identity. An identical existing row counts as restored without writing; a different or
  unreadable same-ID row is a terminal conflict and is preserved.
- Each feedback token owns at most one deleted snapshot. Completed management outcomes wait in
  arrival order without replacing an earlier Undo. Only the visible token can claim its snapshot;
  Close or expiry releases that token's snapshot, while a fresh process or disposal of the owner
  releases the whole queue. The committed deletion remains applied until its own Undo succeeds.
  There is no persistent trash or Undo for edits.
- Undo claims the token once before restoration starts. Late action/timeout callbacks cannot consume
  a claimed operation. Restore failure retains the snapshot for Retry; success or conflict releases
  it. Primary tabs can change during restoration, but new modal sessions wait until it finishes.
- SQLDelight emissions refresh list membership, counts and balances. Mutations preserve History
  selection and Search input/filters; normal list clamping may occur when rows disappear.

Presentation, feedback windows and lifecycle ownership live in
[UI architecture](ui/architecture.md#movimiento-management-and-feedback).

## Moneda e idioma

Multi-moneda: COP, MXN, ARS, CLP, BRL, USD, DOP, PEN. Moneta hoy soporta COP, MXN, ARS, BRL, USD, PEN —
al portar, agregar CLP y DOP a la tabla de mapeo.

**Mecanismo** (portado de moneta, `src/lib/i18n/regionCurrency.ts` + `detectLocale.ts`): se lee el
locale del sistema una sola vez; idioma y región salen del mismo tag (`es-CO` → idioma `es`, región
`CO`); la región mapea a moneda por tabla fija (default `COP` si no matchea); el idioma determina los
strings de la UI. No hay selector de moneda al inicio — es inferido y se cambia después desde Perfil.
Cambiar la moneda principal no convierte montos: cada movimiento conserva su `moneda`, y Home,
Historial, Búsqueda y el default de un movimiento nuevo pasan a usar la moneda elegida.

El idioma sigue al dispositivo salvo que se fije desde Perfil (`es`, `es-AR`, `en`, `pt-BR`). Los
nombres de las categorías predefinidas siguen el idioma activo; la moneda inferida no cambia con él.

La exportación CSV desde Perfil incluye todos los movimientos de todas las monedas, con el monto sin
signo (el tipo indica la dirección) y la moneda de cada fila. Compartir el archivo no modifica datos.

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
calendar context; Year is not a fourth scope. Search is a separate destination. Text search and
non-date filters belong to Search; charts do not belong to History selection.

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

## Search and filters

Search is a separate destination over the primary currency. Its implementation contract lives in
[Search](features/search.md); this section owns agreed product rules, not delivery status.

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
  Interactive selection keeps From on or before To: From is capped at To/today, and To starts at
  From and is capped at today. Same-day ranges are valid. Reversed legacy inputs still normalize
  on Apply/restoration as a compatibility safeguard. Future input cannot apply.
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
  date/category subtitles and signed exact amounts. A real row opens movimiento management; Search
  has no global balance.
- Count derives from the exact settled result list. Pending/stale/failed work never reports an old
  count as current. No-data means no stored movements in this currency; no-results means currency
  data exists but criteria/text match none. Currency presence metadata and structural rows come from
  the same database read snapshot.
- Applied filters/raw text survive supported saved-owner recreation. Results, categories, loading,
  jobs, focus, keyboard and open/closing drafts are not saved. Tab changes retain the entry and scroll.
  Raw text never enters preferences, analytics or logs. Restoration follows the existing platform
  scope; no iOS cross-process persistence bridge is added.

## Tema

Sistema (default), claro u oscuro, elegido en Perfil y aplicado también a las barras del sistema.
De dónde salen los colores/tokens concretos: ver `docs/stack.md`.

## Alcance del MVP

The MVP includes a local-only guest mode and an optional Google-connected mode. Cloud backup requires
Google authentication and explicit Drive permission; guest data never uploads before association.
The connected acceptance flow is sign in, grant Drive, create a movimiento, and verify local plus
Drive persistence. The complete HLC/outbox/conflict-merge engine is a separate feature.

### Access, association and backup

- One primary device publishes the account's accepted backup. Retained Google-owned data can be
  created, edited, deleted and restored with Undo locally without connectivity, regardless of the
  cached primary role. Upload requires connectivity, authentication as the owner, Drive permission
  and a fresh primary-device check. A secondary must explicitly become primary before publishing.
  Taking that role preserves pending local data; it does not restore a cloud copy over it.
- Guest data and retained Google-owned data are isolated. The MVP exposes no multiple-account/profile
  switcher. Its storage ownership must nevertheless support these two access modes without mixing data.
- A Google-account setting controls local access after explicit sign-out. When enabled, the same
  retained account dataset remains locally readable and editable without login. Backup requires
  signing in again to the same account and checking current cloud ownership. When disabled, Google-owned data
  stays inaccessible until authentication; entering guest mode opens separate local data.
- Association or recovery previews available copies and requires confirmation of the complete dataset
  to keep. Apply that copy by replacement, not additive merging. Preserve a recovery copy before
  destructive replacement; new data from another stale installation must not silently overwrite the
  choice. Cancelling leaves existing data and ownership unchanged.
- Guest and accessible Google-owned datasets persist mutations without connectivity or cloud checks.
  Eligible connected sessions
  retry pending backup when connectivity returns while the app is foregrounded or on reopen;
  signed-out local access and guest mode never upload automatically. No closed-app upload is promised.
  Cloud failure does not reverse a successful local mutation or report a false backup success.
- Before publication, compare pending local data and the current accepted Drive copy against the
  last acknowledged baseline. If both changed, require confirmation of which complete copy to keep;
  replace the other only after protecting it. No automatic field/record merging or newest-wins choice.
  A new primary checks this divergence before uploading. Changed candidates invalidate a stale choice.
- Drive copies use hidden app-specific storage. Retain two normal verified backups and one protected
  recovery copy before a restoration that replaces nonempty data. Permission and local mode govern
  whether the protective copy belongs in Drive or remains only on the device.
- Guest/local-only erasure can run offline after confirmation. Erasing Google-associated local and
  Drive data requires authentication as its owner, Drive authorization, connectivity and confirmation.
  Partial deletion stays visibly incomplete and cannot be followed by automatic re-upload. The exact
  deletion boundary includes only Tallybook data; legacy Moneta data and the Google account itself
  are excluded.

Implementation contracts, unresolved device/deletion gates and delivery evidence live in the active
[Google + Drive plan](tasks/google-drive-persistence.md). These rules describe accepted scope, not
already implemented behavior.
