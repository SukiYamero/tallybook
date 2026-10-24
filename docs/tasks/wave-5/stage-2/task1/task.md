# Wave 5 / Stage 2 / Task 1 — Accounts domain, wording and resource split

## Objective

Create the framework-free domain contracts for accounts, payment methods and transfers, with their
validation and the credit-card cycle math, so persistence and UI consume one tested definition. Add the
copy glossary that keeps "Transferencia" (payment method) apart from a transfer between own accounts,
prove that a second string resource file works on CMP 1.11.1 before Stages 3–5 rely on it, and ship
the shared type/medio icons and labels that Stages 3 and 4 both consume.

## Context and skills

- Read `AGENTS.md`, [wave README](../../README.md) (Decisions, Domain rules), `docs/wording.md`.
- Skills: `android-dev`, `kotlin-api-design`, `kotlin-control-flow`, `android-testing`,
  `test-driven-development`; for the icon/label composables `compose` and
  `compose-component-design`.
- **Runs in parallel with Stage 1.** Do not touch `.sq`/`.sqm`, repositories, `Movimiento.kt` or any
  existing model; `Movimiento` gains its fields in task2. `ChangeStamp` is not needed here.
- Reuse `Money` (exact arithmetic, `parseOrNull`), `Moneda`, `TipoMovimiento`, `MovimientoDateRange`,
  `normalizeSearchText` and the 30-character name convention of `CategoriaRules`.
- Pure value → value mappings (medio → compatible types/tipos) are typed lookup tables, not `when`
  chains spread across call sites.

## Files

- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/Cuenta.kt` (`Cuenta`, `TipoCuenta`)
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/MedioPago.kt` (enum + compatibility table)
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/CuentaRules.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/Transferencia.kt` (+ `TransferenciaRules`)
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/CreditCycle.kt`
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/model/CuentaSaldo.kt` (`CuentaSaldo`, `CreditEstimate`)
- Create: `shared/src/commonMain/composeResources/values/strings_accounts.xml` and the same file in
  `values-en`, `values-es-rAR`, `values-pt-rBR` (type and medio labels only)
- Create: `shared/src/commonMain/kotlin/com/kurobello/tallybook/core/ui/account/{CuentaTypeIcon,CuentaTypeLabels,MedioPagoIcon,MedioPagoLabels}.kt`
  — Lucide icons from `icons-lucide-cmp` (verify the exact icon names in the resolved artifact) and
  localized labels; each icon composable takes `modifier` and an accessibility label from the labels file
- Modify: `docs/wording.md` — new glossary section (see Exact rules)
- Test: `shared/src/commonTest/kotlin/com/kurobello/tallybook/core/model/{MedioPagoTest,CuentaRulesTest,TransferenciaRulesTest,CreditCycleTest}.kt`
- Test: `shared/src/androidHostTest/kotlin/com/kurobello/tallybook/core/ui/resources/AccountsResourcesHostTest.kt`
  (pattern of `ProfileResourcesHostTest`)

## Contracts

```kotlin
enum class TipoCuenta {
  EFECTIVO, BANCO, CREDITO;
  fun defaultMedio(tipo: TipoMovimiento): MedioPago // EFECTIVO/DEBITO/CREDITO, then MedioPago.forTipo(tipo)
}

enum class MedioPago {
  EFECTIVO, DEBITO, TRANSFERENCIA, CREDITO;
  val compatibleTipos: Set<TipoCuenta>          // from one lookup table
  fun allows(tipo: TipoMovimiento): Boolean     // DEBITO is gasto-only
}

data class Cuenta(
    val id: String, val nombre: String, val tipo: TipoCuenta, val saldoInicial: Money,
    val diaCorte: Int?, val archivada: Boolean, val createdAt: Instant,
) {
  val moneda: Moneda get() = saldoInicial.moneda
  companion object { fun defaultCashId(moneda: Moneda): String } // "cuenta_efectivo_<moneda lowercase>"
}

/** Persisted domain type; validation reads field values instead (see TransferenciaRules). */
data class Transferencia(
    val id: String, val cuentaOrigenId: String, val cuentaDestinoId: String,
    val montoOrigen: Money, val montoDestino: Money, val fecha: LocalDate,
    val descripcion: String?, val createdAt: Instant,
)

object CuentaRules { const val NOMBRE_MAX = 30; fun nombreKey(nombre: String, moneda: Moneda): String; fun validate(...) }
/** Every failing field at once, like CuentaRules: the Stage 3 form shows all errors together. */
data class TransferenciaErrors(
    val cuentaOrigen: TransferenciaCuentaError?,   // MISSING
    val cuentaDestino: TransferenciaCuentaError?,  // MISSING, SAME_AS_ORIGEN
    val montoOrigen: TransferenciaMontoError?,     // MISSING, WRONG_MONEDA, NOT_POSITIVE
    val montoDestino: TransferenciaMontoError?,    // + DIFFERS_FROM_ORIGEN, only when montoOrigen is valid
    val fechaFuture: Boolean, val descripcionTooLong: Boolean,
) { val isValid: Boolean }
/** Field values, so a form can validate an empty amount (Pay card) without fake ids. */
object TransferenciaRules {
  fun validate(origen: Cuenta?, destino: Cuenta?, montoOrigen: Money?, montoDestino: Money?,
               fecha: LocalDate, descripcion: String?, today: LocalDate): TransferenciaErrors
}

/** Inclusive civil range of the cycle containing [today]; calendar month when [diaCorte] is null. */
fun creditCycle(today: LocalDate, diaCorte: Int?): MovimientoDateRange

data class CuentaSaldo(val cuenta: Cuenta, val saldo: Money)
data class CreditEstimate(val deuda: Money, val cycleSpending: Money, val cycle: MovimientoDateRange)
```

Exact field names may adapt to existing conventions; the semantics may not. `MedioPago` also exposes
the gasto → ingreso switch (`DEBITO` → `TRANSFERENCIA`) as one function. `TipoCuenta.defaultMedio(tipo)` is the single
owner of the medio implied by an account type (BANCO is `DEBITO` for gasto, `TRANSFERENCIA` for
ingreso): Stage 2/task2 `defaultSelection` must pass the movimiento tipo, and it and the Stage 4
parser consume this function instead of re-deriving it.

## Exact rules

- Compatibility, allowed tipos and the gasto/ingreso switch: README
  [MedioPago](../../README.md#mediopago-payment-method).
- Account fields, name uniqueness key per (nombre, moneda), `diaCorte` only for `CREDITO`, signed
  `saldoInicial` (no sign conversion in the domain): README [Cuenta](../../README.md#cuenta-account). Validation returns a typed error per field
  (name blank/too long, `diaCorte` out of range or on a non-credit account).
- Transfer validation: different accounts, both positive amounts in their account's currency,
  `montoDestino == montoOrigen` when currencies match, not future, description ≤ 180: README
  [Transferencia](../../README.md#transferencia-transfer-between-own-accounts).
- Cycle and `deuda`: README [Balances](../../README.md#balances-and-credit-card-estimate) — the
  statement day clamps to the month's last day; the cycle ends on the next statement date (today
  included when today is the statement date) and starts the day after the previous one.
- Wording glossary in `docs/wording.md`, per locale: medio names, "Transferencia" (payment to or from
  a third party) versus the internal-transfer action and noun (e.g. "Mover entre cuentas" /
  "Transferencia entre tus cuentas"), "Deuda estimada", "Consumo del ciclo", account type names. Keep
  the file's voice rules; add, do not rewrite.

## TDD

- [x] RED: table-driven `MedioPagoTest` over every medio × tipo cuenta × tipo movimiento.
- [x] GREEN: lookup table and `allows`/switch.
- [x] RED/GREEN: `CuentaRulesTest` (trim, 30 chars, accent/case key equal to category behaviour,
      `diaCorte` rules) and `TransferenciaRulesTest` (same account, zero, mismatch, future, cross-currency).
- [x] RED/GREEN: `CreditCycleTest`: d = 31 in April, 29/30/31 in February leap and non-leap, today on
      the statement day, today the day after, January ↔ December year change, null `diaCorte`.
- [x] Add the four `strings_accounts.xml`; `AccountsResourcesHostTest` reads one key per locale
      through the generated `Res.string` accessor. If generation fails, stop and report: Stages 3–5
      must then sequence `strings.xml` edits instead.
- [x] Add the shared icon/label composables; a host resources test reads every type and medio label.
- [x] Add the wording glossary.
- [x] `./gradlew :shared:testAndroidHostTest --tests '*core.model*' --tests '*AccountsResources*'`,
      `./gradlew :shared:compileKotlinIosSimulatorArm64`, `./gradlew detekt ktfmtCheck`.

## Evidence

- Resolved by Gradle: `components-resources` 1.11.1, `icons-lucide-cmp` 2.2.1.
- The generated `Res.string.account_type_bank` reads `values{,-en,-es-rAR,-pt-rBR}/strings_accounts.commonMain.cvr`;
  `AccountsResourcesHostTest` resolves every label per locale, so Stages 3–5 can add their own
  `strings_<area>.xml`.
- Lucide accessors verified in the artifact: `Banknote`, `Landmark`, `CreditCard` (account types);
  `Banknote`, `WalletCards`, `Send`, `CreditCard` (medios). Two-way arrows stay free for transfers
  between own accounts.

## Blocks

Not blocked. Blocks Stage 2 task2–task4 and Stage 3/4 UI copy.
