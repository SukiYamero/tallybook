# Smart entry — phase 1 of categories

## Context

Choosing a category before writing anything is the main friction of creating a movimiento. Creation
now starts from one sentence ("Gasté 20.000 en cine ayer"); the app derives tipo, monto, fecha,
categoría and descripción, shows a compact summary and lets the user save or refine. Product rules
live in [business-logic.md](../../business-logic.md#natural-language-entry); this spec owns delivery.

## Scope

In phase 1:

- Sentence step, live preview, compact summary (Edit movimiento | Save), existing form as the third
  step. Manual entry remains available.
- Local sentence parser (tipo, monto, fecha, subject) for `es`, `es-AR`, `en`, `pt-BR`.
- Category suggestion engine: learned history associations plus a keyword dictionary. One category
  per movimiento; suggestions switch the single selection.
- `cat_otros` fallback with the pending treatment in rows and the editor.
- Children selectable in creation and editing; Search category filters roll up parent → children.

Not in phase 1 (accepted, roadmap below): multiple categories, custom categories, Categories screen.
No schema migration is needed: `cat_otros` is a seed row and history reads existing columns.

## Shared contracts

Wave 1 tasks run in parallel against these signatures. A task that needs a change stops and reports
it to the orchestrator instead of diverging.

```kotlin
// core.model — owned by wave-1/task1
data class ParsedMovimientoPhrase(
    val tipo: TipoMovimiento?, // null: no tipo verb found
    val monto: Money?, // null: no valid amount found
    val fecha: LocalDate?, // null: no date expression; caller keeps today
    val subject: String, // sentence without tipo verbs, amount, currency and date tokens
    val reviewFlags: Set<PhraseReviewFlag>,
)

enum class PhraseReviewFlag { AMBIGUOUS_AMOUNT, FUTURE_DATE_IGNORED }

// core.data.entry — owned by wave-1/task1
class MovimientoPhraseParser {
  fun parse(text: String, moneda: Moneda, locale: AppLocale, today: LocalDate): ParsedMovimientoPhrase
}

// core.data.entry — owned by wave-1/task2
data class CategoryHistoryEntry(val subject: String, val categoriaId: String, val createdAt: Instant)

enum class SuggestionConfidence { HIGH, MEDIUM }

data class CategorySuggestion(val categoriaId: String, val confidence: SuggestionConfidence)

class CategorySuggester(private val dictionary: CategoryKeywordDictionary) {
  fun index(history: List<CategoryHistoryEntry>, now: Instant): CategorySuggestionIndex
}

class CategorySuggestionIndex {
  /** Ranked, at most [limit]; only the first element may be HIGH. Never returns excluded IDs. */
  fun suggest(
      subject: String,
      tipo: TipoMovimiento?,
      availableIds: Set<String>,
      excludedIds: Set<String>,
      limit: Int = 3,
  ): List<CategorySuggestion>
}

// core.data — owned by wave-1/task2
data class CategoryHistoryRow(val descripcion: String, val categoriaId: String, val createdAt: Instant)
// MovimientoRepository
fun observeCategoryHistory(limit: Int): Flow<DataOutcome<List<CategoryHistoryRow>>>

// core.model — owned by wave-1/task4
const val CATEGORIA_OTROS_ID: String = "cat_otros"
```

UI models for the new steps are owned by wave-1/task3 (see its task file); wave 2 produces them from
the ViewModel.

## Waves

- **Wave 1** (parallel, four subagents, one worktree each): [tasks](wave-1/tasks.md)
  task1 parser · task2 suggestion engine + history read · task3 entry UI components · task4 Otros,
  child selection support and Search roll-up.
- **Wave 2** (depends on all of wave 1): [task1](wave-2/task1/task.md) ViewModel and sheet
  integration, docs.
- **Wave 3**: [final review and device acceptance](wave-3/README.md).

## Execution

- Subagents: Sonnet 5.5, high effort. Each wave-1 task runs in its own worktree branched from the
  committed `feat/smart-entry`; the orchestrator merges each branch back after its checks pass.
- File ownership is listed per task. `composeResources/values*/strings.xml` is shared by task3 and
  task4; each adds only its own keys in a separate block, and the orchestrator resolves merges.
- Every task finishes with `./gradlew detekt ktfmtCheck`, its targeted host tests and
  `./gradlew :shared:compileKotlinIosArm64` when it adds `commonMain` code.

## Verification

- **Integration contract**: wave-2 host tests drive the real ViewModel from sentence to persisted
  row (parser + suggester + repository with an in-memory database), including the Otros fallback and
  a learned suggestion after a correction.
- **Runtime by target**: Xiaomi smoke check of the main flow (wave 3). Physical iPhone runtime stays
  in the existing backlog item; shared iOS compilation is required.
- No version-sensitive API is introduced; the sheet reuses existing modal, focus and animation
  contracts documented in [Add movimiento](../../features/add-movimiento.md).

## Roadmap after phase 1

- **Phase 2 — multiple categories (max 6)**: `movimiento_categoria` table and a migration that moves
  `categoria_id` into it (table rebuild; Android 12 ships SQLite 3.32 without `DROP COLUMN`),
  `CategoryTagField` (selected chips with remove, suggestion chips with add, "+ Add" opening a
  multi-select picker), rows with "+N", Search/CSV/Undo adaptation. Coordinate the migration number
  with any concurrent schema work (the Google + Drive branch).
- **Phase 3 — custom categories**: creation from the picker's empty search and from a Categories
  screen linked in Profile, icon/tint suggestion reusing `CategoryKeywordDictionary`, movimiento
  counts, hiding predefined categories. Deletion with movimientos is still undecided.
