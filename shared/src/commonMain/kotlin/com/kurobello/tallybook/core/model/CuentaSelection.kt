package com.kurobello.tallybook.core.model

/** A resolved medio and cuenta; [moneda] is the cuenta's, so the draft's amount can use it. */
data class CuentaSelection(val cuentaId: String, val medio: MedioPago, val moneda: Moneda)
