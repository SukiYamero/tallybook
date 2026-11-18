# Several Movimientos in One Sentence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A sentence with several amounts ("Compré 2 hamburguesas por 20mil y 3 gaseosas fueron 30mil, más lo cargué con la tarjeta de crédito") becomes one movimiento per amount, reviewed together on a new review step and saved atomically, with Join to turn them back into one.

**Architecture:** The parser gains a quantity rule and a pure splitter that returns part texts. `MovimientoEntryEngine.interpret` adds per-part interpretations to `PhraseInterpretation`. `AddMovimientoUiState` keeps the existing single-draft fields as the **shared** values and adds a `parts` list plus a new `REVIEW` step; editing a part swaps that part into the single-draft fields so the existing form is reused, and Done swaps it back. Repositories gain atomic batch writes.

**Tech Stack:** Kotlin 2.4.10, Compose Multiplatform 1.11.1, SQLDelight, Koin, kotlin.test (commonTest), Robolectric host tests, Compose UI device tests.

**Spec:** `docs/tasks/multi-movimiento-phrase.md` (read it first; this plan does not repeat its behavior rules).

## Global Constraints

- Code, identifiers, comments, KDoc and docs in English; Spanish domain vocabulary stays (`Movimiento`, `monto`, `titulo`, `categoria`, `Recurrencia`…).
- Comments only for a non-obvious why (AGENTS.md §4).
- No `Dispatchers.Main`/`IO` in shared code; no platform imports in `commonMain`.
- Every Gradle command goes through the chrisbanes `gradle_run.py` wrapper (`create` / `run` / `finish`).
- Before reporting a task done: `./gradlew ktfmtFormat detekt` clean (Detekt `TooManyFunctions` 11 per file, `LongMethod` 60).
- Strings in all four folders: `values`, `values-es-rAR` (voseo), `values-en`, `values-pt-rBR`; copy follows `docs/wording.md`.
- Commit after each task with conventional commits; never push. End commit messages with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Device checks only on the physical Xiaomi (`adb devices`); never emulators.
- Split bounds: 2 to 10 parts; quantity = whole number < 100.

## Review Focus

1. A sentence that is one movimiento today must stay one: "cine y comida 60 mil", "pan y leche 5 mil", "treinta y cinco mil en cine", "2 hamburguesas y 3 gaseosas por 50 mil" (Task 2 corpus).
2. A decimal comma ("1.500,50 y 2.000") must not be read as a connector (Task 2 corpus).
3. Changing the shared account to another currency while in review must re-derive every non-edited part's amount from its own text, not from the whole sentence (Task 5 test `sharedCuentaInAnotherCurrencyRederivesEachPartAmount`).
4. Back from the review and Continue again must not lose a hand-picked part category for the same título (Task 5 test `continueAgainKeepsAHandPickedPartCategoryForTheSameTitulo`).
5. A save failure must persist nothing and keep the review intact (Task 3 repository tests + Task 6 test `saveFailureKeepsTheReviewAndPersistsNothing`).

---

### Task 1: Quantity rule

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/entry/PhraseAmount.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/entry/SuggestionText.kt` (rename private `STOP_WORDS` → `internal val suggestionStopWords`)
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/entry/MovimientoPhraseParserTest.kt`

**Interfaces:**
- Produces: `internal fun scanPhraseAmounts(cursor: PhraseCursor, moneda: Moneda): List<PhraseAmountCandidate>` (quantities already removed) and `internal data class PhraseAmountCandidate(val money: Money, val start: Int, val endExclusive: Int)` (rename of the private `AmountCandidate`). `findPhraseAmount` keeps its signature and uses `scanPhraseAmounts`.

- [ ] **Step 1: Write the failing test** `quantitiesBeforeAThingAreNotAmounts` in `MovimientoPhraseParserTest`:

```kotlin
assertEquals(2_000_000L, minor("compré 2 hamburguesas por 20 mil"))
assertEquals(emptySet(), parse("compré 2 hamburguesas por 20 mil").reviewFlags)
assertEquals("2 hamburguesas por", parse("compré 2 hamburguesas por 20 mil").subject)
assertEquals(5_000_000L, minor("2 hamburguesas y 3 gaseosas por 50 mil"))
assertEquals(800_000L, minor("2 cafés 8000"))
assertEquals(500L, minor("5 cafés")) // lone quantity-shaped number stays the amount
assertEquals(2_000L, minor("spent 20 on lunch", Moneda.USD))
assertEquals(setOf(PhraseReviewFlag.AMBIGUOUS_AMOUNT), parse("cine 20 mil y 30 mil").reviewFlags)
```

- [ ] **Step 2: Run it and see it fail**
Run: `./gradlew :shared:testAndroidHostTest --tests "com.kurobello.tallybook.core.data.entry.MovimientoPhraseParserTest"` → FAIL on the first assertion (ambiguity flag / wrong subject).

- [ ] **Step 3: Implement** in `PhraseAmountScanner`: mark a candidate quantity-shaped when it came from `numeric()` with no factor, no leading symbol, no trailing currency, its token `raw` is digits only, value < 100 (major units), and the next token is a `WORD` not in `suggestionStopWords`, `digitMultipliers`, `adjacentMultipliers` or `currencyWords`. `scanPhraseAmounts` drops quantity-shaped candidates when at least one candidate is not quantity-shaped. Quantity tokens stay unconsumed, so they remain in the subject.

- [ ] **Step 4: Run the test class and the whole entry package** → PASS (`--tests "com.kurobello.tallybook.core.data.entry.*"`).

- [ ] **Step 5: Commit** `feat: read small counts before a thing as quantities`

### Task 2: Sentence splitter

**Files:**
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/entry/PhraseSplit.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/entry/MovimientoPhraseParser.kt` (extract the shared reading pipeline; add `split`)
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/entry/PhraseLexicon.kt` (add `partConnectors`)
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/entry/MovimientoPhraseSplitTest.kt`

**Interfaces:**
- Consumes: `scanPhraseAmounts` (Task 1).
- Produces: `fun MovimientoPhraseParser.split(text: String, moneda: Moneda, locale: AppLocale, today: LocalDate, reservedSpans: List<IntRange> = emptyList()): List<String>?` — the part texts in order (null when the sentence is not split). Each part text is the substring of the sentence with whitespace and `, ; . +` trimmed from the edges and repeated whitespace collapsed. `internal const val MAX_PHRASE_PARTS = 10`. `internal val partConnectors: Set<String> = setOf("y", "e", "and", "mas", "plus", "mais", "tambien", "also", "tambem")` plus PUNCT tokens `","` and `"+"`.

- [ ] **Step 1: Write the failing tests** in `MovimientoPhraseSplitTest` (helper `split(text, moneda = COP)` with today `2026-10-15`, `AppLocale.ES_AR`):

```kotlin
@Test fun theContextSentenceSplitsIntoTwoPartsAndASharedTail() =
  assertEquals(listOf("Compré 2 hamburguesas por 20mil", "3 gaseosas fueron 30mil"),
    split("Compré 2 hamburguesas por 20mil y 3 gaseosas fueron 30mil, más lo cargué con la tarjeta de crédito"))

@Test fun sentencesThatAreOneMovimientoTodayDoNotSplit() = listOf(
  "cine y comida 60 mil", "pan y leche 5 mil", "treinta y cinco mil en cine",
  "2 hamburguesas y 3 gaseosas por 50 mil", "20 mil por 2 entradas de 10 mil",
).forEach { assertNull(split(it), it) }

@Test fun splitsInEveryLanguage() {
  assertEquals(listOf("Gasté 20 mil en cine", "40 mil en comida"), split("Gasté 20 mil en cine y 40 mil en comida"))
  assertEquals(listOf("spent 20 on lunch", "15 on a taxi"), split("spent 20 on lunch and 15 on a taxi", Moneda.USD))
  assertEquals(listOf("gastei 20 no almoço", "15 no uber"), split("gastei 20 no almoço e 15 no uber", Moneda.BRL))
}

@Test fun cutsAtTheConnectorThatIntroducesTheNextAmount() {
  assertEquals(listOf("20 mil en cine y palomitas", "40 mil en comida"), split("20 mil en cine y palomitas y 40 mil en comida"))
  assertEquals(listOf("cine 20 mil", "comida y bebida 40 mil"), split("cine 20 mil y comida y bebida 40 mil"))
  assertEquals(listOf("20 mil en cine", "40 mil en comida y bebida"), split("20 mil en cine y 40 mil en comida y bebida"))
}

@Test fun decimalCommaIsNotAConnector() =
  assertEquals(listOf("pagué 1.500,50", "2.000"), split("pagué 1.500,50 y 2.000"))

@Test fun aCadenceInsideThePartStaysInIt() =
  assertEquals(listOf("netflix 20 mil", "spotify 10 mil cada mes"), split("netflix 20 mil y spotify 10 mil cada mes"))

@Test fun moreThanTenPartsDoNotSplit() =
  assertNull(split((1..11).joinToString(" y ") { "$it mil en algo" }))

@Test fun everyPartParsesToItsOwnAmount() {
  // Guards the contract the ViewModel relies on: parse(part).monto is the part's amount.
  val parts = split("Gasté 20 mil en cine y 40 mil en comida")!!
  assertEquals(listOf(2_000_000L, 4_000_000L), parts.map { parser.parse(it, Moneda.COP, AppLocale.ES_AR, today).monto?.minorUnits })
}
```

- [ ] **Step 2: Run and see it fail** — `--tests "com.kurobello.tallybook.core.data.entry.MovimientoPhraseSplitTest"` → compile failure (`split` undefined).

- [ ] **Step 3: Implement.** Extract the body of `parse` up to and including the date step into a private `read(...)` returning tokens, the cursor, and a `BooleanArray sharedConsumed` (tokens consumed by medio, reserved spans, recurrence or date); `parse` keeps its exact behavior on top of it. `split` algorithm (in `PhraseSplit.kt`, operating on the read result):
  1. `amounts = scanPhraseAmounts(cursor, moneda)`; return null unless `amounts.size in 2..MAX_PHRASE_PARTS`.
  2. For each consecutive pair `(a, b)`, connectors = free tokens in `[a.endExclusive, b.start)` that are a `partConnectors` word or a `","`/`"+"` PUNCT token. None → return null. Cut = the last connector whose following tokens up to `b.start` are all consumed or in `suggestionStopWords`; otherwise the first connector.
  3. Tail: after the last amount, the first connector whose following tokens include a `sharedConsumed` token starts the shared tail; otherwise there is none.
  4. Part `i` spans from the previous cut's end (sentence start for the first) to the next cut's start (tail start or sentence end for the last); clean edges as described above.

- [ ] **Step 4: Run the entry package tests** → PASS, including the existing `MovimientoPhraseParserTest`, `MovimientoPhraseMedioTest`, `MovimientoPhraseRecurrenceTest`.

- [ ] **Step 5: Commit** `feat: split a sentence with several amounts into parts`

### Task 3: Atomic batch writes

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/MovimientoRepository.kt`, `MovimientoRepositoryImpl.kt`
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/RecurrenciaRepository.kt`, `RecurrenciaRepositoryImpl.kt`
- Modify every implementor found by `grep -rln ": MovimientoRepository\|: RecurrenciaRepository" shared/src` (test fakes in `AddMovimientoFixture.kt`, `SearchFixture.kt`, `HomeViewModelTest.kt`, `ProfileViewModelTest.kt`, `ManageMovimientoFixture.kt`, `FakeRecurrenciaRepository.kt`, and any production decorator). Fakes outside `AddMovimientoFixture` may `error("unused")`; `AddFakeMovimientos` records into `inserted` and supports a configurable failure; the add-flow recurrencia fake records created recurrencias.
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/data/MovimientoRepositoryTest.kt`, `RecurrenciaRepositoryTest.kt`

**Interfaces:**
- Produces: `suspend fun MovimientoRepository.insertAll(movimientos: List<Movimiento>): DataOutcome<Unit>` and `suspend fun RecurrenciaRepository.createAll(movimientos: List<Movimiento>, frecuencia: Frecuencia, today: LocalDate): DataOutcome<List<Recurrencia>>`. Both validate every item first, then write all rows inside one `database.transactionWithResult`; any rejection returns `Failure` and writes nothing. Recurrencias share one change stamp per item exactly as `create` does (extract the body of `create` into a private function both use).

- [ ] **Step 1: Write the failing host tests** (backticked names, Robolectric style of the existing classes):
  - `` `insertAll writes every movimiento` `` — two valid movimientos → `getAll()` contains both.
  - `` `insertAll writes nothing when one movimiento is rejected` `` — second one references a missing cuenta → `Failure`, `getAll()` empty.
  - `` `createAll creates one recurrencia per movimiento` `` — two movimientos, `MENSUAL` → two recurrencias, each linked to its movimiento.
  - `` `createAll writes nothing when one movimiento is rejected` ``.
- [ ] **Step 2: Run and see them fail** — `--tests "com.kurobello.tallybook.core.data.MovimientoRepositoryTest" --tests "com.kurobello.tallybook.core.data.RecurrenciaRepositoryTest"` → compile failure.
- [ ] **Step 3: Implement** the two methods and update all implementors.
- [ ] **Step 4: Run** those two classes, then `:shared:compileKotlinIosArm64` (fakes and decorators compile on every target) → PASS.
- [ ] **Step 5: Commit** `feat: write several movimientos or recurrencias atomically`

### Task 4: Per-part interpretation in the engine

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/MovimientoEntryEngine.kt`
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/MovimientoEntryEnginePartsTest.kt`

**Interfaces:**
- Consumes: `MovimientoPhraseParser.split` (Task 2).
- Produces:

```kotlin
internal data class PartInterpretation(
    val titulo: String,
    val monto: Money?,
    val amountIsAmbiguous: Boolean,
    val query: SuggestionQuery,           // the part's subject with the whole sentence's tipo
    val ranking: List<CategorySuggestion>,
    val learnedCategoriaId: String?,
) { val preselectedCategoriaId: String? /* same rule as PhraseInterpretation */ }
```
  `PhraseInterpretation` gains `val parts: List<PartInterpretation> = emptyList()` (empty = single movimiento). `interpret` fills it after the whole-sentence reading: split with the same reserved spans and the final `moneda`; each part is parsed with `parse(partText, context, moneda, cuentas.cuentas)` and ranked with the sentence's `tipo`. Add `fun parsePart(text: String, context: PhraseContext, moneda: Moneda, cuentas: List<Cuenta>): ParsedMovimientoPhrase` only if `parse` cannot be reused as is.

- [ ] **Step 1: Write the failing tests** using the real parser/suggester the existing engine tests build (copy their construction):
  - `contextSentenceInterpretsTwoPartsWithSharedCredito` — parts' títulos and montos (20.000 / 30.000 COP), whole-sentence `sentence.medio` is `CREDITO`, both parts' `preselectedCategoriaId` are the food category the dictionary maps "hamburguesas"/"gaseosas" to (assert the actual seeded id the test fixture exposes).
  - `singleSentenceHasNoParts` — "cine y comida 60 mil" → `parts.isEmpty()`.
- [ ] **Step 2: Run, see it fail; Step 3: implement; Step 4: run → PASS.**
- [ ] **Step 5: Commit** `feat: interpret each part of a split sentence`

### Task 5: Review state — continue, categories, remove, join

**Files:**
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/EntryPart.kt`
- Modify: `AddMovimientoUiState.kt`, `AddMovimientoEntryUi.kt`, `AddMovimientoActions.kt`, `AddMovimientoViewModel.kt` (move new private helpers into a new `AddMovimientoParts.kt` extension file if Detekt `TooManyFunctions`/`LargeClass` fires)
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoPartsTest.kt` (uses `createAddViewModel` from `AddMovimientoFixture.kt`)

**Interfaces:**
- Consumes: `PhraseInterpretation.parts` (Task 4).
- Produces:

```kotlin
internal data class EntryPart(
    val titulo: String,
    val amountInput: String,
    val amountEdited: Boolean = false,
    val amountNeedsReview: Boolean,
    val categoriaId: String?,
    val categoriasEdited: Boolean = false,
    val suggestionQuery: SuggestionQuery,
    val suggestions: List<Categoria> = emptyList(),
    val descripcion: String = "",
    val overrides: PartOverrides = PartOverrides(),
)

/** Values a part's form changed away from the shared ones; null keeps following them. */
internal data class PartOverrides(
    val tipo: TipoMovimiento? = null,
    val fecha: LocalDate? = null,
    val cuentaChoice: CuentaSelectionState? = null,
    val etiquetaIds: List<String>? = null,
)

internal data class PartPreviewUi(val monto: Money?, val titulo: String, val categoria: Categoria?)
internal data class ReviewPartUi(/* index, monto (signed by its tipo), titulo, categoria, categoriaIsPending,
    suggestions, differing fecha/cuenta/medio to show under the título */)
internal data class MovimientoReviewUi(
    val parts: List<ReviewPartUi>, val total: Money?, val excludedFromTotal: Int,
    val tipo: TipoMovimiento, val fecha: LocalDate, val cuenta: MovimientoCuentaUi,
    val etiquetas: List<Etiqueta>, val etiquetasLoad: EtiquetasLoad, val frecuencia: Frecuencia?,
    val canJoin: Boolean, val isSubmitting: Boolean,
)
```
  - `AddMovimientoStep.REVIEW`; `STEP_BACK_TARGETS` gains `REVIEW → PHRASE`.
  - `AddMovimientoUiState` gains `parts: List<EntryPart> = emptyList()`, `editingPart: Int? = null`, `val review: MovimientoReviewUi`. `PhrasePreviewUi` gains `parts: List<PartPreviewUi> = emptyList()`.
  - `AddMovimientoActions` gains `onPartCategoriaSelected(index: Int, id: String)`, `onPartCategoriaCleared(index: Int)`, `onPartRemoved(index: Int)`, `onJoinParts()`.
  - In `REVIEW` the single-draft fields hold the **shared** values (tipo, fecha, cuentaChoice, etiquetaIds, frecuencia); the single `categoriaId`/`categoriasEdited` are left untouched so a hand pick returns if the sentence becomes single again.

- [ ] **Step 1: Write the failing tests** in `AddMovimientoPartsTest`:
  - `continueWithSeveralAmountsOpensTheReview` — context sentence → `step == REVIEW`, two parts with `amountInput` 20.000/30.000, títulos as in Task 2, shared medio `CREDITO`, `review.total` 50.000.
  - `detectedCardListsThePartsWhileTyping` — after the preview debounce, `detected.parts.size == 2`.
  - `pickingAPartCategoryOnlyChangesThatPart` and `aHandPickedSingleCategoryIsNotAppliedToParts`.
  - `removingDownToOnePartOpensTheSummaryForIt` — remove index 0 → `step == SUMMARY`, `amountInput` 30.000, `titulo` "3 gaseosas fueron 30mil".
  - `joinSumsThePartsIntoOneSummary` — `amountInput` 50.000, `titulo` = whole sentence, `categoriaId` = shared category when both parts have it; after a removal of one of three parts, título = remaining títulos joined with ", ".
  - `backFromReviewKeepsTheSentence` and `continueAgainKeepsAHandPickedPartCategoryForTheSameTitulo`.
  - `sharedCuentaInAnotherCurrencyRederivesEachPartAmount` — non-edited parts re-parse their own título in the new moneda (extend `withRederivedAmount`).
- [ ] **Step 2: Run, see it fail; Step 3: implement; Step 4: run the class plus `AddMovimiento*` tests → PASS** (existing single-sentence tests must stay green).
- [ ] **Step 5: Commit** `feat: review the movimientos of a split sentence`

### Task 6: Part form (Done) and Save all

**Files:**
- Modify: `AddMovimientoViewModel.kt` (or `AddMovimientoParts.kt`), `AddMovimientoActions.kt`, `AddMovimientoUiState.kt`
- Test: `AddMovimientoPartsTest.kt`

**Interfaces:**
- Consumes: Task 3 `insertAll`/`createAll`, Task 5 state.
- Produces: `AddMovimientoActions.onPartEdit(index: Int)`, `onPartDone()`. `onSubmit()` in `REVIEW` saves all parts. `stepBackTarget` for `FORM` with `editingPart != null` is `REVIEW` (Back discards the part's edits).
  - `onPartEdit(i)`: stash the shared values in a private `SharedDraft` held in state (`reviewShared: SharedDraft?`), load the part into the single-draft fields (overrides applied, `frecuencia` untouched), `step = FORM`, `editingPart = i`.
  - `onPartDone()`: validate like Save (`submitAttempted = true`); if invalid stay in the form. Otherwise write back amount/título/descripción/category (+ edited flags) and compute `overrides` as the fields that differ from `reviewShared`; restore shared values; `step = REVIEW`, `editingPart = null`.
  - Save all: build each part's `Movimiento` with the existing `toMovimientoOrNull` on the part-loaded state; the first null opens that part's form with `submitAttempted = true`. Then `insertAll` (no frecuencia) or `createAll`; success sends `AddMovimientoEffect.Saved`; failure sets `MovimientoFormError.SAVE_FAILED` and stays in `REVIEW`.
- [ ] **Step 1: Write the failing tests**: `editingAPartAndDoneReturnsToTheReviewWithItsChanges`, `backFromAPartFormDiscardsItsEdits`, `aPartWithAnotherFechaShowsItAndIgnoresLaterSharedChanges`, `saveAllInsertsEveryPartWithSharedValues` (integration contract from the spec: two inserted movimientos with títulos, montos, categories, `CREDITO`), `saveAllWithRepetirCreatesOneRecurrenciaPerPart` ("netflix 20 mil y spotify 10 mil cada mes"), `anInvalidPartOpensItsFormOnSave` (amount above `Money.MAX_MINOR_UNITS`), `saveFailureKeepsTheReviewAndPersistsNothing`, `joinIsDisabledWhenAPartHasAnotherTipo` (change one part's tipo through `onPartEdit` + `onTipoChanged` + `onPartDone`).
- [ ] **Step 2: Run, see it fail; Step 3: implement; Step 4: run `AddMovimiento*` → PASS.**
- [ ] **Step 5: Commit** `feat: edit and save every part of a split sentence`

### Task 7: UI — Detected multi layout, review sheet, part form

Invoke `frontend-design:frontend-design` and `android-skills:compose` before writing UI.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoReviewStep.kt`, `MovimientoReviewStepPreviews.kt`
- Modify: `MovimientoSentenceStep.kt` (multi layout of the Detected card when `detected.parts.size >= 2`: hide Category and Tags sections), `AddMovimientoSheet.kt` (route `REVIEW` with its own test tag `add-movimiento-review`; form primary action label `movimiento_part_done` and `actions::onPartDone` when `editingPart != null`; hide Repetir in the part form), previews/fakes implementing the new actions (`MovimientoFormPreviews.kt`, `MovimientoSentenceStepPreviews.kt`, `MovimientoFormTest.kt`, `AddMovimientoSheetTest.kt`).
- Strings (new file `strings_entry_parts.xml` in the four folders): `movimiento_parts_title` ("Revisá %1$d movimientos" style per locale), `movimiento_parts_total`, `movimiento_parts_total_excluded` (plural), `movimiento_parts_join`, `movimiento_parts_save` ("Guardar %1$d"), `movimiento_parts_remove` (content description), `movimiento_part_done`, `movimiento_parts_detected` ("%1$d movimientos"), `movimiento_part_no_category`.
- Test: `shared/src/androidDeviceTest/kotlin/com/kurobello/tallybook/feature/movimientos/ui/MovimientoReviewStepTest.kt`

**Interfaces:** Consumes `MovimientoReviewUi`, `PhrasePreviewUi.parts` and the Task 5/6 actions. The review reuses the summary's medio/account and tags components and the existing category picker (opened per part).

- [ ] **Step 1: Write the failing device tests**: rows show amount, título and category; tapping a category chip then a suggestion calls `onPartCategoriaSelected(index, id)`; × calls `onPartRemoved`; Join disabled when `canJoin` is false; Save label shows the count; the Detected card shows "2 movimientos" and both rows.
- [ ] **Step 2: Build and install** `:shared:installAndroidDeviceTest`, grant op 10021, run the class with `am instrument -e class …MovimientoReviewStepTest` → FAIL.
- [ ] **Step 3: Implement; Step 4: rerun the class plus `AddMovimientoSheetTest` and `MovimientoSummaryStepTest` → PASS.**
- [ ] **Step 5: Commit** `feat: show the review of a split sentence`

### Task 8: Docs, final review, device smoke

**Files:**
- Modify: `docs/business-logic.md` (Natural-language entry: a "Several movimientos" bullet group with the spec's rules; the consumption-order bullet), `docs/features/add-movimiento.md`, `docs/backlog.md` (remove "Varios movimientos en una frase" and renumber the priority list), `docs/features/README.md` row.
- Delete at merge time: `docs/tasks/multi-movimiento-phrase.md` (compacted into the docs above).

- [ ] **Step 1:** Update the docs.
- [ ] **Step 2:** Broad check through the wrapper: `./gradlew ktfmtFormat detekt :shared:testAndroidHostTest :shared:compileKotlinIosArm64 :androidApp:assembleDebug` → all pass.
- [ ] **Step 3:** One fresh independent code review of the branch diff against the spec; fix confirmed findings and rerun only the affected checks.
- [ ] **Step 4:** Xiaomi smoke (`adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk && adb shell am start -n com.kurobello.tallybook/.MainActivity`): type and dictate the context sentence → review with 2 parts and Crédito; change one category; Save → both in History. Join → one movimiento of 50.000. Remove one → summary. "netflix 20 mil y spotify 10 mil cada mes" → two recurrencias in Recurrentes.
- [ ] **Step 5: Commit** `docs: describe several movimientos in one sentence`
