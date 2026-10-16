package com.kurobello.tallybook.feature.movimientos.ui

import com.kurobello.tallybook.core.model.Moneda
import com.kurobello.tallybook.core.model.Money

// Every Moneda needs an entry: phraseSampleAmount() reads this table with getValue.
@Suppress("MagicNumber")
internal val PHRASE_SAMPLE_MINOR_UNITS: Map<Moneda, Long> =
    mapOf(
        Moneda.COP to 2_000_000L,
        Moneda.MXN to 20_000L,
        Moneda.ARS to 500_000L,
        Moneda.CLP to 5_000L,
        Moneda.BRL to 3_000L,
        Moneda.USD to 1_250L,
        Moneda.DOP to 50_000L,
        Moneda.PEN to 2_500L,
    )

internal fun phraseSampleAmount(moneda: Moneda): String =
    Money(PHRASE_SAMPLE_MINOR_UNITS.getValue(moneda), moneda).format()
