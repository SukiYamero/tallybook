package com.kurobello.tallybook.core.data

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/**
 * Read on every call, so a screen resumed after midnight or a time-zone change sees the new day.
 */
internal class CivilToday(
    private val clock: Clock,
    private val timeZoneProvider: TimeZoneProvider,
) {
  fun date(): LocalDate = clock.now().toLocalDateTime(timeZoneProvider.current()).date
}
