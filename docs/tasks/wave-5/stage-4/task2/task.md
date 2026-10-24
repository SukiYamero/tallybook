# Wave 5 / Stage 4 / Task 2 — Sentence detection of medio and account

## Objective

Detect the payment method and a named account from the creation sentence in every supported
language, keep them sentence-owned until the user picks by hand, and read the amount with the
detected account's currency.

## Context and skills

- Read `AGENTS.md`, [the wave README](../../README.md) → Entry and selection, `docs/business-logic.md`
  → Natural-language entry (tipo/monto/fecha/tags rules and ownership), `docs/wording.md`.
- Skills: `android-skills:android-dev`, `chrisbanes-skills:kotlin-api-design`,
  `chrisbanes-skills:kotlin-control-flow`, `android-skills:android-testing`,
  `superpowers:test-driven-development`.
- Current pipeline: `MovimientoPhraseParser.parse(text, moneda, locale, today)` tokenizes, consumes
  tipo verbs, date and amount, and returns the remaining `subject`; tables live in `PhraseLexicon.kt`
  keyed by search-normalized text, vocabulary is the union of es, es-AR, en and pt-BR.
  `EtiquetaMatcher.match` finds full tag names as whole-word sequences on the raw text.
  `MovimientoEntryEngine.interpret` combines parse, suggestions and tags.

## Files

- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/entry/{PhraseLexicon,MovimientoPhraseParser}.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/entry/CuentaMatcher.kt`
  (extract the whole-word sequence search shared with `EtiquetaMatcher` instead of copying it)
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/entry/EtiquetaMatcher.kt` (shared helper only)
- Modify: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/ParsedMovimientoPhrase.kt` (`medio: MedioPago?`, `medioCandidates`)
- Modify: `feature/movimientos/viewmodel/{MovimientoEntryEngine,AddMovimientoViewModel}.kt` (integration step, after task1)
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/entry/{MovimientoPhraseParserTest,CuentaMatcherTest,EtiquetaMatcherTest}.kt`;
  `shared/src/commonTest/kotlin/com/kurobello/tallybook/feature/movimientos/viewmodel/AddMovimientoEntryFlowTest.kt` (extend)

## Contracts

```kotlin
data class ParsedMovimientoPhrase(
    /* existing fields */,
    val medio: MedioPago?,
    val medioCandidates: Set<MedioPago>, // non-empty only when medio is ambiguous (generic card word or two method words)
)

internal class CuentaMatcher {
  /** Live non-archived accounts whose full name appears as whole words, earliest first. */
  fun match(text: String, cuentas: List<Cuenta>): List<CuentaMatch>
}
internal data class CuentaMatch(val cuentaId: String, val start: Int, val endExclusive: Int)
```

The parser stays pure and account-agnostic for medio words. Account matching runs in the engine
**before** amount parsing, so the amount is read with the matched (or preselected) account's
currency; the matched span and medio words leave the `subject` used for category suggestions, while
the title remains the sentence as typed.

## Exact rules

- Vocabulary (normalized, all languages in one table; the implementer completes regional variants
  and records them here at close):
  - `EFECTIVO`: "en efectivo", "efectivo", "en cash", "cash", "in cash", "em dinheiro", "dinheiro",
    "em especie".
  - `DEBITO`: "con debito", "tarjeta debito", "tarjeta de debito", "debit card", "with debit",
    "cartao de debito".
  - `CREDITO`: "con credito", "tarjeta de credito", "credit card", "with credit",
    "cartao de credito".
  - Excluded on purpose: pt-BR "no débito"/"no crédito" normalize to the Spanish negation "no
    debito"/"no credito" in the shared union vocabulary, so neither form is a method word; pt-BR users
    are covered by "cartão de débito/crédito". Negated mentions ("no con débito", "sin tarjeta",
    "not with credit", "sem cartão") never select or suggest a medio.
  - `TRANSFERENCIA`: "por transferencia", "transferencia", "by transfer", "bank transfer",
    "por pix", "pix", "via pix".
  - Ambiguous card: "con tarjeta", "con la tarjeta", "with card", "by card", "no cartao", "com
    cartao" → `medio = null`, `medioCandidates = {DEBITO, CREDITO}`.
  - Two different explicit method words in one sentence → `medio = null`, `medioCandidates` = both;
    nothing is decided.
- Verbs such as "transferí" are not medio words in this wave (internal transfers are out of the
  sentence flow); a test pins that "le transferí 50k a Juan" detects no medio.
- Account resolution and precedence follow [Entry and selection](../../README.md#entry-and-selection):
  account match implies medio for EFECTIVO/CREDITO, BANCO defaults to DEBITO unless a transfer word
  is present; contradictions keep the account and drop the method word; two accounts matched → the
  earliest wins and the other is offered as a suggestion; the same name in several currencies prefers
  the primary-currency account, otherwise it is ambiguous and the candidates are offered; an account
  name that is also a tag name selects both.
- An incompatible medio with the tipo (DEBITO in an ingreso) is converted per the README rule, never
  left invalid.
- Ownership: sentence-owned medio/account are replaced on each interpretation and on Continue; the
  first manual choice transfers ownership to the user; clearing the sentence clears only
  sentence-owned values — the same lifecycle as sentence-owned tags.
- Changing the account while the amount is sentence-owned re-derives it with that account's currency.

## TDD

- [x] RED parser tables per language: each medio phrase, ambiguous card, two method words, negated mentions, no medio, "transferí" verb, medio words removed from subject, interaction with amount/date tokens ("50 mil en efectivo ayer").
- [x] RED `CuentaMatcherTest`: whole words, accents/case folding, multi-word names, overlapping names (earliest/longest), archived excluded, same name in two currencies (primary preferred / ambiguous), name equal to a tag.
- [x] GREEN; refactor the shared sequence helper with `EtiquetaMatcherTest` green.
- [x] Integration (after task1): `AddMovimientoSentenceCuentaTest` (sibling of `AddMovimientoEntryFlowTest`, already 750 lines) for ownership transfer, clear sentence, Continue/Back, currency-dependent amount re-derivation (COP vs CLP account).
- [x] `./gradlew :shared:testAndroidHostTest :shared:compileKotlinIosSimulatorArm64 detekt ktfmtCheck`.

## Parser layer record (done; integration pending)

Pure layer in `core/data/entry`, tested by `MovimientoPhraseMedioTest`, `CuentaMatcherTest`,
`SentenceSelectionTest` (all languages in one table, `medioWords` in `PhraseLexicon.kt`):

- API: `MovimientoPhraseParser.parse(text, moneda, locale, today, reservedSpans: List<IntRange> = emptyList())`
  fills `medio`/`medioCandidates`; `CuentaMatcher.match` as contracted (char offsets into the raw
  text); `resolveSentenceCuenta(matches, cuentas, primary): SentenceCuenta(cuenta, alternativeIds)`;
  `resolveSentenceMedio(parsed, tipo, cuentaTipo): SentenceMedio(medio, candidates)` applies account
  precedence, the BANCO transfer-word rule and `forTipo` (an ingreso never keeps `DEBITO`).
  `SearchWords.kt` holds the whole-word helper shared with `EtiquetaMatcher`.
- Vocabulary added beyond the list above: EFECTIVO "with cash", "no dinheiro", "dinheiro vivo";
  DEBITO "tarjeta debito", "con la de debito" (es-AR); CREDITO "tarjeta credito", "con la de credito";
  TRANSFERENCIA "con transferencia", "wire transfer", "no pix", "pelo pix"; any card "con mi
  tarjeta", "with a card", "with my card", "com o cartao", "com meu cartao". Negators: no, sin, not,
  without, sem, não; negator + card noun ("sin tarjeta") is consumed without a method.
- Rules added to avoid false positives (challenge for review): a card phrase that is the object
  being paid — after "pago/pagar/pay/fatura/factura/cuota/de/do/da/of", after a determiner not
  introduced by a preposition ("Pagué la tarjeta de crédito"), or before "bill/payment/statement" — is
  not a method word and stays in the subject (it is a Finance keyword). "cash back", "petty cash" and
  "transferencia recibida" are category keywords, not method words. Phrases starting with pt "no"
  ("no pix", "no cartão de crédito") are listed whole so the Spanish negator never cancels them.
- Order inside the parser: tipo verbs → medio words → reserved spans → date → amount.

### Integration record

Done in task1's commit: `MovimientoEntryEngine.interpret` matches cuentas first, parses with the
reserved spans, resolves medio with the final tipo and re-parses when `SentenceCuentas.monedaFor`
(the layered selection's currency) differs; `buildIndex` drops account names from history subjects
too (point 4); a tipo change applies `forTipo` to the selection (point 5).

### What the integration step needed (after task1)

1. `MovimientoEntryEngine` gets a `CuentaMatcher` and the live `List<Cuenta>` plus the primary
   `Moneda` (or reuses task1's account list); `interpret` runs `match` → `resolveSentenceCuenta`
   first, parses with `sentenceCuenta.cuenta?.moneda ?: <selected/preselected account currency>`
   and `reservedSpans = matches.map { it.start until it.endExclusive }` (all matched names, also the
   alternatives), then `resolveSentenceMedio(parsed, tipo, sentenceCuenta.cuenta?.tipo)` with the
   final tipo (parsed or implied).
2. `PhraseInterpretation` carries `SentenceCuenta` + `SentenceMedio`; task1's
   `CuentaSelectionState` layers them as sentence-owned over `defaultSelection` (same lifecycle as
   sentence-owned tags; a medio-only result restricts the account to compatible ones, an ambiguous
   result offers `candidates`/`alternativeIds` and keeps the default).
3. Re-derive a sentence-owned amount when the account changes: re-run `parse` with the new account's
   currency (the parser is pure; no cache to invalidate).
4. `buildIndex` parses history titles without account spans; decide whether history subjects should
   also drop account names (needs the account list at index time) so learned subjects match live ones.
5. A tipo change after interpretation must re-run `resolveSentenceMedio` (or `MedioPago.forTipo`).
6. `AddMovimientoEntryFlowTest` cases listed in TDD above.

## Blocks

Blocked by Stage 2 review (domain types). Integration step blocked by task1. Blocks Stage 6.
