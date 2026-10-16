# Wave 4 / Task 2 — UI foundations for the redesign

## Goal

Stateless, reusable components for the redesigned entry: one medium tag family, the multi-tag
field, type and date badges, a top-anchored modal dialog, the voice placeholder, a multi-select
picker mode and the pure selection rules. Wave 5 wires them.

## Components

- `CategoryTag` (`core/ui/category/`): the medium size used today by the sentence preview chip
  (28dp avatar, `radius20`, compact padding) becomes the only tag size. Variants: selected (tinted
  fill, optional trailing remove "×"), suggested (outline, leading "+"), pending (dashed neutral, for
  Otros), add ("+ Add"). Accessibility: button role where tappable, remove action labelled with the
  category name, selected state never color-only. Replace `CategorySuggestionChip` and the private
  preview chip in `MovimientoPhraseStep`; delete what becomes unused.
- `CategoryTagField` (`core/ui/category/`): label with an `n/6` counter, a `FlowRow` of selected
  tags with remove (hidden on the last real category), the add tag (hidden at six), and an optional
  "Suggestions" sub-label with suggested tags. Inputs: selected categories, pending flag, suggestions,
  max, enabled; callbacks: remove, add, suggestion.
- `MovimientoTypeBadge` (`core/ui/movimiento/`): icon + colored label for gasto/ingreso in two sizes
  (regular for the dialog, large for the summary). Extract the icon/color mapping shared with
  `MovimientoTypeSelector` and `BalanceCard` into one typed table instead of duplicating it.
- `MovimientoDateBadge`: calendar icon + relative date label ("Today", "Yesterday", otherwise a
  short localized date), same two sizes. Reuse `MovimientoRelativeDate.kt`.
- `TallybookModalDialog` (`core/ui/component/`): built on the same Unstyled dialog primitive as
  `TallybookConfirmationDialog`, anchored near the top with the screen gutter so the keyboard never
  moves it, scrim tap and Back routed to `onDismissRequest`, Tallybook surface/shape/motion tokens.
- `VoiceDictationPlaceholder` (`feature/movimientos/ui/`): "or" divider, "Dictate with voice" title,
  a round microphone button that does nothing and announces itself as coming soon, and a "Coming
  soon" caption.
- Multi-select mode in `CategoryPickerSheet`: `selectedIds: List<String>`, toggle with a check
  mark per row, a "Done (n)" primary action, the limit enforced through `CategoriaSelection`, drill
  into parents without selecting them (the parent row inside its children selects it). Keep the
  single-select API working for callers until wave 5 removes it.
- `CategoriaSelection` (`core/model/`): the pure rules in the wave contract, with tests (limit,
  duplicates, parent ↔ child replacement both ways, siblings coexist, last real category kept, Otros
  dropped when a real category is added, Otros kept when it is the only one).

## Constraints

Tallybook tokens and existing primitives only; copy per `docs/wording.md` in the four locales;
previews light/dark and UI Catalog entries for every variant; no ViewModel, repository or DI access.
Do not touch `Movimiento.kt`, repositories, `.sq` files, view models or rows (task1 owns them).

## Done when

`detekt ktfmtCheck :shared:testAndroidHostTest :shared:compileKotlinIosArm64
:shared:compileAndroidDeviceTest :androidApp:assembleDebug` pass on the final tree.
