package com.kurobello.tallybook.core.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.yearMonth

/** Declaration order is severity: [OVER] is the worst. */
enum class BudgetState {
  OVER,
  NEAR,
  PACE,
  FINE,
}

data class BudgetStatus(
    val presupuesto: Presupuesto,
    val categoriaNombre: String,
    val padreId: String?,
    val spent: Money,
    val state: BudgetState,
    /** Monto minus [spent]; negative when [BudgetState.OVER]. */
    val remaining: Money,
    /** Null unless [remaining] is positive. */
    val perDay: Money?,
    /** Non-null only for [BudgetState.PACE]. */
    val paceDay: Int?,
)

/** [padreId] is filled by the caller, who holds the category catalog. */
data class BudgetTouch(
    val categoriaId: String,
    val padreId: String?,
    val moneda: Moneda,
    val fecha: LocalDate,
)

/**
 * [amount] is the over amount for [BudgetState.OVER] and what remains for [BudgetState.NEAR] and
 * [BudgetState.PACE].
 */
data class BudgetNotice(
    val categoriaNombre: String,
    val state: BudgetState,
    val amount: Money,
    val paceDay: Int?,
)

private const val NEAR_NUMERATOR = 4L
private const val NEAR_DENOMINATOR = 5L
private const val MIN_PACE_DAY = 8
private const val SUGGESTION_MONTHS = 3
private const val SUGGESTION_WINDOW_MONTHS = 5
private const val TWO_DIGITS_LIMIT = 100L
private const val DECIMAL_BASE = 10L

internal fun budgetStatus(
    presupuesto: Presupuesto,
    categoriaNombre: String,
    padreId: String?,
    spent: Money,
    today: LocalDate,
): BudgetStatus {
  val monto = presupuesto.monto
  require(spent.moneda == monto.moneda) { "Spent and monto must share a moneda" }
  val remaining = monto - spent
  val daysInMonth = today.yearMonth.numberOfDays
  val elapsed = today.day
  val state =
      when {
        spent > monto -> BudgetState.OVER
        compareProducts(spent.minorUnits, NEAR_DENOMINATOR, monto.minorUnits, NEAR_NUMERATOR) >=
            0 -> BudgetState.NEAR
        elapsed >= MIN_PACE_DAY &&
            spent.minorUnits > 0 &&
            compareProducts(
                spent.minorUnits,
                daysInMonth.toLong(),
                monto.minorUnits,
                elapsed.toLong(),
            ) > 0 -> BudgetState.PACE
        else -> BudgetState.FINE
      }
  val daysLeft = daysInMonth - elapsed + 1
  return BudgetStatus(
      presupuesto = presupuesto,
      categoriaNombre = categoriaNombre,
      padreId = padreId,
      spent = spent,
      state = state,
      remaining = remaining,
      perDay =
          if (remaining.minorUnits > 0) Money(remaining.minorUnits / daysLeft, monto.moneda)
          else null,
      paceDay =
          if (state == BudgetState.PACE) {
            paceDay(monto.minorUnits, elapsed, spent.minorUnits, daysInMonth)
          } else {
            null
          },
  )
}

internal val budgetOrder: Comparator<BudgetStatus> =
    compareBy<BudgetStatus> { it.state }
        .thenComparator { a, b -> shareDescending(a, b) }
        .thenBy { it.categoriaNombre }

/** Only a gasto touches a budget; [categorias] supplies its category's padre. */
internal fun budgetTouch(
    tipo: TipoMovimiento,
    categoriaId: String,
    moneda: Moneda,
    fecha: LocalDate,
    categorias: List<Categoria>,
): BudgetTouch? =
    if (tipo != TipoMovimiento.GASTO) null
    else
        BudgetTouch(
            categoriaId = categoriaId,
            padreId = categorias.firstOrNull { it.id == categoriaId }?.padreId,
            moneda = moneda,
            fecha = fecha,
        )

/**
 * The most severe budget a save touched this month, with the subcategory winning a tie over its
 * parent. Null when every touched budget is [BudgetState.FINE].
 */
internal fun budgetNotice(
    statuses: List<BudgetStatus>,
    touches: List<BudgetTouch>,
    today: LocalDate,
): BudgetNotice? {
  val month = today.yearMonth
  val touched = statuses.filter { status ->
    status.state != BudgetState.FINE &&
        touches.any { touch ->
          status.presupuesto.monto.moneda == touch.moneda &&
              touch.fecha.yearMonth == month &&
              (status.presupuesto.categoriaId == touch.categoriaId ||
                  status.presupuesto.categoriaId == touch.padreId)
        }
  }
  val worst =
      touched
          .sortedWith(
              compareBy<BudgetStatus> { it.state }
                  .thenBy { it.padreId == null }
                  .thenComparator { a, b -> shareDescending(a, b) }
          )
          .firstOrNull() ?: return null
  return BudgetNotice(
      categoriaNombre = worst.categoriaNombre,
      state = worst.state,
      amount = if (worst.state == BudgetState.OVER) -worst.remaining else worst.remaining,
      paceDay = worst.paceDay,
  )
}

/**
 * Average monthly gasto over up to the three latest whole months, among the previous five, that had
 * any gasto in [moneda]. A month in that set where the category had none counts as zero.
 */
internal fun suggestedBudget(
    categoryGasto: Map<YearMonth, Money>,
    monthsWithGasto: Set<YearMonth>,
    moneda: Moneda,
    today: LocalDate,
): Money? {
  val current = today.yearMonth
  val oldest = today.minus(SUGGESTION_WINDOW_MONTHS, DateTimeUnit.MONTH).yearMonth
  val months =
      monthsWithGasto.filter { it < current && it >= oldest }.sorted().takeLast(SUGGESTION_MONTHS)
  if (months.isEmpty()) return null
  val total = months.sumOf { categoryGasto[it]?.minorUnits ?: 0L }
  val average = total / months.size
  return if (average > 0) Money(roundUpTwoSignificant(average), moneda) else null
}

internal fun roundUpTwoSignificant(minorUnits: Long): Long {
  var step = 1L
  var rest = minorUnits
  while (rest >= TWO_DIGITS_LIMIT) {
    rest /= DECIMAL_BASE
    step *= DECIMAL_BASE
  }
  return if (minorUnits <= 0) minorUnits else (minorUnits + step - 1) / step * step
}

private fun shareDescending(a: BudgetStatus, b: BudgetStatus): Int =
    compareProducts(
        b.spent.minorUnits,
        a.presupuesto.monto.minorUnits,
        a.spent.minorUnits,
        b.presupuesto.monto.minorUnits,
    )

/** `monto * elapsed / spent + 1`, the first day whose projection passes monto. */
private fun paceDay(monto: Long, elapsed: Int, spent: Long, daysInMonth: Int): Int {
  val day =
      if (fitsInLong(monto, elapsed.toLong())) monto * elapsed / spent
      else (monto.toDouble() * elapsed / spent).toLong()
  return (day + 1).coerceAtMost(daysInMonth.toLong()).toInt()
}

// Both products overflow Long only for amounts far above Money.MAX_MINOR_UNITS, where a Double
// comparison cannot be wrong by more than rounding noise.
private fun compareProducts(a: Long, b: Long, c: Long, d: Long): Int =
    if (fitsInLong(a, b) && fitsInLong(c, d)) (a * b).compareTo(c * d)
    else (a.toDouble() * b).compareTo(c.toDouble() * d)

private fun fitsInLong(a: Long, b: Long): Boolean = a <= 0L || b <= Long.MAX_VALUE / a
