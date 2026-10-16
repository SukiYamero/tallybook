# Add movimiento

The shared Android/iOS entry flow starts in a top-anchored modal dialog and continues in one modal
bottom sheet, both built from Tallybook components. Domain rules remain defined in
[business-logic.md](../business-logic.md); visual ownership and tokens remain in
[UI architecture](../ui/architecture.md).

## Interaction

Creation moves through three steps; business rules for each live in
[Natural-language entry](../business-logic.md#natural-language-entry),
[Category suggestions](../business-logic.md#category-suggestions) and
[Multiple categories](../business-logic.md#multiple-categories-per-movimiento).

- **Sentence (dialog)**: the labeled two-line input takes focus on entry and the keyboard opens with
  it. Its placeholder is a localized example; Continue is disabled for a blank sentence and Done on
  the keyboard continues. The "Detected" card always shows the tipo and date badges (expense and
  today until the sentence says more), the amount when found, and the tag field: selected tags,
  "+ Add", `n/6` and suggestions. Detection runs after a 250 ms debounce and clears immediately when
  the input is emptied. Below Continue, the "or" divider introduces the voice placeholder, then
  "Fill manually" opens the empty form, discarding anything an earlier Continue derived. Continue and
  Fill manually wait for the initial data to load; a load failure shows the existing retry message.
  The dialog keeps its position when the keyboard opens and scrolls inside the height left over.
- **Review (sheet)**: opens tall rather than hugging its content. A title and one-line subtitle
  lead labeled sections: Type and Date as large badges side by side, Amount, the quoted Description
  and the Categories tag field with suggestions. Edit movimiento (left) opens the form with the same
  draft; Save (right) persists; the actions sit at the bottom, at least `space32` below the content.
  A failed save shows the shared save error above the actions and keeps the draft.
- **Form**: the complete form in the same sheet, with the tag field and the multi-select picker in
  place of a single category field. Save without a real category persists Otros; the field shows the
  pending style and label whenever the effective selection is Otros. The picker never offers Otros.
  Save with an invalid amount from the review opens the form with the field error and focus on it.
- Back follows the path taken: form to review when the form came from the review, review to the
  dialog (text and draft kept), and otherwise the dismissal guard. Fill manually has no way back to
  the dialog.
- Tag rules: tapping a suggestion adds it; every tag but the last has a ×; the Add tag and the
  suggestions disappear at six; removing a suggested category dismisses it for the same subject; the
  dismissals reset when Continue runs for a different subject.
- Validation focus follows the initial submit attempt, not live error changes, so correcting amount
  cannot redirect typing elsewhere. Date and amount share the `fieldLabelSpacing` token and the date
  uses the medium chip.
- The save action of the form hides while the keyboard is present; amount and description remain
  reachable through body scrolling and keyboard dismissal restores the scroll position. Done clears
  focus and does not submit.
- A non-blank sentence or any changed draft requires confirmation before a scrim tap, drag or Back can
  dismiss. A blank sentence closes the dialog directly even if tags were picked. Dismissing the
  confirmation means Continue; Discard resets the whole entry, step included, before closing. Editing
  an existing movimiento keeps opening the form directly with the same tag field and picker.

## Ownership and retained capability

- `AddMovimientoViewModel` owns the draft (`categoriaIds` changes only through `CategoriaSelection`),
  the sentence, the step, validation, submission and the opening baseline. The detected values,
  suggestions, summary and unsaved flag derive from that state; composition never reads the clock.
  `MovimientoEntryEngine` holds the pure parser and suggestion calls; the history index is rebuilt per
  emission on the computation dispatcher and a failed history read falls back to dictionary-only
  suggestions.
- While no category was changed by hand, each detection result writes the high-confidence category
  into the draft; the first manual change takes the categories over.
- Composition owns field focus, picker visibility and confirmation visibility.
  `AddMovimientoSheet` hosts both surfaces: `MovimientoSentenceStep` shows while the step is the
  sentence and `MovimientoSummaryStep` or the form show in the sheet otherwise. The steps are stateless.
  The session releases the ViewModel only when the surface that was showing at close reports hidden.
- `TallybookDismissGuard` and `TallybookConfirmationDialog` are domain-neutral reusable primitives.
  `TallybookModalBottomSheet` owns detents, motion and bottom safe-area policy;
  `TallybookModalDialog` owns the top anchor.
- The multi-select picker, generic tree/search/drill-in and Search roll-up are shared by creation and
  editing.

## Layout integration

Unstyled can measure content against the entire window while rendering a smaller content detent.
A detent's height cannot be read back from the sheet, which is bounded by that same detent: the
measurement can only confirm the previous height and never sees growth. Content detents therefore
record the body's natural height, measured against the window height whatever the current detent,
and invalidate the sheet's anchors when it changes; the sheet clips what the detent has not reached
yet. The form allocates its
scroll body with `weight(1f, fill = false)`, reserving title/footer space before measuring overflow.
This keeps validation helpers inside the scroll viewport instead of reducing the save action.

The sheet keeps one detent per step. The review gets its height from a minimum of 72% of the window
(a weighted child would instead fill the whole window, because the measurement allows the window
height), with the actions pushed to the bottom by `Arrangement.SpaceBetween`; its labeled sections
scroll on short screens. Growth inside a step moves that detent: the form fills its natural height
when it opens from the review and the review grows for a save error. Step changes cross-fade with a
snapped size so the sheet's own detent animation is the only height motion, and the sheet keeps the
content of its last step while it hides on the way back to the dialog.

Version-sensitive contracts:

- [Compose 1.11.1 Column weight](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/foundation/foundation-layout/src/commonMain/kotlin/androidx/compose/foundation/layout/Column.kt#L294)
- [Unstyled 2.9.0 sheet measurement](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-bottom-sheet/src/commonMain/kotlin/com/composeunstyled/BottomSheet.kt)
- [Unstyled modal transition state and dismissal](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-modal-bottom-sheet/src/commonMain/kotlin/com/composeunstyled/ModalBottomSheet.kt)
- [Unstyled dialog panel exit](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-dialog/src/commonMain/kotlin/com/composeunstyled/Dialog.kt)
- [Independent native modal layers](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-modal/src/commonMain/kotlin/com/composeunstyled/Modal.kt)
- [Programmatic focus](https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus)
- [Hidden prepared-form accessibility](https://developer.android.com/reference/kotlin/androidx/compose/ui/semantics/SemanticsProperties#HideFromAccessibility())

## Verification boundaries

Host tests cover sentence to review values, the live detected values and the sentence-owned
categories, suggestion add/remove and dismissal rules, the six-category limit, the parent/child rule,
the Otros fallback and replacement, dialog dirtiness, invalid save routing, step Back, the debounced
detection, the learned correction through real repositories, editor multi-category save and Undo, and
the row count resources. Device tests for the dialog, review, form and editor compile but their
runner stalls before returning results; automated device execution remains unconfirmed. The
Xiaomi smoke covered the dialog with and without the keyboard, both dismissal paths, the review
sheet's height and spacing, adding and removing up to six tags in the dialog, review, form and
editor, rows with "+N", Search through a secondary category and Undo of a multi-category movimiento.
A physical iPhone review remains pending even though shared iOS compilation succeeds.
