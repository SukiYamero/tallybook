package com.kurobello.tallybook.core.model

/** Currencies with a live account, archived included, in [Moneda] order. */
fun List<Cuenta>.monedas(): List<Moneda> = map { it.moneda }.distinct().sorted()

/**
 * The currency History and Search show: the user's [selected] one while it still has accounts, else
 * [primary]. Until [available] is known a restored selection is trusted, so it never flickers
 * through the primary currency.
 */
fun viewedMoneda(primary: Moneda, selected: Moneda?, available: List<Moneda>?): Moneda =
    selected?.takeIf { available == null || it in available } ?: primary
