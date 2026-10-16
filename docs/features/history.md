# History

History is a shared Android/iOS primary destination for browsing persisted movimientos. Domain rules
remain in [business-logic.md](../business-logic.md); layout, safe areas, tokens and navigation remain
in [UI architecture](../ui/architecture.md). The [visual reference](references/history-screen.png)
guides composition; its sample data, chart and eye control do not enable additional functionality.

## Integration

- The period summary reuses Home's shared balance card and skeleton, with the localized Period
  balance label, the period-owned snapshot and signed secondary values. Its typography is owned by
  [UI architecture](../ui/architecture.md#balance-summary-cards).
- The screen uses Day/Week/Month and year context, with a scrollable selected-period summary and
  date-grouped rows (title with category fallback, then description, no date) that open [movimiento management](manage-movimientos.md).
  There is no fixed global-balance banner. Home retains its global balance. The History header starts 20dp below the top safe inset.
- SQLDelight owns persisted movements. Currency/date-bounded repository observations feed one
  History ViewModel selection and a pure rows/count/totals projection. Loading, empty, failures,
  retry, cancellation and stale-request protection use the same ownership boundary.
- Home and History share retained snapshot state, deferred initial loading and reusable skeletons.
  Their contracts live in [UI architecture](../ui/architecture.md) and the shared primitives are listed
  in the [component catalog](../ui/components.md).
- Decorated Navigation 3 entries provide saveable state and ViewModel owners; Koin receives the real
  entry SavedStateHandle. Home/History retain selection and scroll on tab changes. Android supports
  saved-Activity restoration; a fresh launch initializes the normal defaults. iOS cross-process
  restoration is outside the current contract.
- The shell owns one creation session. Its store lives until the sheet finishes hiding; originating
  session identity guards delayed callbacks. The existing creation flow is shared by Home/History.
- Text search and non-date filters belong to the separate
  [Search delivery](search.md); shared row actions belong to movimiento management. Charts remain
  outside the delivered scope.

## Verification boundaries

The shared balance-card follow-up passed 21 focused History presentation/projection and money-row
host tests, Kotlin style, Android production/device-fixture builds and Native production compilation.
Independent read-only review found no mandatory issues. Xiaomi checks cover Home/History cards,
Period balance wording, selected-day/month totals and signed period values. The stacked-row follow-up
passes style and Android/device-fixture/Native compilation. An isolated preview on Xiaomi verifies
50,000 COP income and 2,000,000 COP expenses at 320dp and 100%/130% font scale: labels and amounts
remain complete, amount right edges align, and narrow rows move the label above the trailing value.
The preview uses no database or preferences; no production money was seeded. Original device settings
and the production foreground app are preserved.
The new retained/error/retry UI fixture is compiled but unexecuted because the instrumented runner
remains blocked; manual ready-state checks do not establish runtime coverage of its failure branches.

Android app/device-test builds, shared host tests, Detekt and format checks pass. Independent review
approved the shared loading contracts, Home integration and compact date selection controls.

Physical Xiaomi checks cover period selection, Home/History retention, Back, creation cancellation,
keyboard dismissal, Activity recreation and saved-Activity restoration. App smoke confirms compact
Day/Week/Month headers, the shared year floor, leap-day singleton weeks and navigation retention.
Test-only preview activities confirm slow-read placeholders and cached empty revalidation without
reappearing skeletons. Placeholder sample text is absent from accessibility. Automated instrumentation
of the loading transitions remains unconfirmed because the runner stalls before assertions.
A single isolated 10,000-row probe passed; it establishes a target observation, not a capacity or leak guarantee.
Future checks follow [proportional verification](../README.md#proportional-verification).

Native production/test compilation passes. iPhone runtime verification remains explicitly deferred;
compilation does not establish iOS runtime behavior. TalkBack, additional devices and a complete
locale matrix remain unconfirmed. No production test data was seeded.
