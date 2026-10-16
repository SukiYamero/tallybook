# Task 3 — Category form: draft state, composable and all feature copy

Depends on task1 (`CategoriaDraft`, `CategoriaRules`) and task2 (`CategoryAppearanceSuggester`).
Blocks task4 and task5.

## Objective

One draft owner and one stateless form reused by quick-create (compact) and the Categories editor
(full), plus every new string of the feature so wave 3 tasks never edit the resource files.

## Files

- `feature/categories/viewmodel/CategoryDraftState.kt` (new): immutable state + pure reducer used
  inside ViewModels (not a ViewModel itself).

  ```kotlin
  data class CategoryDraftState(nombre, icono, color, descripcion,
      iconoTouched: Boolean, colorTouched: Boolean, moreOptions: Boolean,
      duplicate: Categoria?, saving: Boolean)
  // onNombre(name, suggester) re-suggests untouched fields; onIcono/onColor mark touched;
  // nombreError: Blank | TooLong; canSave; toDraft(): CategoriaDraft
  ```

  A new name clears `duplicate`. Name input is capped at 40 and description at 340 chars in the
  reducer, so paste cannot exceed them.
- `feature/categories/ui/CategoryForm.kt` (new): `CategoryForm(state, actions, mode: Compact|Full,
  modifier)`. Name `TallybookTextField` with counter and error; icon row showing the selected icon in a
  `CategoryAvatar`, expanding to a `FlowRow` grid of every `CategoryIcon` (48dp targets, selected
  state, content descriptions); tint swatches (`CategoryTint.tintColors()`); description multiline
  with counter. Compact shows name + icon and a "More options" text button revealing tint and
  description; Full shows all. Duplicate message slot with the action label supplied by the caller.
  Tokens only; add named component tokens for any new dimension.
- `composeResources/values{,-en,-es-rAR,-pt-rBR}/strings.xml`: all copy for task3–5 (form labels,
  errors, "Create “%s”", "More options", "Use %s", "Open %s", duplicate/Otros messages, Categories
  screen title, sections "Your categories"/"Predefined", count plurals, hide/show/hidden,
  delete/confirm/in-use "Hide instead", Profile row, icon/tint accessibility labels). Follow
  `docs/wording.md`.

## Tests

- commonTest `CategoryDraftStateTest`: suggestion follows the name until touched (per field),
  caps, trim/blank errors, duplicate cleared on edit, `toDraft` blank description → `null`.
- androidDeviceTest `CategoryFormTest` (compile-only until the runner is repaired; see backlog):
  compact hides tint/description until "More options", icon selection semantics.

## Exit

`testAndroidHostTest`, `detekt`, `ktfmtCheck`, iOS compile; `CategoryResourcesHostTest`-style
check that every new key resolves in the four locales.
