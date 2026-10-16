# Wave 1 — parallel foundations

The four tasks share no source files except the string resources noted in the
[README](../README.md#execution). Each runs in its own subagent and worktree.

- **task1 — Sentence parser**: tipo, monto, fecha and subject extraction. [task](task1/task.md)
- **task2 — Suggestion engine**: dictionary, learned history index, history query.
  [task](task2/task.md)
- **task3 — Entry UI components**: sentence step, summary card, suggestion chip, pending treatment,
  localized placeholder. [task](task3/task.md)
- **task4 — Otros and hierarchy**: `cat_otros` seed, neutral tint, picker exclusion and child
  selection support, Search roll-up. [task](task4/task.md)

## Blocking

No task blocks another inside this wave. Wave 2 starts only after the four branches are merged into
`feat/smart-entry` and `./gradlew detekt ktfmtCheck :shared:testAndroidHostTest` passes on the merge.
