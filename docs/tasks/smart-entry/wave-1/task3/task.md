# Wave 1 / Task 3 — Entry UI components

## Goal

Stateless Compose Multiplatform components for the sentence and summary steps, the suggestion chip
and the pending treatment, with previews and UI Catalog entries. Wave 2 wires them to the ViewModel.

## Context

- Follow [UI architecture](../../../../ui/architecture.md) and [components](../../../../ui/components.md):
  Tallybook tokens (`TallybookTheme.spacing/typography/colors/radius`), existing primitives
  (`TallybookTextField` with `singleLine = false`, `TallybookButton` with primary/secondary colors,
  `TallybookTextButton`, `TallybookSurface`, `TallybookSectionHeading`, `CategoryAvatar`,
  `CategoryChip` colors via `CategoryTintColors`, `neutralCategoryTintColors()`). No raw Material
  styling that bypasses Tallybook components. Copy follows [wording.md](../../../../wording.md) in
  `values` (es), `values-es-rAR` (voseo), `values-en`, `values-pt-rBR`.
- Accessibility: chips expose role and selected/unselected state; the add/selected distinction is
  never color-only (outlined + leading `+` versus filled); the amount and review flags are announced.
- Composables take UI models plus callbacks; no ViewModel, repository or Koin access.

## UI models (owned here, package `feature.movimientos.viewmodel`)

```kotlin
internal enum class AddMovimientoStep { PHRASE, SUMMARY, FORM }

internal data class PhrasePreviewUi(
    val tipo: TipoMovimiento?,
    val monto: Money?,
    val fecha: LocalDate?,
    val categoria: Categoria?,
)

internal data class MovimientoSummaryUi(
    val tipo: TipoMovimiento,
    val fecha: LocalDate,
    val monto: Money?, // null: missing, shown as a required-value message
    val amountNeedsReview: Boolean,
    val descripcion: String,
    val categoria: Categoria, // may be the Otros fallback
    val categoriaIsPending: Boolean,
    val suggestions: List<Categoria>, // at most 3, never the selected one
    val isSubmitting: Boolean,
)
```

## Components

- `MovimientoPhraseStep` (`feature/movimientos/ui/`): multi-line input with the localized placeholder,
  focus requested on first composition through a passed `FocusRequester`, the live preview row
  (signed amount, category chip, relative date label; absent values omitted, nothing shown when all
  are null), a full-width Continue button disabled for blank text, and a "Fill manually" text button.
  Respects `MOVIMIENTO_DESCRIPTION_MAX_LENGTH` with the existing counter style.
- `MovimientoSummaryStep`: `TallybookSurface` card with tipo · relative date label, prominent signed
  amount (reuse `signedMovimientoAmountText`) or a "Add the amount" required message, review hint
  when flagged, the description in quotes, the selected category chip (pending style when flagged)
  and suggestion chips (tap = select). Footer: one row, two equal-weight buttons — left secondary
  "Edit movimiento", right primary "Save" (loading state while submitting).
- `CategorySuggestionChip` (`core/ui/category/`): compact pill with leading `+`, category icon and
  name, outlined with the category tint; selected variant filled. Designed to be reused by phase 2's
  `CategoryTagField`, so keep the API category-agnostic (`categoria`, `selected`, `onClick`).
- Pending treatment: a `pending` flag on `CategoryAvatar` (dashed outline, neutral colors) and on
  `MovimientoRowContent` (muted "To complete" label in the subtitle). Callers pass the flag; this task
  does not decide what is pending. Update row semantics so TalkBack/VoiceOver reads the label.
- Placeholder: string per locale with one amount argument
  (`movimiento_phrase_placeholder` = "Gasté %1$s en cine", es-AR "Gasté %1$s en el súper",
  en "Spent %1$s on lunch", pt-BR "Gastei %1$s no cinema"), formatted through the existing money
  formatter with a typed `Record`-style table `Moneda → sample minor units`
  (COP 20.000, MXN 200, ARS 5.000, CLP 5.000, BRL 30, USD 12.50, DOP 500, PEN 25).

## Files

- Create the components above and `feature/movimientos/viewmodel/AddMovimientoEntryUi.kt` for the
  models. Modify `CategoryAvatar.kt`, `MovimientoRowContent.kt`, `UiCatalogScreen.kt` /
  `UiCatalogFixtures.kt`, the four `strings.xml` (own block of keys only).
- Previews in a `*Previews.kt` file next to each step, light and dark.
- Device tests in `androidDeviceTest` for the chip and summary semantics (compile required; execution
  follows the runner limits in the backlog).

## Done when

Components render in previews and the UI Catalog for all states (empty, partial, complete, missing
amount, ambiguous amount, pending category, submitting), `detekt ktfmtCheck` clean, Android and iOS
compile. No changes to `AddMovimientoSheet`, `MovimientoForm` or the ViewModel.
