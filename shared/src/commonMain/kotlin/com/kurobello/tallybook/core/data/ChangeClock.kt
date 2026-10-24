package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataOutcome

/** The single process-wide issuer of [ChangeStamp]s. */
interface ChangeClock {
  /**
   * Reads the node once and advances to [storedMax]; idempotent, never moves backward. Called at
   * startup before the first mutation and again after any dataset replacement (future Drive
   * restore). Fails without preparing when the node or the stored maximum cannot be read.
   */
  suspend fun prepare(storedMax: suspend () -> DataOutcome<ChangeStamp?>): DataOutcome<Unit>

  /**
   * Synchronous so repositories can call it inside a SQLDelight transaction. Calling it before a
   * successful [prepare] is a programming error.
   */
  fun next(): ChangeStamp
}
