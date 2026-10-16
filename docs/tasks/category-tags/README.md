# Single category + tags

> **For implementing subagents:** run one `taskN/task.md` per fresh context. Read this README, the
> stage `tasks.md`, your task and `AGENTS.md` before touching code. Challenge the spec instead of
> implementing something that does not close. One owner per file: never edit a file owned by another
> task of the same stage.

## Context

A movimiento currently has one to six equal categories, so category totals overlap and categories
cannot be read as parts of a whole. This feature separates the two concepts that
were mixed: **exactly one categoría** (what the money was for; exclusive, sums to 100%) and **zero or
more tags** (cross-cutting labels the user creates, such as "Empresa" or "Viaje Chile").

Accepted product rules already live in `docs/business-logic.md` (§ Category and tags, § Tags); this
spec owns only the implementation contracts.

## Accepted scope

Confirmed with the user:

1. A movimiento has exactly one categoría. Multiple categories, the six-category limit and the
   parent/child coexistence rule disappear. Hierarchy, hidden categories, the Otros fallback (with the
   pending state) and filter roll-up stay.
2. Custom categories keep today's create/edit/hide rules. Delete keeps today's rule: only while no
   movimiento uses it; otherwise the sheet offers hiding.
3. A movimiento has 0..`MAX_ETIQUETAS_POR_MOVIMIENTO` optional tags. Tags are user-created, flat and
   can always be deleted (deleting unlinks them from movimientos; the movimientos stay).
4. Tags get their own management screen from Profile, modeled on the Categories screen.
5. Visual identity: a categoría shows its icon and tint; a tag is a neutral chip with a tag/`#` icon
   and no selectable icon or color. Sections are labeled "Categoría" and "Tags · opcional" everywhere
   both appear.
6. Creation/editing: the category area offers suggestions that behave like a switch: choosing one
   selects it and dims the others (still tappable to switch). A "Tags · opcional" section below allows
   multi-select and one-tap creation.
7. Natural-language entry detects tags only by matching names of existing tags; it never creates
   tags.
8. Migration: each existing movimiento keeps its first category (lowest `posicion`); the other
   categories are discarded. A movimiento with no category link gets Otros. Device data is mock.

## Proposed defaults (flag to the user if an implementer disagrees)

- `MAX_ETIQUETAS_POR_MOVIMIENTO = 6` (reuses the existing UI limit behavior).
- Tag name: trimmed, 1..40 characters (`CategoriaRules.NOMBRE_MAX`), unique among tags by the same
  `nombreKey()` normalization; a tag may share a name with a categoría. No description, icon, tint or
  hidden flag.
- Creating a duplicate tag name offers the existing tag; renaming to a duplicate shows an inline error.
- Deleting a tag asks for confirmation showing how many movimientos use it; no Undo.
- Tags are user-ordered on a movimiento (display order kept); management lists sort by name.
- Search: tag filter is OR within tags and AND with category and every other filter. Text search also
  matches tag names. Rows do not render tags.
- CSV keeps one category column and the parent column, and adds a `Tags` column joined by `" · "`.
- In the sentence dialog and summary the user may deselect the category (back to the Otros pending
  fallback); the editor of an existing movimiento always keeps one category.

## Domain contracts (owned by Stage 1)

```kotlin
// core/model
const val MAX_ETIQUETAS_POR_MOVIMIENTO: Int = 6

data class Movimiento(
    /* existing fields minus categoriaIds */
    val categoriaId: String,
    val etiquetaIds: List<String>, // 0..MAX, distinct, display order
)

data class Etiqueta(val id: String, val nombre: String, val createdAt: Instant)

sealed interface EtiquetaWrite { data class Saved(val etiqueta: Etiqueta); data class Duplicate(val existing: Etiqueta) }

// core/data
interface EtiquetaRepository {
    fun observeAll(): Flow<DataOutcome<List<Etiqueta>>>
    fun observeMovimientoCounts(moneda: Moneda): Flow<DataOutcome<Map<String, Long>>>
    suspend fun create(nombre: String): DataOutcome<EtiquetaWrite>
    suspend fun rename(id: String, nombre: String): DataOutcome<EtiquetaWrite>
    suspend fun countMovimientos(id: String): DataOutcome<Long>
    suspend fun delete(id: String): DataOutcome<Unit> // unlinks, then deletes, in one transaction
}
```

`MovimientoChanges`, `MovimientoReadCriteria` (adds `etiquetaIds: Set<String>`),
`CategoryHistoryEntry` and `MovimientoDraft` (lives in
`feature/movimientos/viewmodel/AddMovimientoUiState.kt`) follow the same single `categoriaId` +
`etiquetaIds` shape. Follow the existing `DataOutcome`/`DataError` conventions of
`CategoriaRepository` (counts are `Long`); exact signatures may adapt to them.

**Dependency wiring is frozen by Stage 1:** `EtiquetaRepository` is already injected into
`AddMovimientoRepositories`, `ManageMovimientoViewModel`, `SearchRepositories` and
`ProfileExportDependencies`, registered in every test Koin module, and available as
`UnsupportedEtiquetaRepository` + an in-memory fake in `commonTest`. Stage 2 never changes those
constructors or test modules.

## Persistence (owned by Stage 1)

Migration `5.sqm` → schema 6, following the rebuild-and-rename pattern of `2.sqm`/`4.sqm`, in this
order:

1. `DROP VIEW movimiento_con_categorias` (a table rename rewrites dependent views).
2. `ALTER TABLE movimiento RENAME TO movimiento_legacy`.
3. Create the new `movimiento` with every current column and `categoria_id TEXT NOT NULL` **last**
   (same order in `Movimiento.sq`).
4. `INSERT … SELECT` from `movimiento_legacy`, with
   `COALESCE((SELECT categoria_id FROM movimiento_categoria WHERE movimiento_id = movimiento_legacy.id
   ORDER BY posicion, categoria_id LIMIT 1), 'cat_otros')` (`CATEGORIA_OTROS_ID`).
5. `DROP INDEX movimiento_recent`, drop `movimiento_legacy`.
6. Drop index `movimiento_categoria_categoria` and table `movimiento_categoria`.
7. Recreate `movimiento_recent`; create index `movimiento_by_categoria` on `movimiento(categoria_id)`
   (not `movimiento_categoria`: SQLite shares one namespace for tables and indexes).
8. Create `etiqueta(id TEXT PRIMARY KEY, nombre TEXT NOT NULL, created_at TEXT NOT NULL)` (ISO text,
   like `movimiento.created_at`), `movimiento_etiqueta(movimiento_id, etiqueta_id, posicion INTEGER
   NOT NULL, PRIMARY KEY(movimiento_id, etiqueta_id))` and index `movimiento_etiqueta_etiqueta`.
9. Create view `movimiento_con_etiquetas` with the same `group_concat`/`char(30)`/`char(31)` encoding,
   exposed as `etiqueta_ids`; the private `MovimientoMapper.toOrderedCategoriaIds` becomes a neutral
   ordered-ids parser.

- `.sq` files match the migrated DDL exactly. Commands:
  `./gradlew :shared:generateCommonMainTallybookDatabaseSchema` (writes `databases/6.db`) and
  `./gradlew :shared:verifyCommonMainTallybookDatabaseMigration`.
- Filtering with two optional sets: four named queries (none / category / tag / both) selected in
  `MovimientoRepositoryImpl.observeFiltered`, extending today's two-query switch. SQLDelight `IN :list`
  cannot express "no filter".
- SQLite foreign keys stay disabled: repositories own link integrity.
- Undo restore: if the snapshot's category no longer exists (custom category deleted after the
  movimiento was deleted), restore with Otros; tag ids that no longer exist are skipped.

## Copy

New keys in all four `strings.xml` (`values` = es default, `values-en`, `values-es-rAR`,
`values-pt-rBR`), written per `docs/wording.md`. Prefix `tag_`/`tags_`; "Etiqueta" is already an icon
label (`category_icon_tag`). The user-facing term is **Tag/Tags** in every locale. Each key mirrors the
named category key's structure (format args, plural form).

| Key | Mirrors | es default |
| --- | --- | --- |
| `csv_header_tags` (**Stage 1 / task1**) | `csv_header_category` | Tags |
| `movimiento_category_section` | — | Categoría |
| `movimiento_tags_section` | — | Tags · opcional |
| `tag_add`, `tag_add_description` | `category_tag_add*` | Agregar / Agregar tag |
| `tag_remove` | `category_tag_remove` | Quitar %1$s |
| `tag_picker_title` | `category_picker_title` | Elige tags |
| `tag_picker_create` | `category_picker_create` | Crear tag “%1$s” |
| `tag_picker_limit_reached` | `category_picker_limit_reached` | Llegaste al máximo de %1$d tags |
| `tag_picker_done` | `category_picker_done` | Listo (%1$d) |
| `category_picker_single_title` | `category_picker_title` | Elige una categoría |
| `tags_unavailable` | `movimiento_categories_error` | — |
| `profile_tags_label`, `profile_tags_body` | `profile_categories_*` | Tags / Agrupa movimientos de distintas categorías, como “Empresa”. |
| `tags_title`, `tags_empty`, `tags_search_empty`, `tags_load_error` | `categories_*` | — |
| `tag_new_title`, `tag_edit_title`, `tag_open` | `category_new_title`, `category_edit_title`, `category_open` | — |
| `tag_name_label`, `tag_name_placeholder`, `tag_name_blank`, `tag_name_too_long` | `category_form_name_*` | — |
| `tag_save`, `tag_save_error`, `tag_created`, `tag_updated` | the category sheet save action, `category_save_error`, `category_created`, `category_updated` | — |
| `tag_duplicate_message`, `tag_duplicate_open` | `category_duplicate_message`, `category_duplicate_use` | — |
| `tag_movimiento_count` (plural), `tag_no_movimientos` | `category_movimiento_count` | — |
| `tag_view_movimientos_count` (plural) | `category_view_movimientos_count` | — |
| `tag_delete`, `tag_delete_title`, `tag_delete_message` (plural, N uses), `tag_delete_message_unused`, `tag_deleted`, `tag_delete_error` | `category_delete*` | — |
| `search_tags_label` | `search_category_label` | Tags |

Applied Search tag chips reuse `search_remove_filter`. "—" means the implementer writes it following
the mirrored key and `docs/wording.md`. Every key except `csv_header_tags` is added by Stage 1 / task2;
Stage 2 never edits `strings.xml`. Obsolete multi-category keys are removed in Stage 3 together with
their last consumer.

## Stages

- [**Stage 1 — Contract switch**](stage-1/tasks.md) (sequential; intermediate review: migration and
  shared contracts block everything else).
- [**Stage 2 — Feature surfaces**](stage-2/tasks.md) (three tasks with disjoint file ownership;
  authoring in parallel, Gradle and device runs serialized by the orchestrator).
- [**Stage 3 — Integration and close**](stage-3/tasks.md) (catalog, copy cleanup, docs, final
  independent review, physical-device acceptance).

## Requirement coverage

| Requirement | Task |
| --- | --- |
| 1 single category, 8 migration, 2 category delete-while-unused | Stage 1 / task1 |
| 3 tag data, CSV, search criteria, DI wiring | Stage 1 / task1 |
| 5 tag visuals, single-select picker, switch suggestions | Stage 1 / task2 |
| 4 Tags screen, tag delete, Profile entry | Stage 2 / task1 |
| 6, 7 creation/editor UX, tag detection | Stage 2 / task2 |
| Search tag filter, rows without "+N" | Stage 2 / task3 |
| Docs, catalog, copy cleanup, acceptance | Stage 3 / task1 |

## Verification

- **Migration contract:** `DatabaseSchemaTest` migrates a schema-5 database with 0, 1 and 3 linked
  categories per movimiento and asserts kept-first / Otros / discarded, tag tables empty, view readable
  and `verifyMigrations` green.
- **Integration contract (Stage 1 / task1, `CategoryTagsIntegrationTest`):** a host test creates a movimiento with one category and two
  tags through the real repositories, filters it by tag and by parent category (roll-up), deletes one
  tag and observes the movimiento with the remaining tag.
- **Runtime by target:** Xiaomi physical device: upgrade install over the current build (migration on
  real data), create with sentence → category switch → tags, edit, Tags screen create/rename/delete,
  Search by tag. iOS: Native compilation is required; physical iPhone validation stays in the existing
  backlog item unless an authorized iPhone is connected.
- Commands: `./gradlew :shared:testAndroidHostTest`, `./gradlew :shared:compileAndroidDeviceTest`,
  `./gradlew :androidApp:assembleDebug`, `./gradlew :shared:compileKotlinIosSimulatorArm64`,
  `./gradlew :shared:verifyCommonMainTallybookDatabaseMigration`, `./gradlew detekt ktfmtCheck`.
  Running device tests needs a confirmed physical device (`AGENTS.md`).
