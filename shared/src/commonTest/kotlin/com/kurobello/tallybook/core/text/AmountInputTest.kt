package com.kurobello.tallybook.core.text

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AmountInputTest {
  @Test
  fun amountPrefillRoundTripsExactCurrencyAndLongBoundaries() {
    listOf(
            Money(1234, Moneda.COP),
            Money(1234, Moneda.USD),
            Money(1234, Moneda.CLP),
            Money(Long.MAX_VALUE, Moneda.COP),
            Money(1, Moneda.USD),
        )
        .forEach { amount ->
          assertEquals(
              AmountInputResult.Valid(amount),
              parseAmountInput(amount.toAmountInput(), amount.moneda),
          )
          assertFalse(amount.toAmountInput().contains(amount.moneda.symbol))
        }
  }

  @Test
  fun parsesPlainAmountsForEachSupportedScaleAndSeparator() {
    val cases =
        listOf(
            AmountCase("12", Moneda.CLP, AmountInputResult.Valid(Money(12, Moneda.CLP))),
            AmountCase("12.34", Moneda.USD, AmountInputResult.Valid(Money(1234, Moneda.USD))),
            AmountCase("12,34", Moneda.COP, AmountInputResult.Valid(Money(1234, Moneda.COP))),
            AmountCase("000,01", Moneda.BRL, AmountInputResult.Valid(Money(1, Moneda.BRL))),
        )

    cases.forEach { case ->
      assertEquals(case.expected, parseAmountInput(case.raw, case.moneda), case.raw)
    }
  }

  @Test
  fun classifiesEmptyMalformedAndNonPositiveAmounts() {
    val cases =
        listOf(
            AmountCase("", Moneda.USD, invalid(AmountInputError.EMPTY)),
            AmountCase("   ", Moneda.USD, invalid(AmountInputError.EMPTY)),
            AmountCase("1,234", Moneda.USD, invalid(AmountInputError.MALFORMED)),
            AmountCase("1.2.3", Moneda.USD, invalid(AmountInputError.MALFORMED)),
            AmountCase("1.", Moneda.USD, invalid(AmountInputError.MALFORMED)),
            AmountCase("+1", Moneda.USD, invalid(AmountInputError.MALFORMED)),
            AmountCase("1e2", Moneda.USD, invalid(AmountInputError.MALFORMED)),
            AmountCase("NaN", Moneda.USD, invalid(AmountInputError.MALFORMED)),
            AmountCase("Infinity", Moneda.USD, invalid(AmountInputError.MALFORMED)),
            AmountCase("1 2", Moneda.USD, invalid(AmountInputError.MALFORMED)),
            AmountCase("0", Moneda.USD, invalid(AmountInputError.NOT_POSITIVE)),
            AmountCase("-1", Moneda.USD, invalid(AmountInputError.NOT_POSITIVE)),
        )

    cases.forEach { case ->
      assertEquals(case.expected, parseAmountInput(case.raw, case.moneda), case.raw)
    }
  }

  @Test
  fun rejectsPrecisionBeyondTheMonedaScaleAndMinorUnitOverflow() {
    val cases =
        listOf(
            AmountCase("1.234", Moneda.USD, invalid(AmountInputError.TOO_MANY_DECIMALS)),
            AmountCase("1,0", Moneda.CLP, invalid(AmountInputError.TOO_MANY_DECIMALS)),
            AmountCase(
                "92233720368547758.08",
                Moneda.USD,
                invalid(AmountInputError.OVERFLOW),
            ),
        )

    cases.forEach { case ->
      assertEquals(case.expected, parseAmountInput(case.raw, case.moneda), case.raw)
    }
  }

  private data class AmountCase(
      val raw: String,
      val moneda: Moneda,
      val expected: AmountInputResult,
  )

  private fun invalid(reason: AmountInputError): AmountInputResult =
      AmountInputResult.Invalid(reason)
}
