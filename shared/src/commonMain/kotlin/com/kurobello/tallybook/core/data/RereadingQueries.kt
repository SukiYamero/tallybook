package com.kurobello.tallybook.core.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

// Queries are built inside the flow so a storage failure while building them is a Local outcome.
internal fun <T> localSnapshots(
    dispatcher: CoroutineDispatcher,
    source: suspend () -> Flow<T>,
): Flow<DataOutcome<T>> = flow { emitAll(source()) }.flowOn(dispatcher).asLocalDataOutcome()

/**
 * Runs [read] once and again whenever a table behind [queries] changes. A newer change cancels the
 * read in flight, which checks cancellation while mapping, and a read finishing after it was
 * superseded or its collector left never publishes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> rereading(
    dispatcher: CoroutineDispatcher,
    vararg queries: Query<*>,
    read: suspend () -> T,
): Flow<T> =
    combine(queries.map { it.asFlow() }) {}
        .mapLatest {
          // mapLatest starts this block undispatched, and a blocking read never suspends: without
          // yielding first, the next change could not arrive to cancel it.
          yield()
          withContext(dispatcher) { read().also { currentCoroutineContext().ensureActive() } }
        }
