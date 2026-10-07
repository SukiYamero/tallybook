package com.kurobello.tallybook.feature.history.ui

internal data class HistoryNavigationCallbacks(
    val onAdd: () -> Unit,
    val onOpenMovimiento: ((String) -> Unit)? = null,
)
