# Wave 5 / Task 1 — Redesigned entry and multi-category integration

## Goal

Wire wave 4 into the product: the sentence dialog, the review sheet, multi-category drafts, the tag
field in the form and the editor, rows with "+N", and documentation.

## Flow

- The add action opens the sentence `TallybookModalDialog`: title, labeled input, a "Detected" area
  that always shows `MovimientoTypeBadge` and `MovimientoDateBadge` (defaults until detected), the
  amount when found and a `CategoryTagField` (selected, add, suggestions), the Continue action, the
  `VoiceDictationPlaceholder` below an "or" divider, and "Fill manually".
- Dismissal: scrim or Back closes immediately when the sentence is blank; otherwise the existing
  discard confirmation. Continue closes the dialog and opens the review sheet; Back from the sheet's
  summary reopens the dialog with the text and draft preserved. "Fill manually" opens the sheet on
  the empty form.
- Review sheet: tall (it starts at a large detent rather than hugging content), title in the spirit
  of "Review before saving" with a one-line subtitle, labeled sections (Type and Date side by side
  with large badges, Amount, Description, Categories with the tag field and suggestions), and the
  two equal-level actions separated from the content by at least `space32`. Edit movimiento opens the
  form in the same sheet.
- Form and `ManageMovimiento` editor: replace the single category selection field with
  `CategoryTagField` + multi-select picker. Validation: Otros fallback when no real category is
  chosen at creation; editing keeps at least one category.
- Draft: `categoriaIds: List<String>` in the ViewModels, updated only through `CategoriaSelection`.
  A HIGH suggestion is preselected; tapping a suggestion adds it; removing a suggested category
  dismisses it for the same subject.
- Rows in Home, History and Search: first category's avatar and name plus a "+N" count when more
  exist; pending treatment when the only category is Otros. Search filter chips are unchanged.
- Remove the old phrase step from the sheet and the single-select picker API once unused.

## Docs

Update `docs/features/add-movimiento.md`, `docs/ui/components.md` and the registry row. Keep rules
only in `business-logic.md`.

## Tests

ViewModel flow (dialog → sheet → back to dialog, blank vs dirty dismissal, add/remove/suggestion
rules, Otros fallback and removal, limit), editor multi-category save and Undo, rows "+N", device
tests updated to the dialog and tag field (compile required).

## Done when

The full check command passes and the orchestrator's Xiaomi smoke confirms: dialog stable with the
keyboard, detected type/date always visible, voice placeholder, review sheet height and spacing,
adding/removing up to six tags in dialog, summary, form and editor, "+N" rows, Search through a
secondary category.
