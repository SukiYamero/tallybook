package com.kurobello.tallybook.feature.search.viewmodel

import com.kurobello.tallybook.core.data.TimeZoneProvider
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

internal class SearchDateProvider(
    private val clock: Clock,
    private val timeZoneProvider: TimeZoneProvider,
) {
  fun today(): LocalDate {
    val now = clock.now()
    val zone = timeZoneProvider.current()
    return now.toLocalDateTime(zone).date
  }
}
