package com.kurobello.tallybook.core.model

private const val GROUP_SIZE = 3

/**
 * Monto en unidades mínimas de su moneda (centavos para escala 2, la unidad entera para CLP).
 * Guardar el entero y no un decimal deja el `SUM()`/`GROUP BY` del lado de SQLite y evita todo
 * redondeo intermedio: las 8 monedas soportadas son de escala 0 o 2.
 */
data class Money(val minorUnits: Long, val moneda: Moneda) : Comparable<Money> {

  operator fun plus(other: Money): Money {
    requireSameMoneda(other)
    return Money(addExact(minorUnits, other.minorUnits), moneda)
  }

  operator fun minus(other: Money): Money {
    requireSameMoneda(other)
    return Money(subtractExact(minorUnits, other.minorUnits), moneda)
  }

  operator fun unaryMinus(): Money = Money(subtractExact(0L, minorUnits), moneda)

  override fun compareTo(other: Money): Int {
    requireSameMoneda(other)
    return minorUnits.compareTo(other.minorUnits)
  }

  fun format(): String {
    val digits = minorUnits.toString().removePrefix("-").padStart(moneda.scale + 1, '0')
    // Signo pegado a los dígitos, no antes del símbolo: el orden que moneta impone sobre la salida
    // de Intl en attachSignToNumber (movimientoView.ts).
    val sign = if (minorUnits < 0) "-" else ""
    val corte = digits.length - moneda.scale
    val entero = groupDigits(digits.take(corte), moneda.groupSeparator)
    val decimales = if (moneda.scale == 0) "" else moneda.decimalSeparator + digits.drop(corte)
    return moneda.symbol + moneda.symbolSeparator + sign + entero + decimales
  }

  private fun requireSameMoneda(other: Money) {
    require(moneda == other.moneda) {
      "No se pueden operar montos de monedas distintas: $moneda y ${other.moneda}"
    }
  }

  companion object {

    /**
     * Lee un decimal simple sin separador de miles (`-?\d+(\.\d{1,scale})?`). Rechaza —en vez de
     * truncar— más decimales de los que admite la moneda: perder plata en silencio al importar o al
     * parsear un formulario es peor que fallar.
     */
    fun parseOrNull(text: String, moneda: Moneda): Money? {
      val body = text.removePrefix("-")
      val entero = body.substringBefore('.')
      val decimales = body.substringAfter('.', "")
      val bienFormado =
          entero.isNotEmpty() &&
              entero.all { it in '0'..'9' } &&
              decimales.all { it in '0'..'9' } &&
              decimales.length <= moneda.scale &&
              (!body.contains('.') || decimales.isNotEmpty())
      val minorUnits =
          if (bienFormado) (entero + decimales.padEnd(moneda.scale, '0')).toLongOrNull() else null
      return minorUnits?.let { Money(if (text.startsWith('-')) -it else it, moneda) }
    }

    fun parse(text: String, moneda: Moneda): Money =
        requireNotNull(parseOrNull(text, moneda)) { "Monto inválido para $moneda: '$text'" }
  }
}

private fun overflow(): Nothing = throw ArithmeticException("Monto fuera del rango de Long")

// El bit de signo delata el wrap: si ambos operandos difieren del resultado, la suma se desbordó.
private fun addExact(a: Long, b: Long): Long =
    (a + b).also { if (((a xor it) and (b xor it)) < 0) overflow() }

private fun subtractExact(a: Long, b: Long): Long =
    (a - b).also { if (((a xor b) and (a xor it)) < 0) overflow() }

private fun groupDigits(digits: String, separator: String): String = buildString {
  digits.forEachIndexed { index, digit ->
    if (index > 0 && (digits.length - index) % GROUP_SIZE == 0) append(separator)
    append(digit)
  }
}
