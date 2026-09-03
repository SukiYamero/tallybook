package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DataOutcomeTest {

  @Test
  fun foldEjecutaRamaCorrectaEnSuccess() {
    val outcome: DataOutcome<Int> = DataOutcome.Success(5)
    assertEquals(10, outcome.fold({ it * 2 }, { -1 }))
  }

  @Test
  fun foldEjecutaRamaCorrectaEnFailure() {
    val error = DataError.Unknown(RuntimeException("boom"))
    val outcome: DataOutcome<Int> = DataOutcome.Failure(error)
    assertEquals(-1, outcome.fold({ it * 2 }, { -1 }))
  }

  @Test
  fun mapTransformaElDatoEnSuccess() {
    val outcome: DataOutcome<Int> = DataOutcome.Success(5)
    assertEquals(DataOutcome.Success("5"), outcome.map { it.toString() })
  }

  @Test
  fun mapPropagaElErrorSinTocarlo() {
    val failure = DataOutcome.Failure(DataError.Unknown(RuntimeException("boom")))
    assertSame(failure, failure.map { it })
  }

  @Test
  fun elErrorLocalConservaLaCausa() {
    val causa = IllegalStateException("db cerrada")
    assertSame(causa, DataError.Local(causa).cause)
  }
}
