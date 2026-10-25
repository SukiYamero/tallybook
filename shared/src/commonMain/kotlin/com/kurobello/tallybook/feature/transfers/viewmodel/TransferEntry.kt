package com.kurobello.tallybook.feature.transfers.viewmodel

import com.kurobello.tallybook.core.model.Money

internal enum class TransferPurpose {
  Move,
  PayCard,
}

internal sealed interface TransferEntry {
  /**
   * A transfer to create. A null [origenId] takes the repository's default origin, as "Pay card"
   * does; [prefill] is the destination-side amount, the card's estimated debt.
   */
  data class New(
      val origenId: String?,
      val destinoId: String?,
      val prefill: Money?,
      val purpose: TransferPurpose = TransferPurpose.Move,
  ) : TransferEntry

  data class Existing(val id: String) : TransferEntry
}
