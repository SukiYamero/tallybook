package com.kurobello.tallybook.feature.movimientos.ui

internal data class MovimientoCategoryEntryPolicy(
    val startWithCategory: Boolean = false,
    val focusAmountAfterSelection: Boolean = true,
    val interactionEnabled: Boolean = true,
)
