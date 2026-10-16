# Wave 2 / Task 1 — ViewModel and sheet integration

## Goal

Connect the wave-1 pieces into the creation flow: sentence → summary → form, suggestions with
learning, the Otros fallback, child selection in creation and editing, and the pending treatment.
Update the feature documentation.

## Context

- Rules: [Natural-language entry](../../../../business-logic.md#natural-language-entry),
  [Category suggestions](../../../../business-logic.md#category-suggestions),
  [Otros](../../../../business-logic.md#otros-and-pending-movimientos).
- Current flow and ownership: [Add movimiento](../../../../features/add-movimiento.md).
  `AddMovimientoViewModel` owns draft data, validation and submission; composition owns focus and
  transient presentation. Keep MVVM + MVI split (`UiState`, `Channel` effects, `Actions`).
- Inject dispatchers; never `Dispatchers.*` directly. The parser and suggester are pure and can run
  on the computation dispatcher already injected elsewhere (see `di/DispatcherModule.kt`).

## ViewModel

- New state in `AddMovimientoUiState`: `step: AddMovimientoStep`, `phrase: String`,
  `preview: PhrasePreviewUi?`, `suggestions: List<Categoria>`, `dismissedSuggestionIds: Set<String>`,
  `amountNeedsReview: Boolean`. Derive `MovimientoSummaryUi` from the draft, not from the parse, so
  the summary reflects edits made in the form.
- New actions: `onPhraseChanged`, `onPhraseContinue`, `onFillManually`, `onEditMovimiento`,
  `onSuggestionSelected`, `onStepBack`. Keep existing `MovimientoFormActions`.
- History: `observeCategoryHistory(1_000)` → subjects via the parser → `CategorySuggester.index`,
  rebuilt per emission off the main thread; failures leave dictionary-only suggestions (never block
  entry).
- Live preview: debounce phrase edits by 250 ms (same as Search), parse, compute suggestions; clearing
  is immediate.
- Continue: capture today/timezone once, parse, write `tipo` (parsed or suggestion-implied or GASTO),
  `amountInput` (formatted for the input field via the existing round-trip), `fecha` (parsed or
  today), `descripcion` (trimmed sentence), category (HIGH suggestion or none) and go to SUMMARY.
- Selecting a suggestion switches the single selection; the previous selection, if it was a
  suggestion, returns to the list. Selecting a category in the form updates the draft as today.
- Save from SUMMARY or FORM: a missing category persists `CATEGORIA_OTROS_ID`. Invalid required input
  from SUMMARY moves to FORM with `submitAttempted = true` and focus on the first invalid field.
  Success emits `Saved` as today.
- Dismissal guard: a non-empty phrase or any draft change counts as unsaved. Back: FORM → SUMMARY when
  the flow came through the summary, SUMMARY → PHRASE (phrase kept), PHRASE → existing guard.
  `onFillManually` enters FORM directly; Back from that FORM uses the existing guard.

## Sheet and form

- `AddMovimientoSheet`: host the three steps with the existing modal sheet and an `AnimatedContent`
  transition that respects the measured-detent rules in the feature doc; focus the sentence input on
  entry; route sheet Back through `onStepBack`.
- `MovimientoForm` in creation: remove the category-first entry and the "More details" disclosure —
  the description is a regular visible field, because it now carries the sentence. Remove what becomes
  dead (`MovimientoCategoryEntryPolicy.startWithCategory`, disclosure state) rather than leaving flags.
- Both creation and editing pickers: `allowSubcategories = true`,
  `excludedIds = setOf(CATEGORIA_OTROS_ID)`. The editor shows the pending style on the category field
  when the stored category is Otros.
- Rows in Home, History and Search pass `pending = categoriaId == CATEGORIA_OTROS_ID`.
- Koin: provide `MovimientoPhraseParser`, `CategoryKeywordDictionary`, `CategorySuggester` and the new
  ViewModel dependencies; extend `RepositoryModulesTest`/graph tests.

## Docs

Rewrite the Interaction and Ownership sections of `docs/features/add-movimiento.md` for the new flow;
add the new components to `docs/ui/components.md`; keep business rules only in `business-logic.md`.

## Tests

- `AddMovimientoViewModelTest`: sentence → summary values; HIGH preselect vs MEDIUM suggestions;
  suggestion switching and dismissal; Otros fallback on save; invalid save from summary → form with
  errors; Back between steps preserves phrase and draft; dismissal guard with only a phrase.
- `FirstMovimientoIntegrationTest` (real repositories, in-memory DB): sentence to persisted row; a
  saved correction makes the next identical subject suggest the corrected category as HIGH.
- Update affected device tests (`AddMovimientoSheet*`) to the new entry; compile required.

## Done when

`./gradlew detekt ktfmtCheck :shared:testAndroidHostTest :androidApp:assembleDebug
:shared:compileKotlinIosArm64` pass on the merged tree, and the flow runs on the Xiaomi before handing
to wave 3.
