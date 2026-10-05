# Wave 3 / Stage 4 — Pure History composition

## Objective and tasks

Render pure History state using the [accepted image](../references/history-screen.png) and Tallybook
components, after reviewed Stage 3. Follow the [wave contract](../README.md); shell wiring is Stage 5.

- [task1 — History screen and minimal row renderer](task1/task.md): active.
- [task2 — Search/filter UI](task2/task.md): deferred; no UI/session/popup extension for Search now.

## Exit gate and review

Context/date title, period count, year selector and neutral Day/Week/Month controls follow the guide.
Compact global and period summaries are distinct; global remains visible. Description/date History
rows share rendering with the unchanged category-first Home wrapper. Pure states/resources/previews,
read-only semantics, callback contracts and current host/device-test compilation checks are verified.
No search/no-results/matching/clear-query/type-category filter/chart/breakdown/eye action is introduced.

Reject cloned avatars/amount formatting, anonymous visual literals, second bar/sheet, app-wide state
and detail click semantics. Preserve accepted creation behavior and supported locales/large text.
Stage 5 owns wiring/insets; Stage 6 owns physical visual/accessibility acceptance.
