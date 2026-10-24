package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChangeStampTest {

  @Test
  fun ofPadsMillisToThirteenDigitsAndCounterToFourHex() {
    assertEquals(
        "0000000000042-000a-0123456789abcdef",
        ChangeStamp.of(epochMillis = 42, counter = 10, node = NODE).value,
    )
    assertEquals(
        "1730000000000-ffff-0123456789abcdef",
        ChangeStamp.of(epochMillis = 1_730_000_000_000, counter = 0xFFFF, node = NODE).value,
    )
  }

  @Test
  fun ofRejectsComponentsThatCannotBeEncoded() {
    listOf(
            { ChangeStamp.of(epochMillis = -1, counter = 0, node = NODE) },
            { ChangeStamp.of(epochMillis = 10_000_000_000_000, counter = 0, node = NODE) },
            { ChangeStamp.of(epochMillis = 0, counter = -1, node = NODE) },
            { ChangeStamp.of(epochMillis = 0, counter = 0x10000, node = NODE) },
            { ChangeStamp.of(epochMillis = 0, counter = 0, node = "0123456789ABCDEF") },
            { ChangeStamp.of(epochMillis = 0, counter = 0, node = "0123456789abcde") },
        )
        .forEach { build -> assertFailsWith<IllegalArgumentException> { build() } }
  }

  @Test
  fun parseRoundTripsAWellFormedStamp() {
    val stamp = ChangeStamp.of(epochMillis = 1_730_000_000_123, counter = 7, node = NODE)

    assertEquals(stamp, ChangeStamp.parse(stamp.value))
  }

  @Test
  fun parseRejectsAnythingOffFormat() {
    listOf(
            "",
            "1730000000000-0000-0123456789abcde",
            "173000000000-0000-0123456789abcdef",
            "17300000000000-0000-0123456789abcdef",
            "1730000000000-000-0123456789abcdef",
            "1730000000000-0000-0123456789ABCDEF",
            "1730000000000-000A-0123456789abcdef",
            "1730000000000_0000_0123456789abcdef",
            "1730000000000-0000-0123456789abcdeg",
            "17300000000x0-0000-0123456789abcdef",
            " 1730000000000-0000-0123456789abcdef",
        )
        .forEach { value ->
          assertFailsWith<IllegalArgumentException>(value) { ChangeStamp.parse(value) }
        }
  }

  @Test
  fun textOrderEqualsMillisThenCounterThenNodeOrder() {
    val ordered =
        listOf(
            ChangeStamp.of(epochMillis = 9, counter = 0xFFFF, node = "ffffffffffffffff"),
            ChangeStamp.of(epochMillis = 10, counter = 0, node = "ffffffffffffffff"),
            ChangeStamp.of(epochMillis = 10, counter = 1, node = "0000000000000000"),
            ChangeStamp.of(epochMillis = 10, counter = 1, node = "0000000000000001"),
            ChangeStamp.of(epochMillis = 1_730_000_000_000, counter = 0, node = NODE),
        )

    assertEquals(ordered, ordered.reversed().sorted())
    assertEquals(ordered.map { it.value }, ordered.map { it.value }.reversed().sorted())
  }

  @Test
  fun seedIsTheMinimumStamp() {
    assertEquals("0000000000000-0000-0000000000000000", ChangeStamp.SEED.value)
    assertEquals(ChangeStamp.SEED, ChangeStamp.of(epochMillis = 0, counter = 0, node = ZERO_NODE))
    assertTrue(ChangeStamp.SEED < ChangeStamp.of(epochMillis = 0, counter = 0, node = NODE))
  }

  private companion object {
    const val NODE = "0123456789abcdef"
    const val ZERO_NODE = "0000000000000000"
  }
}
