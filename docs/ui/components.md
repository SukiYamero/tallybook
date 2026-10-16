# Tallybook UI component catalog

This is a concise inventory of shared UI building blocks. Use it to choose an existing component before
creating another one. Visual tokens, interaction rules, and ownership remain defined in
[architecture.md](architecture.md).

## Foundations

| Component | Use it for | Do not use it for |
|---|---|---|
| `TallybookTheme` | Providing the shared color, typography, spacing, dimension, radius, and motion tokens | Storing feature state or screen layout decisions |
| `TallybookScreenFrame` | A top-level screen with safe-area policy, screen insets, and optional bottom chrome | A modal or a nested card |
| `TallybookScreenHeader` | A page title row with optional navigation and actions (Categories) | A sheet title (use `TallybookSheetTitle`); its 48dp controls center on the title and extend past the row, so the title sits where title-only headers put it |
| `TallybookSurface` | A tokenized card or the visual foundation of another shared component | A feature-specific layout with business behavior |
| `TallybookSheetForm` | A modal sheet form with text inputs: edit/add movimiento, the Categories editor and picker quick-create | Header and footer stay outside a body that scrolls up to a maximum height (optionally a minimum one). The sheet never moves for the keyboard: while it is open the body keeps its resting height, gains the keyboard height as scroll room, keeps an input marked `revealWhenFocused()` centered above it on Android, and the footer gives way until the keyboard closes. |
| `TallybookModalBottomSheet` | Modal creation, movimiento detail/edit or category selection | A permanent screen or navigation destination. Optional `onBack` runs in the native modal dispatcher; content modes with different natural heights need distinct detent identifiers, and each tracks its own natural height as the content grows. |
| `TallybookModalDialog` | A form-like modal over the app, such as the sentence entry | Card centered vertically when it opens, with a narrow `space12` side gutter and `space24` inner padding. That top edge is kept while the content grows, so height changes animate downward without a jump; only the keyboard or the screen edge moves it up, by the minimum needed, and it returns to the captured top when they leave. The height left above the keyboard bounds its content, which must tolerate that. Scrim tap and Back call `onDismissRequest`; `onHidden` runs after the exit animation, once the content has left composition. |
| `TallybookConfirmationDialog` | Confirming a decision over an existing screen or sheet | Presenting a form or replacing a bottom sheet |

## Actions and selection

| Component | Use it for | Notes |
|---|---|---|
| `TallybookButton` | A primary, filled action, optionally with an icon | Use the action label as the visible content; it preserves the shared subtle press feedback. `TallybookButtonDefaults.destructiveColors()` supplies paired danger colors for a destructive CTA; confirmation callers select them with `confirmColors`. |
| `TallybookTextButton` | A secondary, low-emphasis action with text and optional icon | Use when a filled background would overstate hierarchy. |
| `TallybookIconButton` | A compact icon-only action such as search clear or a contextual control | It requires an accessibility description, supports neutral, brand and ghost (icon only, no container, for navigation over the screen background) styles, accepts explicit colors, and takes an optional icon size (default `iconButtonIconSize`). `destructiveColors()` pairs a soft red container with the danger icon color. |
| `TallybookSegmentedControl` | Selecting exactly one peer option, such as Gasto or Ingreso | Tonal and neutral styles share selection behavior. `labelStyle` lets a caller choose a typography token without changing other controls. |
| `TallybookPeriodPickerHeader` | Giving the active day, week, or month its contextual title next to the year control | The caller supplies the title, optional subtitle, and year-control slot. |
| `TallybookPeriodPickerRow` | Choosing one bounded day, week, or month period in a horizontally scrollable row | The caller owns the typed period values; optional captions and data indicators are presentational metadata. |
| `TallybookYearMenu` | Changing the year that scopes a period picker | It is an anchored, accessible popup with the same scrim and motion as the date picker; four year rows are visible before its list scrolls. |
| `TallybookSelectionField` | Opening a dedicated selection flow from a form | It displays a value or placeholder; it is not a free-text input or inline dropdown. |
| `CategoryChip` | Selecting a category inside the category picker, or opening one from the Categories screen | Use it in the selection flow, not as a substitute for the form field. `trailingCheck` adds the multi-select check mark; `trailingContent` follows the name (the Categories screen's movimiento count and hidden mark, read through the chip's state description); a disabled chip is dimmed. `pending` swaps in the neutral dashed treatment for Otros. A `null` `onClick` renders it as non-interactive content that keeps the selected semantics. |
| `CategoryTag` | Showing a category of the current selection at the one tag size (28 dp avatar, 36 dp tall) | Tinted fill; `pending` swaps in the neutral dashed Otros treatment; `onRemove` adds a trailing × whose 48 dp tap area is a separate button labelled with the category name. The tag body is never tappable and keeps selected semantics. |
| `CategorySuggestionTag` | Offering one category as a tappable suggestion | Outlined with a leading plus and plain button semantics: choosing it moves the category into the selection, where `CategoryTag` shows it as selected. |
| `CategoryAddTag` | The "+ Add" tag that opens the multi-select category picker | Neutral outline; its accessibility label is "Add category". |
| `CategoryPickerSheet` | Choosing one to six categories from the catalog | One level, no drill-in: a search field, a "Selected · tap to remove" section of small removable tags (hidden while nothing is selected), then `CategoryChip` tags grouped by family (`categoryFamilies`, shared with the Search filters), the parent first and its children after, with extra space between families and no headers. Controlled by `selectedIds`: each tap applies `CategoriaSelection` and reports the new list live, so a parent/child replacement shows at once; "Done (n)" closes. Search lists every matching tag in one flat block. Tags that would change nothing (the last selected one, or any other at the limit) are dimmed and the reason is explained. It is the only picker: creation, review and editing share it, and the Otros fallback is excluded by the caller. |
| `CategoryForm` | The fields of a custom category, shared by picker quick-create (`COMPACT`) and the Categories editor (`FULL`) | Stateless over `CategoryDraftState` and `CategoryFormActions`; callers own title, save and dismissal. Name with counter/error, current-icon avatar expanding to a 48 dp radio grid (the dashed Otros icon is excluded), tint options that preview the current icon as an avatar in each tint, and description with counter; `COMPACT` keeps tint and description behind "More options". Duplicate names show as the name error; the caller supplies the optional action ("Use X", "Open X"), never shown for Otros. |
| `CategoryTagField` | Every category surface of the entry flow: label, `n/max` counter, tags with remove, Add tag and optional suggestions | Stateless: callers pass the selected categories, `max`, the pending flag and suggestions. The × is hidden on the only remaining tag, Add and suggestions disappear at `max`, and suggestions already selected are skipped. Selection rules live in `CategoriaSelection`. |

## Sheet headings

Existing and future bottom sheets use these shared headings and the standard defined in
[UI architecture](architecture.md#bottom-sheet-typography-hierarchy).

| Component | Use it for | Notes |
|---|---|---|
| `TallybookSheetTitle` | The visible primary title of a bottom sheet | Manrope sheetTitle token, 20sp/800; heading semantics and caller placement. |
| `TallybookSectionHeading` | A section label above its values or controls | Manrope sectionHeading token, 16sp/600; tertiary gray, optional error color and heading semantics. |

## Inputs and feedback

| Component | Use it for | Notes |
|---|---|---|
| `TallybookInputLabel` | Identifying an input field, including Description | Independent inputLabel token, 14sp/400; gray or supplied error color, without section-heading semantics. |
| `TallybookTextField` | A themed single-line or multi-line text input with label, placeholder, error, and leading or trailing content | It owns focus, cursor, border, and disabled appearance; optional `labelContent` preserves the semantic label. `minLines`/`maxLines` fix the height of a multi-line field that must not grow its container. |
| `TallybookSearchField` | Search queries with a search affordance and optional clear action | Its outer size remains stable while the query changes. |
| `TallybookAmountInput` | A prominent monetary value with its currency symbol, decimal keyboard, and field feedback | It does not parse, normalize, or format while the user is editing; optional `labelContent` supplies a field label. |
| `TallybookDateChipPicker` | Selecting one calendar date from a compact date chip | S and M variants preserve the minimum touch target; both apply a date immediately and support inclusive `minDate`/`maxDate`. Omitted bounds preserve existing callers. |
| `TallybookChoicePill` | Choosing one option in a wrapping group, including Search date presets | Caller owns selection and layout. Centered 36 dp visual surface, minimum 48 dp target, selected/disabled semantics and no press ripple or shading. |
| `TallybookFilterTag` | Showing an applied filter with one remove action | Compact 28 dp surface and caption text, centered in a minimum 48 dp target. Caller supplies the localized remove description, an optional category palette and an optional `leadingIcon`. |
| `TallybookFieldMessage` | Error or supporting text associated with a field | Prefer the field's `message` parameter when it is available. |
| `TallybookSnackbar`, `TallybookSnackbarHost` | The current localized notification in a caller-owned queue | Plain rendering accepts leading/action/dismiss slots and a root modifier; the host times only the visible head. Callers own FIFO delivery, token/revision acceptance, foreground/modal activity and placement/insets. Duration and Close rules follow the shared [presentation policy](architecture.md#snackbar-presentation). Busy exposes leading progress and disables any Close without click semantics. |
| `TallybookSkeleton`, `TallybookSkeletonLine`, `TallybookSkeletonCard` | Static placeholders for data whose layout is known | Lines measure typography and font scaling; cards accept a content slot. The caller owns feedback timing and loading semantics; sample text is excluded from accessibility. |
| `RetainedLoadState<Content, Failure>` | Owning one complete snapshot and typed loading/error status in a ViewModel | `refreshing()` and `failed(error)` preserve the last snapshot; replace it with `Ready(value)` on success. A query change starts `Loading()` without old data. Completed empty results retain their content identity. |
| `LoadingPresentation`, `rememberLoadingPresentation`, `deferLoading` | Deferring initial visual loading feedback while preserving a coherent rendered snapshot | Select `isInitialLoading` and a typed `requestKey` only from screen state. Metadata must not change request identity; cached content, results and errors bypass the delay. Skeleton layout and semantics belong to each screen. |
| `InputFocusReveal` | Android-only smooth centering of an already focused form input inside a scrollable sheet | It is an implementation helper, not a feature-level component. iOS keeps its native input relocation. |
| `TallybookDismissGuard` | Protecting unsaved drafts when a modal closes | A non-visual state holder; callers provide `hasUnsavedChanges`, discard and dismissal callbacks. |

## Content and navigation

| Component | Use it for | Notes |
|---|---|---|
| `BalanceCard` | The Home or selected-period summary of balance and income/expense context | Full-width stacked income/expense rows with trailing amounts. Caller supplies the localized label, typed secondary-amount formatter and optional feedback footer; reuse its matching skeleton. |
| `BalanceCardSkeleton` | A pending Home balance summary | Matches the existing card's typography, spacing, surfaces and income/expense mini-cards; placeholder text has no semantics. |
| `MovimientoRow` | A compact movement item in a list | Optional `onClick` opens the stored row ID through the caller; placeholders remain non-interactive. Shared card amounts and skeletons use the size14 token. Callers pass `pending` for Otros: a dashed avatar and a muted "to complete" label announced with the row. A movimiento with several categories shows the first one's avatar and name plus a "+N" badge on the avatar, announced as "N more categories". |
| `MovimientoSentenceStep` | The content of the sentence dialog | Stateless: a `MovimientoSentenceUi` and callbacks. A fixed title sits above a scrolling body (labeled two-line input, a "Detected" card with the tipo and date badges, the amount when found and the tag field with suggestions, Continue, the voice placeholder and "Fill manually"), because the keyboard only shrinks the dialog's available height. |
| `MovimientoTypeBadge`, `MovimientoDateBadge` | The tipo and relative fecha of a movimiento as tinted pills | `Regular` for the dialog, `Large` for the review sheet. The tipo icon, label and accent come from one typed table shared with the type selector and the balance card. |
| `VoiceDictationPlaceholder` | The "or" divider, title, microphone disc and "Coming soon" caption under the main entry action | Non-functional: announced as one disabled button until dictation ships. |
| `MovimientoSummaryStep` | The review content after the sentence | Stateless: title and subtitle, labeled sections (Type and Date as large badges side by side, Amount, quoted Title, an optional Description field kept above the keyboard, the tag field with suggestions) and the Edit movimiento (left) / Save (right) footer anchored at the bottom, at least `space32` below the content. Callers give it a minimum height to make the sheet tall (`rememberReviewMinimumHeight`). Amount review and missing-amount messages are announced. |
| `MovimientoRowSkeleton` | A pending movement row | Shares the current row's avatar size, spacing and surface, with title, description and amount lines stacked like the row. |
| `TallybookBottomNavigation` | Stable primary destinations and the centered Add movimiento action | A `null` selection callback renders a non-interactive bar for catalogs and fixtures. |
| `TallybookNavHost` | Connecting approved routes to shared navigation UI | It owns route composition, not feature business logic. |

## Reference environment

`UiCatalogScreen` is the in-app debug catalog used to inspect component states, themes, and interaction
variants on physical devices. It is a verification surface; production screens should consume the shared
components directly.

The current creation flow and retained category capabilities are documented in
[Add movimiento](../features/add-movimiento.md).
