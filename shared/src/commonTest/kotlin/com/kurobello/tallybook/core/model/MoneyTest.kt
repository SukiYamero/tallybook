package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun usd(raw: String) = Money.parse(raw, Moneda.USD)

class MoneyTest {

  @Test
  fun sumaMontosDeLaMismaMoneda() {
    assertEquals(usd("15.75"), usd("10.50") + usd("5.25"))
  }

  @Test
  fun sumarMonedasDistintasFalla() {
    val cop = Money.parse("10", Moneda.COP)
    assertFailsWith<IllegalArgumentException> { usd("10") + cop }
  }

  @Test
  fun restaMontosDeLaMismaMoneda() {
    assertEquals(usd("5.25"), usd("10.50") - usd("5.25"))
  }

  @Test
  fun restarMonedasDistintasFalla() {
    val cop = Money.parse("10", Moneda.COP)
    assertFailsWith<IllegalArgumentException> { usd("10") - cop }
  }

  @Test
  fun negarInvierteElSignoYConservaLaMoneda() {
    val negado = -usd("10.50")
    assertEquals(-1050L, negado.minorUnits)
    assertEquals(Moneda.USD, negado.moneda)
  }

  @Test
  fun comparaMontosDeLaMismaMoneda() {
    assertTrue(usd("10.50") > usd("5.25"))
    assertEquals(0, usd("10.50").compareTo(usd("10.5")))
  }

  @Test
  fun compararMonedasDistintasFalla() {
    val cop = Money.parse("10", Moneda.COP)
    assertFailsWith<IllegalArgumentException> { usd("10") > cop }
  }

  @Test
  fun laIgualdadIgnoraLosCerosNoSignificativos() {
    assertEquals(usd("10.5"), usd("10.50"))
    assertEquals(usd("10.5").hashCode(), usd("10.50").hashCode())
  }

  @Test
  fun parseNormalizaAUnidadesMinimas() {
    assertEquals(1050L, usd("10.50").minorUnits)
    assertEquals(1050L, usd("10.5").minorUnits)
    assertEquals(1000L, usd("10").minorUnits)
    assertEquals(-1050L, usd("-10.50").minorUnits)
    assertEquals(5L, usd("0.05").minorUnits)
    assertEquals(1234567L, Money.parse("1234567", Moneda.CLP).minorUnits)
  }

  @Test
  fun parseRechazaMasDecimalesQueLaEscalaDeLaMoneda() {
    assertNull(Money.parseOrNull("0.994", Moneda.USD))
    assertNull(Money.parseOrNull("5.5", Moneda.CLP))
    assertFailsWith<IllegalArgumentException> { usd("0.994") }
  }

  @Test
  fun parseRechazaTextoQueNoEsUnDecimalSimple() {
    val invalidos = listOf("", "-", "abc", "1.2.3", "1,50", " 10", "10.", "+10", "1e3")
    invalidos.forEach { assertNull(Money.parseOrNull(it, Moneda.USD), "debería rechazar '$it'") }
  }

  @Test
  fun formateaCadaMonedaConSuSimboloYSeparadores() {
    val esperado =
        mapOf(
            Moneda.COP to "$\u00A01.234.567,89",
            Moneda.MXN to "$1,234,567.89",
            Moneda.ARS to "$\u00A01.234.567,89",
            Moneda.BRL to "R$\u00A01.234.567,89",
            Moneda.USD to "$1,234,567.89",
            Moneda.DOP to "$1,234,567.89",
            Moneda.PEN to "S/\u00A01,234,567.89",
        )
    esperado.forEach { (moneda, texto) ->
      assertEquals(texto, Money.parse("1234567.89", moneda).format())
    }
  }

  @Test
  fun formateaSinDecimalesLasMonedasDeEscalaCero() {
    assertEquals("$1.234.568", Money.parse("1234568", Moneda.CLP).format())
    assertEquals("$6", Money.parse("6", Moneda.CLP).format())
  }

  @Test
  fun elSignoNegativoVaEntreElSimboloYLosDigitos() {
    assertEquals("$\u00A0-1.234.567,89", Money.parse("-1234567.89", Moneda.COP).format())
    assertEquals("$-1,234,567.89", usd("-1234567.89").format())
  }

  @Test
  fun rellenaLosDecimalesHastaLaEscalaDeLaMoneda() {
    assertEquals("$0.00", usd("0").format())
    assertEquals("$5.50", usd("5.5").format())
    assertEquals("$0.05", usd("0.05").format())
  }

  @Test
  fun sumarMasAllaDelRangoDeLongFalla() {
    val tope = Money(Long.MAX_VALUE, Moneda.USD)
    assertFailsWith<ArithmeticException> { tope + Money(1L, Moneda.USD) }
  }

  @Test
  fun restarMasAllaDelRangoDeLongFalla() {
    val piso = Money(Long.MIN_VALUE, Moneda.USD)
    assertFailsWith<ArithmeticException> { piso - Money(1L, Moneda.USD) }
  }

  @Test
  fun negarElMinimoDeLongFalla() {
    assertFailsWith<ArithmeticException> { -Money(Long.MIN_VALUE, Moneda.USD) }
  }
}
