package com.kurobello.tallybook.core.ui.component

import com.kurobello.tallybook.core.text.AmountInputError
import kotlin.test.Test
import kotlin.test.assertEquals

class AmountInputErrorMessagesTest {
  @Test
  fun everyAmountErrorHasItsOwnMessage() {
    assertEquals(AmountInputError.entries.toSet(), amountInputErrorMessages.keys)
    assertEquals(
        amountInputErrorMessages.size,
        amountInputErrorMessages.values.toSet().size,
    )
  }
}
