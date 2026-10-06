# History

History is a shared Android/iOS primary destination for browsing persisted movimientos. Domain rules
remain in [business-logic.md](../business-logic.md); layout, safe areas, tokens and navigation remain
in [UI architecture](../ui/architecture.md). The [visual reference](references/history-screen.png)
guides composition; its sample data, chart and eye control do not enable additional functionality.

## Integration

- The screen uses Day/Week/Month and year context, with a scrollable selected-period summary and
  date-grouped, description-first read-only rows. There is no fixed global-balance banner. Home
  retains its global balance. The History header starts 20dp below the top safe inset.
- SQLDelight owns persisted movements. Currency/date-bounded repository observations feed one
  History ViewModel selection and a pure rows/count/totals projection. Loading, empty, failures,
  retry, cancellation and stale-request protection use the same ownership boundary.
- Decorated Navigation 3 entries provide saveable state and ViewModel owners; Koin receives the real
  entry SavedStateHandle. Home/History retain selection and scroll on tab changes. Android supports
  saved-Activity restoration; a fresh launch initializes the normal defaults. iOS cross-process
  restoration is outside the current contract.
- The shell owns one creation session. Its store lives until the sheet finishes hiding; originating
  session identity guards delayed callbacks. The existing creation flow is shared by Home/History.
- Search and Profile remain reserved destinations. Text search, non-date filters, charts and
  transaction editing are outside this delivery. [Search proposals](../tasks/wave-3/README.md) remain
  available for a separate plan.

## Verification boundaries

Android app/device-test builds, shared host tests, Detekt and format checks pass. The complete
integration run executed 258 host tests; the fixed-banner removal executed 93 focused regressions.
The final 20dp padding adjustment passed the narrow app build and mandatory lint/format checks.
Independent review approved the source and the focused follow-up changes.

Physical Xiaomi checks cover period selection, Home/History retention, Back, creation cancellation,
keyboard dismissal, Activity recreation and saved-Activity restoration. The latest app smoke confirms
banner absence, period-summary presence and the additional 20dp header clearance. A single isolated
10,000-row probe passed; it establishes a target observation, not a capacity or leak guarantee.
Future checks follow [proportional verification](../README.md#proportional-verification).

Native production/test compilation passes. iPhone runtime verification remains explicitly deferred;
compilation does not establish iOS runtime behavior. TalkBack, additional devices and a complete
locale matrix remain unconfirmed. No production test data was seeded.
