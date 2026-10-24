# Wave 5 / Stage 5 / Task 2 — CSV export with accounts, medio and transfers

## Objective

Extend Profile's CSV export with the account and payment method of each movimiento and with one row
per transfer, keeping the RFC 4180/BOM format and all existing columns.

## Context and skills

- Read [the wave README](../../README.md) → Display surfaces → CSV export; `docs/business-logic.md`
  → Moneda e idioma (current export rule); `docs/features/profile.md` (export verification limits).
- Skills: `android-skills:android-dev`, `chrisbanes-skills:kotlin-api-design`,
  `android-skills:android-testing`, `superpowers:test-driven-development`.
- Current state: `buildMovimientoCsv(movimientos, categorias, etiquetas, labels)` in
  `core/data/export/MovimientoCsv.kt` sorts by `fecha DESC, createdAt DESC, id DESC`, writes unsigned
  plain decimals with `Moneda.scale` digits; `ProfileViewModel` gathers the data
  (`ProfileViewModel.kt:198`) and `ProfilePresentation.kt:84` builds the localized `MovimientoCsvLabels`.

## Files

- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/export/MovimientoCsv.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/profile/viewmodel/ProfileViewModel.kt` (read accounts with `CuentaRepository.observeAll`/first value and transfers with `TransferenciaRepository.getAll()` next to the existing export read; inject both)
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/profile/ui/ProfilePresentation.kt` (labels)
- Modify: `di/ViewModelModule.kt` (Profile constructor only)
- Create entries: `composeResources/values*/strings_integration.xml` (headers, transfer type label; medio labels come from Stage 2/task1's `MedioPagoLabels`)
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/export/MovimientoCsvTest.kt` (extend); `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/profile/viewmodel/ProfileViewModelTest.kt` (extend, with Stage 2's fakes)

## Contracts

```kotlin
data class MovimientoCsvLabels(
    /* existing */,
    val accountHeader: String,
    val medioHeader: String,
    val destinationAccountHeader: String,
    val destinationAmountHeader: String,
    val destinationCurrencyHeader: String,
    val transferLabel: String,
    val medioLabels: Map<MedioPago, String>,
)

fun buildMovimientoCsv(
    movimientos: List<Movimiento>,
    transferencias: List<Transferencia>,
    cuentas: List<Cuenta>,
    categorias: List<Categoria>,
    etiquetas: List<Etiqueta>,
    labels: MovimientoCsvLabels,
): String
```

## Exact rules

- Column order: existing nine columns, then account, medio, destination account, destination amount,
  destination currency. Existing columns keep their position so earlier spreadsheets still line up.
- Movimiento rows: account name, medio label; the three destination cells empty.
- Transfer rows: type = transfer label; category, parent category, tags, title and medio empty;
  description = transfer description; amount/currency = origin side; account = origin; destination
  cells filled (destination amount equals origin amount for same currency).
- Both kinds merge into one ordering `fecha DESC, createdAt DESC, id DESC`.
- Tombstoned rows never appear (the repository reads already exclude them; the test asserts it via
  the ViewModel path). A missing account name exports empty, like a missing category.
- Account names and descriptions go through the existing `toCsvField` escaping (quotes, commas, line
  breaks, leading formula characters if the existing escaping handles them — do not weaken it).

## TDD

- [x] RED `MovimientoCsvTest`: header order, movimiento row with medio/account, same- and cross-currency transfer rows, CLP scale, merged ordering, escaping of an account name with a comma and quote.
- [x] RED `ProfileViewModelTest`: export includes transfers and accounts, excludes tombstones, failure of either read fails the export without a partial file.
- [x] GREEN; labels in four locales.
- [x] `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64 detekt ktfmtCheck :androidApp:assembleDebug`.
- [ ] Physical check: export from Profile on the Xiaomi, open in a spreadsheet app, verify columns.

## Blocks

Blocked by Stage 2 review (repositories, `TransferenciaRepository.getAll()`, `MedioPagoLabels` from
Stage 2/task1). Parallel with task1 — it touches no `ActividadRepository` or `.sq` file. Blocks Stage 6.
