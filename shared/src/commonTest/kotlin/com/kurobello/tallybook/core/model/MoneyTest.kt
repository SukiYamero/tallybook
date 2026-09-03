package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun usd(raw: String) = Money.parse(raw, Moneda.USD)

class MoneyTest {

  @Test
  fun addsMontosOfTheSameMoneda() {
    assertEquals(usd("15.75"), usd("10.50") + usd("5.25"))
  }

  @Test
  fun addingDifferentMonedasFails() {
    val cop = Money.parse("10", Moneda.COP)
    assertFailsWith<IllegalArgumentException> { usd("10") + cop }
  }

  @Test
  fun subtractsMontosOfTheSameMoneda() {
    assertEquals(usd("5.25"), usd("10.50") - usd("5.25"))
  }

  @Test
  fun subtractingDifferentMonedasFails() {
    val cop = Money.parse("10", Moneda.COP)
    assertFailsWith<IllegalArgumentException> { usd("10") - cop }
  }

  @Test
  fun negatingFlipsTheSignAndKeepsTheMoneda() {
    val negated = -usd("10.50")
    assertEquals(-1050L, negated.minorUnits)
    assertEquals(Moneda.USD, negated.moneda)
  }

  @Test
  fun comparesMontosOfTheSameMoneda() {
    assertTrue(usd("10.50") > usd("5.25"))
    assertEquals(0, usd("10.50").compareTo(usd("10.5")))
  }

  @Test
  fun comparingDifferentMonedasFails() {
    val cop = Money.parse("10", Moneda.COP)
    assertFailsWith<IllegalArgumentException> { usd("10") > cop }
  }

  @Test
  fun equalityIgnoresNonSignificantZeros() {
    assertEquals(usd("10.5"), usd("10.50"))
    assertEquals(usd("10.5").hashCode(), usd("10.50").hashCode())
  }

  @Test
  fun parseNormalizesToMinorUnits() {
    assertEquals(1050L, usd("10.50").minorUnits)
    assertEquals(1050L, usd("10.5").minorUnits)
    assertEquals(1000L, usd("10").minorUnits)
    assertEquals(-1050L, usd("-10.50").minorUnits)
    assertEquals(5L, usd("0.05").minorUnits)
    assertEquals(1234567L, Money.parse("1234567", Moneda.CLP).minorUnits)
  }

  @Test
  fun parseRejectsMoreDecimalsThanTheMonedaScale() {
    assertNull(Money.parseOrNull("0.994", Moneda.USD))
    assertNull(Money.parseOrNull("5.5", Moneda.CLP))
    assertFailsWith<IllegalArgumentException> { usd("0.994") }
  }

  @Test
  fun parseRejectsTextThatIsNotAPlainDecimal() {
    val invalid = listOf("", "-", "abc", "1.2.3", "1,50", " 10", "10.", "+10", "1e3")
    invalid.forEach { assertNull(Money.parseOrNull(it, Moneda.USD), "should reject '$it'") }
  }

  @Test
  fun formatsEachMonedaWithItsSymbolAndSeparators() {
    val expected =
        mapOf(
            Moneda.COP to "$\u00A01.234.567,89",
            Moneda.MXN to "$1,234,567.89",
            Moneda.ARS to "$\u00A01.234.567,89",
            Moneda.BRL to "R$\u00A01.234.567,89",
            Moneda.USD to "$1,234,567.89",
            Moneda.DOP to "$1,234,567.89",
            Moneda.PEN to "S/\u00A01,234,567.89",
        )
    expected.forEach { (moneda, text) ->
      assertEquals(text, Money.parse("1234567.89", moneda).format())
    }
  }

  @Test
  fun formatsScaleZeroMonedasWithoutDecimals() {
    assertEquals("$1.234.568", Money.parse("1234568", Moneda.CLP).format())
    assertEquals("$6", Money.parse("6", Moneda.CLP).format())
  }

  @Test
  fun negativeSignGoesBetweenTheSymbolAndTheDigits() {
    assertEquals("$\u00A0-1.234.567,89", Money.parse("-1234567.89", Moneda.COP).format())
    assertEquals("$-1,234,567.89", usd("-1234567.89").format())
  }

  @Test
  fun padsDecimalsUpToTheMonedaScale() {
    assertEquals("$0.00", usd("0").format())
    assertEquals("$5.50", usd("5.5").format())
    assertEquals("$0.05", usd("0.05").format())
  }

  @Test
  fun addingBeyondLongRangeFails() {
    val max = Money(Long.MAX_VALUE, Moneda.USD)
    assertFailsWith<ArithmeticException> { max + Money(1L, Moneda.USD) }
  }

  @Test
  fun subtractingBeyondLongRangeFails() {
    val min = Money(Long.MIN_VALUE, Moneda.USD)
    assertFailsWith<ArithmeticException> { min - Money(1L, Moneda.USD) }
  }

  @Test
  fun negatingLongMinValueFails() {
    assertFailsWith<ArithmeticException> { -Money(Long.MIN_VALUE, Moneda.USD) }
  }
}
