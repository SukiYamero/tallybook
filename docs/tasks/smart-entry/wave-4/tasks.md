# Wave 4 — Multiple categories and entry redesign foundations

Device review of phase 1 led to a redesign: the sentence entry becomes a top-anchored modal dialog
with voice dictation as a placeholder, the summary becomes a taller labeled review sheet, and every
category surface becomes a multi-tag field. Multiple categories (previously phase 2) join this
delivery. Rules: [business-logic.md](../../../business-logic.md#natural-language-entry) and
[Multiple categories](../../../business-logic.md#multiple-categories-per-movimiento).

- **task1 — Multi-category data**: migration, model, repositories, queries, CSV, Undo.
  [task](task1/task.md)
- **task2 — UI foundations**: `CategoryTag`, `CategoryTagField`, `MovimientoTypeBadge`, date
  badge, top-anchored modal dialog, voice placeholder, multi-select picker, selection rules.
  [task](task2/task.md)

## Shared contracts

```kotlin
// core.model — owned by task1
const val MAX_CATEGORIAS_POR_MOVIMIENTO: Int = 6

data class Movimiento(
    val id: String,
    val tipo: TipoMovimiento,
    val monto: Money,
    val fecha: LocalDate,
    val categoriaIds: List<String>, // 1..6, distinct, display order
    val descripcion: String?,
    val createdAt: Instant,
)
// MovimientoChanges replaces its single category with `categoriaIds: List<String>`.

// core.model — owned by task2 (pure, used by pickers and the ViewModel)
object CategoriaSelection {
  /** Appends, replacing the id's parent or children; ignores it at the limit or if present. */
  fun add(current: List<String>, id: String, catalog: List<Categoria>): List<String>
  /** Removes unless it is the last real category; removing never empties the list. */
  fun remove(current: List<String>, id: String): List<String>
  /** Drops the Otros fallback as soon as a real category is present. */
  fun normalized(current: List<String>): List<String>
}
```

## Blocking

task1 and task2 share no files. task1 keeps every existing caller compiling by reading the first
category where a single one is still displayed; wave 5 replaces those reads. Wave 5 starts after both
branches merge and the full suite passes.
