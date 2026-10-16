# Stage 3 / Task 1 — Catalog, copy cleanup and docs

## Objective

Update the UI catalog, remove obsolete multi-category copy and make the canonical docs describe the
current rules.

## Context and skills

- Requires Stage 2 complete on the combined tree.
- Skills: `android-skills:compose`, `chrisbanes-skills:grounded-writing` for docs.

## Files

- `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/catalog/*`: replace the multi-category
  matrices (`FIELD_LIMIT`, at-limit samples, `CategoriaSelection` usage) with `CategorySingleField`,
  `EtiquetaTag`, `EtiquetaTagField`, `EtiquetaPickerSheet` samples; update `UiCatalogScreenTest.kt`.
- Delete `core/ui/category/CategoryTagField.kt`, its previews, catalog usage and
  `CategoryTagTest.kt` cases for the field if nothing consumes it after Stage 2.
- All four `strings.xml`: remove keys with no remaining consumer (`category_picker_done`,
  `category_picker_limit_reached`, `category_picker_selected_label`, `movimiento_row_extra_categories`,
  `ui_catalog_state_at_limit`, unused `category_tag_field_*`); update `EntryResourcesHostTest.kt`.
- `docs/business-logic.md` already describes the accepted rules (§ Category and tags, § Tags and the
  sections that reference them). Only correct it where the delivered behavior intentionally differs,
  in place and without history.
- `docs/features/add-movimiento.md`, `custom-categories.md`, `search.md`, `manage-movimientos.md`
  (tags no longer out of scope), `profile.md` (Tags card); new `docs/features/tags.md` only if the
  Tags screen needs boundaries not covered elsewhere.
- `docs/features/README.md`: rename the "Smart entry and multiple categories" row and set this
  feature's row status.
- `docs/ui/components.md`: replace multi-category rows with `CategorySingleField`, `EtiquetaTag`,
  `EtiquetaChip`, `EtiquetaTagField`, `EtiquetaPickerSheet`, single-select `CategoryPickerSheet`; fix
  the stale "`n/max` counter" mention.
- `docs/backlog.md`: update fixture names in the Android device-check and iOS items.

## Exit

`grep -rn "categoriaIds\|MAX_CATEGORIAS\|CategoriaSelection\|seis categor\|six categor" shared docs`
returns nothing outside this task folder; docs follow "one reference per topic".
