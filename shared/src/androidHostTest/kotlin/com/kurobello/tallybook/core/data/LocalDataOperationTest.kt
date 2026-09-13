package com.kurobello.tallybook.core.data

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class LocalDataOperationTest {

  @Test
  fun `cancellation is rethrown instead of becoming a local error`() = runTest {
    assertFailsWith<CancellationException> {
      runLocalDataOperation(StandardTestDispatcher(testScheduler)) {
        throw CancellationException("cancelled")
      }
    }
  }
}
