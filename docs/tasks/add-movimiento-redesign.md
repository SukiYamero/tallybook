# Add movimiento redesign

## Context

The add-movimiento sheet guides quick entry through category and amount, with persistent contextual
copy and space for future optional fields. It preserves drafts when a dismissal is accidental.

## Approved behavior

- The collapsed form uses its content height.
- A new entry opens category selection as soon as its data is ready, without waiting for the main
  sheet's entrance animation. The main sheet is prepared invisibly underneath until the category layer
  is shown, selected, or canceled. Category remains the top modal layer, so the initial entrance only
  presents the selector. Canceling returns to the prepared form without reopening or moving focus.
- The selector includes expense/income controls and contextual guidance. In this entry flow, only parent
  categories can be selected or searched; subcategory data and the generic picker drill-in are retained.
- Selecting a category closes the selector and focuses amount after its closing animation. Amount and
  save are unavailable before category selection. Category appears before date and amount in the form.
- Persistent copy stays brief: category purpose in the selector and expense/income-specific amount
  questions. The sheet and initial selector display an Add movement title. All supported locales use
  the same intent.
- The date chip supports S and M sizes in the shared component and catalog. S uses a minimum 48 dp
  touch target; M uses 48 dp with 15 sp text and 16 dp icons, plus slightly larger padding than S.
  Movimiento entry uses M with a visible
  Date label, centered below category with a 24 dp section gap and a 40 dp gap before amount.
- The amount question uses the body-emphasis typography token with 10 dp between label and input.
- Category, date, and amount share the field-label spacing token (10 dp).
- Opening More details lets the sheet grow by the description section plus half of one additional
  detail section, bounded by the available window height. The form body scrolls when later fields
  exceed that limit.
- The save action stays fixed below the scrollable body while the keyboard is closed. It is not moved
  above the keyboard. It becomes available again after focus is cleared or the keyboard is dismissed.
- The body scrolls whenever its content exceeds the height limit, including while the keyboard is
  closed. Keyboard dismissal restores the body's scroll to the top.
- The input method may offer a Done action, but the form never depends on its exact label or presence.
  Done clears focus; it does not submit the form.
- A dismissal request from the sheet drag, scrim, or system back closes immediately only when the draft
  has no user changes. Otherwise it opens a discard-changes confirmation dialog.
- Dismissing that confirmation by tapping its scrim or using back means Continue editing: the dialog
  closes and the original sheet and its draft remain open.
- Confirming Discard clears the draft and then closes the sheet. Saving does not show a discard
  confirmation.
- More details visibility alone is not a draft change. Current and future editable draft fields use the
  same dirty-state comparison against the opening baseline.
- Compact and expanded details use distinct registered detents with measured heights. Details use a
  short fade/slide and a rotating chevron. The CTA remains below the scrollable body.
- Direct scrolling keeps platform physics. Focusing amount reveals it within the keyboard-safe viewport.

## Ownership

- AddMovimientoViewModel owns the draft baseline, the current draft, the derived
  `hasUnsavedChanges` value, and the action that discards a draft.
- Composition owns the transient confirmation visibility and the pending dismissal request. These are
  UI mechanics and do not belong to the ViewModel.
- Composition owns category-entry phases and details visibility. The ViewModel remains the sole owner
  of the selected category and amount; input focus waits for selector dismissal and selection state.
- TallybookModalBottomSheet remains the reusable modal bottom-sheet primitive. A new reusable
  confirmation-dialog primitive renders the discard decision over an existing sheet.

## Waves

- **Wave 1:** draft baseline and dirty-state behavior, generic dismissal guard, reusable confirmation
  dialog.
- **Wave 2:** bounded More details layout, scrollable body, keyboard-safe fixed save action.
- **Wave 3:** host tests, Android device tests, static analysis, build, and physical-device review.

## Verification

- Unit tests prove empty forms dismiss directly, changed forms request confirmation, canceling the
  confirmation preserves the draft, and confirmed discard resets it.
- UI tests prove the save action remains outside the scrolling body and More details uses bounded body
  scrolling.
- Verify on the connected Xiaomi physical device: drag, scrim, and back each follow the discard guard;
  the keyboard does not expand the sheet; the fixed action is reachable after the keyboard closes.
- Verify new-entry automatic selection, parent-only search, cancel/reopen, expense/income wording,
  and selection-to-amount focus with the amount visible above the keyboard.

## External API evidence

- Compose Unstyled 2.9.0 Back dismissal calls `confirmDetentChange(Hidden)` before hiding, so it must
  remain enabled to use the existing draft guard:
  https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-modal-bottom-sheet/src/commonMain/kotlin/com/composeunstyled/ModalBottomSheet.kt
- `FocusRequester` is attached to the actual amount input and requested from a keyed effect after
  selector dismissal:
  https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus
- Compose Unstyled 2.9.0 mounts each modal in an independent native window without a ModalHost.
  Preparing the movement sheet invisibly preserves its measurement while the nested category window
  remains above it:
  https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-modal/src/commonMain/kotlin/com/composeunstyled/Modal.kt
- The invisible prepared form consumes pointer events, disallows focus, and uses
  `hideFromAccessibility` (available since Compose UI 1.8.0; resolved UI version 1.11.2):
  https://developer.android.com/reference/kotlin/androidx/compose/ui/semantics/SemanticsProperties#HideFromAccessibility()

## Validation status

- Android host tests, Detekt, ktfmtCheck, app build, and device-test APK compilation pass.
- Xiaomi M2012K11AG: automatic category opening, expense/income prompts, parent-only search with stable
  height, selection-to-amount keyboard focus and reveal, category cancellation, details expansion, and
  Back/scrim discard protection verified manually.
- Medium date chip opens the anchored calendar and applies the selected day. Bounded details content
  and its support text are reachable by scrolling without moving the save action.
- Frame-by-frame Xiaomi capture confirms category is the first visible sheet. Canceling reveals the
  prepared form, reopening works, and selecting a category still focuses amount. All three field
  labels use the shared 10 dp gap.
- Instrumented tests build, but the runner stalls before returning test results. Automated device
  execution remains unconfirmed; it must not be reported as passing.
- Product review of the new wording and category-first interaction remains with the user before merging.
