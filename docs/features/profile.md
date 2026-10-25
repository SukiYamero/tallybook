# Profile and preferences

Profile is the fifth bottom-navigation position (Home, History, Add, Statistics, Profile): a retained
primary destination whose root Back returns to Home. The user accepted the Android flow; the reviewed
implementation is integrated into `main`. [Business logic](../business-logic.md#moneda-e-idioma) owns
currency, language and theme rules and the [CSV export](../business-logic.md#csv-export); [UI architecture](../ui/architecture.md#tallybook-destination-rule)
owns the destination set and modal admission.

## Sections

- **Your data.** A card stating that records stay on this device, the Accounts, Categories and Tags
  cards (secondary destinations described in [Accounts](accounts.md),
  [Custom categories](custom-categories.md) and [Tags](tags.md)), and CSV export. The first card is the slot the
  [Google + Drive plan](../tasks/google-drive-persistence.md) replaces with account, access mode and
  backup status.
- **Preferences.** Main currency and language open one shared option sheet; theme (System, Light,
  Dark) is an inline segmented control. All three persist in DataStore and the screen renders the
  stored values, never an optimistic copy. A failed write shows an error under its own row; a read
  failure disables editing and offers Retry.
- **About.** The platform app version.

## Integration

- `ProfileViewModel` owns one sheet state (kind, opening snapshot, closing) admitted through
  `PrimaryModalCoordinator` via the shared `DestinationModalController` seam that Search filters also
  use. Tabs, new modals and shell feedback such as Undo wait until the sheet reports it is hidden.
- `AppDisplayViewModel` resolves theme and language at the root before the first frame.
  `TallybookTheme` receives the resolved theme; the platform entry aligns system bars (Android
  `enableEdgeToEdge` styles, iOS `overrideUserInterfaceStyle`).
- Language uses the Compose Multiplatform `LocalAppLocale` workaround
  (<https://kotlinlang.org/docs/multiplatform/compose-resource-environment.html>) behind
  `ProvideAppLocale`. Android updates the process default and provided `Configuration` in place, keeping
  navigation and screen state; iOS writes `AppleLanguages` and recreates the subtree, returning to Home.
  After each applied locale, `CategoriaRepository.localizeSeeded` rewrites seeded category names and
  skips the write when nothing changed. Region-based currency inference reads the device locale, not
  the override.
- CSV export reads one database snapshot and builds an RFC 4180 document (UTF-8 BOM, CRLF,
  formula-injection guard, unsigned amounts with `Moneda.scale` decimals, localized headers) on the
  computation dispatcher and opens the platform
  share sheet: Android `FileProvider` scoped to `cache/exports/` (cleared before each export) with an
  `ACTION_SEND` chooser, iOS `UIActivityViewController`.

## Verification boundaries

The final tree passed 435 shared host tests with zero failures, Detekt, ktfmtCheck, the Android debug
APK and Native production compilation. Two independent reviews ran; confirmed findings (modal admission,
a sheet stuck closing after a read failure, unknown display values hiding the currency) were fixed and
covered by tests. Manual checks on a physical Xiaomi covered currency switching across Home/History/
Search, theme with status-bar icons, in-place language switching including category names, restart
persistence, export share sheet and CSV content, and tab navigation after sheets close.

Not covered: iOS runtime (tracked in the [backlog](../backlog.md)), an automated Android share-sheet
test, TalkBack and enlarged-text passes on this screen.

## Pending in Profile

Account, access mode, backup and erase belong to the Google + Drive feature. Biometric/PIN lock is
deferred until that work settles. First day of week was rejected because History weeks are month-bound.
