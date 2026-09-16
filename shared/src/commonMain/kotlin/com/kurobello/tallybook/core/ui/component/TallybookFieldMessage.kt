package com.kurobello.tallybook.core.ui.component

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface TallybookFieldMessage {
  val text: String

  @Immutable data class Supporting(override val text: String) : TallybookFieldMessage

  @Immutable data class Error(override val text: String) : TallybookFieldMessage
}
