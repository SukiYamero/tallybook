package com.kurobello.tallybook.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.kurobello.tallybook.core.ui.component.LoadingPresentation
import com.kurobello.tallybook.core.ui.component.deferLoading
import com.kurobello.tallybook.core.ui.component.rememberLoadingPresentation
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.Flow

internal typealias HomePresentation = LoadingPresentation<HomeUiState>

internal fun Flow<HomeUiState>.deferHomeLoading(
    timeSource: TimeSource = TimeSource.Monotonic,
): Flow<HomePresentation> =
    deferLoading(
        isLoading = { it.content.isInitialLoading },
        requestKey = { it.moneda },
        timeSource = timeSource,
    )

@Composable
internal fun rememberHomePresentation(state: HomeUiState): State<HomePresentation> =
    rememberLoadingPresentation(
        state,
        isLoading = { it.content.isInitialLoading },
        requestKey = { it.moneda },
    )
