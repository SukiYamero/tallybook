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

- **Obligatorios**: tipo (gasto / ingreso), monto, fecha, moneda y exactamente una categoría.
- **Opcionales**: tags (ver [Category and tags](#category-and-tags)), título y descripción, de hasta
  180 caracteres cada uno. El título nombra el
  movimiento (en la creación por frase es la frase escrita); la descripción agrega contexto extra si
  el usuario quiere. `createdAt` es metadata técnica para ordenar de forma estable, no un campo
  solicitado al usuario.
- **Tarjetas**: toda tarjeta de movimiento (Inicio, Historial, Buscar) muestra el título, o el nombre
  de la categoría si no hay título, en hasta dos líneas, debajo la descripción en una línea con un
  tamaño menor (sin descripción esa línea no aparece) y al final el monto. La tarjeta no muestra la fecha (Historial
  y Buscar ya agrupan por día). El texto completo se ve al abrir la tarjeta.
- **Signo del monto**: el signo va antes del símbolo de la moneda (`-$ 25,00`, `+$ 5.000,00`), el orden
  de CLDR que siguen las apps bancarias y de finanzas.
- La categoría es obligatoria en el dato, no en la interacción: si el usuario guarda una creación sin
  elegir ninguna, el movimiento queda en la categoría de sistema **Otros** y se marca como pendiente
  de completar (ver [Categorías](#categorías)).

### Natural-language entry

Creation starts from one sentence that describes the movimiento ("Gasté 20.000 en cine ayer"). The
app derives the draft from it and the user confirms or refines it; the manual form stays available.

- The entry is a centered modal dialog anchored near the top, so the keyboard never moves it. It
  opens without focus or keyboard: the user sees the input, whose placeholder is a localized example
  with an amount formatted for the primary currency, and taps it to type. The sentence respects the
  title length limit.
- Below the input, a "Detected" area appears once the sentence has text and goes away if it is
  cleared. It shows tipo and fecha (their defaults, gasto and today, until the sentence says
  otherwise), the amount when found (otherwise a note that it was not detected), a "Category"
  section and an optional "Tags" section below it. Values update while typing; nothing is persisted
  until Save. While the user has not touched the category, the sentence owns it: the high-confidence
  category shows selected and the other suggestions stay visible but dimmed. Tapping a dimmed
  suggestion switches to it; the × on the selected one clears it. The first manual choice hands the
  category to the user, and later sentences or Continue no longer replace it. Tags follow the same
  ownership: tags the sentence detected stay owned by it until the user adds or removes a tag.
- The Continue action is pinned in a footer under the scrolling content, so it stays visible with
  the keyboard open. Inside the scrolling area, below the Detected card, an "or" divider introduces
  voice dictation (a non-functional placeholder marked as coming soon), followed by a secondary
  "fill manually" action that opens the empty form. Fill manually drops everything the sentence
  derived but keeps the category and tags the user picked by hand in the dialog.
- Line breaks typed or pasted into the sentence become single spaces. Clearing the sentence also
  clears what it derived (tipo, amount, fecha, título, descripción and the sentence-owned category and
  tags), so the dialog counts as clean again even after a Continue and Back; a category and tags
  picked by hand remain.
- Tapping outside or the Back gesture closes the dialog directly while the sentence is blank;
  otherwise it asks to discard first. Continue parses the sentence once, writes tipo, monto, fecha,
  categoría and tags (each unless the user took it over) and título into the draft (replacing earlier
  derived values), closes the dialog and opens the summary. A description typed in the summary is
  kept.
- **Tipo**: expense and income verbs in every supported language decide it (`gasté`, `pagué`,
  `me cobraron` → gasto; `recibí`, `me pagaron`, `cobré`, `vendí` → ingreso). Without a verb, an
  income category suggestion selects ingreso; otherwise gasto.
- **Monto**: digits with the primary currency's group/decimal separators, a currency symbol,
  multipliers (`k`, `mil`, `millón`/`millones`, regional slang such as `lucas` or `palos`) and basic
  number words. A separator followed by exactly three digits groups; any other separator is decimal.
  The amount is always in the primary currency. When several candidates remain, the largest one wins
  and the summary flags the amount for review. No amount leaves the field empty and required; the
  dialog and the summary say it was not detected, and the summary points to Edit movimiento.
- **Fecha**: today, yesterday, the day before yesterday, a weekday (its most recent occurrence, today
  included) and explicit day/month dates (day-first except in English). A day without month resolves
  to its most recent past occurrence. Future expressions are ignored and the date stays today.
- **Título**: the sentence as typed, trimmed and with repeated whitespace collapsed, like every
  optional text. Parsing never rewrites what the user wrote. The
  description starts empty; it is optional extra context the user adds in the summary or the form.
- **Categoría**: the suggestion engine below runs on the sentence without its verb, amount, date
  and stop words. A high-confidence result is preselected; other results appear as dimmed
  suggestions that replace the selected category when tapped. Without a selection at Save, the
  movimiento falls back to Otros.
- **Tags**: the sentence selects existing tags whose full name appears in it as whole words, compared
  with the shared search normalization. Detection never creates a tag.
- The summary is a tall bottom sheet titled as a review-and-confirm step. Labeled sections show tipo
  and fecha (prominent), the amount, the title, an optional description field, the category with its
  suggestions and the optional tags. Its two
  equal-level actions, separated from the content by generous space, are Edit movimiento (left),
  which opens the complete form with the same draft, and Save (right). Save with an invalid required
  value opens the form with that field's error and focus instead of persisting. Back returns from
  the form to the summary (when reached through it); Back from the summary reopens the sentence
  dialog, preserving the draft and the typed text.
- A non-empty sentence counts as an unsaved change for the existing dismissal guard. Editing an
  existing movimiento keeps opening the form directly.

### Category suggestions

Suggestions are computed on the device from the user's own history and a built-in keyword
dictionary. No text leaves the device and no separate learning store exists: saved movimientos are
the training data, so correcting a suggestion teaches the next one.

- Text is compared after the shared search normalization (case and supported accent folding), stop
  word removal and basic plural reduction.
- Signals, strongest first: an exact match with a previous title's subject; word-to-category
  associations learned from history, weighted by frequency and recency; dictionary phrases; then
  dictionary words and brands. Phrases outweigh their words ("cambio de aceite" → Transporte).
- Ambiguous words ("aceite", "oil", "óleo", "ticket", "gas") point at several categories and never
  produce a high-confidence result on their own; when unsure, the app offers up to three of them as
  suggestions instead of guessing.
- Each past movimiento is one full vote for its category. When a clear majority of the past
  movimientos with exactly the same subject share a category, that category is the high-confidence
  suggestion.
- The dictionary covers every seeded category in all supported languages, with regional variants
  and common local brands, and points at the most specific category.
- High confidence means an exact history match or one strong dictionary match without a competing
  category. At most three suggestions are shown besides the selected category. Clearing a category
  the sentence suggested dismisses it: it is not offered again for the same subject, and a
  different subject starts clean. Otros is never suggested.

### Editing and deletion

Home, History and Search open the current persisted movimiento by ID. Missing records cannot be
edited or deleted; failed reads require Retry before mutation actions become available.

- Editing permits type, positive exact amount, civil date, category, tags, optional title and
  optional description. Stored currency, ID and `createdAt` remain fixed. Amount input round-trips through the stored
  currency's scale and decimal separator without grouping or a currency symbol.
- Creation's validation and title/description normalization also apply to editing. Civil today/timezone
  is captured at editor entry and again on Save; future dates are rejected without silently changing
  the draft when the clock moves backward.
- The category picker selects exactly one existing category, parent or child, except Otros; choosing
  one replaces the current category and closes the picker. The editor always keeps one category and
  offers no way to clear it. An existing Otros or missing category remains visible and valid when
  untouched; a replacement must exist in the loaded catalog. Category and tag updates cannot
  overwrite typed input or clear an unretried save failure.
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
signo (el tipo indica la dirección), la moneda, la categoría, su categoría principal (una categoría raíz
repite su propio nombre) y los tags unidos por " · " de cada fila. Compartir el archivo no modifica datos.

Home recent movimientos use the primary currency before applying the six-row limit, ordered by
`fecha DESC, created_at DESC, id DESC`. Newer foreign-currency rows cannot hide eligible movements.

## Categorías

Lista predefinida portada de moneta — no se retipea acá para no tener una segunda copia que se
desincronice; fuente real: `web/moneta/src/lib/schema.ts` (`CATEGORIAS_SEMILLA`). Las categorías
personalizadas son alcance aceptado en una fase posterior (abajo).

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

### Otros and pending movimientos

- **Otros** (`cat_otros`) is a seeded, localized system root with a neutral tint. It only exists as
  the creation fallback: pickers and suggestions never offer it, while Search filters list it so
  pending movimientos can be found.
- A movimiento in Otros is pending. Rows and the editor show a subtle pending treatment (dashed
  avatar and a muted "to complete" label) without blocking any action. Choosing a real category in
  the editor resolves it.

### Hierarchy and filtering

- Parents and children are both selectable. A parent means the general case ("transport, not more
  specific"); a child is the specific case.
- Filtering by a parent includes the parent and all its children. Filtering by a child includes only
  that child. Roll-up never goes upward.
- Children keep their parent's tint, so the family stays recognizable; parents and children have
  the same visual weight in chips. The hierarchy is shown as grouped families on a single level in
  the picker and in the Search filters: each family is a block with the parent first and its
  children after, every chip responds to one tap and nothing drills in. Searching flattens the
  blocks into one list of matches.

### Category and tags

- A movimiento has exactly one category: what the money was for. Category totals therefore add up to
  the overall total, and a category breakdown can be read as parts of a whole.
- A movimiento has zero to six **tags**: user-created labels that group movimientos across
  categories ("Empresa", "Viaje Chile"). Tags are flat, have only a name and keep the user's order on
  the movimiento for display. A tag can share its name with a category; they are different concepts.
- Every creation and editing surface shows a "Category" section followed by a "Tags · optional"
  section. A category shows its icon and tint; a tag is a neutral chip with a tag icon and no icon or
  tint of its own.
- The tags section shows the selected tags, each removable, and an "Add" tag that opens a
  multi-select tag picker with search; the Add tag disappears at six. A search without results offers
  to create a tag with the typed name in one tap, and the created tag becomes selected while the limit
  allows.
- Choosing a real category resolves the Otros fallback. Movimiento rows show the category only, never
  tags.

### Tags

- A tag name is trimmed, up to 40 characters and unique among tags ignoring case and supported
  accents. Creating a name that already exists offers the existing tag instead of a duplicate;
  renaming to an existing name is rejected with an inline error. Tag names are not translated.
- Profile links to a Tags screen modeled on Categories: search, a create action, and the tags sorted
  by name, each with its movimiento count in the primary currency. Tapping a tag opens its sheet with
  rename, "View movements" (opens Search filtered by that tag) and delete.
- Tags are never hidden. Deleting a tag asks for confirmation showing how many movimientos use it,
  then removes it from those movimientos and deletes it; the movimientos themselves stay and no Undo
  is offered.
- Undoing a movimiento deletion restores the tags that still exist; if its custom category was
  deleted meanwhile, the movimiento is restored in Otros.

### Custom categories

- The user creates root categories with a required name (trimmed, up to 40 characters, unique among
  all categories ignoring case and supported accents), an icon preselected by default and suggested
  from the name, an optional tint (suggested with the icon) and an optional description of up to 340
  characters. Custom names are not translated.
- Creating a name that already exists offers the existing category instead of a duplicate.
- From the movimiento picker, a search without results offers a non-intrusive action to create a
  category with the typed name; the quick form shows name and icon, with tint and description behind
  "More options", and the created category becomes the movimiento's category.
  Custom categories come first in pickers, sorted by name.
- Profile links to a dedicated Categories screen that shows chips, like the picker: search, the
  user's categories first, then the predefined ones grouped by parent, each with its movimiento count
  in the primary currency. Tapping a chip opens its sheet: the editor for a custom category, a
  read-only view for a predefined one. Both offer hide/show and "View movements", which opens Search
  filtered by that category; the custom editor also offers delete.
- Any category except Otros can be hidden. Hidden categories leave the movimiento pickers and
  suggestions but stay on existing movimientos, History and Search, and remain filterable.
- A custom category is deleted only while no movimiento in any currency uses it; when in use, the
  sheet offers hiding it instead, and no movimiento is rewritten.

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

- A fresh entry has an empty query, All history, all types and no selected categories or tags. All
  history removes the date predicate; it never imposes a year floor or result limit.
- Text matches one contiguous substring in title, description, the displayed category name or any tag
  name. Trim and
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
- Date/type/category/tag dimensions compose AND; selected IDs within a dimension compose OR. No IDs
  means no restriction in that dimension; selecting a parent category matches the parent and its
  children (see
  [Hierarchy and filtering](#hierarchy-and-filtering)). All real categories, including children, remain
  selectable regardless of type. The tags dimension appears only when the user has tags. Missing
  selected category IDs remain visible with a localized fallback and explicit remove action; a deleted
  tag leaves the applied filters.
- Filter edits form a draft. Apply atomically commits and closes; Back, scrim or drag discards and
  closes. Clear atomically commits default filters and closes, resetting Custom endpoints to current
  today and preserving raw search text. Apply is available for
  valid unchanged filters and zero results; its label/accessibility name contains no quantity.
- Removing an applied chip commits just that dimension. Draft edits never update results, applied
  chips or count. Closing sheets accept no edits or repeated commits.
- Rows use `fecha DESC, created_at DESC, id DESC`, grouped under one full-date header per day that
  has results. Rows follow the shared card rule (title with category fallback, then description) and
  show signed exact amounts. A real row opens movimiento management; Search has no global balance.
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
