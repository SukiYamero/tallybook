# Statistics

Statistics answers where the money went, whether the period is unusual and how the trend is moving.
It is a primary tab (Home · History · Add · Statistics · Profile), and Home's "Your month in
numbers" card opens it. Product rules live in [business-logic.md](../business-logic.md#statistics);
navigation and presentation in [UI architecture](../ui/architecture.md#statistics-presentation); the
chart components in [UI components](../ui/components.md#charts); the Vico pin in
[stack.md](../stack.md).

## Interaction

- **Screen**: Month or Year scope with period chips that stop at today, a year menu and the currency
  selector when accounts use more than one currency. Cards, in order:
  - the summary, with the variation against the same stretch of the previous period;
  - "Where did your money go?", grouped by Categories, Tags, Accounts or Medios (donut and rows,
    or bars and a note for Tags);
  - the trend (gasto and ingreso columns with a balance line; a tapped month lists its figures as
    rows);
  - the current month's pace;
  - "What changed".
- **Rows**: a category opens its detail sheet, a tag or an account opens Search filtered by that id,
  currency, period and GASTO, and a medio opens nothing.
- **Category sheet**: total, share of the period's gasto, count and variation, the last months with
  the average of the complete ones, subcategories, the three largest gastos and "View N movements",
  which opens Search with the same N once the sheet has hidden.
- **Home card**: top three categories plus the rest as a stacked bar, and the pace line when there is
  one. It is hidden when the month has no gasto or the read fails.
- **Search**: no longer has a tab. It opens from Home's search field (focused on every entry) and
  from the "View movements" intents; while it shows, no tab is selected.

## Integration

- `StatisticsRepository` reads one `StatisticsSnapshot` (or `CategoryStatisticsSnapshot`) per
  emission in a single transaction and re-reads when movimiento, cuenta, categoria or
  movimiento_etiqueta change. The aggregates live in `Movimiento.sq`: root and subcategory sums
  with a `LEFT JOIN` so missing categories still count, the category predicate copied from Search
  so N matches, and zero-filled months and days. Totals beyond `Money.MAX_MINOR_UNITS` fail the read
  instead of crashing.
- `core/model/StatisticsRanges.kt` builds periods, comparable stretches, trends and requests;
  `StatisticsRules.kt` holds integer-only variations, shares (basis points add up to 10,000), pace and
  insights. Home uses them directly, without importing the feature.
- `StatisticsViewModel` owns the selection, currency, dimension and the one open sheet (selection,
  currency and dimension survive process death). The route turns `OpenSearch` and `OpenCreation`
  effects into `PrimaryNavigator.showStatisticsFilter` and the creation sheet, and registers the sheet
  with `PrimaryModalCoordinator`. `SearchFilterRequest` carries an optional range and `SearchTipo`.
- `PrimaryNavigationState` adds `statisticsVisited` with a default, so states saved before the tab
  existed still restore.
- Charts use Vico 3.2.3 column and line layers. The donut and distribution bars are Compose.
  Each chart is one semantics node with a localized summary, and the trend chart exposes one
  accessibility action per month.

## Verification boundaries

The final tree passes 1255 shared host tests with zero failures, iOS main and test compilation, the
Android debug APK, Detekt and ktfmtCheck. Host tests cover the repository invariants (category sums
equal gasto including a missing category, a two-tag movimiento counts once per tag, transfers,
deleted rows and other currencies excluded, N equal to Search's count), calendar edges of the
comparable stretch, rounding and caps, pace and insight thresholds, the ViewModel lifecycle and saved
state, presentation, the Search range request and navigation-state restore.

On the Xiaomi, the affected instrumented classes pass (49 tests: Statistics navigation, Home, Search
navigation, Categories and Tags navigation, UI catalog, chart components and the Statistics screen).
A manual dark-theme, English pass covered the tab, Home card, Month and Year, the Food sheet and
"View 7 movements" reaching Search with the same seven rows; the user accepted the delivered flows on
that device. A light-theme pass in es, es-AR and pt-BR covered the tab, Month and Year, and the
category sheet.

Not covered: TalkBack and enlarged text, and iOS runtime
(tracked in the [backlog](../backlog.md)).
