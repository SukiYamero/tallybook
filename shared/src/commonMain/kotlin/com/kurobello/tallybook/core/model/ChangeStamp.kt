package com.kurobello.tallybook.core.model

import kotlin.jvm.JvmInline

/**
 * Hybrid logical clock value encoded as `<13-digit epoch millis>-<4 hex counter>-<16 hex node>`.
 * Every component is fixed-width ASCII, so text order equals (millis, counter, node) order and the
 * encoded value can be compared directly in SQL.
 */
@JvmInline
value class ChangeStamp private constructor(val value: String) : Comparable<ChangeStamp> {

  internal val epochMillis: Long
    get() = value.substring(0, MILLIS_DIGITS).toLong()

  internal val counter: Int
    get() = value.substring(COUNTER_START, COUNTER_END).toInt(HEX_RADIX)

  override fun compareTo(other: ChangeStamp): Int = value.compareTo(other.value)

  override fun toString(): String = value

  companion object {
    const val MAX_EPOCH_MILLIS: Long = 9_999_999_999_999
    const val MAX_COUNTER: Int = 0xFFFF

    private const val MILLIS_DIGITS = 13
    private const val COUNTER_DIGITS = 4
    private const val NODE_DIGITS = 16
    private const val HEX_RADIX = 16
    private const val COUNTER_START = MILLIS_DIGITS + 1
    private const val COUNTER_END = COUNTER_START + COUNTER_DIGITS

    private val FORMAT = Regex("[0-9]{13}-[0-9a-f]{4}-[0-9a-f]{16}")
    private val NODE_FORMAT = Regex("[0-9a-f]{16}")

    val SEED: ChangeStamp = of(epochMillis = 0, counter = 0, node = "0".repeat(NODE_DIGITS))

    fun of(epochMillis: Long, counter: Int, node: String): ChangeStamp {
      require(epochMillis in 0..MAX_EPOCH_MILLIS) { "Stamp millis out of range: $epochMillis" }
      require(counter in 0..MAX_COUNTER) { "Stamp counter out of range: $counter" }
      require(NODE_FORMAT.matches(node)) { "Stamp node must be $NODE_DIGITS lowercase hex chars" }
      val millisText = epochMillis.toString().padStart(MILLIS_DIGITS, '0')
      val counterText = counter.toString(HEX_RADIX).padStart(COUNTER_DIGITS, '0')
      return ChangeStamp("$millisText-$counterText-$node")
    }

    fun parse(value: String): ChangeStamp {
      require(FORMAT.matches(value)) { "Malformed change stamp: '$value'" }
      return ChangeStamp(value)
    }
  }
}
