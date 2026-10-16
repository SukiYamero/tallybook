# Add movimiento

The shared Android/iOS entry flow starts in a vertically centered modal dialog and continues in one modal
bottom sheet, both built from Tallybook components. Domain rules remain defined in
[business-logic.md](../business-logic.md); visual ownership and tokens remain in
[UI architecture](../ui/architecture.md).

## Interaction

Creation moves through three steps; business rules for each live in
[Natural-language entry](../business-logic.md#natural-language-entry),
[Category suggestions](../business-logic.md#category-suggestions) and
[Category and tags](../business-logic.md#category-and-tags).

- **Sentence (dialog)**: the labeled two-line input opens unfocused, with no keyboard, so the user
  sees it and taps to type (the dialog container holds the window's initial focus so the input does not
  take it). Its placeholder is a localized example; Continue is disabled for a blank
  sentence and Done on the keyboard continues. The "Detected" card is absent while the sentence is
  blank and fades in, expanding, with the first text (it leaves the same way). It shows the tipo and
  date badges (expense and today until the sentence says more), the amount when found or a note that
  it was not detected, the Category section and the optional Tags section. Detection runs after a 250 ms debounce and clears immediately when
  the input is emptied, which also resets what an earlier Continue derived. Continue is pinned in a
  footer outside the scrolling body, so it stays on screen with the keyboard open and suggestions
  showing. The body scrolls the input, the Detected card, the "or" divider with the voice placeholder
  and "Fill manually", which opens the empty form, discarding anything an earlier Continue derived but
  keeping tags picked by hand. Continue and Fill manually wait for the initial data to load; a load
  failure shows the existing retry message. The dialog opens vertically centered and that top edge is kept: the
  Detected card and the suggestions grow it downward with an animated size change, so it never jumps.
  When the keyboard opens the dialog moves up only as far as its bottom needs to stay above it, with
  Continue still visible, scrolls inside the height left over, and returns to the captured top when the
  keyboard closes. Scrim taps and Back dismiss as before.
- **Review (sheet)**: opens tall rather than hugging its content. A title and one-line subtitle
  lead labeled sections: Type and Date as large badges side by side, Amount (a missing one reads "not detected", with a hint to use Edit movimiento), the quoted
  Title (the sentence), an optional Description field, the Category section with its suggestions and the Tags section, each
  section `space24` apart. Edit movimiento (left) opens the form with the same
  draft; Save (right) persists; the actions sit at the bottom, at least `space32` below the content.
  A failed save shows the shared save error above the actions and keeps the draft.
- **Form**: the complete form in the same sheet, with the same Category and Tags sections. Save
  without a real category persists Otros; the Category section shows the pending style and, beside
  its label, the "to complete" caption whenever the effective category is Otros.
  Save with an invalid amount from the review opens the form with the field error and focus on it.
- Back follows the path taken: form to review when the form came from the review, review to the
  dialog (text and draft kept), and otherwise the dismissal guard. Fill manually has no way back to
  the dialog.
- Category section: while a real category is selected, the suggestions stay dimmed and tapping one
  switches to it; "Choose" opens the single-select picker, which never offers Otros and closes on a
  tap. In the dialog, the review and the creation form the selected category has a × that returns to
  the Otros fallback; removing a suggested category dismisses it for the same subject, and the
  dismissals reset when Continue runs for a different subject.
- Tags section: each selected tag has a ×, and "+ Add" opens the multi-select tag picker until six
  are selected. A picker search without results offers creating the typed name; the new tag is
  selected while the limit allows, and a failed create shows the tag save error under that offer
  while the query still names it. An unavailable tag catalog replaces only this section with a retry,
  and the movimiento still saves without tags.
- Validation focus follows the initial submit attempt, not live error changes, so correcting amount
  cannot redirect typing elsewhere. Date and amount share the `fieldLabelSpacing` token and the date
  uses the medium chip.
- The form has optional Title and Description fields, each capped at 180 characters with a counter.
- The save action of the form hides while the keyboard is present; amount, title and description remain
  reachable through body scrolling and keyboard dismissal restores the scroll position. Done clears
  focus and does not submit.
- A non-blank sentence or any changed draft requires confirmation before a scrim tap, drag or Back can
  dismiss. A blank sentence closes the dialog directly even if tags were picked. Dismissing the
  confirmation means Continue; Discard resets the whole entry, step included, before closing. Editing
  an existing movimiento keeps opening the form directly with the same sections, without the ×
  on the category.

## Ownership and retained capability

- `AddMovimientoViewModel` owns the draft (one `categoriaId` and the ordered `etiquetaIds`), the
  sentence, the step, validation, submission and the opening baseline. The detected values,
  suggestions, summary and unsaved flag derive from that state; composition never reads the clock.
  `MovimientoEntryEngine` holds the pure parser and suggestion calls; the history index is rebuilt per
  emission on the computation dispatcher and a failed history read falls back to dictionary-only
  suggestions.
- While no category was changed by hand, each detection result writes the high-confidence category
  into the draft; the first manual change takes the category over. Detected tags follow the same
  ownership until the user adds or removes one (`etiquetasEdited`).
- Composition owns field focus, picker visibility and confirmation visibility.
  `AddMovimientoSheet` hosts both surfaces: `MovimientoSentenceStep` shows while the step is the
  sentence and `MovimientoSummaryStep` or the form show in the sheet otherwise. The steps are stateless.
  The session releases the ViewModel only when the surface that was showing at close reports hidden.
- `TallybookDismissGuard` and `TallybookConfirmationDialog` are domain-neutral reusable primitives.
  `TallybookModalBottomSheet` owns detents, motion and bottom safe-area policy;
  `TallybookModalDialog` owns the vertical placement (`DialogVerticalPlacement`: centered at first, captured top, minimum shift above the keyboard).
- The single-select category picker (one level of family blocks, flat search and quick-create), the
  multi-select tag picker and the Search roll-up are shared by creation and editing.

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

The final tree passed 640 shared host tests with zero failures, Detekt, ktfmtCheck, the SQLDelight
migration verification (schema 2 → 3 with real v2 data), the Android debug APK and shared iOS
compilation. Host tests cover the parser's amount/date/tipo rules across locales, suggestion
confidence, learned categories and ambiguous words, sentence to review values, sentence-owned
versus hand-picked categories, the Otros fallback, dialog dirtiness, invalid save routing, step Back,
debounced detection, the learned correction through real repositories, dialog placement, and the
flat picker grouping. Two independent reviews and a final code review ran; confirmed findings were
fixed with regression tests.

Manual checks on a physical Xiaomi covered the centered dialog without autofocus, the Detected area
appearing on typing, keyboard behavior with the pinned Continue, both dismissal paths, the review
sheet's height and section spacing, the not-detected amount copy and the schema 2 → 3 migration on
existing data. The Category and Tags sections have their own boundaries in
[Tags](tags.md#verification-boundaries).

Not covered: device test execution (the runner stalls before returning results), a physical iPhone,
TalkBack and enlarged-text passes, light theme screenshots and native-speaker review of es-AR/pt-BR
copy. Outstanding work is tracked in the [backlog](../backlog.md).
