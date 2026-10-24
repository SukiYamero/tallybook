package com.kurobello.tallybook.feature.transfers.ui

import com.kurobello.tallybook.feature.transfers.viewmodel.TransferEntry
import com.kurobello.tallybook.feature.transfers.viewmodel.TransferPurpose
import kotlin.test.Test
import kotlin.test.assertEquals
import tallybook.shared.generated.resources.Res
import tallybook.shared.generated.resources.account_pay_card
import tallybook.shared.generated.resources.transfer_edit_title
import tallybook.shared.generated.resources.transfer_new_title

class TransferSheetTitleTest {
  @Test
  fun payCardKeepsItsOwnTitle() {
    assertEquals(
        Res.string.account_pay_card,
        TransferEntry.New(null, "nu", null, TransferPurpose.PayCard).titleResource(),
    )
    assertEquals(Res.string.transfer_new_title, TransferEntry.New(null, null, null).titleResource())
    assertEquals(Res.string.transfer_edit_title, TransferEntry.Existing("move").titleResource())
  }
}
