# Single category and tags

Movimientos carry one category and optional tags, and the user manages tags from a Tags screen
reached from Profile. Product rules live in
[business-logic.md](../business-logic.md#category-and-tags); the Category and Tags sections of the
entry flow are described in [Add movimiento](add-movimiento.md); visual ownership and tokens live in
[UI architecture](../ui/architecture.md).

## Interaction

- **Tags screen** (Profile → Tags): a secondary destination over Profile with the bottom bar hidden
  and Back to Profile, laid out like the Categories screen. The header has a ghost back arrow and an
  icon-only add action. Below the search field, every tag is an `EtiquetaChip` sorted by name with its
  movimiento count in the primary currency; a search shows the matches or a no-results message. A
  failed load shows a message with Retry.
- **Tag sheet**: the add action opens a new-tag sheet prefilled with the current search; tapping a
  chip opens the rename sheet. The name caps at 40 characters, and the blank-name error waits until the
  user edits. A duplicate name on create offers "Open <name>", which swaps the sheet to that tag; on
  rename it shows the duplicate error and blocks saving. The rename sheet adds a trash action and,
  when the count is above 0, "View N movements", which closes the sheet and, once hidden, opens Search
  filtered by that tag. Saves and deletes report through a snackbar; a failed save or delete keeps the
  sheet with an inline error.
- **Delete**: the confirmation counts the movimientos in every currency that use the tag (or says
  none do). Confirming unlinks the tag from them and deletes it; there is no Undo.

## Integration

- Schema 6 (`5.sqm`) rebuilds `movimiento` with one `categoria_id`, keeping each movimiento's first
  category by `posicion` and falling back to Otros, then drops `movimiento_categoria` and adds
  `etiqueta`, `movimiento_etiqueta` and the `movimiento_con_etiquetas` view. SQLite foreign keys stay
  disabled, so repositories own link integrity.
- `EtiquetaRepository` owns create, rename, delete and counts. Validation, duplicate checks and writes
  share one transaction; delete unlinks and deletes in the same one. Undo of a movimiento deletion
  skips tags that no longer exist and restores in Otros when the custom category is gone.
- `TagsViewModel` owns the query, the content state and the one open sheet; the route turns its
  effects into the snackbar and the Search request. Navigation keeps Tags in the `secondary` slot, and
  Search receives "View N movements" as a one-shot `SearchFilterRequest`. The Search filter itself is
  described in [Search](search.md).

## Verification boundaries

The current tree passes the 757 shared host tests with zero failures,
`verifyCommonMainTallybookDatabaseMigration`, the Android debug APK, Native compilation, Detekt and
ktfmtCheck. Host tests cover the schema 5 → 6 migration (0, 1 and 3 linked categories per
movimiento), the tag repository rules, an integration test through the real repositories (create with
one category and two tags, filter by tag and by parent category, delete a tag), sentence tag
detection and ownership, the tag limit, tag creation from the movimiento picker including a failed
create, the Tags screen ViewModel, Search tag filters and the CSV Tags column.

Not covered: the device tests for the Category and Tags sections, the tag picker, the Tags screen and
its navigation, the Search tag filter and the UI catalog compile but have not run (tracked in the
[backlog](../backlog.md)). The final independent review, the upgrade install over the previous build
on the Xiaomi and the physical acceptance of the main flows are pending, as are iOS runtime, TalkBack
and enlarged-text passes.
