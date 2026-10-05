# Tallybook UI component catalog

This is a concise inventory of shared UI building blocks. Use it to choose an existing component before
creating another one. Visual tokens, interaction rules, and ownership remain defined in
[architecture.md](architecture.md).

## Foundations

| Component | Use it for | Do not use it for |
|---|---|---|
| `TallybookTheme` | Providing the shared color, typography, spacing, dimension, radius, and motion tokens | Storing feature state or screen layout decisions |
| `TallybookScreenFrame` | A top-level screen with safe-area policy, screen insets, and optional bottom chrome | A modal or a nested card |
| `TallybookSurface` | A tokenized card or the visual foundation of another shared component | A feature-specific layout with business behavior |
| `TallybookModalBottomSheet` | Modal flows such as creating a movimiento or choosing a category | A permanent screen or navigation destination |
| `TallybookConfirmationDialog` | Confirming a decision over an existing screen or sheet | Presenting a form or replacing a bottom sheet |

## Actions and selection

| Component | Use it for | Notes |
|---|---|---|
| `TallybookButton` | A primary, filled action, optionally with an icon | Use the action label as the visible content; it preserves the shared subtle press feedback. |
| `TallybookTextButton` | A secondary, low-emphasis action with text and optional icon | Use when a filled background would overstate hierarchy. |
| `TallybookIconButton` | A compact icon-only action such as search clear or a contextual control | It requires an accessibility description and supports neutral and brand styles. |
| `TallybookSegmentedControl` | Selecting exactly one peer option, such as Gasto or Ingreso | Tonal and neutral styles share selection behavior. `labelStyle` lets a caller choose a typography token without changing other controls. |
| `TallybookPeriodPickerHeader` | Giving the active day, week, or month its contextual title next to the year control | The caller supplies the title, optional subtitle, and year-control slot. |
| `TallybookPeriodPickerRow` | Choosing one bounded day, week, or month period in a horizontally scrollable row | The caller owns the typed period values; optional captions and data indicators are presentational metadata. |
| `TallybookYearMenu` | Changing the year that scopes a period picker | It is an anchored, accessible popup with the same scrim and motion as the date picker; four year rows are visible before its list scrolls. |
| `TallybookSelectionField` | Opening a dedicated selection flow from a form | It displays a value or placeholder; it is not a free-text input or inline dropdown. |
| `CategoryChip` | Selecting a category inside the category picker | Use it in the selection flow, not as a substitute for the form field. |

## Inputs and feedback

| Component | Use it for | Notes |
|---|---|---|
| `TallybookTextField` | A themed single-line or multi-line text input with label, placeholder, error, and leading or trailing content | It owns focus, cursor, border, and disabled appearance. |
| `TallybookSearchField` | Search queries with a search affordance and optional clear action | Its outer size remains stable while the query changes. |
| `TallybookAmountInput` | A prominent monetary value with its currency symbol, decimal keyboard, and field feedback | It does not parse, normalize, or format while the user is editing. |
| `TallybookDateChipPicker` | Selecting one calendar date from a compact date chip | S and M variants preserve the minimum touch target; both apply a date immediately and support `maxDate`. |
| `TallybookFieldMessage` | Error or supporting text associated with a field | Prefer the field's `message` parameter when it is available. |
| `InputFocusReveal` | Android-only smooth centering of an already focused form input inside a scrollable sheet | It is an implementation helper, not a feature-level component. iOS keeps its native input relocation. |
| `TallybookDismissGuard` | Protecting unsaved drafts when a modal closes | A non-visual state holder; callers provide `hasUnsavedChanges`, discard and dismissal callbacks. |

## Content and navigation

| Component | Use it for | Notes |
|---|---|---|
| `BalanceCard` | The Home summary of total balance and income/expense context | Use the semantic balance colors; do not reuse it as a generic card. |
| `MovimientoRow` | A compact movement item in a list | It renders movement-specific presentation only. |
| `TallybookBottomNavigation` | Stable primary destinations and the centered Add movimiento action | Reserved destinations remain non-interactive until their screens exist. |
| `TallybookNavHost` | Connecting approved routes to shared navigation UI | It owns route composition, not feature business logic. |

## Reference environment

`UiCatalogScreen` is the in-app debug catalog used to inspect component states, themes, and interaction
variants on physical devices. It is a verification surface; production screens should consume the shared
components directly.

The current creation flow and retained category capabilities are documented in
[Add movimiento](../features/add-movimiento.md).
