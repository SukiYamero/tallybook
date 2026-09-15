package com.kurobello.tallybook.core.data

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class FlowDataOperationTest {

  @Test
  fun `cancellation is rethrown instead of becoming a local error`() = runTest {
    assertFailsWith<CancellationException> {
      flow<Int> { throw CancellationException("cancelled") }.asLocalDataOutcome().toList()
    }
  }
}
