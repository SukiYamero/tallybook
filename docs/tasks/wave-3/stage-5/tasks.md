# Wave 3 / Stage 5 — Home/History shell and navigation

## Objective and task

Integrate Home/History as retained primary destinations with one bar and the accepted creation sheet.
Follow the [wave contract](../README.md).

- [task1 — Shared navigation and sheet lifetime](task1/task.md): split into an early real adapter/
  saved-owner/Koin probe before Stage 3, and full-shell integration after reviewed Stage 4.

## Exit gate and review

Home/History selection has bounded entries/correct semantics; Search/Profile remain inert. Actual
entry scope preserves selection/scroll with Android supported saved-owner restoration and iOS
retained-controller/saveable scope. Fresh cold launch defaults are explicit; no unimplemented iOS
cross-process bridge is assumed. One owner handles bar/insets and one session owns creation.

Inactive subscriptions stop after transition settling and five-second timeout; first re-entry state
marks/revalidates stale cache. Back handles year popup/sheet before selecting Home, then normal root
platform behavior. Originating creation-session callbacks cannot clear a newer Closing session.

Fresh reviewer checks early probe and later shell separately. Reject ambient app-wide ViewModels,
second sheet/bar, duplicate/reset History entries, root blank stack, Search enablement/focus routing
and serializer-only ownership claims. Stage 6 still needs actual physical target evidence.
