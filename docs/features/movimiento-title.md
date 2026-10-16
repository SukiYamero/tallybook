# Movimiento title and day-grouped Search

A movimiento has an optional title next to its optional description. The entry sentence becomes the
title, and every movement card shows title, description and amount without a date. Search groups its
results by day. Product rules live in [business-logic.md](../business-logic.md#movimiento-gasto-o-ingreso);
card tokens live in [UI architecture](../ui/architecture.md#movement-card-amounts).

## Interaction

- **Entry**: Continue stores the sentence as `titulo`. The review sheet shows it quoted under "Título"
  and offers an optional "Descripción" field. That field stays above the keyboard through
  `centerFocusedInputIn` plus scroll room for the keyboard's height. Back and a new Continue keep a
  typed description.
- **Form and editor**: optional "Título" and "Descripción" fields (`MovimientoOptionalTextField`),
  each capped at 180 characters with a counter and normalized like every optional text.
- **Cards** (Home, History, Search):
  - The avatar is followed by one stacked column.
  - First comes the title (`cardTitle`, 14sp bold, at most two lines), with the category name as
    fallback.
  - Then the description (`caption`, one line), only when present.
  - The signed amount comes last, so long titles and amounts never share a line.
  - Text ends with an ellipsis and opening a card shows the full fields.
- **Search**:
  - One `CivilDayHeader` (`dayHeading`, 15sp semibold, shared with History) per day that has results,
    newest first.
  - Text matches title, description or category.
- **Amounts**: the sign leads the symbol everywhere `Money.format` is used (`-$ 25,00`, `+$ 5.000,00`).

## Integration

- **Schema 5 (`4.sqm`)** rebuilds `movimiento` with `titulo` as the last column, so the migrated DDL
  matches a fresh install.
  - It drops the view first and recreates it, because the rename would rewrite it.
  - Existing `descripcion` values, which held the typed sentence, move to `titulo`.
- **Category-suggestion history** (`selectCategoryHistory`, `MovimientoEntryEngine`) reads `titulo`.
- **CSV export** adds a title column before the description.
- **`SearchResultsUi.groups`** derives the day groups once from the ordered rows.

## Verification boundaries

- **Checks passed:** Detekt, ktfmtCheck, the SQLDelight migration verification, 721 shared host
  tests, `compileAndroidDeviceTest` and the Android debug APK.
- **Migration test:** a 4 → 5 test upgrades a populated snapshot and compares it with a fresh schema.
- **Review:** an independent review covered the final branch.
- **Xiaomi Mi 11X, manual checks:**
  - The in-place v4 → v5 upgrade: 11 sentences moved to `titulo`.
  - Phrase entry with a summary description and the keyboard.
  - Truncation of a long title and a long description.
  - The editor fields, Home/History/Search cards, Search day headers and signed amounts.

Not covered:
- iOS runtime.
- The updated and new device UI tests. They compile, but the instrumented run hung before its first
  activity launched on the Xiaomi.
- TalkBack and enlarged-text passes.
