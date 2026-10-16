# Wave 3 — Final review and device acceptance

## Final independent review

One fresh reviewer reads the focused diff of `feat/smart-entry` against `main` for correctness
(parser money exactness, date edges, suggestion confidence, Otros fallback), ownership (ViewModel vs
composition, single source of truth for the draft), duplication (one normalization function, no
second amount parser) and simpler alternatives. Confirmed findings are fixed and only affected
checks rerun.

## Xiaomi smoke (3–5 minutes)

On the confirmed physical Xiaomi, with the debug APK installed:

1. Open Add: focus is on the sentence; the placeholder shows the primary currency's sample amount.
2. "Gasté 20.000 en cine ayer" → preview while typing → Continue → summary shows gasto, yesterday,
   −$20.000, the sentence and Cine; Save → row appears in Home and History.
3. "aceite 30 mil" → suggestions without preselection; pick one; save. Repeat "aceite 15 mil" → the
   picked category is now preselected.
4. "algo raro" → Continue → summary marks the missing amount; Save opens the form on amount; enter
   an amount and save without category → row shows the pending treatment; open it and choose a
   category → pending disappears.
5. Edit movimiento from the summary, change the date, Back returns to the summary with the change.
6. Search: filter by Transporte → includes a Gasolina movement; filter by Gasolina → excludes a
   generic Transporte movement.

## Closing

After the user's acceptance: compact this spec into `docs/features/add-movimiento.md` (current state
only), set the registry row to `DONE`, merge to `main`, delete the branch and worktree. Phase 2 starts
from the roadmap in the [README](../README.md#roadmap-after-phase-1).
