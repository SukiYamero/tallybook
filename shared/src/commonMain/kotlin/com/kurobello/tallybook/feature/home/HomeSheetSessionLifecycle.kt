package com.kurobello.tallybook.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
internal fun HomeSheetSessionLifecycle(sheetSession: AddMovimientoSheetSession) {
  DisposableEffect(sheetSession) { onDispose { sheetSession.dispose() } }
}
