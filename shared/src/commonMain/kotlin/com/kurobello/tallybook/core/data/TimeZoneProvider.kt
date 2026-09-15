package com.kurobello.tallybook.core.data

import kotlinx.datetime.TimeZone

fun interface TimeZoneProvider {
  fun current(): TimeZone
}
