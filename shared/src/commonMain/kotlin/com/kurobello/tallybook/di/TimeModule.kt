package com.kurobello.tallybook.di

import com.kurobello.tallybook.core.data.IdGenerator
import com.kurobello.tallybook.core.data.TimeZoneProvider
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import org.koin.dsl.module

val timeModule = module {
  single<Clock> { Clock.System }
  single<TimeZoneProvider> { TimeZoneProvider { TimeZone.currentSystemDefault() } }
  single<IdGenerator> { IdGenerator { Uuid.random().toString() } }
}
