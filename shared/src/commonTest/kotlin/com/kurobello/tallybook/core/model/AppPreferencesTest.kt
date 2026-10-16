package com.kurobello.tallybook.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class AppPreferencesTest {

  @Test
  fun primaryMonedaReadsReadyPreferences() {
    val ready: DataOutcome<AppPreferencesState> =
        DataOutcome.Success(AppPreferencesState.Ready(AppPreferences(Moneda.ARS)))
    assertEquals(DataOutcome.Success(Moneda.ARS), ready.primaryMoneda())
  }

  @Test
  fun primaryMonedaTreatsUninitializedPreferencesAsAFailure() {
    val uninitialized: DataOutcome<AppPreferencesState> =
        DataOutcome.Success(AppPreferencesState.Uninitialized)
    val failure = assertIs<DataOutcome.Failure>(uninitialized.primaryMoneda())
    assertIs<DataError.InvalidInput>(failure.error)
  }

  @Test
  fun primaryMonedaPropagatesAReadFailure() {
    val failure = DataOutcome.Failure(DataError.Unknown(RuntimeException("boom")))
    assertSame(failure, failure.primaryMoneda())
  }
}
