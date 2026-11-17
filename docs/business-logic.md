# Business logic — Tallybook

Única referencia de reglas de negocio del dominio, compartidas por Android e iOS vía `shared/commonMain`.
Se actualiza en el lugar a medida que la lógica cambia — no se duplica en otro `.md` ni se versiona por
decisión histórica (eso lo cubre git).

## Qué es la app

App de finanzas personales tipo budget/wallet para Android e iOS. El objetivo es que el usuario entienda
mejor sus finanzas — en qué gastó, cuánto ingresó, cómo queda su balance — no reemplaza un banco ni
sincroniza cuentas bancarias reales.

## Movimiento (gasto o ingreso)

Un **movimiento** registra un gasto o un ingreso.

- **Obligatorios**: tipo (gasto / ingreso), monto, fecha, cuenta, medio de pago y exactamente una
  categoría. La moneda es la de la [cuenta](#cuentas): el movimiento no guarda una moneda propia. El
  monto es positivo y no supera `Money.MAX_MINOR_UNITS` (10^15 unidades menores), el mismo tope de las
  transferencias y del saldo inicial de una cuenta (en ambos signos).
- **Opcionales**: tags (ver [Category and tags](#category-and-tags)), título y descripción, de hasta
  180 caracteres cada uno. El título nombra el
  movimiento (en la creación por frase es la frase escrita); la descripción agrega contexto extra si
  el usuario quiere. `createdAt` es metadata técnica para ordenar de forma estable, no un campo
  solicitado al usuario.
- **Tarjetas**: toda tarjeta de movimiento (Inicio, Historial, Buscar) muestra el título, o el nombre
  de la categoría si no hay título, en hasta dos líneas, debajo la descripción en una línea con un
  tamaño menor (sin descripción esa línea no aparece) y al final el monto, precedido por el ícono del
  [medio de pago](#medio-de-pago), que se anuncia con su nombre. La tarjeta no muestra la fecha (Historial
  y Buscar ya agrupan por día), la cuenta ni los tags. El texto completo se ve al abrir la tarjeta.
- **Signo del monto**: el signo va antes del símbolo de la moneda (`-$ 25,00`, `+$ 5.000,00`), el orden
  de CLDR que siguen las apps bancarias y de finanzas.
- La categoría es obligatoria en el dato, no en la interacción: si el usuario guarda una creación sin
  elegir ninguna, el movimiento queda en la categoría de sistema **Otros** y se marca como pendiente
  de completar (ver [Categorías](#categorías)).

### Natural-language entry

Creation starts from one sentence that describes the movimiento ("Gasté 20.000 en cine ayer"). The
app derives the draft from it and the user confirms or refines it; the manual form stays available.

- The entry is a centered modal dialog anchored near the top, so the keyboard never moves it. It
  opens without focus or keyboard: the user sees the input, whose placeholder is a localized example
  with an amount formatted for the primary currency, and taps it to type. The sentence respects the
  title length limit.
- Below the input, a "Detected" area appears once the sentence has text and goes away if it is
  cleared. It shows tipo and fecha (their defaults, gasto and today, until the sentence says
  otherwise), the amount when found (otherwise a note that it was not detected), the Medio and
  Account sections, a "Category" section and an optional "Tags" section below it. Values update while typing; nothing is persisted
  until Save. While the user has not touched the category, the sentence owns it: the high-confidence
  category shows selected and the other suggestions stay visible but dimmed. Tapping a dimmed
  suggestion switches to it; the × on the selected one clears it. The first manual choice hands the
  category to the user, and later sentences or Continue no longer replace it. Tags follow the same
  ownership: tags the sentence detected stay owned by it until the user adds or removes a tag. Medio
  and account have their own owners (see [Selection in entry and editing](#selection-in-entry-and-editing)).
- The Continue action is pinned in a footer under the scrolling content, so it stays visible with
  the keyboard open. Inside the scrolling area, below the Detected card, an "or" divider introduces
  voice dictation, followed by a secondary "fill manually" action that opens the empty form. Fill
  manually drops everything the sentence derived but keeps the category and tags the user picked by
  hand in the dialog.
- **Voice dictation** fills the same sentence, so everything below applies to dictated text too.
  - Recognition runs only on the device (Android's on-device `SpeechRecognizer`, iOS
    `SFSpeechRecognizer` with on-device recognition required): the sentence describes the user's
    money and never reaches a speech server. Without an on-device recognizer the microphone shows as
    not available on this device.
  - Language models ship separately (a phone set to English may only have en-US). On Android 13+
    the app checks them before listening: another installed region of the same language is used
    as is; a missing one is requested from the system, which asks the user to confirm the download,
    and the app says to try again once it finishes. A language the device cannot recognize offline
    says so.
  - The recognizer's language follows the app language with one fixed variant each: Spanish (es and
    es-AR) dictates es-ES, English en-US and Portuguese pt-BR, the variants most offline recognizers
    can install. An installed model of another region of that language is used instead when present.
  - The microphone is held to talk: pressing it starts listening and letting go ends it, keeping what
    was heard. Sliding the finger off it does not end it, nor does the dialog scroll under it; 30
    seconds does. The app asks the recognizer to wait through pauses of up to 10 seconds, but some
    devices may still end listening sooner. Listening goes on 0.7 seconds after the finger lifts,
    because people let go while saying the last word. The disc turns green, and the caption asks to
    speak, only once the microphone is open; until then it says to wait. Screen readers, which
    cannot hold a press, toggle listening with their activation instead.
  - Sliding the held microphone 96 dp to the left cancels at once, as in messaging apps. While it
    can be cancelled, a guide left of the disc shows "Slide to cancel" with chevrons lighting up
    toward the left; the caption under the disc keeps saying to let go to finish. The disc follows
    the finger and fades, the phone vibrates, listening stops and the sentence returns to what was
    typed before the press, without a notice; lifting the finger afterwards does nothing. A shorter
    or sideways drift keeps listening. Screen readers get a "Cancel dictation" action while
    listening, and the guide is hidden from them. Once the finger has lifted, what was said is the
    user's text: neither the release tail nor an ended dictation can be cancelled.
  - The first press asks for permission (on iOS, speech recognition and then the microphone) and
    does not listen; after granting it, the next press does. A refusal explains why it is needed; a
    second refusal, or a denial iOS already remembers, offers the app settings instead.
  - Dictated words extend whatever was already typed and appear in the sentence while the user
    speaks. The Detected card keeps the visibility it had when the press began, so the microphone
    never slides from under the finger; it updates once the final result arrives. Hearing nothing,
    as after a quick tap, asks to hold the microphone while speaking; a failure keeps the text and
    says it can be retried.
  - Typing, Continue, Fill manually, Discard, closing the dialog or leaving the app stops listening at
    once.
- Line breaks typed or pasted into the sentence become single spaces. Clearing the sentence also
  clears what it derived (tipo, amount, fecha, título, descripción, the sentence-owned category and
  tags, and a sentence-owned medio and account, which return to the default selection), so the dialog
  counts as clean again even after a Continue and Back; a category, tags, medio and account picked by
  hand remain.
- Tapping outside or the Back gesture closes the dialog directly while the sentence is blank;
  otherwise it asks to discard first. Continue parses the sentence once, writes tipo, monto, fecha,
  medio, cuenta, categoría and tags (each unless the user took it over) and título into the draft (replacing earlier
  derived values), closes the dialog and opens the summary. A description typed in the form is
  kept.
- **Tipo**: expense and income verbs in every supported language decide it (`gasté`, `pagué`,
  `me cobraron` → gasto; `recibí`, `me pagaron`, `cobré`, `vendí` → ingreso). Without a verb, an
  income category suggestion selects ingreso; otherwise gasto.
- **Monto**: digits with the draft currency's group/decimal separators (a space, as dictation writes
  "50 000", groups only before exactly three digits), a currency symbol,
  multipliers (`k`, `mil`, `millón`/`millones`, regional slang such as `lucas` or `palos`) and basic
  number words. A separator followed by exactly three digits groups; any other separator is decimal.
  The draft currency is the selected account's: the sentence is read again in another currency when
  the account it names is in one. While the amount is sentence-owned, choosing another account
  re-derives it from the sentence in that account's currency; a typed amount keeps its value,
  reformatted for the new currency, and Save validates it against that currency's scale (CLP has no
  decimals). When several candidates remain, the largest one wins and the summary flags the amount
  for review. No amount leaves the field empty and required; the dialog and the summary say it was not
  detected, and the summary points to Edit movimiento. The parser does not apply the amount cap; Save
  routes an amount above it to the form's amount error.
- **Fecha**: today, yesterday, the day before yesterday, a weekday (its most recent occurrence, today
  included) and explicit day/month dates (day-first except in English). A day without month resolves
  to its most recent past occurrence. Future expressions are ignored and the date stays today.
- **Frecuencia**: a stated cadence turns Repetir on with the matching frecuencia when the sentence
  continues, unless the user already switched Repetir or picked a frecuencia, which then stays theirs.
  - 7 days, a week or a weekday ("todos los lunes", "every monday", "toda segunda") → Semanal;
    28–31 days, 4 weeks or a month ("cada 30 días", "todos los meses", "al mes", "per month",
    "todo mês") → Mensual; 365–366 days, 52 weeks, 12 months or a year → Anual. Adjectives
    ("mensual", "weekly", "anual") count too, but the first stated interval wins over them
    ("compra semanal cada mes" is Mensual). A bare number before "every month" is the amount;
    only a marked day ("el 5 de cada mes", "the 1st of every month") is the day of the month.
  - A weekday or a day of every month ("el 5 de cada mes") also sets the fecha to its most recent
    occurrence when the sentence names no date.
  - Any other cadence ("cada 15 días", "quincenal", "trimestral") leaves Repetir off and the summary
    says that Repetir only works weekly, monthly or yearly.
  - Interval phrases leave the subject, so their numbers are never the amount or a date; a lone
    adjective stays in it because category keywords use it ("compra semanal").
  - A price per day ("200000 por día") and a duration ("curso de 12 meses") are not recurrences.
- **Título**: the sentence as typed, trimmed and with repeated whitespace collapsed, like every
  optional text. Parsing never rewrites what the user wrote. The
  description starts empty; it is optional extra context the user adds in the form.
- **Categoría**: the suggestion engine below runs on the sentence without its verb, amount, date
  and stop words. A high-confidence result is preselected; other results appear as dimmed
  suggestions that replace the selected category when tapped. Without a selection at Save, the
  movimiento falls back to Otros.
- **Tags**: the sentence selects existing tags whose full name appears in it as whole words, compared
  with the shared search normalization. Detection never creates a tag.
- **Medio and account**: detected in every supported language, with the vocabulary owned by
  `PhraseLexicon` (`medioWords`) and compared after the shared search normalization.
  - Method phrases ("en efectivo", "con débito", "tarjeta de crédito", "por transferencia", "pix",
    "credit card", "cartão de débito"…) select their medio.
  - A card without its kind ("con tarjeta", "with my card", "no cartão") decides no medio and offers
    Débito and Crédito. Two different explicit methods in one sentence decide none and offer both.
    A phrase naming the kind wins over a generic card mention that overlaps it.
  - A method mention directly after a negation ("no", "sin", "not", "without", "sem", "não") decides
    nothing and leaves the subject, as do "sin tarjeta" and "sem cartão".
  - A card that is the thing being paid is not a method: one preceded by "de", "pago", "pagar", "pay",
    "fatura", "factura" or "cuota", by a determiner with no method preposition ("pagué la tarjeta de
    crédito") or followed by "bill", "payment" or "statement" stays in the subject for the category
    suggestion. Category phrases that contain a method word ("cash back", "petty cash",
    "transferencia recibida") are never methods.
  - A live, non-archived account whose full name appears as whole words is selected. Where names
    overlap the earliest, then the longest, owns the words; the earliest named account wins and other
    named accounts are offered. A name shared across currencies resolves to the primary-currency
    account, otherwise it stays undecided and offers the candidates. An account name never becomes a
    date or an amount and leaves the subject.
  - A named account outranks method words: its type implies the medio (`TipoCuenta.defaultMedio`:
    Efectivo, Crédito, and for a bank account Débito on a gasto and Transferencia on an ingreso),
    except that a transfer word with a bank account selects Transferencia. A contradicting method word
    is dropped. Every result is adapted to the final tipo.
  - Words are consumed in a fixed order: tipo verbs, method phrases (before account names, so "en
    efectivo" is not split by the seeded "Efectivo" account), account names, recurrence, fecha, monto;
    the rest is the subject.
- The summary is a tall bottom sheet titled as a review-and-confirm step. It only reviews: labeled
  sections show tipo and fecha (prominent), the amount and the title, and the only choices it offers
  are medio and account, the category with its suggestions and the optional tags. It has no text
  input; the description lives in the form. Its two
  equal-level actions, separated from the content by generous space, are Edit movimiento (left),
  which opens the complete form with the same draft, and Save (right). Save with an invalid required
  value opens the form with that field's error and focus instead of persisting. Back returns from
  the form to the summary (when reached through it); Back from the summary reopens the sentence
  dialog, preserving the draft and the typed text.
- A non-empty sentence counts as an unsaved change for the existing dismissal guard. Editing an
  existing movimiento keeps opening the form directly.

### Category suggestions

Suggestions are computed on the device from the user's own history and a built-in keyword
dictionary. No text leaves the device and no separate learning store exists: saved movimientos are
the training data, so correcting a suggestion teaches the next one.

- Text is compared after the shared search normalization (case and supported accent folding), stop
  word removal and basic plural reduction.
- Signals, strongest first: an exact match with a previous title's subject; word-to-category
  associations learned from history, weighted by frequency and recency; dictionary phrases; then
  dictionary words and brands. Phrases outweigh their words ("cambio de aceite" → Transporte).
- Ambiguous words ("aceite", "oil", "óleo", "ticket", "gas") point at several categories and never
  produce a high-confidence result on their own; when unsure, the app offers up to three of them as
  suggestions instead of guessing.
- Each past movimiento is one full vote for its category. When a clear majority of the past
  movimientos with exactly the same subject share a category, that category is the high-confidence
  suggestion.
- The dictionary covers every seeded category in all supported languages, with regional variants
  and common local brands, and points at the most specific category.
- High confidence means an exact history match or one strong dictionary match without a competing
  category. At most three suggestions are shown besides the selected category. Clearing a category
  the sentence suggested dismisses it: it is not offered again for the same subject, and a
  different subject starts clean. Otros is never suggested.

### Editing and deletion

Home, History and Search open the current persisted movimiento by ID. Missing records cannot be
edited or deleted; failed reads require Retry before mutation actions become available.

- Editing permits type, positive exact amount, civil date, medio, account, category, tags, optional
  title and optional description. The account can change only to one in the stored currency, so the
  currency, ID and `createdAt` remain fixed; an archived account stays valid while untouched. Amount
  input round-trips through the stored currency's scale and decimal separator without grouping or a
  currency symbol.
- Creation's validation and title/description normalization also apply to editing. Civil today/timezone
  is captured at editor entry and again on Save; future dates are rejected without silently changing
  the draft when the clock moves backward.
- The category picker selects exactly one existing category, parent or child, except Otros; choosing
  one replaces the current category and closes the picker. The editor always keeps one category and
  offers no way to clear it. An existing Otros or missing category remains visible and valid when
  untouched; a replacement must exist in the loaded catalog. Category and tag updates cannot
  overwrite typed input or clear an unretried save failure.
- Unsaved-change guarding compares input with its opening baseline. A valid normalized no-op performs
  no database update and closes the editor without saved feedback. A changed successful Save closes
  management; failure retains the draft for explicit retry. Saving a missing ID never inserts it.
- Selecting a movement opens its prefilled editor directly after the read succeeds. Cancel or Back
  closes the editor. Changed input first requires Continue or Discard; Continue preserves the draft,
  while Discard closes without saving. Scrim/drag uses the same draft guard. Nested selection surfaces
  close first; submission cannot be dismissed or repeated.
- Delete requires confirmation from the editor and commits immediately against the persisted row,
  without saving the draft first. Cancelling deletion retains all unsaved input. Invalid drafts and
  unavailable category catalogs do not block deletion. Pending confirmations and submission reject
  competing edits or mutations. The transaction validates that
  its captured row can be restored before deleting it; an ineligible legacy record stays intact.
  Failure keeps the editor and draft for retry. An already-missing record produces informational feedback.
- Deleting leaves a tombstone (see [Change stamps and tombstones](#change-stamps-and-tombstones)).
  Only the exact row captured by the delete transaction is eligible for Undo. Restore revives the
  tombstone with the same ID, all captured fields and a new stamp. An identical live row counts as
  restored without writing; a different or unreadable same-ID row, or a tombstone with different
  content, is a terminal conflict and is preserved.
- Restoring into an archived account is allowed and leaves it archived. An account that no longer
  exists gives `RestoreResult.MissingCuenta`: its own warning message, without Retry, releases the
  snapshot, and the money never moves to another account. An account whose currency changed, or whose
  type no longer takes the captured medio, is a conflict. Transfers share the same outcomes.
- Each feedback token owns at most one deleted snapshot, a movimiento or a transfer. Completed
  management outcomes wait in arrival order without replacing an earlier Undo. Only the visible token can claim its snapshot;
  Close or expiry releases that token's snapshot, while a fresh process or disposal of the owner
  releases the whole queue. The committed deletion remains applied until its own Undo succeeds.
  There is no user-visible trash or Undo for edits.
- Undo claims the token once before restoration starts. Late action/timeout callbacks cannot consume
  a claimed operation. Restore failure retains the snapshot for Retry; success or conflict releases
  it. Primary tabs can change during restoration, but new modal sessions wait until it finishes.
- SQLDelight emissions refresh list membership, counts and balances. Mutations preserve History
  selection and Search input/filters; normal list clamping may occur when rows disappear.

Presentation, feedback windows and lifecycle ownership live in
[UI architecture](ui/architecture.md#movimiento-management-and-feedback).

## Cuentas

A **cuenta** is where the money of a movimiento came from or went to. Balances are derived, never
stored (see [Balances and credit-card estimate](#balances-and-credit-card-estimate)).

- Fields: `id`, `nombre`, `tipo`, `moneda`, `saldoInicial`, optional `diaCorte`, `color`,
  `archivada`, `createdAt` and change stamps.
- `color` is a `CategoryTint` storage key, the same theme-token palette as categories, so it reads in
  light and dark. A new account starts with its type's color (`EFECTIVO` emerald, `BANCO` blue,
  `CREDITO` purple) until the user picks another; in-place creation from entry keeps that default.
  Changing it is a normal account update; upgrading existing data gave each account its type's color
  without a new stamp. Accounts rows and the account sheet show the account's icon
  in its color, and the entry selector and picker tint the type icon with it; movimiento rows do not
  show the account.
- Types: `EFECTIVO`, `BANCO` (bank or wallet: Nequi, Daviplata, Mercado Pago, debit cards) and
  `CREDITO` (credit card). An account has only a name: no card numbers or other sensitive data.
- `nombre` is trimmed, up to 30 characters and unique per currency among live accounts, archived
  included, ignoring case and supported accents. Creating a name that already exists offers the
  existing account. A tombstoned name can be reused and gets a new ID. Account names are not
  translated.
- `saldoInicial` is a signed exact `Money` in the account's currency. The form asks for an unsigned
  amount; for a credit card it is the opening debt, which the form state stores negated (the only
  place that conversion happens).
- `diaCorte` (1–31) is the statement day of a credit card and is accepted only on that type.
- Pickers, selectors and rows add the currency code to accounts outside the primary currency, so
  same-named accounts stay distinguishable.
- The default cash account has the deterministic ID `cuenta_efectivo_<moneda in lowercase>` and the
  name localized at creation ("Efectivo", "Cash", "Dinheiro"). Bootstrap creates one in the primary
  currency only while no live account exists (a fresh install); changing the primary currency never
  creates an account. If a tombstone holds that ID, the seed takes a generated ID instead.
- Locks, checked after the duplicate name: the currency is locked while any live movimiento or
  transfer references the account, and always for a default cash account, whose ID names its currency
  (`CuentaWrite.CurrencyLocked`); the type is locked while referenced, because stored medios were
  validated against it (`CuentaWrite.TypeLocked`). The account sheet shows locked type and currency as
  read-only before Save.
- Archiving removes the account from pickers and sentence detection; its balance still counts in
  totals and it stays under Archived in Accounts. Unarchiving is allowed. The last non-archived
  account cannot be archived or deleted.
- Deletion tombstones an account no live movimiento or transfer references; otherwise the sheet offers
  archiving, like custom categories. Being the last active account is reported before being in use.
- Upgrading existing data assigned every movimiento to the "Efectivo" account of its currency with
  medio Efectivo. A currency whose movimientos were all deleted got its cash account as a tombstone,
  so Undo of those rows is `MissingCuenta`.

### Accounts screen

Profile → Accounts, also opened from Home's balance card.

- A search by name, one total per currency with a live account, then the accounts grouped by type,
  each with its balance. Credit cards show the estimated debt (or a positive balance as credit in the
  user's favour) and this cycle's spending with the cycle's dates. Archived accounts sit in a
  collapsible Archived group. Without a search, the five most recent transfers follow.
- An account opens its sheet: edit, archive or unarchive, delete, "View movements" (Search with that
  account's currency and the account filter), "Move between accounts" and, for a credit card, "Pay
  card".

## Medio de pago

How a movimiento was paid or received (`MedioPago`):

| Medio | Compatible account types | Allowed for |
| --- | --- | --- |
| `EFECTIVO` | `EFECTIVO` | gasto, ingreso |
| `DEBITO` | `BANCO` | gasto |
| `TRANSFERENCIA` (paying or receiving from someone else) | `BANCO` | gasto, ingreso |
| `CREDITO` | `CREDITO` | gasto, ingreso (refund) |

- The repository rejects an incompatible medio/account/tipo combination, an archived newly chosen
  account and an account in another currency on edit; the UI never offers them.
- Changing a draft from gasto to ingreso while the medio is `DEBITO` changes it to `TRANSFERENCIA`
  on the same account.
- `TRANSFERENCIA` as a medio (money sent to or received from a third party) is distinct from a
  [transfer between own accounts](#transferencias); copy keeps them apart following
  [wording](wording.md#7-glosario-cuentas-medios-de-pago-y-transferencias).

### Selection in entry and editing

- Every creation and editing surface shows a Medio section followed by an Account section. Medio is a
  single choice among the medios the tipo allows. The account selector lists live, non-archived
  accounts compatible with the medio (in the editor, only in the stored currency); with one it shows
  it without a picker, with none it offers creating one in place: name, type preset by the medio,
  currency defaulting to the primary one and fixed to the stored one in the editor. An undecided medio
  blocks Save with a field error.
- Preselection without sentence evidence has one producer,
  `MovimientoRepository.defaultSelection(primary, tipo, medio?)`: one SQL read, no stored preference,
  returning a `CuentaSelection` (account, medio and the account's currency). The fallback order,
  restricted to accounts compatible with `medio` when given: with a medio only, the account of the
  newest live movimiento with that medio if it is live and active → the primary cash account → the
  first active cash-type account in the primary currency → the first active account in the primary
  currency → the first active account. Without a medio the newest movimiento is not used, so entry
  defaults to cash. The given medio is kept; otherwise the account gets
  `TipoCuenta.defaultMedio(tipo)`. The result is null only when no active account fits the given
  medio. The editor passes the stored currency as `primary` and drops a default in another currency.
- Ownership follows the tags model with three owners: `DEFAULT` follows the repository's selection,
  `SENTENCE` is replaced by every interpretation and `USER` survives both. Clearing the sentence or
  discarding returns to `DEFAULT`. Choosing a medio keeps a compatible account or takes that medio's
  default; choosing an account keeps a compatible medio or takes the type's default.
- If Save fails because the repository no longer takes the account, that becomes the account field's
  error; any other failure stays the shared save error.

## Transferencias

A **transferencia** moves money between two of the user's own accounts.

- Fields: `id`, `cuentaOrigenId`, `cuentaDestinoId` (different), `montoOrigen` (positive, origin
  currency), `montoDestino` (positive, destination currency; equal to `montoOrigen` and not asked when
  both accounts share the currency), `fecha` (civil date, not future), optional `descripcion` (up to
  180 characters), `createdAt` and change stamps. No category, tags or medio. Different currencies
  are allowed with a sent and a received amount; there is no exchange rate.
- A transfer is neither income nor expense: it never enters ingresos/gastos, category totals,
  suggestions or budgets. It changes the two account balances only.
- An archived account is rejected as a newly chosen side (insert, or an edit that changes that side);
  an edit keeps an unchanged archived side, and Undo restores into it.
- Transfers are created from Accounts ("Move between accounts" or "Pay card"), never from the
  sentence. Editing may change every field; currency rules apply per side. Deletion asks for
  confirmation and offers Undo through the same feedback queue as movimientos, with the outcomes in
  [Editing and deletion](#editing-and-deletion).
- The origin defaults to the newest live transfer's origin when it is active and not a credit card,
  else the primary cash account, else the first active non-credit account in the primary currency,
  then in any currency. With only credit cards there is no default.
- "Pay card" opens a transfer with the card as destination and the estimated debt as `montoDestino`.
  With a same-currency origin that is the only amount; with another currency `montoOrigen` starts
  empty. Paying a card is never an expense: its purchases were already recorded as gastos.
- Rows are neutral (no sign or income/expense color), show the route ("Nequi → NU") and the amount of
  the side being viewed.

## Balances and credit-card estimate

- `saldo(cuenta) = saldoInicial + Σ ingresos − Σ gastos + Σ montoDestino of incoming transfers −
  Σ montoOrigen of outgoing transfers`, over live rows of every date.
- The total balance of a currency is the Σ saldo of every live account in it, archived included.
- Credit card: `deuda = max(0, −saldo)`; a positive balance is shown as a credit in the user's favour.
- Cycle spending is the card's gastos minus ingresos (refunds) inside the current cycle. With
  `diaCorte = d` the cycle runs from the day after the previous statement date through the next one,
  today included when it is the statement date; a statement day past the month's end falls on its last
  day (31 → 30 April, 28/29 February). Without `diaCorte` the cycle is the calendar month. Debt and
  cycle spending are labelled as estimates, since the user may not record every purchase.
- All arithmetic is exact `Long` minor units in SQL `SUM()`; currencies are never added together.

## Moneda e idioma

Multi-moneda: COP, MXN, ARS, CLP, BRL, USD, DOP, PEN. Moneta hoy soporta COP, MXN, ARS, BRL, USD, PEN —
al portar, agregar CLP y DOP a la tabla de mapeo.

**Mecanismo** (portado de moneta, `src/lib/i18n/regionCurrency.ts` + `detectLocale.ts`): se lee el
locale del sistema una sola vez; idioma y región salen del mismo tag (`es-CO` → idioma `es`, región
`CO`); la región mapea a moneda por tabla fija (default `COP` si no matchea); el idioma determina los
strings de la UI. No hay selector de moneda al inicio — es inferido y se cambia después desde Perfil.
Cambiar la moneda principal no convierte montos ni crea cuentas: cada cuenta conserva su moneda, y
Home, la moneda inicial de Historial y Búsqueda y la preselección de un movimiento nuevo pasan a usar
la moneda elegida.

El idioma sigue al dispositivo salvo que se fije desde Perfil (`es`, `es-AR`, `en`, `pt-BR`). Los
nombres de las categorías predefinidas siguen el idioma activo; la moneda inferida no cambia con él.

## Home

- The balance card shows the total balance of the primary currency (see
  [Balances](#balances-and-credit-card-estimate)), zero while that currency has no account, with this
  month's ingresos and gastos in the primary currency as secondary values. Tapping it opens Accounts.
- Recent movimientos are the six newest whose account is in the primary currency, filtered before the
  limit and ordered by `fecha DESC, created_at DESC, id DESC`, so newer foreign-currency rows cannot
  hide eligible ones. Transfers are not listed on Home.
- "Your month in numbers" sits between the balance and the recent movimientos while this month has
  gasto in the primary currency: the three largest root categories of the month's
  [distribution](#statistics), every other one as a single rest share, and the month's pace when it
  applies. Tapping it opens Statistics with its retained selection. A failed read hides the card
  instead of becoming a Home error.
- While any [recurrente](#recurrentes) occurrence is pending, a compact row "N recurrentes por
  confirmar" sits under the balance card and opens Recurrentes. N counts occurrences, not
  recurrencias. It never changes the balance, the month's totals or the recent movimientos, and a
  failed read hides only the row.

## Recurrentes

Rent, bills, salary and subscriptions repeat: the user registers one once and confirms each due date
with the real amount. An unconfirmed occurrence is never money that moved, so it counts in no saldo,
Home total, History, Search, Statistics or CSV export until it becomes a movimiento.

- **Template.** A `recurrencia` holds the movimiento fields (tipo, monto, cuenta, medio, categoría,
  tags, título, descripción), `frecuencia` (`SEMANAL`, `MENSUAL`, `ANUAL`), `fechaAncla`,
  `proximaFecha` and `createdAt`, with change stamps and tombstone like every root entity (see
  [Change stamps and tombstones](#change-stamps-and-tombstones)); `recurrencia_etiqueta` is its child
  table. `movimiento.recurrencia_id` (nullable) links a confirmed movimiento to its recurrencia.
- **Creating.** The creation form and the sentence summary offer Repetir (Mensual by default,
  Semanal, Anual); a sentence that states a cadence turns it on (see
  [Natural-language entry](#natural-language-entry)). Saving with it on creates the movimiento as
  usual plus its recurrencia in one write: `fechaAncla` is the movimiento's fecha and
  `proximaFecha` the first occurrence strictly after both that fecha and today. The editor of an existing movimiento never offers it.
- **Occurrences.** Occurrence n is `fechaAncla + n` periods, always counted from the anchor so it
  never drifts; a monthly or annual day past the month's end falls on its last day, like `diaCorte`
  (31 → 30 April → 31 May; 29 February → 28 February in non-leap years). Pending means every
  occurrence from `proximaFecha` through today, read from the injected civil day, never the clock.
- **Recurrentes (Profile, or the Home row).** Pending occurrences come first, oldest due date first
  across recurrencias, headed by their count; then every live recurrencia by next date. Only a
  recurrencia's oldest pending date can be acted on; later ones wait for it. With no recurrencia the
  section shows an empty state.
- **Pagado** confirms the oldest pending occurrence: the movimiento is created with
  `recurrencia_id`, the amount prefilled with the template's and the fecha with the due date, both
  editable (fecha up to today). A different amount also becomes the template's amount. Confirming late
  does not move it into another month, because its fecha starts at the due date, not today. **Todavía no** closes and leaves
  it pending. **Saltar** skips that occurrence without money. Pagado and Saltar each advance
  `proximaFecha` by one occurrence in one transaction and one stamp, and offer Undo.
- **Undo** of Pagado or Saltar restores `proximaFecha` and the amount and tombstones the created
  movimiento, only while neither the recurrencia nor that movimiento changed since (compared by
  stamp); otherwise nothing is written and the conflict feedback says so.
- **Template problems.** Pagado only takes amount and fecha. An archived or deleted cuenta, or a
  hidden or deleted category, makes the confirm fail with a message that offers the recurrencia's
  sheet, where the template is fixed first.
- **Editing.** A recurrencia opens in the movimiento form, its date replaced by frecuencia and next
  date. Those two are locked, with the reason shown, while any occurrence is pending; saving a new
  frecuencia or next date re-anchors `fechaAncla` and `proximaFecha` on that date. Every other field
  can always change and applies to pending and future occurrences. Past movimientos never change.
- **Deleting** asks first and has no Undo. It tombstones the recurrencia; its movimientos keep
  `recurrencia_id`. Deleting or re-dating a confirmed movimiento does not bring its occurrence back.

## CSV export

The export from Profile reads every live movimiento, transfer, account, category and tag in one
snapshot and writes all currencies, newest first (`fecha`, `createdAt`, `id`). Sharing the file does not
modify data.

- Columns: fecha, tipo, categoría, categoría principal (a root category repeats its own name), tags
  joined by " · ", título, descripción, monto, moneda, then cuenta, medio, cuenta destino, monto
  destino and moneda destino. The account columns follow the original nine so spreadsheets built on
  earlier exports keep lining up.
- Amounts are unsigned plain decimals with the currency's scale: the tipo carries the direction.
- A transfer is one row of type "Transferencia entre cuentas": its origin fills the monto, moneda and
  cuenta columns, its destination the last three; categoría, tags, título and medio stay empty.

## Categorías

Lista predefinida portada de moneta — no se retipea acá para no tener una segunda copia que se
desincronice; fuente real: `web/moneta/src/lib/schema.ts` (`CATEGORIAS_SEMILLA`). Las categorías
personalizadas son alcance aceptado en una fase posterior (abajo).

- 12 categorías padre, 58 en total (cada padre con 3–6 hijas). Estructura por entrada: `{id, nombre,
  padreId?, icono, color}`.
- `icono` es un nombre de Lucide — compatible directo con `icons-lucide`, ya elegido.
- `color` es un tinte con nombre (`CategoryTint`: esmeralda, turquesa, azul, índigo, morado, rosa,
  rojo, naranja, ámbar, gris), no un hex — se resuelve contra los tonos de gráfico del tema
  (`docs/stack.md`), así cada tinte se distingue de los demás en claro y oscuro. La clave guardada no
  cambia si el tinte se renombra.
- Los ingresos son una categoría padre más (`cat_ingresos`, con `cat_sueldo`/`cat_freelance`/
  `cat_ventas`/`cat_regalo`/`cat_reembolso` como hijas) — no hay un campo `tipo` en la categoría, el
  tipo (gasto/ingreso) lo determina el movimiento, no la categoría.
- Nombres de categoría traducidos por idioma en moneta (`src/lib/seedConfig.ts`,
  `SEED_CATEGORY_NAMES`) — mismo criterio a portar, ver `docs/wording.md`.

### Otros and pending movimientos

- **Otros** (`cat_otros`) is a seeded, localized system root with a neutral tint. It only exists as
  the creation fallback: pickers and suggestions never offer it, while Search filters list it so
  pending movimientos can be found.
- A movimiento in Otros is pending. Rows and the editor show a subtle pending treatment (dashed
  avatar and a muted "to complete" label) without blocking any action. Choosing a real category in
  the editor resolves it.

### Hierarchy and filtering

- Parents and children are both selectable. A parent means the general case ("transport, not more
  specific"); a child is the specific case.
- Filtering by a parent includes the parent and all its children. Filtering by a child includes only
  that child. Roll-up never goes upward.
- Children keep their parent's tint, so the family stays recognizable; parents and children have
  the same visual weight in chips. The hierarchy is shown as grouped families on a single level in
  the picker and in the Search filters: each family is a block with the parent first and its
  children after, every chip responds to one tap and nothing drills in. Searching flattens the
  blocks into one list of matches.

### Category and tags

- A movimiento has exactly one category: what the money was for. Category totals therefore add up to
  the overall total, and a category breakdown can be read as parts of a whole.
- A movimiento has zero to six **tags**: user-created labels that group movimientos across
  categories ("Gastos cubiertos", "Viaje Chile"). Tags are flat, have a name and an optional description, and
  keep the user's order on the movimiento for display. A tag can share its name with a category; they are different concepts.
- Every creation and editing surface shows a "Category" section followed by a "Tags" section whose
  header carries a barely perceptible "optional" marker. A category shows its icon and tint; a tag is a neutral chip with a tag icon and no icon or
  tint of its own.
- The category picker keeps the current category visible: a pinned row between the search field and
  the results shows it, and the results open scrolled to it.
- The tags section shows the selected tags, each removable, and an "Add" tag that opens a
  multi-select tag picker with search; the Add tag disappears at six. The tag picker opens at the same
  tall height as the category picker, so its search field stays above the keyboard, and its Done
  action stays reachable while typing. Without any tag it shows an empty state that invites typing a
  name to create the first one; a search without results offers to create a tag with the typed name
  in one tap, and the created tag becomes selected while the limit allows.
- Choosing a real category resolves the Otros fallback. Movimiento rows show the category only, never
  tags.

### Tags

- A tag name is trimmed, up to 30 characters and unique among tags ignoring case and supported
  accents. A tag may have an optional description of up to 140 characters, edited from its sheet in
  the Tags screen; quick creation from the picker sets only the name. Creating a name that already exists offers the existing tag instead of a duplicate;
  renaming to an existing name is rejected with an inline error. Tag names are not translated.
- Profile links to a Tags screen modeled on Categories: search, a create action, and the tags sorted
  by name, each with its movimiento count in the primary currency. Tapping a tag opens its sheet with
  name and description, "View movements" (opens Search filtered by that tag) and delete.
- Tags are never hidden. Deleting a tag asks for confirmation showing how many movimientos use it,
  then unlinks it from those movimientos, bumping their change stamps, and tombstones it in the same
  transaction; the movimientos themselves stay and no Undo is offered.
- Undoing a movimiento deletion restores the tags that still exist; if its custom category was
  deleted meanwhile, the movimiento is restored in Otros.

### Custom categories

- The user creates root categories with a required name (trimmed, up to 30 characters, unique among
  all categories ignoring case and supported accents), an icon preselected by default and suggested
  from the name, an optional tint (suggested with the icon) and an optional description of up to 340
  characters. Custom names are not translated.
- Creating a name that already exists offers the existing category instead of a duplicate.
- From the movimiento picker, a search without results offers a non-intrusive action to create a
  category with the typed name; the quick form shows name and icon, with tint and description behind
  "More options", and the created category becomes the movimiento's category.
  Custom categories come first in pickers, sorted by name.
- Profile links to a dedicated Categories screen that shows chips, like the picker: search, the
  user's categories first, then the predefined ones grouped by parent, each with its movimiento count
  in the primary currency. Tapping a chip opens its sheet: the editor for a custom category, a
  read-only view for a predefined one. Both offer hide/show and "View movements", which opens Search
  filtered by that category; the custom editor also offers delete.
- Any category except Otros can be hidden. Hidden categories leave the movimiento pickers and
  suggestions but stay on existing movimientos, History and Search, and remain filterable.
- A custom category is deleted only while no movimiento in any currency uses it; when in use, the
  sheet offers hiding it instead, and no movimiento is rewritten.

## Tracking and History

History browses movimientos and transfers with three scopes: Day, Week and Month. A year selector supplies
calendar context; Year is not a fourth scope. Text search and non-date filters belong to
[Search](#search-and-filters), a separate destination; charts belong to [Statistics](#statistics), not
to History selection.

- Fresh History entry selects Day at today. Scope and one civil-date anchor own selection; context,
  selected chips, options and read range are derived.
- Week uses month-bound blocks 1–7, 8–14, 15–21, 22–28 and 29–month end. Blocks never cross months.
  Current-month Day and Week options stop at today; historical months are complete. Month chips
  represent full calendar months, while History's current-month read range ends at today.
- Scope changes preserve anchor. Choosing a different Week chip anchors at that block's start;
  choosing the selected chip preserves its interior anchor. A Day chip selects its exact date.
  Year/month changes and Month-chip selection preserve day-of-month where valid, clamp month-end
  and leap dates, and clamp future dates. A future month in the current year uses the current month.
- Allowed years include every year from the shared `FIRST_SELECTABLE_YEAR` (2019) through the current
  year, descending. Earlier years with a movimiento or a transfer side in the viewed currency or a
  selected/restored anchor remain available, so the selection floor never hides existing records.
  Selected/restored past years remain available for the retained entry even after their last row
  disappears and another year is selected. Future years are excluded after backward date refresh.
  Fresh entries derive years again; metadata changes never reset selection.
- Each date refresh captures the clock and timezone once. An anchor equal to the previous today
  follows the new today; historical anchors stay fixed unless they become future, then clamp to
  today. Resume refresh makes newly available years visible without a background midnight timer.
- History shows one currency, the primary one by default. While live accounts (archived included)
  exist in more than one currency, a selector offers those currencies; a selected currency left
  without accounts falls back to the primary one.
- Reads use inclusive civil dates in `fecha`, with no UTC-instant conversion. Movimientos in the
  viewed currency and transfers with a side in it are read in one transaction, so rows, totals and
  years share a snapshot. Group by `fecha`, preserving `fecha DESC, created_at DESC, id DESC`;
  `createdAt` is only the secondary order. Transfers appear in their day as neutral rows with the
  viewed side's amount.
- Period income, expense and balance describe every movimiento in the selected date/currency range,
  derived from the same bounded snapshot; transfers never enter them. Period balance is income minus
  expense. The count line reads "N movimientos · M transferencias entre cuentas" and omits the second
  part when M is 0. History presents only the selected period; Home owns the total balance.
- Saved selection contains only schema version, scope, ISO anchor and the selected currency. Unknown
  versions/scopes or malformed/future anchors recover to Day/today; valid inputs recompute derived
  state against current today/currency. Repository rows and query state are never saved. Android supported saved-owner
  recreation and iOS retained-controller/saveable recreation define restoration; fresh cold launches
  use normal defaults, with no iOS cross-process persistence bridge.

## Search and filters

Search is a separate destination over one currency, chosen with the same selector rules as
[History](#tracking-and-history). Its implementation contract lives in [Search](features/search.md);
this section owns agreed product rules, not delivery status.

- A fresh entry has the primary currency, an empty query, All history, all types and no selected
  medios, accounts, categories or tags. All history removes the date predicate; it never imposes a
  year floor or result limit.
- Text matches one contiguous substring in a movimiento's title, description, displayed category name
  or any tag name, and in a transfer's description or either account's name. Trim and
  lowercase with the existing supported-locale accent folding; preserve `ñ` versus `n`. Blank text
  matches everything. Internal spaces are literal; no token/fuzzy/amount/date/category-ID search.
  Unknown-category display labels are searchable.
- Nonblank text settles after 250 ms without another edit; clearing or whitespace-only text settles
  immediately. Text edits do not resubscribe to the database. Clear text keeps filters; Clear filters
  keeps raw text.
- Inclusive date presets: All; today minus six days through today; today minus 29 days through today;
  first of this month through today; January 1 through today; Custom. Relative presets resolve from
  one captured civil today/timezone on resume and Apply. Custom remains absolute.
- Custom starts with today/today. Preserve its last valid endpoints when choosing another preset.
  Interactive selection keeps From on or before To: From is capped at To/today, and To starts at
  From and is capped at today. Same-day ranges are valid. Reversed legacy inputs still normalize
  on Apply/restoration as a compatibility safeguard. Future input cannot apply.
  Restored malformed/future Custom input recovers to default filters. If the clock moves backward,
  keep an existing now-future Custom selection visible with an invalid-range error until corrected;
  never silently clamp it or run its invalid query.
- Date/type/medio/account/category/tag dimensions compose AND; selected IDs within a dimension
  compose OR. No IDs means no restriction in that dimension; selecting a parent category matches the
  parent and its children (see
  [Hierarchy and filtering](#hierarchy-and-filtering)). All real categories, including children, remain
  selectable regardless of type. The tags dimension appears only when the user has tags. Missing
  selected category IDs remain visible with a localized fallback and explicit remove action; a deleted
  tag leaves the applied filters.
- The type dimension is all, gasto, ingreso or "Entre cuentas" (`SearchTipo`; `TipoMovimiento` stays
  gasto/ingreso). Transfers match only while the type is all or Entre cuentas and no medio, category
  or tag is selected; an account filter matches either side of a transfer.
- The account dimension lists the viewed currency's accounts, archived included, plus selected
  accounts of another currency and a localized fallback for selected IDs no account holds. A missing
  selected account stays applied until removed.
- A filter request from another surface ("View movements" in a category, tag or account sheet, or a
  [Statistics](#statistics) drill-down) clears the text and replaces the filters with exactly the
  requested IDs, switching to the requested currency (an account's own one) when given. A Statistics
  request also sets type gasto and the period as a Custom range; the others keep All history and
  every type.
- Filter edits form a draft. Apply atomically commits and closes; Back, scrim or drag discards and
  closes. Clear atomically commits default filters and closes, resetting Custom endpoints to current
  today and preserving raw search text. Apply is available for
  valid unchanged filters and zero results; its label/accessibility name contains no quantity.
- Removing an applied chip commits just that dimension. Draft edits never update results, applied
  chips or count. Closing sheets accept no edits or repeated commits.
- Rows use `fecha DESC, created_at DESC, id DESC`, grouped under one full-date header per day that
  has results. Movimiento rows follow the shared card rule (title with category fallback, then
  description) and show signed exact amounts; transfer rows are neutral. A real row opens its
  management; Search has no global balance.
- Count derives from the exact settled result list. Pending/stale/failed work never reports an old
  count as current. No-data means no stored movimientos or transfers in this currency; no-results
  means currency data exists but criteria/text match none. Currency presence metadata and structural rows come from
  the same database read snapshot.
- Applied filters, raw text and the selected currency survive supported saved-owner recreation. Results, categories, loading,
  jobs, focus, keyboard and open/closing drafts are not saved. Leaving Search retains the entry and
  scroll.
  Raw text never enters preferences, analytics or logs. Restoration follows the existing platform
  scope; no iOS cross-process persistence bridge is added.

## Statistics

Statistics answers where the money went, whether a period is unusual and how the trend looks, over
one currency at a time. Presentation lives in
[UI architecture](ui/architecture.md#statistics-presentation).

- **Scopes.** Month (default) and Year; there is no Week, because History's week blocks have
  different lengths. A fresh entry selects the current month. A period in progress ends today. Month
  chips are the selected year's months up to today, with a year menu; Year chips are the selectable
  years under [History's rule](#tracking-and-history). Changing year keeps the month and the
  day-of-month where valid, clamped to month end and to today. On resume a selection in the period in
  progress follows today; a historical one stays fixed.
- **Retention.** Scope, anchor, selected currency and distribution dimension survive like History's
  selection; unknown or malformed values recover to the current month, the primary currency and
  Categories.
- **Comparison.** A period in progress is compared with the same stretch of the previous period:
  October 1–24 against September 1–24, and a year through October 24 against the previous year
  through October 24. A day the previous period lacks moves to its last day (31 → 30, February 29 →
  28). A historical period is compared whole against the whole previous one. Without gasto in the
  comparison no variation is shown. A variation is the size of the change relative to the previous
  amount, rounded half away from zero; above 999% it reads ">999%".
- **Currency.** One currency, with the same selector rule as History; no FX conversion.
- **What counts.** Live movimientos in the currency, each on its `fecha` whatever its medio (a credit
  purchase counts when it was made). Transfers are neither gasto nor ingreso. Otros counts like any
  other category. A movimiento whose category is hidden, deleted or missing still counts; a missing
  one groups under its own ID with History's unknown-category label, so category totals always add
  up to the period's gasto. All sums are exact minor units in SQL.
- **Summary.** The period's gasto with its variation and the compared amount, plus ingresos and
  balance (ingresos − gastos).
- **Distribution.** Gastos only, grouped by Categories (summed into their root through `padre_id`),
  Tags, Accounts or Medios. Rows list every group with gasto in the period, by amount descending then
  ID, each with its variation, amount and share.
  - Categories, Accounts and Medios are exclusive: shares are measured against their own sum and add
    up to exactly 100%, the largest absorbing the rounding remainder. The donut shows at most six
    slices: with more than six groups, the five largest plus "Otras". The list keeps every group;
    the ones folded into Otras take its color.
  - Tags overlap: a movimiento counts whole under each of its tags, so each share is measured against
    the period's gasto, there is no donut, and a note says the shares do not add up to 100%.
  - A share that rounds below 1% reads "<1%".
  - Tapping a category row opens its detail; a tag or account row opens Search on that group's gastos
    in the period (a [filter request](#search-and-filters)); a medio row does nothing.
- **Evolution.** Monthly gastos and ingresos with the balance as a line. Month shows the six months
  ending in the selected one; Year shows the selected year's months through the period's end. A
  month in progress is marked partial with its last counted day. The last month starts selected, and
  selecting a month shows its values.
- **Pace.** Only for the current month with gasto. Daily cumulative gasto is compared against the
  average of up to the three latest whole months with gasto among the previous five. Projection =
  gasto to date ÷ elapsed days × days in the month; its variation against that average is the pace.
  Without any previous whole month with gasto there is no pace.
- **What changed.** Up to three deterministic findings, in this order: the category that rose the
  most and the one that fell the most, by amount, each only when its variation is at least 10% and
  the difference reaches 5% of the period's gasto (a category that fell to zero qualifies); and the
  day with the most gasto when at least two days had gasto. Without findings the section is hidden.
- **Without gasto in the period.** The distribution shows a brief empty message; pace and "What
  changed" are hidden; the summary and evolution stay.
- **Category detail.** A sheet for a root category: total, its share of the period's gasto,
  movimiento count and variation; the evolution months with the average of the complete ones (a month
  in progress is excluded); subcategories with the root's own movimientos as "General", when there is
  more than one group; and the three largest gastos (monto, then `fecha`, descending). "View N
  movements" appears while N > 0 and, once the sheet has closed, opens Search filtered by the root
  (children included), the currency, type gasto and the period. N equals Search's count for that
  filter.
- **States.** A refresh keeps the previous content (`RetainedLoadState`). A failed read shows an
  error card with retry, keeping any previous content marked as previous data. With no movimiento at
  all in the currency, a single empty state offers to add one through the same creation flow as the
  centered add action.

## Change stamps and tombstones

Every persisted root entity is ready for the future Drive backup and sync engine without another data
migration.

- `movimiento`, `categoria`, `etiqueta`, `cuenta`, `transferencia` and `recurrencia` have
  `updated_at` (non-null) and `deleted_at` (null while live). Both hold a `ChangeStamp`: a hybrid
  logical clock value encoded as sortable text,
  `<13-digit epoch millis>-<4 hex counter>-<16 hex node>`, whose text order equals stamp order. A
  stamp is never read back as a wall-clock time for business logic.
- One `ChangeClock` per process issues stamps: `next = max(wallClock, last.millis)`, with the counter
  incremented on ties or when the wall clock moves backward. Bootstrap prepares it from the maximum
  stored stamp of every stamped table before the first mutation, so stamps never go backward across
  restarts; preparing again only ever advances it (to be repeated after a dataset replacement).
- The node is a random per-installation ID in a device-scoped DataStore file, separate from the
  financial preferences, excluded from Android auto backup/device transfer and from iOS backups. It
  appears in data only inside stamps.
- An aggregate is a root row plus the child rows it owns (`movimiento_etiqueta` belongs to its
  movimiento, `recurrencia_etiqueta` to its recurrencia). A change to a child bumps the root's
  `updated_at`; child rows carry no stamps.
- Every successful insert, update, hide/show, rename, delete and restore issues one stamp per
  transaction, shared by every row it writes. A normalized no-op writes nothing. Renaming seeded
  categories for the active language is presentation and issues no stamp. Seeded rows (predefined
  categories, default cash accounts) use the constant `ChangeStamp.SEED`
  (`0000000000000-0000-0000000000000000`) so every installation agrees on their first version.
- Stamps stay in the persistence layer; domain models do not carry them, so content equality (Undo)
  ignores them.
- Deleting sets `deleted_at = updated_at = next stamp`; the row keeps its fields and owned child rows
  are removed. Every read, count, aggregate, uniqueness check, export and suggestion source excludes
  tombstones. Tombstones are never purged; compaction belongs to the sync engine.
- Inserts are strict: an ID that exists, live or tombstoned, fails instead of reviving the row. Only
  Undo revives, on the tombstone it captured (see [Editing and deletion](#editing-and-deletion)).

## Tema

Sistema (default), claro u oscuro, elegido en Perfil y aplicado también a las barras del sistema.
De dónde salen los colores/tokens concretos: ver `docs/stack.md`.

## Alcance del MVP

The MVP includes a local-only guest mode and an optional Google-connected mode. Cloud backup requires
Google authentication and explicit Drive permission; guest data never uploads before association.
The connected acceptance flow is sign in, grant Drive, create a movimiento, and verify local plus
Drive persistence. The complete HLC/outbox/conflict-merge engine is a separate feature.

### Access, association and backup

- One primary device publishes the account's accepted backup. Retained Google-owned data can be
  created, edited, deleted and restored with Undo locally without connectivity, regardless of the
  cached primary role. Upload requires connectivity, authentication as the owner, Drive permission
  and a fresh primary-device check. A secondary must explicitly become primary before publishing.
  Taking that role preserves pending local data; it does not restore a cloud copy over it.
- Guest data and retained Google-owned data are isolated. The MVP exposes no multiple-account/profile
  switcher. Its storage ownership must nevertheless support these two access modes without mixing data.
- A Google-account setting controls local access after explicit sign-out. When enabled, the same
  retained account dataset remains locally readable and editable without login. Backup requires
  signing in again to the same account and checking current cloud ownership. When disabled, Google-owned data
  stays inaccessible until authentication; entering guest mode opens separate local data.
- Association or recovery previews available copies and requires confirmation of the complete dataset
  to keep. Apply that copy by replacement, not additive merging. Preserve a recovery copy before
  destructive replacement; new data from another stale installation must not silently overwrite the
  choice. Cancelling leaves existing data and ownership unchanged.
- Guest and accessible Google-owned datasets persist mutations without connectivity or cloud checks.
  Eligible connected sessions
  retry pending backup when connectivity returns while the app is foregrounded or on reopen;
  signed-out local access and guest mode never upload automatically. No closed-app upload is promised.
  Cloud failure does not reverse a successful local mutation or report a false backup success.
- Before publication, compare pending local data and the current accepted Drive copy against the
  last acknowledged baseline. If both changed, require confirmation of which complete copy to keep;
  replace the other only after protecting it. No automatic field/record merging or newest-wins choice.
  A new primary checks this divergence before uploading. Changed candidates invalidate a stale choice.
- Drive copies use hidden app-specific storage. Retain two normal verified backups and one protected
  recovery copy before a restoration that replaces nonempty data. Permission and local mode govern
  whether the protective copy belongs in Drive or remains only on the device.
- Guest/local-only erasure can run offline after confirmation. Erasing Google-associated local and
  Drive data requires authentication as its owner, Drive authorization, connectivity and confirmation.
  Partial deletion stays visibly incomplete and cannot be followed by automatic re-upload. The exact
  deletion boundary includes only Tallybook data; legacy Moneta data and the Google account itself
  are excluded.

Implementation contracts, unresolved device/deletion gates and delivery evidence live in the active
[Google + Drive plan](tasks/google-drive-persistence.md). These rules describe accepted scope, not
already implemented behavior.
