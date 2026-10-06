package com.kurobello.tallybook.feature.history.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.ui.component.LoadingPresentation
import com.kurobello.tallybook.core.ui.component.deferLoading
import com.kurobello.tallybook.core.ui.component.rememberLoadingPresentation
import com.kurobello.tallybook.feature.history.viewmodel.HistorySelection
import com.kurobello.tallybook.feature.history.viewmodel.HistoryUiState
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

internal typealias HistoryPresentation = LoadingPresentation<HistoryUiState>

internal fun Flow<HistoryUiState>.deferHistoryLoading(
    timeSource: TimeSource = TimeSource.Monotonic,
): Flow<HistoryPresentation> =
    deferLoading(
        isLoading = { it.period.isInitialLoading },
        requestKey = { PendingHistoryRead(it.selection, it.today, it.moneda) },
        timeSource = timeSource,
    )

private data class PendingHistoryRead(
    val selection: HistorySelection,
    val today: LocalDate,
    val moneda: Moneda?,
)

@Composable
internal fun rememberHistoryPresentation(state: HistoryUiState): State<HistoryPresentation> =
    rememberLoadingPresentation(
        state,
        isLoading = { it.period.isInitialLoading },
        requestKey = { PendingHistoryRead(it.selection, it.today, it.moneda) },
    )
