package com.kurobello.tallybook.core.data

import com.kurobello.tallybook.core.database.TallybookDatabase
import com.kurobello.tallybook.core.model.ChangeStamp
import com.kurobello.tallybook.core.model.DataOutcome
import kotlinx.coroutines.CoroutineDispatcher

internal suspend fun TallybookDatabase.maxStoredChangeStamp(
    dispatcher: CoroutineDispatcher
): DataOutcome<ChangeStamp?> =
    runLocalDataOperation(dispatcher) {
      changeStampQueries.selectMaxChangeStamp().executeAsOne().max_stamp?.let(ChangeStamp::parse)
    }
