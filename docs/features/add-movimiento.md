# Add movimiento

The shared Android/iOS entry flow uses a compact bottom sheet and Tallybook components. Domain rules
remain defined in [business-logic.md](../business-logic.md); visual ownership and tokens remain in
[UI architecture](../ui/architecture.md).

## Interaction

Creation runs three steps inside one modal sheet; business rules for each live in
[Natural-language entry](../business-logic.md#natural-language-entry) and
[Category suggestions](../business-logic.md#category-suggestions).

- **Sentence**: the multi-line input takes focus on entry and the keyboard opens with it. Its
  placeholder is a localized example; Continue is disabled for a blank sentence and Done on the
  keyboard continues. "Fill manually" opens the empty form. While typing, a preview row shows the
  detected amount, date and high-confidence category after a 250 ms debounce and clears immediately
  when the input is emptied. Continue and Fill manually wait for the initial data to load; a load
  failure shows the existing retry message instead.
- **Summary**: a card with tipo and date, the amount, the sentence, the selected category and up to
  three suggestion chips. A chip switches the single selection and the replaced suggestion returns to
  the list; replacing a suggestion from the form's picker dismisses it for the draft. Edit movimiento
  (left) opens the form with the same draft; Save (right) persists. A failed save shows the shared
  save error under the card and keeps the draft.
- **Form**: the complete form with description as a regular field. Save without a category persists
  Otros; the category field shows the pending style and label when Otros is selected. The picker lists
  parents and children and never offers Otros. Save with an invalid amount from the summary opens the
  form with the field error and focus on it.
- Back follows the path taken: form to summary when the form came from the summary, summary to the
  sentence (text kept), and otherwise the dismissal guard. Fill manually has no way back to the sentence.
- Validation focus follows the initial submit attempt, not live error changes, so correcting amount
  cannot redirect typing elsewhere. Category, date and amount share the `fieldLabelSpacing` token and
  the date uses the medium chip.
- The save action of the form hides while the keyboard is present; amount and description remain
  reachable through body scrolling and keyboard dismissal restores the scroll position. Done clears
  focus and does not submit.
- A non-blank sentence or any changed draft requires confirmation before drag, scrim or Back can
  dismiss. Dismissing the confirmation means Continue; Discard resets the whole entry, step included,
  before closing. Editing an existing movimiento keeps opening the form directly with the same picker
  rules.

## Ownership and retained capability

- `AddMovimientoViewModel` owns the draft, the sentence, the step, validation, submission and the
  opening baseline. The summary, suggestions and the unsaved flag derive from that state; composition
  never reads the clock. `MovimientoEntryEngine` holds the pure parser and suggestion calls; the
  history index is rebuilt per emission on the computation dispatcher and a failed history read falls
  back to dictionary-only suggestions.
- Composition owns field focus, picker visibility, the keyboard-lift flag and confirmation visibility.
  `AddMovimientoSheet` hosts the steps; `MovimientoPhraseStep` and `MovimientoSummaryStep` are stateless.
- `TallybookDismissGuard` and `TallybookConfirmationDialog` are domain-neutral reusable primitives.
  `TallybookModalBottomSheet` owns detents, motion and bottom safe-area policy.
- Picker, generic tree/search/drill-in and Search roll-up are shared by creation and editing.

## Layout integration

Unstyled can measure content against the entire window while rendering a smaller content detent.
The modal body uses the positive captured active height only after the underlying sheet has settled;
entrance and detent transitions remain free to measure their natural content. The form allocates its
scroll body with `weight(1f, fill = false)`, reserving title/footer space before measuring overflow.
This keeps validation helpers inside the scroll viewport instead of reducing the save action.

The sheet keeps one detent per step and gives content that grows after its step settled a detent of
its own: the keyboard-lifted sentence step (bottom padding follows the IME where the platform does not
move the sheet) and the summary with a save error. Step changes cross-fade with a snapped size so the
sheet's own detent animation is the only height motion. The sentence field and its preview row keep a
constant size and overflow scrolls inside the step body, so Continue never leaves the sheet.

Version-sensitive contracts:

- [Compose 1.11.1 Column weight](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/foundation/foundation-layout/src/commonMain/kotlin/androidx/compose/foundation/layout/Column.kt#L294)
- [Unstyled 2.9.0 sheet measurement](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-bottom-sheet/src/commonMain/kotlin/com/composeunstyled/BottomSheet.kt)
- [Unstyled modal transition state and dismissal](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-modal-bottom-sheet/src/commonMain/kotlin/com/composeunstyled/ModalBottomSheet.kt)
- [Independent native modal layers](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-modal/src/commonMain/kotlin/com/composeunstyled/Modal.kt)
- [Programmatic focus](https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus)
- [Hidden prepared-form accessibility](https://developer.android.com/reference/kotlin/androidx/compose/ui/semantics/SemanticsProperties#HideFromAccessibility())

## Verification boundaries

Host tests cover sentence to summary values, suggestion preselection, switching and dismissal, the
Otros fallback, invalid save routing, step Back, the unsaved guard, the debounced preview and the
learned correction through real repositories. Device tests for the steps compile but their runner
stalls before returning results; automated device execution remains unconfirmed. The Xiaomi smoke of
the sentence flow and a physical iPhone review remain pending even when shared iOS compilation
succeeds.
