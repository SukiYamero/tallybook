# Learning from sentence corrections

## Context

The sentence parser (typed or dictated) gets some fields wrong or leaves them empty, and the user
fixes them before saving. Today that fix teaches nothing except the category, which history already
learns per exact subject. This feature records every correction made between Continue and Save, and
turns a correction the user repeats into a personal rule: the first time it is only offered, from
the second consistent time it is applied. The records are also the data source for enriching the
shared dictionary later, when a server exists.

Goals: record corrections on the device; learn personal rules for tipo, categoría, medio, cuenta and
frecuencia from repeated corrections; keep the records ready for a future server upload.
Non-goals: a server, uploading, a screen to browse or edit learned rules, learning from edits made to
movimientos after they were saved, learning amounts or dates.
Anti-goals: a one-off exception becoming a rule; a learned rule overriding what the user explicitly
picked; any record leaving the device.

## Approaches

- A. Record only — cheapest, but the user gets nothing from it yet.
- **B. Record and learn personal rules (chosen)** — one record table feeds both the personal rules
  and, later, the shared dictionary.
- C. B plus an anonymous opt-in upload — needs the server; later.

## Behavior

### What counts as a correction

- A correction is a difference between what the sentence derived at the last Continue and what Save
  persisted, for a movimiento created from a sentence (single or a part of a split sentence).
- Compared fields: tipo, monto, fecha, categoría, medio, cuenta, frecuencia (Repetir), the título's
  words, and for split sentences the structure (parts joined or removed).
- Each compared field has two derived values, both snapshotted at the last Continue:
  - `leido`: the parser's own reading, before any learned rule (none when it left the field
    undecided; for tipo, none when the sentence has no tipo verb).
  - `propuesto`: what the app proposed after learned rules and suggestions (the preselected
    category, the applied rule value, or none).
- Only fields the user set by hand count: the existing ownership flags decide (`categoriasEdited`,
  a USER-owned medio/cuenta choice, `frecuenciaEdited`, `amountEdited`, a tipo or fecha the user
  changed, the título edited in the form). A field filled by a default (the default cuenta and
  medio, Otros) or changed only as a side effect of another change (a medio adapted to a new cuenta
  or tipo) records nothing. A hand pick counts even when it equals `propuesto`, because it confirms
  the value. Picking a dimmed category suggestion in the dialog, or choosing a medio the sentence
  left undecided, is a correction.
- Only corrections made inside the add flow count. Editing a saved movimiento later, confirming a
  recurrencia occurrence, Fill manually and the form opened without a sentence record nothing.
- A título rewritten beyond recognition (more than half of its words changed) records no título
  correction: the user changed their mind, not a misheard word.
- Each record says whether the sentence was dictated (any dictated words reached it) or only typed.

### What a record holds

- `palabras`: the sentence's subject words after the shared suggestion normalization (search
  normalization, stop words removed, plurals reduced), as used by category suggestions. Never the
  amount, never the whole sentence.
- `campo`, `leido` and `propuesto` (nullable) and the saved value `guardado` (nullable). Values are
  stable identifiers: enum names for tipo, medio and frecuencia, the categoría id, the cuenta id,
  minor units and currency for monto, ISO date for fecha. For the título: `propuesto` = the words
  removed, `guardado` = the words added (`leido` empty). For the structure: `propuesto` = number of
  parts, `guardado` = `JOINED` or the number of parts saved.
- Origin (`DICTADO` / `ESCRITO`) and creation time.
- Records are a stamped root entity like the others (`updated_at`, `deleted_at`), so they join the
  future Drive backup and server sync without another migration. They are excluded from CSV export
  and from every user-facing count. The app has no erase-all path yet; when one is added (with the
  Drive work) it must erase them too. Older records are kept (no
  pruning) for the future upload; only rules ignore them after 180 days. Volume stays small: one row
  per hand-set field per saved movimiento.

### Personal rules

- Learned fields: tipo, categoría, medio, cuenta, frecuencia. Monto, fecha, título and structure are
  recorded only.
- A rule is (word, field, value). It exists when, among the live records of the last 180 days whose
  `palabras` contain the word and whose `campo` is that field:
  - at least 2 have `guardado` = value, and
  - the most recent one has `guardado` = value (a later contradiction suspends the rule until two
    newer agreeing corrections).
- With exactly one agreeing record (and no newer contradiction) the value is only **offered**:
  categoría as a dimmed suggestion chip, medio and cuenta among the offered alternatives the
  sentence dialog already shows. Tipo and frecuencia have no offered state.
- Words shorter than 3 characters never form rules. When several words of one sentence have rules
  for the same field with different values, none applies and their values are offered.
- Applying a rule:
  - When the parser left the field undecided, the rule fills it.
  - When the parser decided a value, the rule overrides it only if the rule's agreeing records were
    corrections of that same reading (`leido` equal to the parser's value); otherwise the parser's
    explicit reading wins ("con débito" is never overridden by a habit).
  - Order: a tipo rule is resolved first, before the category ranking and medio resolution that
    depend on tipo; a tipo rule fills tipo only when the sentence has no tipo verb (it replaces the
    income-category hint) and overrides a verb only under the `leido` condition above. Then
    categoría, cuenta, medio (adapted to the final tipo and cuenta) and frecuencia.
  - For categoría, an exact-subject history match still wins over a word rule; a word rule wins
    over dictionary matches.
  - A cuenta rule whose cuenta is archived or deleted is ignored; a medio or cuenta rule is adapted
    to the final tipo like any detected value.
- Applied values are sentence-owned: a hand pick in the dialog, summary, review or form wins, and
  changing it records a new correction (which is how a rule is unlearned).
- Rules are computed in memory from the records (no rules table), like the suggestion index from
  history, and refresh when records change.

### Several movimientos

- Each saved part produces its own records, with the part's own `palabras` and derived values.
  Shared fields (tipo, fecha, medio, cuenta, frecuencia) compare the part's saved value with the
  sentence's derived shared value.
- Joining records one structure correction for the sentence and then compares the joined movimiento
  against the whole sentence's derived values.

### Future server (design constraint only)

- Records must be uploadable without exposing personal data: `palabras` are normalized words, values
  are enums or seeded ids. Cuenta ids and custom category ids are personal and must be mapped or
  dropped by the future upload, not stored differently now. No upload code in this feature.

## Waves

- **Wave 1** (parallel): (a) the next schema migration (`11.sqm`) + `correccion_frase` table, repository with
  insert-many, observe-recent (live, last 180 days) and erase, stamped like other roots, host tests;
  (b) pure rule engine (`CorrectionRules` from records: rule / offered / none per word and field,
  with the conflict, minimum-length and recency rules) + commonTest.
- **Wave 2** (depends on 1): engine integration — `MovimientoEntryEngine` takes the learned rules and
  applies/offers them per the rules above (single sentence and each part); tests.
- **Wave 3** (depends on 2): capture — snapshot the derived values at Continue (single and parts),
  dictated flag from the voice helper, diff at successful Save (single and Save all) into records in
  the same transaction scope as the save (a failed save records nothing); tests including the split
  flow and Fill manually.
- **Wave 4**: docs (`business-logic.md`: "Category suggestions" and a "Learning from corrections"
  section; `stack`/`ui` untouched), final review, Xiaomi smoke.

## Verification

- **Integration contract**: a ViewModel test types "almuerzo 20 mil con la visa" twice, picks
  Crédito both times and saves (the second time Crédito is already offered); the third time Crédito
  is preselected; changing it to Débito and saving makes the fourth sentence apply nothing and offer
  Débito.
- **Runtime (Xiaomi)**: dictate a sentence with an unknown word, correct the category, save; repeat;
  the third dictation preselects it. Records survive an app restart.
