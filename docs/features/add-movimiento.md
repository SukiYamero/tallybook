# Add movimiento

The shared Android/iOS entry flow uses a compact bottom sheet and Tallybook components. Domain rules
remain defined in [business-logic.md](../business-logic.md); visual ownership and tokens remain in
[UI architecture](../ui/architecture.md).

## Interaction

- A new entry presents category selection first, with the complete form prepared behind it. Expense
  and income share that selector and use type-specific contextual copy.
- Selecting a category closes the selector, then focuses amount. Canceling reveals the form without
  selecting a category, reopening the selector, or forcing input focus. Amount remains editable.
- Category, date, and amount use the shared `fieldLabelSpacing` token. The date uses the medium chip;
  the movement type selector uses `bodySmall` without changing other segmented controls.
- Save remains available for incomplete input. Submission displays the applicable field helpers and
  never inserts invalid data. Only an active submission disables Save. An explicit retry after a save
  failure preserves the draft and revalidates it; load errors retain their separate retry action.
- Validation focus follows the initial submit attempt, not live error changes, so correcting amount
  cannot redirect typing to a still-missing category.
- More details reveals description with a short transition and a directional chevron. Its measured
  detent is bounded, and excess content scrolls while the title and save action retain their space.
- The save action hides while the keyboard is present. Amount and description remain reachable through
  body scrolling; keyboard dismissal restores the body's scroll position. Done clears focus and does
  not submit. No behavior depends on every keyboard offering that action.
- A changed draft requires confirmation before drag, scrim, or Back can dismiss it. Dismissing the
  confirmation means Continue; Discard resets the draft before closing. Opening More details alone
  does not count as a draft edit.

## Ownership and retained capability

- `AddMovimientoViewModel` owns editable data, validation, submission and the opening draft baseline.
  `canSubmit` describes valid submission readiness, not whether the validation-triggering CTA is enabled.
- Composition owns category-entry phases, field focus, disclosure and confirmation visibility.
- `TallybookDismissGuard` and `TallybookConfirmationDialog` are domain-neutral reusable primitives.
  `TallybookModalBottomSheet` owns detents, motion and bottom safe-area policy.
- Subcategory data, generic picker tree/search/drill-in and their tests are retained. Movimiento entry
  currently passes `allowSubcategories = false`; retaining support does not enable it in this flow.

## Layout integration

Unstyled can measure content against the entire window while rendering a smaller content detent.
The modal body uses the positive captured active height only after the underlying sheet has settled;
entrance and detent transitions remain free to measure their natural content. The form allocates its
scroll body with `weight(1f, fill = false)`, reserving title/footer space before measuring overflow.
This keeps validation helpers inside the scroll viewport instead of reducing the save action.

Version-sensitive contracts:

- [Compose 1.11.1 Column weight](https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/foundation/foundation-layout/src/commonMain/kotlin/androidx/compose/foundation/layout/Column.kt#L294)
- [Unstyled 2.9.0 sheet measurement](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-bottom-sheet/src/commonMain/kotlin/com/composeunstyled/BottomSheet.kt)
- [Unstyled modal transition state and dismissal](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-modal-bottom-sheet/src/commonMain/kotlin/com/composeunstyled/ModalBottomSheet.kt)
- [Independent native modal layers](https://github.com/composablehorizons/compose-unstyled/blob/2.9.0/composeunstyled-modal/src/commonMain/kotlin/com/composeunstyled/Modal.kt)
- [Programmatic focus](https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus)
- [Hidden prepared-form accessibility](https://developer.android.com/reference/kotlin/androidx/compose/ui/semantics/SemanticsProperties#HideFromAccessibility())

## Verification boundaries

The product flow is accepted on Xiaomi. Current physical checks confirm initial category cancellation,
both validation helpers with a complete save action, uninterrupted amount correction, details
expand/collapse/reopen and scrolling, and amount/description keyboard reveal. Shared Android builds,
host tests, Detekt, format checks and iOS Arm64 compilation pass.

Host tests cover validation, save retry, duplicate-submit protection and draft dismissal decisions.
Device tests cover entry, focus, field errors, complete CTA bounds and details scrolling. Instrumented
tests compile, but their runner stalls before returning results; automated device execution remains
unconfirmed. Physical iPhone review remains pending even when shared iOS compilation succeeds.
