# Custom categories (phase 3 of categories)

## Context

Users can only pick from the 59 seeded categories. This feature lets them create their own root
categories, manage every category from a dedicated screen and hide predefined ones they never use.
Product rules: [business-logic.md](../../business-logic.md#custom-categories-phase-3-accepted).

## Scope

Included:

- Create and edit custom root categories: name, icon, optional tint, optional description.
- Quick creation from the multi-select picker when a search has no results.
- A Categories screen reached from Profile: search, custom and predefined categories with
  movimiento counts, edit, hide/show, delete, and tap-to-Search.
- Hidden categories leave pickers and suggestions but keep showing on existing movimientos.
- Migration `3.sqm` (schema 3 → 4).

Excluded: child categories under custom roots, moving or renaming predefined categories, matching
custom names in the sentence parser or keyword dictionary (custom categories are suggested only from
learned history), CSV export changes, Drive sync, iPhone runtime validation (deferred, as for smart
entry), voice dictation.

## Decisions

| Topic | Decision |
| --- | --- |
| Delete a custom category | Allowed only while no movimiento in any currency references it; it is then deleted after a confirmation dialog. When in use, Delete is replaced by "Hide instead" with the count shown. No movimiento is rewritten. |
| Hide | Any category except `cat_otros`, custom or predefined, with or without movimientos. Hidden categories leave every movimiento picker and the category suggestions, stay on existing movimientos, History and Search results, and remain filterable in Search. Hiding is per category; a hidden parent does not hide its children. |
| Identity | Custom ids are random UUID strings (`IdGenerator`), never `cat_*`, so `ensureSeeded`/`localizeSeeded` cannot collide. |
| Origin | `origen` column: `sistema` (seeded) or `usuario` (custom). `localizeSeeded` only rewrites `sistema` rows. |
| Name | Trimmed, 1–40 UTF-16 chars (the field caps input at 40). Uniqueness compares `normalizeSearchText` keys against every stored category name, hidden ones included, at write time. A later language change can make a custom name equal a localized seed name; that is accepted, no runtime renaming. |
| Duplicate | Write returns the existing category instead of inserting. Quick-create offers "Use <name>" (selecting it, and showing it first if hidden). Categories screen offers "Open <name>" (scrolls to and highlights the row; opening its editor when it is custom). A duplicate of `cat_otros` shows the duplicate message with no action, because Otros is never selectable. |
| Icon/tint suggestion | From the name via `CategoryKeywordDictionary.builtIn` evidence: the best-scoring seed id gives its icon and tint. No match gives `TAG` (new Lucide icon) with `NEUTRAL`. Suggestion follows the name until the user picks an icon or tint by hand; each field stops following independently. |
| Description | Optional, trimmed, up to 340 chars; blank is stored as `NULL`. Shown under the name in the Categories screen and editable there; not shown in pickers. |
| Counts | Distinct movimientos in the primary currency carrying the category, a parent including its children: the same set Search shows for that single category filter over All history. |
| Ordering | Custom categories sort by normalized name. In pickers they form one block shown before the predefined families; in the Categories screen they are the first section. |
| Picker selection limit | When 6 categories are already selected the quick-create action is still offered; the category is created but not selected, and the existing limit hint stays visible. |
| Navigation | `Categories` is a secondary destination pushed over Profile like `UiCatalog` (bottom bar hidden); Back returns to Profile. `catalogOpen` is generalized into one `secondary` slot instead of adding a second flag. Tapping a category selects Search with a one-shot request that resets Search to an empty query, All history, all types and only that category. Back from Search follows the existing root rule (Home). |

## Contracts

Data (`Categoria.sq`, migration `3.sqm`):

```sql
ALTER TABLE categoria ADD COLUMN descripcion TEXT;
ALTER TABLE categoria ADD COLUMN origen TEXT NOT NULL DEFAULT 'sistema'
    CHECK (origen IN ('sistema', 'usuario'));
ALTER TABLE categoria ADD COLUMN oculta INTEGER NOT NULL DEFAULT 0 CHECK (oculta IN (0, 1));
```

Existing positional `insertCategoria`/`insertCategoriaIfAbsent` keep their five columns, so seeding
and existing fixtures keep compiling; new named queries handle custom rows.

Domain (`core.model`):

```kotlin
enum class CategoriaOrigen { SISTEMA, USUARIO }
data class Categoria(id, nombre, padreId, icono, color,
    descripcion: String? = null, origen: CategoriaOrigen = SISTEMA, oculta: Boolean = false)
data class CategoriaDraft(nombre: String, icono: CategoryIcon, color: CategoryTint, descripcion: String?)
sealed interface CategoriaWrite { Saved(categoria) ; Duplicate(existing) }
object CategoriaRules { NOMBRE_MAX = 40; DESCRIPCION_MAX = 340; fun nombreKey(nombre): String }
sealed interface CategoriaDelete { Deleted ; InUse(count: Long) }
```

Repository (`CategoriaRepository`, additions):

```kotlin
suspend fun create(draft: CategoriaDraft): DataOutcome<CategoriaWrite>
suspend fun update(id: String, draft: CategoriaDraft): DataOutcome<CategoriaWrite>   // usuario only
suspend fun setOculta(id: String, oculta: Boolean): DataOutcome<Unit>               // never cat_otros
suspend fun delete(id: String): DataOutcome<CategoriaDelete>                        // usuario only
fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>>
```

Validation, duplicate check and write run in one transaction. The UI validates first; the
repository still rejects a blank/too-long name or description and invalid targets (seed
edit/delete, hiding Otros, unknown id) with `DataError.InvalidInput`, never a silent no-op.

## Stages

- **Wave 1** (parallel): [task1 data layer](wave-1/task1/task.md) ·
  [task2 appearance suggester](wave-1/task2/task.md). Exit gate (orchestrator):
  `verifySqlDelightMigration` and the 3 → 4 migration test pass before Wave 2 starts.
- **Wave 2**: [task3 category form and copy](wave-2/task3/task.md).
- **Wave 3** (parallel): [task4 picker quick-create and hidden filtering](wave-3/task4/task.md) ·
  [task5 Categories screen](wave-3/task5/task.md).
- **Final**: one independent code review of the full diff, full checks, Xiaomi smoke, docs
  compaction (`business-logic.md` drops "open decision", `features/` doc, registry row, backlog item).

## Verification

- **Checks**: `./gradlew detekt ktfmtCheck`, `:shared:verifySqlDelightMigration`,
  `:shared:testAndroidHostTest`, iOS compile (`:shared:compileKotlinIosSimulatorArm64`),
  `:shared:compileAndroidDeviceTest` (task name resolved at run time), `:androidApp:assembleDebug`.
  Prefix Gradle with `ANDROID_HOME=/Users/sukiyamero/Library/Android/sdk` (no `local.properties`).
- **APIs sensitive to version/platform**: SQLDelight 2.3.2 `ALTER TABLE ... ADD COLUMN ... CHECK`
  in migrations (verified by `verifySqlDelightMigration` against the generated `4.db`); Navigation 3
  secondary entry and saved `PrimaryNavigationState` (existing `DestinationSerializationTest`
  extended); Compose resources for new strings in the four locales.
- **Integration contract**: `DatabaseSchemaTest` migrates a copy of `3.db` filled with seeded
  categories, movimientos and multi-category links, asserting rows, defaults (`sistema`, `0`, `NULL`)
  and that the schema matches a fresh `Schema.create`. A repository test creates a custom category,
  links it to a movimiento, and checks `delete` returns `InUse` and `localizeSeeded` leaves it alone.
- **Runtime on Xiaomi Mi 11X (`aaa9cea2`)**, `adb install -r` over the installed schema-3 build so
  the real upgrade path runs (never uninstall or clear data). Smoke: app opens with existing data
  intact; quick-create "GYM" from the picker gets the dumbbell icon and is selected; create, edit and
  delete an unused custom category from Categories; hide a used predefined category (gone from the
  picker, still on its movimientos and filterable); tap a category opens Search filtered; Back
  returns to Profile from Categories. iPhone runtime is not claimed.
