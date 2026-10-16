# Wave 1 / Task 1 — Sentence parser

## Goal

A pure, deterministic `MovimientoPhraseParser` in `commonMain` that turns one sentence into
`ParsedMovimientoPhrase` following the rules in
[business-logic.md](../../../../business-logic.md#natural-language-entry).

## Context

- Contracts: [README](../../README.md#shared-contracts). `subject` is what remains after removing tipo
  verbs, amount tokens (digits, multipliers, number words, currency symbols/words) and date
  expressions, joined by single spaces and keeping the user's words in order. Stop words, plurals and
  accent folding belong to task2, not here.
- Vocabulary is the union of all supported languages regardless of `locale`; `locale` only selects
  day/month order for numeric dates (month-first for `EN`, day-first otherwise). Users mix languages
  and dictation output.
- Reuse `normalizeSearchText` for matching keywords (`core/ui/text/SearchText.kt`). It currently
  lives in `core.ui.text`; if the parser in `core.data.entry` cannot depend on it cleanly, move it to
  a framework-free package and update its imports — do not duplicate it.
- Amount conversion must produce exact `Money` minor units with `Moneda.scale`. Reuse the existing
  overflow semantics of `AmountInput.kt` (Long minor units; reject overflow instead of wrapping).
- Pure value mappings (verb → tipo, multiplier word → factor, weekday word → `DayOfWeek`) use typed
  lookup tables, not `when`/`if` chains.

## Rules to implement

- **Tipo**: expense verbs (`gasté, gaste, pagué, compré, me cobraron, spent, paid, bought, gastei,
  paguei, comprei`…) and income verbs (`recibí, me pagaron, cobré, vendí, me depositaron, gané,
  received, got paid, earned, sold, recebi, ganhei, me pagaram, vendi`…). `me cobraron` must not
  match `cobré`. No verb → `tipo = null`.
- **Amount candidates**: `20000`, `20.000`, `20,000`, `12.50`, `12,50`, `$20.000`, `R$ 30`, `S/ 25`,
  `20k`, `20 mil`, `1,5 millones`, `1.5M`, `20 lucas`, `2 palos` (millón), `5 mangos` (unit), number
  words `veinte mil`, `cien`, `quinientos`, `twenty`, `vinte reais`. A separator followed by exactly
  three digits is grouping; any other is decimal. More decimals than `Moneda.scale` invalidates the
  candidate. Several candidates → choose the largest and add `AMBIGUOUS_AMOUNT`. Numbers consumed by a
  date are not amount candidates.
- **Dates** relative to `today`: `hoy/today/hoje`, `ayer/yesterday/ontem`,
  `anteayer/antier/anteontem/day before yesterday`, weekday names (most recent occurrence including
  today, with or without `el`/`on`/`na`), `el 12`, `12 de octubre`, `12/10`, `10/12` (EN). A day
  without month resolves to its most recent past occurrence. A resolved future date is dropped with
  `FUTURE_DATE_IGNORED`.
- Blank input returns all nulls and an empty subject.

## Files

- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/ParsedMovimientoPhrase.kt`.
- Create `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/data/entry/MovimientoPhraseParser.kt`
  and focused private helpers in the same package (`PhraseAmount.kt`, `PhraseDate.kt`,
  `PhraseLexicon.kt`) when a file would exceed Detekt limits.
- Test `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/data/entry/MovimientoPhraseParserTest.kt`
  (table-driven; camelCase names).

## Tests (grouped, table-driven)

Cover at least: each language's expense/income verbs including `me cobraron` vs `cobré`; every amount
form above for COP, USD, CLP (scale 0) and BRL; ambiguous amounts; overflow; dates across month and
year boundaries, weekday equal to today, future dropping and EN month-first; subject extraction for
"Gasté 20.000 en cine ayer" → "en cine"; "Cambio de aceite 80 mil el lunes" → "Cambio de aceite".

## Done when

Tests pass on `:shared:testAndroidHostTest` and compile for iOS
(`:shared:compileTestKotlinIosSimulatorArm64`), `detekt ktfmtCheck` clean. No UI or DI changes.
