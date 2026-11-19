# Tallybook UI architecture

This document is the durable source of truth for how Tallybook UI is designed, implemented, and
reviewed. It covers the shared Compose Multiplatform UI on Android and iOS. Feature specifications may
reference this guide, but must not duplicate it.

## Product direction

Tallybook follows Moneta's visual language and interaction density while remaining a native shared UI.
The goal is visual and behavioral parity, not a literal translation of HTML, CSS, or React components.

The target UI does not use Material components or Material visual defaults. Existing Material-based
screens remain valid during the incremental migration, but every migrated surface must use the
Tallybook-owned theme and components described here.

The reference implementation is the Moneta repository:

- `web/moneta/src/styles/index.css` is the visual ground truth.
- `web/moneta/docs/ui/design-tokens.md` explains the rationale behind the tokens.
- The relevant Moneta component is inspected before its Compose counterpart is designed. A screenshot
  alone is not enough when the source can answer layout, state, or motion questions.

When the reference and current Tallybook behavior disagree, preserve business behavior and treat the
visual difference as a design decision. UI migration must not silently change navigation, persistence,
validation, or domain rules.

## Chosen foundation

The UI stack has three layers:

1. **Compose Foundation and runtime** provide layout, drawing, text, gestures, semantics, animation,
   focus, and platform insets.
2. **Compose Unstyled 2.x** provides renderless interaction primitives when a component has non-trivial
   behavior such as dialogs, modal sheets, popups, or menus. Add only the module required by the
   component.
3. **Tallybook UI** owns appearance, tokens, component APIs, accessibility policy, and composition.

Compose Unstyled is the native equivalent of the Radix layer used under shadcn: it supplies behavior,
not a visual identity. Composables UI may be used as a source-owned implementation reference, similar
to copying a shadcn component into the application. Copied code must be adapted to this architecture;
it is not a new runtime design-system dependency.

Lucide remains the icon family and Manrope remains the typeface. Navigation 3 remains responsible for
destinations and back-stack behavior; it does not determine navigation chrome.

### Dependency rules

- Do not add a visual component library to make the UI look approximately right.
- Reuse a Compose Unstyled primitive before implementing complex focus, dismissal, or drag behavior.
- Use Foundation directly for simple components such as cards, rows, buttons, chips, and navigation
  chrome.
- Do not add a dependency for a single component unless its behavior is difficult to implement and
  verify correctly on both platforms.
- Any version-sensitive library must pass the external API gate in `docs/README.md` before adoption.

Primary references:

- <https://composables.com/compose-unstyled/docs/overview>
- <https://composables.com/compose-unstyled/docs/custom-themes>
- <https://composables.com/ui>
- <https://kotlinlang.org/docs/multiplatform/compose-platform-specifics.html>
- <https://developer.android.com/develop/ui/compose/system/insets-ui>

## Architecture and ownership

The component hierarchy is intentionally small:

```text
TallybookTheme
└── TallybookAppShell
    ├── feature screen
    │   └── reusable Tallybook components
    │       └── Foundation or Compose Unstyled primitives
    └── bottom navigation chrome when its increment is enabled
```

Responsibilities are split as follows:

| Layer | Owns | Must not own |
|---|---|---|
| Theme | All visual tokens: colors, typography, spacing, dimensions, radii, motion, and layout constants | Feature state or component composition |
| App shell | Edge-to-edge background, bottom-navigation placement, shared overlay host | Feature-specific content or destination policy |
| Screen frame | Safe-area edges, screen content width, screen-level spacing | Bottom-bar internals or modal insets |
| Component | Its token selection, shape, internal composition, states, semantics, minimum hit target | Raw visual values, screen position, or business state |
| Feature | Composition and domain-specific copy/state | Raw visual values, new token systems, or duplicated primitives |

Reusable components accept state and callbacks. They do not read ViewModels. Feature routes connect
`UiState` and `Actions` to stateless screen content.

### Intended package structure

Create packages only as the pilot needs them:

```text
core/ui/theme/       colors, type, spacing, dimensions, radii, motion, theme locals
core/ui/layout/      TallybookAppShell, TallybookScreenFrame, inset policy
core/ui/component/   reusable application components
feature/<name>/ui/   feature composition
```

Avoid a catch-all component catalog. A component moves into `core/ui/component` only after it has a
stable, domain-neutral API or a second real consumer.

## Component catalog

The concise inventory in [components.md](components.md) records the Tallybook components currently
available and the product cases each one covers. This architecture document remains the source of truth
for tokens, ownership, interaction policy, and review rules; the catalog does not duplicate those
decisions.

## Design tokens

Every static visual value is a token. Production composables must not contain anonymous `dp`, `sp`,
color, radius, duration, alpha, or easing literals. This rule applies even when a value is used only
once: a genuinely unique value receives a semantic component token instead of remaining embedded in
the component.

This gives every measurement a name, makes design review searchable, and prevents small variations
from accumulating across screens. It does not mean every value is global: primitive scales are shared,
while genuinely unique measurements live in a clearly named component-token group.

Examples:

- Use `TallybookSpacing.space16`, not `16.dp`.
- Use `TallybookTypography.body`, not `fontSize = 14.sp`.
- Use `TallybookComponentSize.searchHeight`, not `46.dp` inside `HomeSearch`.
- Add `TallybookComponentSize.amountDisplayHeight` before using a new one-off measurement.

Token declarations are the only files allowed to contain static visual literals. Runtime values
reported by the platform, measured constraints, calculated percentages, data-driven chart values, and
animation progress are not design literals and remain local to the logic that calculates them.

### Required token groups

- **Colors:** semantic canvas, surfaces, five foreground tiers, borders, status colors, and charts.
  Preserve the palette already ported from Moneta, but expose it without `MaterialTheme`.
- **Typography:** a primitive size scale and semantic `TextStyle` roles backed by Manrope. Components
  consume semantic roles; they never set `fontSize`, `lineHeight`, or `letterSpacing` directly.
- **Spacing:** a finite primitive scale used for every padding, gap, offset, and layout separation.
- **Dimensions:** named sizes for controls, icons, touch targets, screen constants, and unique component
  measurements that do not represent spacing. The shared icon scale is 16 dp, 20 dp, and 24 dp;
  an additional semantic token preserves the established movement-summary icon size.
- **Radii:** explicit named steps. Moneta's radii are not a geometric scale.
- **Motion:** duration and easing roles, including the iOS-like sheet curve and reduced-motion policy.
- **Screen layout:** semantic composition tokens such as the horizontal screen inset, top content
  floor, and bottom-navigation content height.

### Spacing scale

The initial `TallybookSpacing` scale is ported from the values already present in Moneta:

| Token | Value |
|---|---:|
| `space0` | 0 dp |
| `space2` | 2 dp |
| `space4` | 4 dp |
| `space6` | 6 dp |
| `space8` | 8 dp |
| `space10` | 10 dp |
| `space12` | 12 dp |
| `space14` | 14 dp |
| `space16` | 16 dp |
| `space18` | 18 dp |
| `space20` | 20 dp |
| `space22` | 22 dp |
| `space24` | 24 dp |
| `space28` | 28 dp |
| `space32` | 32 dp |
| `space40` | 40 dp |
| `space48` | 48 dp |

The names are primitive steps, not semantic instructions. A component may expose semantic aliases such
as `screenHorizontal = TallybookSpacing.space20` or
`modalContentHorizontal = TallybookSpacing.space22`. If a future design needs a missing value, add one
documented step to the scale or a semantic dimension token before using it. Do not round it to the
nearest existing value merely to avoid adding a token.

### Typography scale

`TallybookTypeScale` defines the complete size range already observed in Moneta:

| Token | Size |
|---|---:|
| `size10` | 10 sp |
| `size11` | 11 sp |
| `size12` | 12 sp |
| `size13` | 13 sp |
| `size14` | 14 sp |
| `size15` | 15 sp |
| `size16` | 16 sp |
| `size17` | 17 sp |
| `size18` | 18 sp |
| `size19` | 19 sp |
| `size20` | 20 sp |
| `size21` | 21 sp |
| `size22` | 22 sp |
| `size28` | 28 sp |
| `size30` | 30 sp |

`TallybookTypography` maps these primitives to semantic roles such as `caption`, `label`, `body`,
`controlValue`, `bodyEmphasis`, `title`, `headline`, `amount`, and `amountSymbol`, including weight and line height. Feature code
uses those roles rather than primitive sizes. If a new visual role needs a different size, weight, or
line height, define or extend the semantic role first; even one-off display text must be named.

### Bottom-sheet typography hierarchy

Bottom sheets use Tallybook's Manrope tokens and own visual language. Radix's distinction between
[Heading and Text](https://www.radix-ui.com/themes/docs/theme/typography) is a hierarchy reference;
its theme, fonts and components are not adopted.

This is the standard for existing and future bottom sheets. Use `TallybookSheetTitle` and
`TallybookSectionHeading` with their shared semantic roles; do not define sheet-specific font sizes
in feature content.

- `TallybookSheetTitle`: `sheetTitle`, 20sp/800, primary foreground, heading semantics.
- `TallybookSectionHeading`: `sectionHeading`, 16sp/600, tertiary gray foreground, heading semantics.
  These sheet-specific roles preserve typography outside bottom sheets. Section headings identify
  filter date/type/category groups and creation's type/category-choice groups.
- `TallybookInputLabel`: `inputLabel`, 14sp/400 with 20sp line height, gray foreground and optional
  error color. Category/Date/Amount/Description field labels use this separate role, not the sheet
  section heading. Input labels retain their field descriptions without heading semantics.
- Section values use regular weight: `controlValue` is 13sp/400 for compact choice/category/type
  options; `body` is 15sp/400 for field/date values. Selection is conveyed by existing surfaces,
  borders, icons and color, without adding bold text.
- Amount-entry values/symbols keep their 36sp/30sp sizes and use regular weight. Movement-card
  monetary values keep their separate row style.
- From/To identify nested endpoint fields; they remain subordinate labels, not section headings.
  Scrollable bodies, reachable footers, 48dp interaction targets and font scaling remain required.

Shared inputs retain their semantic label strings and accept optional `labelContent` for caller-owned
labels. Omitted slots use the same `inputLabel` role. Category-picker pane titles follow the
visible caller header when overridden.

### Balance summary cards

Home and History reuse `BalanceCard` and its matching `BalanceCardSkeleton`. Home supplies the
primary currency's total balance with this month's income and expenses, under its default localized
balance label, and the whole card opens Accounts. History supplies the selected period's snapshot and
localized Period balance label; it does not observe the total balance.

Income and Expenses occupy two full-width white rows under the total. Each row places its icon/label
at the start and the complete amount at the end, preserving currency, sign and decimals. A shared
single-pass layout measures both regions at the available width. If both fit beside one another,
place them on one line; otherwise place the whole label above the trailing amount in the same
full-width surface. Both real rows and skeletons use this layout, respecting font scale and direction. The
usual COP range through 2,000,000 must remain legible at the physical target's default and enlarged
font sizes; no abbreviation, rounding, ellipsis or arbitrary font shrinking is introduced.

The shared card uses `balanceLabel` (size15, semibold) for its title and Income/Expenses labels,
`balanceSecondaryAmount` (size17, bold) for income/expense values, and the existing `headline`
(size21, extrabold) for the total. Skeletons consume the same roles. History retains signed secondary
values through the existing money formatter; Home retains its current unsigned value convention.
Caller-owned loading/error/retry/stale notices remain associated with History's retained period state.
A failed initial read displays feedback without invented zero totals; deferred loading remains delayed.

### Movement-card amounts

All movement cards in Home, History and Search share `movementRowAmountStyle`: the `title` role with
`TallybookTypeScale.size14`, retaining bold weight and its line height. The row skeleton consumes the
same style. Formatting, signs and currency rules remain in the existing money presentation helper.

Their text column is the title (`cardTitle`, 14sp bold, at most two lines) with the category name as fallback,
then the description (`caption`, one line) only when present, and the signed amount last, below
them, so long titles and long amounts never compete for the same line. Text ends with an ellipsis and
no card shows the date.

The amount line starts with the movimiento's payment-method icon (`MedioPagoIcon`, `iconSmall`,
`foreground.secondary`); the amount keeps its style and color rule. The icon has no description of
its own: the row's merged description ends with the medio label after the amount and currency
("…, -$ 50.000,00, COP, Crédito"). Rows never show the account name. The skeleton reserves the same
icon slot so loading does not shift the amount.

### Token organization

The intended files are:

```text
core/ui/theme/Color.kt
core/ui/theme/Spacing.kt
core/ui/theme/Dimensions.kt
core/ui/theme/Radius.kt
core/ui/theme/Typography.kt
core/ui/theme/Motion.kt
```

Tokens are immutable values exposed through `TallybookTheme`. Their names describe either a stable
primitive step or a semantic purpose. Do not name tokens after the first screen that happens to use
them if the concept is shared.

Before adding a token:

1. Reuse an existing semantic token when it represents the same design role.
2. Otherwise reuse an exact primitive value and introduce a semantic alias when the role deserves a
   name.
3. If no exact primitive exists, add the new primitive or unique component dimension first.
4. Never reuse a close value when the design intentionally calls for a different one.

### Measured Moneta baseline

The following measurements are the starting contract for a 390 x 844 phone viewport. They are
reference dimensions, not a fixed device frame:

| Element | Baseline |
|---|---:|
| Screen horizontal content inset | 20 dp |
| Screen top content floor | 24 dp |
| Home header height | 44 dp |
| Header to search spacing | 20 dp |
| Search container | 46 dp high, 14 dp horizontal padding, 18 dp radius |
| Search to content spacing | 18 dp |
| Empty-state card | 26 dp radius, 24 dp horizontal and 48 dp vertical padding |
| Modal sheet top radius | 30 dp |
| Modal sheet horizontal content inset | 22 dp |
| Modal sheet bottom content inset | 28 dp plus the applicable system inset |
| Primary sheet action | 54 dp high, 18 dp radius |

These values may adapt for larger text, compact height, and wide screens. The visual body may be
smaller than the hit target, but every interactive target must be at least 48 x 48 dp in Tallybook.

## Edge-to-edge and safe areas

The application background draws edge to edge. Content and interactive chrome apply only the system
insets they own. Insets are conditional values reported by the platform; no iPhone model, Android
manufacturer, or hardcoded notch size is used.

### Ownership rules

- A normal top-level screen uses an effective top inset of
  `max(safeDrawing.top, TallybookLayout.screenTopFloor)`.
- Portrait content adds `TallybookLayout.screenHorizontal` after the safe left and right edges. On a
  phone without lateral insets, the result remains the same layout token.
- A standalone screen without bottom chrome uses an effective bottom inset of
  `max(safeDrawing.bottom, TallybookLayout.screenBottomFloor)`.
- The bottom navigation component owns its bottom system inset. Screen content receives the
  bar's published clearance and must not apply the same bottom inset again.
- A modal sheet owns its bottom safe inset and IME response. The obscured screen behind it does not add
  modal padding.
- Fixed overlays use the relevant safe edge directly. They do not inherit a blanket screen padding.
- `WindowInsets.ime` and `WindowInsets.safeDrawing` are separate concerns. Keyboard movement must not
  double-apply the navigation-bar inset.

The implementation should centralize this policy in `TallybookScreenFrame` and the owners of fixed
chrome. Do not scatter `statusBarsPadding()`, `navigationBarsPadding()`, or arbitrary top padding across
feature screens.

### Orientation support policy

Tallybook is designed and reviewed **portrait-first** on phones and tablets. Portrait is the reference
orientation for Moneta parity, spacing decisions, component composition, screenshots, and final visual
acceptance.

Landscape remains supported, but it is a functional compatibility target rather than an equal visual
optimization target. A landscape layout must:

- remain usable without crashes, clipped critical actions, or content hidden behind system UI;
- keep essential content reachable through scrolling when the available height is compact;
- preserve screen state and user input across rotation; and
- handle the keyboard, sheets, dialogs, and back behavior without trapping the user.

Landscape does not require pixel parity with the portrait reference, bespoke two-pane layouts, or the
same degree of spacing and density refinement. Do not add landscape-only composition or optimize a
screen exhaustively unless a feature requirement or observed usability problem justifies the extra
complexity. The application remains rotatable; this policy does not authorize locking it to portrait.

### Verification matrix

Every new top-level layout is checked in at least these conditions:

- Android gesture navigation and three-button navigation.
- Authorized physical iPhone with a Dynamic Island or notch and a physical device without one when available.
- Portrait as the visual acceptance reference, with keyboard open and enlarged system text.
- Landscape as a functional smoke check against the compatibility requirements above.
- Light and dark themes.

## Navigation architecture

Tallybook uses bottom navigation for its stable primary destinations. The bar is anchored to the
bottom edge and spans the full application width. It is not a detached floating pill like Moneta's
current bar.

Its background extends behind the bottom safe area and comes from semantic theme tokens:

- `navigationSurface` for the light/dark background.
- `navigationContent` for unselected icons and labels.
- `navigationSelected` for the active destination.
- `navigationDivider` for an optional subtle top boundary.

The bar has no external horizontal margin or detached pill shadow. An optional subtle divider or tonal
elevation may separate it from content, but it must come from the theme. The interactive row stays
above the home indicator or Android navigation area, while the themed surface fills behind it. Its
content height, icon sizes, label style, spacing, and published screen clearance are tokens; none are
hardcoded in the component.

### Patterns used by current finance apps

- **Monarch Money:** core destinations remain visible in bottom navigation while extra sections and
  settings live behind a profile or menu entry. This is a useful hybrid when the product has more
  sections than a bottom bar can hold.
- **Copilot Money:** Dashboard is the stable home; users can reorder the surrounding app sections.
  Creation is contextual: a manual transaction is added from the Transactions tab instead of a
  permanent center action.
- **Revolut:** Home is a stable bottom destination, while secondary financial tools such as analytics
  are entered from an action inside Home. The bottom bar represents broad destinations, not every
  capability.
- **Wallet by BudgetBakers:** navigation differs by platform: Android exposes a top-left menu while
  iOS exposes a bottom `More` destination. This scales, but Tallybook should avoid platform divergence
  unless user testing demonstrates a clear benefit.

Sources:

- <https://www.monarchmoney.com/new-mobile-navigation>
- <https://help.monarch.com/hc/en-us/articles/360054839131-Refreshing-Accounts>
- <https://help.copilot.money/en/articles/6045480-dashboard-tab-overview>
- <https://help.copilot.money/en/articles/9554412-transactions-tab-overview>
- <https://help.revolut.com/en-MT/help/accounts/budget-and-analytics/what-is-the-analytics-dashboard/>
- <https://support.budgetbakers.com/hc/en-us/articles/212249009-How-do-I-contact-support>

### Tallybook destination rule

Use bottom navigation only when there are three to five stable, peer-level destinations used often.
Do not use it as an index of every future feature. Less frequent sections belong in a `More` or profile
surface, and actions belong in the screen where their result is visible.

The chosen structure is a hybrid architecture:

- Keep Home as the stable entry destination.
- Reserve the bottom navigation for a small set of proven primary destinations.
- Put growing or administrative areas in a secondary menu.
- Keep `Create movimiento` as the centered navigation action.

The approved order and ownership are:

| Position | Role | Expected frequency | Current behavior |
|---|---|---:|---|
| Home | Primary destination | Daily | Enabled; fresh launch starts here |
| History | Primary destination | Frequent | Enabled; retained period and scroll |
| Add movement | Contextual action | Frequent | Opens the existing movement sheet |
| Statistics | Primary destination | Frequent | Enabled; retained period, currency and dimension |
| Profile | Primary destination | Occasional | Enabled; preferences and the account/data slot |

Search is a primary destination without a tab: retained query, filters and scroll, entered only from
Home's search field and the "View movements" intents (category, tag and account sheets, and
Statistics). While it is shown no tab is selected, and its root Back selects Home. If primary
destinations outgrow the bar, a menu is evaluated before adding a sixth position.

The bar uses icons without visible labels. Every icon still has a localized accessibility description.
The centered Add movement action opens the shared creation session; Home, History, Statistics and
Profile expose selected tab semantics.

`TallybookAppShell` measures the persistent bar before composing primary content and publishes its
actual occupied clearance, including its sole bottom safe inset. Home and History use the screen
frame's top/horizontal window insets and zero bottom floor; their lists add that clearance once.
Standalone/catalog frame defaults preserve `max(bottom safe inset, bottom floor)`. The creation sheet
retains its own safe/IME policy.

`TallybookNavHost` owns a small typed visited/selected navigation snapshot. Home always has an entry;
History is visited lazily and retained when hidden. Official saveable and ViewModel decorators are
hoisted above the active display, in that order, with stable content keys and parent creation extras.
Snapshotting extras for each parent lifetime prevents Activity Intent Bundles from resetting stores.
Tabs select existing entries; History root Back selects Home, Home root delegates platform exit, and
Search, Statistics and Profile root Back select Home. One `secondary` slot shows a secondary
destination over the selected primary with the bottom bar hidden: the UI catalog over any primary,
Categories and Tags only over Profile, Accounts, Recurrentes and Presupuestos over Home or Profile (`PrimaryNavigator.openSecondary`).
A snackbar's "Ver" reaches Presupuestos from anywhere through `PrimaryNavigator.showSecondary`, which
replaces an open secondary and falls back to Home when the selected primary cannot hold it. Back closes the
secondary first, and selecting any primary clears it. Home search opens Search with a one-time focus
intent; "View N movements" in a Categories or Tags sheet, or "View movements" in an account sheet,
selects Search, once that sheet has hidden, with a one-shot `SearchFilterRequest` (category, tag or
account ids, plus the account's currency) that the retained Search ViewModel applies once. Statistics
sends the same request, with the extra fields its [filter rule](../business-logic.md#search-and-filters)
names, from a tag or account row directly and from its category sheet's "View N movements" once the
sheet has hidden. That sheet opens through the shell's modal coordinator, so navigation and creation
wait while it is up.

Home/History repository observations use five-second subscription sharing. Feature routes revalidate
before lifecycle collection. `RetainedLoadState<Content, Failure>` is the shared snapshot contract:
`Ready` owns one complete value, `refreshing()` retains it in `Loading(previous)`, and `failed(error)`
retains it with a typed failure. Empty collections are valid completed snapshots; snapshot/error types
are non-null. ViewModels own this state and clear the snapshot when query identity changes. Snapshot
retention is process-local presentation state, not a second database or a cache of every query.

Home and History render valid retained content immediately when returning to a section, including
completed empty results, while their repositories revalidate in the background. The shared
`LoadingPresentation` mechanism delays initial feedback by 200 ms only when no valid snapshot exists;
results and failures bypass that delay. Callers select `isInitialLoading` and a typed request identity
derived only from screen state, including currency/period and excluding presentation metadata. Updates
within the same pending request preserve the original deadline. Initial content reserves summary space
without inventing totals; perceptible first reads show static skeletons. Read failures remain visible
and retry preserves the last valid snapshot only for the same request identity.
The period summary places balance above income/expenses in both content and skeleton states, avoiding
amount-dependent horizontal wrapping at ordinary text sizes.
Temporary loading/error lists use unsaved scroll state so they cannot clamp the retained content
position before fresh rows arrive. Typed History input is checkpointed lazily from its sole ViewModel
owner; rows, jobs and changed creation drafts are never saved.

One shell-owned creation session/store survives its hide transition and clears after its originating
hidden callback. Keyed callbacks carry that origin token; a delayed old completion cannot clear a new
session. Navigation stays fixed while the modal is open or closing. Session UI uses an ephemeral
saveable boundary, so a recreated shell starts a fresh draft. Android saved-owner recreation differs
from iOS retained-controller/saveable scope; a fresh iOS process starts with normal cold-launch defaults.

## Incremental migration

The redesign is delivered as small, device-verifiable increments. Business behavior remains unchanged
unless a separate feature specification says otherwise.

### Increment 0 — architecture guide

This document is the only deliverable. No production UI is changed.

### Increment 1 — Home outer shell pilot

The first implementation increment validates the architecture with the smallest recognizable slice:

- Replace Material ownership at the root with Tallybook theme locals and an edge-to-edge canvas.
- Add the shared screen frame and safe-area policy.
- Recreate Home's outer layout: header placement, search surface, content spacing, and the outer
  loading/empty/content containers.
- Preserve the existing Home state and actions. Inner dashboard cards may remain visually unchanged
  behind the new outer composition until their own increment.
- Add semantics and minimum hit targets to any migrated interaction.

Explicitly out of scope:

- Bottom navigation.
- The global or centered create button.
- The add-movement sheet and its fields.
- A redesign of balance, chart, category, or movement-row internals.
- New navigation destinations or business behavior.

Visual acceptance uses side-by-side review against Moneta on the authorized physical target, with
light/dark screenshots and no content under system UI. Android and iPhone runtime evidence remain
separate; record unavailable targets explicitly and retain their checks in the backlog.

### Later increments

1. Migrate Home dashboard components one family at a time.
2. Implement the themed, full-width bottom navigation with the approved destination set.
3. Decide the placement and persistence of `Create movimiento`.
4. Migrate the add-movement sheet and form components.
5. Apply the established components to the remaining screens.

Each increment must leave the app shippable. Do not create a parallel second screen tree for the
redesign.

## Component API rules

- Prefer value parameters and callbacks over slot APIs when the content has a stable visual role.
- Use slots only where variable content is a real requirement.
- Every reusable layout accepts a root `Modifier` as its first optional parameter.
- The caller controls external placement; the component controls its internal padding and shape.
- Model mutually exclusive visual states with a sealed type or enum, not several booleans.
- Colors are semantic inputs. Feature code must not contain raw brand colors.
- Interactive components expose enabled, selected, loading, pressed/focused, error, and disabled states
  when those states exist in the product.
- Components must work with screen readers, keyboard/focus traversal where supported, and enlarged
  text before they are considered reusable.

## Motion

Motion communicates hierarchy and state; it is not decoration.

- Sheets use the Moneta iOS-like easing curve and a shared duration role.
- Navigation transitions preserve spatial direction and back behavior.
- Touch controls use a subtle, shape-clipped tonal pressed overlay. A short tap preserves that overlay
  briefly after release so feedback remains perceptible. They do not use Material ripple or
  layout-changing press feedback; text fields retain focus and border feedback instead.
- On Android, a focused input requests smooth relocation into the visible viewport; iOS retains its
  native keyboard relocation behavior. An unconsumed background tap clears focus.
- Reduced-motion preference removes non-essential translation and scale while preserving state
  visibility.
- Motion values live in the theme only when at least two components share the same role.

## Testing and review

The narrowest useful verification for a UI increment includes:

- Unit tests for token or state transformations when logic exists.
- Compose UI tests for semantics, actions, selection, focus, and visibility.
- Screenshot/golden coverage for stable visual components when the project adds a supported harness.
- `./gradlew detekt ktfmtCheck`.
- `./gradlew :shared:testAndroidHostTest` and `./gradlew :androidApp:assembleDebug` for shared UI changes.
- Native iOS compilation and available authorized physical iPhone checks for affected APIs or layout.
- Final visual review on the confirmed physical target before merge; record unverified platforms
  and the user-accepted runtime scope. Compilation does not establish iPhone runtime acceptance.

Pixel comparison is evidence, not the only acceptance criterion. Accessibility, dynamic text, system
insets, and state behavior may require intentional differences from the web reference.

## Maintenance checklist

Use this checklist in every UI pull request:

- Does the change reuse an existing Tallybook component or prove why a new one is needed?
- Does every static visual value come from a primitive or semantic token?
- If the design introduced a new value, was its token added before the component used it?
- Is Material appearance or behavior leaking into a migrated surface?
- Does exactly one owner apply each system inset?
- Are touch targets at least 48 dp and semantics meaningful?
- Does the screen support loading, empty, error, and content states that already exist?
- Was behavior preserved while appearance changed?
- Was portrait treated as the visual reference and landscape kept functionally usable without
  unnecessary orientation-specific complexity?
- Was the result compared with the Moneta source and verified on Android and iOS?

## History presentation

History presentation uses the existing period picker with three scopes and a year context.
The header shows the full month name and the year menu once. Day adds the weekday and day number;
Week adds its day range, or one day for a single-day block; Month needs no supporting date line.
Month chips use localized abbreviations and week chips use compact day ranges. Accessibility labels
keep full dates, including the year.
The temporal and selection rules live in [business logic](../business-logic.md#tracking-and-history).
History starts directly with its contextual header and period controls. Scrollable content adds
`space20` above the header after the frame safe inset. Selected-period totals have
an explicit label and scroll with the content. No fixed global balance, currency banner or independent
global-summary status appears in History; the total balance remains on Home.
While accounts exist in more than one currency a `MonedaSelector` follows the header; day groups show
transfers as `ActividadTransferenciaRow`s that open transfer management. The totals leave transfers out;
the count line names them in a second part ("N movimientos · M transferencias entre cuentas") only
when there are any. History has no text-search field, non-date filters, balance-visibility
toggle or fourth annual scope. Search is a separate destination.

## Statistics presentation

[Business logic](../business-logic.md#statistics) owns scopes, comparison, sums and thresholds; this
section owns layout. Statistics starts with the same frame and `space20` top as History: a
`TallybookPeriodPickerHeader` with the month or year as title, a "Through <day>" subtitle while the
period is in progress and the year menu in Month scope; a `MonedaSelector` while accounts exist in
more than one currency; a neutral Month/Year `TallybookSegmentedControl`; and a
`TallybookPeriodPickerRow` of month or year chips. Cards follow in this order, each a
`TallybookSurface` with a heading:

- **Summary:** gasto as the headline with its variation and the compared amount, then ingresos and
  balance side by side. A variation reads up (danger) or down (success), since more gasto is
  unfavorable, and its accessibility label says the direction in words.
- **Where did your money go?:** a neutral dimension control (Categories, Tags, Accounts, Medios), the
  donut with the total and a group count in its center (no donut for Tags, which shows its note
  instead), then one `TallybookDistributionRow` per group and a hint naming what a tap opens. Medio
  rows are not interactive.
- **Evolution:** the selected month's gastos, ingresos and balance above the `TallybookTrendChart`,
  a legend, and a note for a partial last month.
- **Pace** (`TallybookPaceChart`) and **What changed** (icon rows), only when they apply.

The header has no movement count. A first read uses `LoadingPresentation` with two skeleton cards;
a failed refresh keeps the cards with "Previous data" under the summary and an error card with retry
below it; a failure without content shows only the error card. With no movimiento in the currency the
header and selector stay and a single empty card replaces the controls and cards.

The category detail is a `TallybookModalBottomSheet` with the pattern of the Categories chip sheet:
title, period subtitle and a ghost close button; a sunken total card with the real category avatar,
share, count and variation; the `TallybookCategoryTrendChart` with its average; subcategory rows in
the category's chart color; the three largest gastos; and a full-width "View N movements" button.
While it closes it keeps the content it last rendered, and it closes when the selection, currency or
day changes.

Group colors come from `colors.chart` by rank (`chartPaletteIndex`): ranks below 3 use their own index
and later ranks the next one, skipping index 3, whose red reads as gasto. Otras and any group past the
palette use `foreground.faint`. Category tints are not used, because several roots share a tint.
Home's "Your month in numbers" card colors its categories by the same rank rule.

Charts expose no semantics of their own: every chart is one node whose `contentDescription` carries
the localized summary, and the card's numbers repeat that information without the drawing.

## Remaining product decisions

The bottom navigation pattern, ordered positions, and centered placement of `Create movimiento` are
decided, and every destination is enabled. Profile grows only through the evaluated preference list in
its feature document; account and backup surfaces plug into its data section.

## Search presentation

This is the presentation contract for [Search](../features/search.md).
[Business logic](../business-logic.md#search-and-filters) owns matching and filter/date/count semantics.

Search follows the supplied composition: header/title and result status, filter icon, text input,
a `MonedaSelector` while accounts exist in more than one currency, horizontal removable applied tags using the
shared `TallybookFilterTag` (medio and account tags included), then a lazy movement list, transfers as
`ActividadTransferenciaRow`s, grouped under one `CivilDayHeader` per day (`dayHeading`, 15sp semibold, shared with History). Use the frame's top/horizontal safe area, `space20` above the header and shell-measured bottom clearance exactly once. Header/field
scroll with content; no fixed global balance or chart. Rows reuse MovimientoRowContent with the shared
title/description card text; category/currency/type semantics remain accessible.

The filter sheet occupies 72% of available height, revealing the underlying Search field in portrait.
It uses compact, vertically centered wrapping date presets, two existing date chips for Custom, wrapping
Todos/Gastos/Ingresos/Entre cuentas type pills, Medio (`MedioPagoFilterControl`) and Cuentas (the viewed
currency's accounts, archived included, plus selected ones of another currency and missing-ID fallbacks with
remove semantics), then parent-grouped real category chips with children visible. Show all persisted categories, with
their existing icons/tints, plus selected missing-ID fallback chips with remove semantics. While the
user has tags, a Tags section of `EtiquetaChip`s follows the categories. The body
scrolls while a separate Limpiar/Aplicar footer remains reachable above safe/IME insets. Preserve
minimum 48 dp targets and font scaling. Closing renders the captured draft as disabled until hidden.
Only one calendar popup is active; Back closes it before cancelling filters. Custom date rows show
localized From/To labels to the left of their controls. They use inclusive opposite-endpoint bounds:
To disables days before From, and From disables days after To;
both preserve today's maximum and allow a same-day range. Custom chips/rows include
year in display and full date semantics. Earlier persisted/restored years extend the shared date
picker's 2019 floor without changing its default behavior for existing callers. The expanded year
selector uses bounded lazy/scrollable rows, with selected-year reveal once on opening; older history
such as 1900 must remain selectable on compact screens without constructing an unbounded popup.
Calendar width respects horizontal safe insets and viewport margins. On narrow windows, only the
weekday/day grid scrolls horizontally; its 48dp Search targets retain their size while the month header
and year selector stay within the popup bounds.

### Loading and result currency

The shared `RetainedLoadState` contract remains the snapshot owner. A Search snapshot carries its
structural and normalized-text identity; derived `isCurrent` controls count/empty semantics. A single
last-settled snapshot may be rendered during text debounce/matching for the same structural criteria,
explicitly as updating, never as current. Different currency/date/type/category criteria discard it.
This bounded display is not a cache of prior queries. Failure of a different text request does not
retain that old text result as an error fallback.

| Condition | Body / loading treatment | Header and controls |
| --- | --- | --- |
| Fresh/recreated entry, no valid snapshot, read under 200 ms | Reserve result space; no skeleton flash; ready/error bypass delay | Live title/input/chips; no invented count or empty message |
| Same initial read still pending at 200 ms | Four static MovimientoRowSkeleton rows; one loading semantic group | Localized loading text replaces the count; real input stays usable; filters wait for real category/read metadata |
| New structural criteria/currency without snapshot | Clear unrelated rows immediately; delayed skeleton if read exceeds 200 ms | Applied chips/input update immediately; pending count unavailable |
| Return/resume/retry with same request snapshot, including empty | Render retained snapshot immediately; no skeleton | Pending count unavailable; discreet updating indicator only if actual revalidation exceeds 200 ms |
| Text debounce for same structural criteria | Keep last-settled rows as an updating display; no skeleton or spinner for the timer alone | Immediate localized loading text in the reserved count line; no old count/no-results assertion; busy semantics |
| Text computation still running 200 ms after it starts | Retain bounded settled rows; small shared progress indicator beside result status | Localized loading text; no numeric count until settled |
| Draft edits, sheet opening/cancel closing | No result query, loading or count preview | Underlying applied rows/count remain unchanged |
| Clear filters | Atomically commit defaults and close; results follow the new applied request | Raw text remains unchanged |
| Matching insert/update/delete/restore or category/locale change | Atomically replace when projection settles; retain same-request render during work | Suppress old count as current while known reprocessing is pending; no full-screen skeleton |
| Same-request failure with valid snapshot | Retain readable content plus explicit error/retry; no skeleton | Count is stale/unavailable, inputs preserved |
| Initial/different-request failure or invalid applied Custom dates | Explicit error/retry or date correction; never a perpetual skeleton/empty success | No count; fields/chip removal remain usable |
| Settled no-data or no-results | Distinct localized state; zero count from settled result | No spinner; no-data offers existing creation; no-results offers clear text/open filters |

Feedback uses `TallybookMotion.loadingFeedbackDelayMillis` (200 ms), TallybookProgressIndicator and
existing loading semantics. Do not add minimum loading duration or artificial request delay. During
debounce, hide prior empty-state assertions rather than stating that the newly typed query matches
nothing. Metadata/session changes cannot restart a pending structural-read deadline.

Apply `LoadingPresentation` to the result presentation only. Field text, applied chips and filter
actions must read live state while feedback is deferred. Reset the result presentation boundary on
structural identity change so a previous query cannot remain visible during the grace period. Do not
delay the entire SearchUiState or share a pending timer with session state. Results/failures cancel
feedback promptly; no spinner remains after cancellation or disposal.

Result count exists only for a settled current Ready snapshot and derives from its rows. Its line
shows localized loading text while work is pending, including first-read and debounce; resources own
the wording. Announce settled result count
politely, not each keystroke or placeholder. Filter active/expanded state and chip removals must be
meaningful without dots/color. Avoid repeating the same status through multiple live regions.

The content list has one saveable scroll owner. Temporary skeleton/error lists use unsaved state so
they do not clamp its retained position. Returning to Search and same-request updates preserve
scroll; a newly settled text/structural query resets results to the top once. Restoration preserves the saved content
position until rebuilt results can render; it must not trigger the new-query reset accidentally.

Home-to-Search focus is a one-time navigation intent: reveal the lazy header/field, wait for its
composition and successful focus, then acknowledge the token. This explicit entry may reset retained
scroll to reveal the field; an ordinary return retains position. Cancel the intent if Search becomes
inactive before completion. Selection/recomposition/restore never repeatedly refocuses or reopens
the keyboard. Opening filters
hides IME; dismissal returns accessibility focus to the filter button without forcing text keyboard.
The shell blocks navigation/creation while filters are Editing or Closing; actions carry session
identity and cannot reopen until the originating hidden cleanup. Applied input checkpoints contain no
draft, rows, focus or keyboard state. Existing-ViewModel Activity recreation retains its draft; a new
saved owner restores applied input with a Hidden session.

When an applied Custom becomes invalid after a clock change, retain its visible chip/error and the
last coherent same-currency years/category inputs for correction, while clearing invalid result rows
and starting no invalid read. Filter opening remains available with initialized currency and ready
categories in this correction state. If currency-year metadata is unavailable, the correction picker
uses the shared floor plus applied/draft endpoint years; cached metadata never establishes current
no-data. Currency change clears currency-specific metadata. A future selected date remains visible
as invalid, but its correction popup opens at a valid month capped by current today.


## Movimiento management and feedback

Domain rules live in [business logic](../business-logic.md#editing-and-deletion); accepted delivery
evidence and supported-version sources live in [Manage movimientos](../features/manage-movimientos.md).
Real Home/History/Search rows send their persisted ID through the shell's management admission.
Skeletons and empty states have no click semantics. Row semantics retain complete date, type,
currency and signed amount when the visible row uses compact presentation.

### Direct editor and shared form

Selecting a movement opens a content-sized bottom sheet with the localized Edit movement title,
including during its initial read. A successful read supplies the original record and prefilled draft.
The editor reuses creation's state-independent controls
through `MovimientoFormState`/`MovimientoFormActions`: the type selector retains its directional icons
and status colors, `MovimientoCuentaField` follows it with the Medio and Account sections (accounts in
the stored currency only; in-place creation keeps that currency fixed), the currency precedes the
numeric input, the Category section uses
`CategorySingleField` without a × and the single-select picker, and the Tags section uses
`EtiquetaTagField` with the tag picker. It opens with no initial picker and no forced keyboard.
Existing title and description open filled. Focus, disclosure, keyboard and category-entry phases belong to
composition.

An icon-only Delete action sits at the logical end of the title row with its localized accessible name.
Its rounded container uses the danger color at 12% opacity, matching snackbar status-icon backgrounds,
and the icon uses the full danger color; disabled styling retains the shared icon-button defaults.
It depends on a loaded persisted record, submission/Closing state and pending confirmations, not category
catalog readiness. Delete confirmation contains one concise question and a themed red primary CTA;
it does not repeat the amount, date or category. Cancelling it preserves the draft. Other confirmation
dialogs retain their default CTA styling.

The edit owner keeps original/draft/validation/save-error inputs. Session catalog/load/submission
status is the sole owner of those values; `session.form` derives presentation without another writable
copy or Flow. Field validation, failed Save and management errors remain inline in the scrollable body.
Cancel and Save changes share one footer row. The whole footer is absent while IME is visible and
returns after dismissal; the existing scrolling/focus-reveal behavior keeps
inputs reachable. Cancel/Back/discard behavior follows the domain contract.

The shared form accepts optional header-action, supporting-content, footer and date slots; creation keeps
its default header, date field and submit footer, and places Repetir in its supporting content. The
recurrencia sheet replaces the date with its schedule (frecuencia and next date). Management replaces the entire footer while retaining the shared
submit readiness/loading behavior. Use separate content-detent identifiers for loading, read failure,
missing, compact editor and expanded editor. Closing keeps the rendered editor snapshot and disables
input until its originating hidden callback. Android reason-specific Back handling is composed
inside Unstyled's modal content, where
the current ComposeView resolves the native dialog dispatcher; an Activity-level registration cannot
handle that window. Add's default behavior remains unchanged. Nested selectors/popups and confirmation
dialogs consume Back before their parent management surface.

### Shell ownership and admission

One remembered child ViewModel owner outside primary Navigation 3 entries hosts the shell's
management view models. `ManagementFeedbackViewModel` owns the single `ManagementFeedbackQueue`
(`core/ui/feedback`): the transient FIFO of feedback tokens, each holding at most one
`UndoSnapshot` (a deleted movimiento or transferencia, or a confirmed or skipped recurrencia
occurrence), the claim/restore cycle and session admission, so an open management sheet blocks Undo
claims and a claimed restore blocks new sheets. A destination's own sheet (Recurrentes) emits its
committed outcome as an effect once hidden and its route enqueues it without a session: the shell's
modal coordinator already keeps that sheet and a claimed restore exclusive.
`ManageMovimientoViewModel` lives in the same owner, receives that queue on creation and owns the
read generation, management session, editable draft, mutation and the pending committed outcome it
enqueues once its sheet is hidden. `ManageTransferenciaViewModel` (`feature/transfers`) lives in
the same owner under the same contract for the transfer sheet (create, edit, delete); creating a
transfer enqueues nothing; movimiento creation enqueues only a budget notice. The queue outlives every sheet so Undo does not
depend on an entry or closing modal. The existing creation store/session remains separate.

The child uses the root parent's remembered creation extras and no saved-state registry.
Supported Android configuration recreation retains the owner and claimed mutation; current iOS
ownership is scene-based and clears the child on active-scene disposal. Financial drafts, snapshots,
jobs and feedback tokens never enter saved state or preferences. A fresh process/scene starts closed.
Host tests establish the actual owner/provider seam; physical iOS teardown is a separate runtime check.

`PrimaryModalCoordinator` admits creation, movimiento management, transfer management and the
active destination's modal (Search filters, Profile option sheets) through one controller each, including loading, confirmations, submission
and Closing. Primary navigation waits while a modal
owns the window. During Undo restoration, tabs can change but all new modal sessions wait.
Callbacks carry origin/token/revision; delayed callbacks cannot close a new session, duplicate feedback
or consume a claimed restore. Read jobs are cancelled and late generations rejected.

### Snackbar presentation

The plain `TallybookSnackbar` accepts caller-provided leading/action/dismiss slots and a root modifier;
its host owns the composition timer and status presentation. The snackbar uses `surface.raised`
(`#F0EFE9` in light mode) and the default thin gray border, without a shadow. Horizontal padding is
12 dp; vertical padding is 10 dp. Success, information, warning and error icons occupy a tinted
32 dp leading badge. Busy progress replaces that icon in the same leading position with a transparent
badge background.
One shell host renders committed management feedback after
the originating sheet is hidden. It sits above measured bottom chrome, safe drawing and IME clearance,
respecting horizontal safe edges and the shared content width. Inline messages remain the recovery
surface inside an open sheet; creation enqueues only the budget notice, and only when a saved gasto
leaves a budget in a non-fine state. That notice stands alone (Short, action "Ver"); after an edit or
Pagado its line is merged into that save's own feedback instead, so one save never queues two
snackbars, Undo keeps priority over "Ver", and claiming Undo drops the line with the save.

Informational feedback normally uses the Short 3.7-second token; Undo/Retry uses Long 9.7 seconds.
Once messages are waiting, that visible batch uses half the baseline: Short 1.85 seconds and Long
4.85 seconds, including its final queued message. The active deadline shortens once, counting the
visible time the message has already accumulated; if that deadline has passed, it expires immediately.
Enqueueing or recomposing cannot repeatedly halve the window or restart it. An empty queue resets
the policy so a subsequent isolated message receives its normal baseline.
Provided accessibility recommendations apply after the baseline is selected and may extend the
window, including indefinitely without overflow. Indefinite and busy feedback never expires.
A window belongs to one token/revision and counts only visible time. Background or modal coverage
and busy progress pause it without dismissing the message; the next reveal of the same generation
resumes with the remaining time. A different token/revision, an empty host or disposal discards it.
Timeout delivery reports acceptance: a refused timeout is retried every 50 ms while the same
generation stays visible, and a generation still present after an accepted timeout retries on its
next reveal. Feedback visibility follows actual modal coverage, not navigation admission, so a
destination that has not attached its modal controller yet does not hide the snackbar.
Activity or scene recreation (rotation, theme, locale, resize) starts a new host, so a message that
survives it receives a new window.
Secondary screens (`TallybookScreenSnackbar`) also count as hidden unless their lifecycle is resumed.
Root delivery checks live resumed lifecycle and modal/viewport admission, then current token/revision.

Undo/Retry remains a dynamic trailing action, with adaptive wrapping and 48 dp controls. Timed
Short/Long feedback has no Close control. Indefinite feedback, including an effectively indefinite
accessibility recommendation, exposes Close. Claimed restoration shows localized progress, hides the
action, disables any Close control and suspends expiry.
Feedback is queued in arrival order in its owner's state. Only the head is rendered and receives a
timer; enqueueing does not replace it or restart its current window. Queue pressure can shorten that
window through the batch timing policy above. Close or expiry advances one
matching token/revision. Undo/Retry updates the head through progress and its result without consuming
waiting messages. The next head receives its selected window when first revealed; an indefinite head
blocks delivery until explicitly dismissed or resolved. The catalog demonstrates the same sequence.
Message announcements are polite, controls and pane titles are localized, and no countdown is
announced. The resolved iOS/Skiko
manager returns the baseline timeout; VoiceOver-adapted duration is not established by compilation.
