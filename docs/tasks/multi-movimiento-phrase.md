# Several movimientos in one sentence

## Context

People buy several things at once and say it in one go: "Compré 2 hamburguesas por 20mil y 3
gaseosas fueron 30mil, más lo cargué con la tarjeta de crédito". Today that creates a single
movimiento with the largest amount, flagged for review. The sentence (typed or dictated) should
instead offer one movimiento per amount, each with its own category, reviewed together and saved with
one tap, with a way to join them into one when that is what the user meant.

Goals: split a sentence with several amounts into parts; review all parts on one screen; save them
together; join them back into one. Non-goals: per-part tipo, fecha, medio or cuenta read from the
sentence (the form can still change them per part); transfers from the sentence; any LLM. Anti-goal:
a sentence that today makes one correct movimiento ("cine y comida 60 mil", "pan y leche 5 mil") must
not be split.

## Behavior

### Splitting (pure, `core/data/entry`)

- **Connectors**: "y", "e", "and", "más"/"mas", "plus", "mais", "también"/"also"/"também", "+" and a
  comma that is its own punctuation token (a comma inside a number such as "1.500,50" is a decimal
  separator, not a connector). Compared after the shared search normalization.
- Two consecutive amounts with a connector between them are cut there. With several connectors
  between them, the cut is the connector right before the next amount (only stop words or a tipo
  verb in between: "20 mil en cine y palomitas y 40 mil en comida"), otherwise the first connector
  after the previous amount ("cine 20 mil y comida y bebida 40 mil"). Two amounts with no connector
  between them do not split. Text before the first amount belongs to the first part ("pan y leche 5
  mil" stays one part). After the last amount, a connector followed by words that name a medio,
  account, date or cadence starts the **shared tail** ("más lo cargué con la tarjeta de crédito");
  any other trailing text stays in the last part ("y 40 mil en comida y bebida").
- The sentence splits only when it yields **2 to 10 parts**. One part, or more than 10, keeps today's
  single-movimiento flow unchanged.
- **Shared attributes** — tipo, fecha, medio, cuenta, frecuencia and tags — are read from the whole
  sentence with today's rules and apply to every part. Only monto, título and categoría are per part.
- **Monto** per part: the part's own amount, with the quantity rule below.
- **Título** per part: the part's own text as typed, edges cleaned like the subject (leading
  connector and punctuation removed; the shared tail is not included). Parsing still never rewrites
  the user's words, and history learning re-parses each título into a clean per-item subject.
- **Categoría** per part: the suggestion engine runs on the part's subject (its text without verb,
  amount, quantity-free stop words, date, medio and account words), with the same preselection and
  suggestion rules as a single sentence.
- **Quantities** (also applies to single sentences), decided over the whole sentence **before**
  chunks are classified: a whole number below 100 written without separator, symbol, decimals or
  multiplier, directly followed by a word that is not a stop word ("en", "de", "por", "on", "for",
  "no"…), multiplier or currency, is a quantity when the sentence has at least one amount candidate
  that is not quantity-shaped. Quantities are not amounts, do not trigger the ambiguous-amount flag
  and stay in the título and subject ("compré 2 hamburguesas por 20 mil" → 20.000, no review flag;
  "2 hamburguesas y 3 gaseosas por 50 mil" → one part of 50.000; "spent 20 on lunch and 15 on a
  taxi" → two parts, because "on" is a stop word). A lone quantity-shaped number with no other
  candidate stays the amount, as today. Within a part, the existing largest-wins + review flag still
  applies to the remaining candidates.
- "y", "e" and "and" inside a spelled number ("treinta y cinco mil") are number connectors
  (`PhraseLexicon`), never part connectors.
- A category picked by hand in the single Category section is not applied while the sentence is
  split; it is kept and returns if the sentence goes back to one movimiento. Fill manually keeps it
  as today.

### Sentence dialog

- While the sentence splits, the Detected card switches to a multi layout: "N movimientos" with the
  total, the shared tipo and fecha, then one compact row per part (amount, título, preselected
  category or "no category"). The Medio and Account sections stay and apply to all parts. The single
  Category and Tags sections are hidden; categories are chosen per part in the review.
- Continue opens the **review** instead of the summary. Fill manually, voice dictation, the dismissal
  guard and clearing the sentence behave as today.

### Review (new step, same tall bottom sheet as the summary)

```
Revisar 2 movimientos            Total -$ 50.000
Gasto · Hoy · [Crédito] [Cuenta principal]   Repite: —
Tags: …
  -$ 20.000   Compré 2 hamburguesas por 20mil   [Comida]   ×
  -$ 30.000   3 gaseosas fueron 30mil           [Comida]   ×
[Unir en uno]                                  [Guardar 2]
```

- **Shared section**: tipo and fecha as in the summary; medio and account selectors apply to every
  part; the tags selector applies to every part. Repetir is shared too: the review shows the same
  Repetir control as the summary (on with the sentence's frecuencia, or switched by the user), and
  saving creates one recurrencia per part when it is on. A part's form hides Repetir.
- **Rows**: amount with sign, título, category chip. The chip opens the category picker for that part
  with that part's suggestions (same ownership rule: a manual pick hands the category to the user).
  Tapping the row opens the full form for that part; Back from that form returns to the review. In
  that form the primary action is **Done** (new string), which validates the part and returns to the
  review; nothing is saved until Guardar N, so a partial set is never persisted. A
  part edited in the form may diverge from the shared values (another fecha, cuenta, medio or tipo);
  its row then shows the differing value under the título, and later shared changes no longer touch
  the fields that part changed.
- **×** removes a part. With one part left the review becomes the normal summary for that part.
- **Total** sums the parts' signed amounts in the shared currency. Parts that diverged into another
  currency are excluded from the total, and the total label says so.
- **Unir en uno** turns the remaining parts into one draft: monto = sum, título = the whole sentence
  (or, after a removal, the remaining parts' títulos joined with ", "), categoría = the shared one if
  every part has the same category, otherwise none selected with the whole sentence's suggestions. The shared values win: per-part fecha, cuenta and medio overrides are dropped. Join is
  disabled while a part has another tipo or currency than the shared one, since the sum would be
  meaningless. It opens the normal summary. Back from it returns to the sentence dialog; Continue
  splits again.
- **Guardar N** validates every part; the first invalid part opens its form with the field error (as
  today's Save). Valid parts are saved **atomically**: all movimientos (or all recurrencias with their
  first movimiento) in one database transaction, so a failure saves none and keeps the review.
  Feedback after save is the same as a single save.
- **Back** from the review returns to the sentence dialog with the text. Continue again rebuilds the
  parts; a category picked by hand is kept for a rebuilt part with the same título.

## Waves

- **Wave 1** (parallel):
  - (a) Splitter + quantity rule in `core/data/entry` (`MovimientoPhraseParser` stays the single
    reader of a part; the splitter returns parts and the shared tail as text ranges). Corpus tests
    (es/en/pt): the context sentence → 2 parts, 20.000 and 30.000, Crédito; "Gasté 20 mil en cine y
    40 mil en comida" → 2; "cine y comida 60 mil" and "pan y leche 5 mil" → 1; "compré 2
    hamburguesas por 20 mil" → 1, 20.000, no flag; "netflix 20 mil y spotify 10 mil cada mes" → 2,
    Mensual; "20 mil más 5 mil de propina" → 2; "spent 20 on lunch and 15 on a taxi"; "gastei 20 no
    almoço e 15 no uber"; "pagué 1.500,50 y 2.000" (decimal comma); "2 hamburguesas y 3 gaseosas
    por 50 mil" → 1; "treinta y cinco mil en cine" → 1; 11 amounts → not split.
  - (b) Atomic batch writes: insert several movimientos, and create several recurrencias, each in one
    transaction (`MovimientoRepository`, `RecurrenciaRepository`), with failure tests showing nothing
    is persisted. Repetir is shared, so one Save never mixes plain movimientos and recurrencias.
- **Wave 2** (depends on 1): ViewModel/state — parts list in `AddMovimientoUiState` (per-part
  amount, título, category and ownership, per-part overrides), the review step, actions (edit part,
  pick category, remove, join, save all), `MovimientoEntryEngine` interpreting each part. Tests in
  `AddMovimientoViewModel` style for split, join, remove to one, divergent part, save all / failure,
  back and re-continue.
- **Wave 3** (depends on 2): UI — Detected multi layout, review sheet, strings in the 4 locales
  (`values`, `values-es-rAR`, `values-en`, `values-pt-rBR`), previews, device UI tests for the review.
- **Wave 4**: docs (`business-logic.md` Natural-language entry, `features/add-movimiento.md`, remove
  backlog item), one final independent review, Xiaomi smoke.

## Left to the builder

Splitter data shape and file names; how per-part overrides are stored; the review's exact layout and
spacing within the existing sheet and design tokens; the "no category" wording; how the total label
mentions excluded currencies.

## Verification

- **Integration contract**: a ViewModel test from sentence text through Continue, review and Save
  that checks the two persisted movimientos (amounts, títulos, categories, shared medio) — catches a
  splitter/state/repository mismatch.
- **Runtime (Xiaomi)**: type and dictate the context sentence; review shows 2 parts with Crédito;
  change one category; Save → both appear in History. Join → one movimiento of 50.000. Remove one →
  normal summary. "netflix 20 mil y spotify 10 mil cada mes" → two recurrencias in Recurrentes.
