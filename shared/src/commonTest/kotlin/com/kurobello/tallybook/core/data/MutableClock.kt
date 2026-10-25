package com.kurobello.tallybook.core.data

import kotlin.time.Clock
import kotlin.time.Instant

internal class MutableClock(var now: Instant) : Clock {
  override fun now(): Instant = now
}
